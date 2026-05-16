import Hls from 'hls.js'
import { getTidalAudioElement, getTidalHlsInstance } from '@/lib/tidalStreamPlayback'

// Console-only probe for validating the new Visual EQ signal path.
// It does not drive the UI. Use DevTools:
//
//   window.__visualizerProbe.start()
//   // start TIDAL playback
//   window.__visualizerProbe.status()
//   await window.__visualizerProbe.decodeLatest()
//
// Goal: prove whether HLS.js exposes enough fMP4 bytes to reconstruct and
// decode TIDAL audio without using HTMLMediaElement.captureStream().

type SegmentType = 'audio' | 'video' | 'audiovideo' | string
type SegmentKind = 'init' | 'media' | 'unknown'

interface CodecRecord {
    type: SegmentType
    container: string | null
    codec: string | null
    levelCodec: string | null
    initByteLength: number
    initHead: string | null
    initSegment: Uint8Array | null
    arrivedAt: number
}

interface SegmentRecord {
    id: number
    type: SegmentType
    kind: SegmentKind
    byteLength: number
    startPts: number | null
    endPts: number | null
    start: number | null
    duration: number | null
    offset: number | null
    boxes: string[]
    head: string
    payload: Uint8Array
    arrivedAt: number
}

interface HlsTrackLike {
    container?: string
    codec?: string
    levelCodec?: string
    initSegment?: Uint8Array
}

interface HlsBufferCodecsDataLike {
    audio?: HlsTrackLike
    video?: HlsTrackLike
    audiovideo?: HlsTrackLike
}

interface HlsFragLike {
    startPTS?: number
    endPTS?: number
    start?: number
    duration?: number
    sn?: number | 'initSegment'
}

interface HlsBufferAppendingDataLike {
    type: SegmentType
    data: Uint8Array
    offset?: number
    frag?: HlsFragLike
}

const HEAD_BYTES = 32
const MAX_KEEP = 48

let counter = 0
let detach: (() => void) | null = null
let attachTimerId: number | null = null
const records: SegmentRecord[] = []
const codecs = new Map<SegmentType, CodecRecord>()

const toHex = (bytes: Uint8Array, count: number) =>
    Array.from(bytes.slice(0, count))
        .map((b) => b.toString(16).padStart(2, '0'))
        .join(' ')

const readAscii = (bytes: Uint8Array, start: number, length: number) =>
    Array.from(bytes.slice(start, start + length))
        .map((byte) => String.fromCharCode(byte))
        .join('')

const readUint32 = (bytes: Uint8Array, offset: number) => {
    if (offset + 4 > bytes.length) {
        return null
    }
    return (
        bytes[offset] * 0x1000000 +
        bytes[offset + 1] * 0x10000 +
        bytes[offset + 2] * 0x100 +
        bytes[offset + 3]
    )
}

const readTopLevelBoxes = (bytes: Uint8Array, maxBoxes = 8) => {
    const boxes: string[] = []
    let offset = 0

    while (offset + 8 <= bytes.length && boxes.length < maxBoxes) {
        const size = readUint32(bytes, offset)
        const type = readAscii(bytes, offset + 4, 4)
        if (!size || size < 8 || !/^[\x20-\x7e]{4}$/.test(type)) {
            break
        }
        boxes.push(type)
        offset += size
    }

    return boxes
}

const classifySegment = (data: Uint8Array, frag?: HlsFragLike): SegmentKind => {
    if (frag?.sn === 'initSegment') {
        return 'init'
    }
    const boxes = readTopLevelBoxes(data, 4)
    if (boxes.includes('ftyp') || boxes.includes('moov')) {
        return 'init'
    }
    if (boxes.includes('moof') || boxes.includes('mdat')) {
        return 'media'
    }
    return 'unknown'
}

const cloneBytes = (bytes?: Uint8Array | null) =>
    bytes ? bytes.slice() : null

const summarizeCodec = (record: CodecRecord) => ({
    type: record.type,
    container: record.container,
    codec: record.codec,
    levelCodec: record.levelCodec,
    initBytes: record.initByteLength,
    initHead: record.initHead,
})

const summarizeSegment = (record: SegmentRecord) => ({
    id: record.id,
    type: record.type,
    kind: record.kind,
    bytes: record.byteLength,
    startPts: record.startPts,
    endPts: record.endPts,
    start: record.start,
    duration: record.duration,
    offset: record.offset,
    boxes: record.boxes,
    head: record.head,
})

const pickInitSegment = (type: SegmentType) =>
    codecs.get(type)?.initSegment ??
    (type !== 'audio' ? codecs.get('audio')?.initSegment : null) ??
    (type !== 'audiovideo' ? codecs.get('audiovideo')?.initSegment : null) ??
    null

const concatBytes = (chunks: Uint8Array[]) => {
    const total = chunks.reduce((sum, chunk) => sum + chunk.byteLength, 0)
    const merged = new Uint8Array(total)
    let offset = 0
    for (const chunk of chunks) {
        merged.set(chunk, offset)
        offset += chunk.byteLength
    }
    return merged
}

const toArrayBuffer = (bytes: Uint8Array) =>
    bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength)

const recordCodec = (type: SegmentType, track?: HlsTrackLike) => {
    if (!track) {
        return
    }
    const initSegment = cloneBytes(track.initSegment)
    const record: CodecRecord = {
        type,
        container: track.container ?? null,
        codec: track.codec ?? null,
        levelCodec: track.levelCodec ?? null,
        initByteLength: initSegment?.byteLength ?? 0,
        initHead: initSegment ? toHex(initSegment, HEAD_BYTES) : null,
        initSegment,
        arrivedAt: performance.now(),
    }
    codecs.set(type, record)
    // eslint-disable-next-line no-console
    console.log('[probe] BUFFER_CODECS', summarizeCodec(record))
}

const recordSegment = (data: HlsBufferAppendingDataLike) => {
    const payload = data.data.slice()
    const boxes = readTopLevelBoxes(payload)
    const id = ++counter
    const record: SegmentRecord = {
        id,
        type: data.type,
        kind: classifySegment(payload, data.frag),
        byteLength: payload.byteLength,
        startPts: data.frag?.startPTS ?? null,
        endPts: data.frag?.endPTS ?? null,
        start: data.frag?.start ?? null,
        duration: data.frag?.duration ?? null,
        offset: data.offset ?? null,
        boxes,
        head: toHex(payload, HEAD_BYTES),
        payload,
        arrivedAt: performance.now(),
    }

    records.push(record)
    if (records.length > MAX_KEEP) {
        records.shift()
    }
    // eslint-disable-next-line no-console
    console.log('[probe] BUFFER_APPENDING', summarizeSegment(record))
}

const tryAttach = (): boolean => {
    const hls = getTidalHlsInstance()
    if (!hls) {
        return false
    }

    const onCodecs = (_evt: typeof Hls.Events.BUFFER_CODECS, data: HlsBufferCodecsDataLike) => {
        recordCodec('audio', data.audio)
        recordCodec('audiovideo', data.audiovideo)
        recordCodec('video', data.video)
    }

    const onAppending = (_evt: typeof Hls.Events.BUFFER_APPENDING, data: HlsBufferAppendingDataLike) => {
        if (data.type === 'video') {
            return
        }
        recordSegment(data)
    }

    hls.on(Hls.Events.BUFFER_CODECS, onCodecs)
    hls.on(Hls.Events.BUFFER_APPENDING, onAppending)
    detach = () => {
        hls.off(Hls.Events.BUFFER_CODECS, onCodecs)
        hls.off(Hls.Events.BUFFER_APPENDING, onAppending)
    }
    return true
}

const start = () => {
    if (detach) {
        // eslint-disable-next-line no-console
        console.log('[probe] already attached')
        return
    }
    if (tryAttach()) {
        // eslint-disable-next-line no-console
        console.log('[probe] attached to live HLS instance, listening for BUFFER_CODECS and BUFFER_APPENDING')
        return
    }
    // eslint-disable-next-line no-console
    console.log('[probe] no HLS instance yet; polling every 500ms until TIDAL playback starts')
    attachTimerId = window.setInterval(() => {
        if (tryAttach()) {
            if (attachTimerId !== null) {
                window.clearInterval(attachTimerId)
                attachTimerId = null
            }
            // eslint-disable-next-line no-console
            console.log('[probe] attached')
        }
    }, 500)
}

const stop = () => {
    if (attachTimerId !== null) {
        window.clearInterval(attachTimerId)
        attachTimerId = null
    }
    detach?.()
    detach = null
    // eslint-disable-next-line no-console
    console.log('[probe] detached')
}

const reset = () => {
    records.splice(0, records.length)
    codecs.clear()
    counter = 0
    // eslint-disable-next-line no-console
    console.log('[probe] cleared captured codecs and segments')
}

const list = () => records.map(summarizeSegment)

const listCodecs = () => Array.from(codecs.values()).map(summarizeCodec)

const status = () => ({
    attached: Boolean(detach),
    codecCount: codecs.size,
    segmentCount: records.length,
    codecs: listCodecs(),
    recentSegments: list().slice(-8),
})

const get = (id: number) => records.find((record) => record.id === id) ?? null

const findDecodeTargets = (which?: number | string) => {
    if (typeof which === 'number') {
        return records.filter((record) => record.id === which)
    }
    if (typeof which === 'string') {
        return records.filter((record) => record.type === which && record.kind !== 'init').slice(-1)
    }
    return records
        .filter((record) => (record.type === 'audio' || record.type === 'audiovideo') && record.kind !== 'init')
        .slice(-1)
}

const decode = async (which?: number | string) => {
    const target = findDecodeTargets(which)
    if (target.length === 0) {
        // eslint-disable-next-line no-console
        console.log('[probe] no media segment matched', { which })
        return null
    }

    const initSegment = pickInitSegment(target[0].type)
    if (!initSegment) {
        // eslint-disable-next-line no-console
        console.log('[probe] no init segment captured for media segment', summarizeSegment(target[0]))
        return null
    }

    const merged = concatBytes([initSegment, ...target.map((record) => record.payload)])
    const ctx = new AudioContext()
    try {
        const buffer = await ctx.decodeAudioData(toArrayBuffer(merged))
        const result = {
            ok: true,
            source: target.map(summarizeSegment),
            initBytes: initSegment.byteLength,
            mergedBytes: merged.byteLength,
            sampleRate: buffer.sampleRate,
            channels: buffer.numberOfChannels,
            frames: buffer.length,
            duration: buffer.duration,
            firstSamples: Array.from(buffer.getChannelData(0).slice(0, 16)),
        }
        // eslint-disable-next-line no-console
        console.log('[probe] decodeAudioData OK', result)
        return result
    } catch (error) {
        const result = {
            ok: false,
            source: target.map(summarizeSegment),
            initBytes: initSegment.byteLength,
            mergedBytes: merged.byteLength,
            error,
        }
        // eslint-disable-next-line no-console
        console.log('[probe] decodeAudioData FAILED', result)
        return result
    } finally {
        void ctx.close().catch(() => undefined)
    }
}

const decodeLatest = () => decode()

// Direct-stream path: TIDAL HIGH quality can return a direct MP4 URL instead
// of HLS. MSE intercept cannot see that. This probes whether browser fetch can
// read and decode the direct URL, which may fail because of CORS.
const probeDirect = async () => {
    const audio = getTidalAudioElement()
    if (!audio) {
        // eslint-disable-next-line no-console
        console.log('[probe] no tidal audio element yet; start playback first')
        return null
    }
    const url = audio.currentSrc || audio.src
    if (!url) {
        // eslint-disable-next-line no-console
        console.log('[probe] audio element has no src yet')
        return null
    }
    // eslint-disable-next-line no-console
    console.log('[probe] currentSrc', url)
    let resp: Response
    try {
        resp = await fetch(url, { method: 'GET', mode: 'cors' })
    } catch (error) {
        // eslint-disable-next-line no-console
        console.log('[probe] fetch threw, likely CORS blocked', error)
        return { ok: false, error }
    }
    // eslint-disable-next-line no-console
    console.log('[probe] fetch response', {
        status: resp.status,
        type: resp.type,
        headers: Object.fromEntries(resp.headers.entries()),
    })
    if (!resp.ok) {
        return { ok: false, status: resp.status, type: resp.type }
    }
    const buf = await resp.arrayBuffer()
    // eslint-disable-next-line no-console
    console.log('[probe] body size', buf.byteLength)
    const ctx = new AudioContext()
    try {
        const decoded = await ctx.decodeAudioData(buf.slice(0))
        const result = {
            ok: true,
            sampleRate: decoded.sampleRate,
            channels: decoded.numberOfChannels,
            frames: decoded.length,
            duration: decoded.duration,
            firstSamples: Array.from(decoded.getChannelData(0).slice(0, 16)),
        }
        // eslint-disable-next-line no-console
        console.log('[probe] decodeAudioData OK', result)
        return result
    } catch (error) {
        // eslint-disable-next-line no-console
        console.log('[probe] decodeAudioData FAILED', error)
        return { ok: false, error }
    } finally {
        void ctx.close().catch(() => undefined)
    }
}

if (typeof window !== 'undefined') {
    ;(window as Window & { __visualizerProbe?: unknown }).__visualizerProbe = {
        start,
        stop,
        reset,
        status,
        list,
        listCodecs,
        decode,
        decodeLatest,
        get,
        probeDirect,
    }
}

export {}
