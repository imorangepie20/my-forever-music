export interface DecodedPcmSegment {
    sampleRate: number
    samples: Float32Array
    duration: number
}

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

const createDecodeContext = () => {
    const ctor = window.AudioContext ?? (window as Window & { webkitAudioContext?: typeof AudioContext }).webkitAudioContext
    if (!ctor) {
        throw new Error('AudioContext is not available for segment decoding.')
    }
    return new ctor()
}

export const decodeFragmentedMp4Segment = async (
    initSegment: Uint8Array,
    mediaSegment: Uint8Array,
): Promise<DecodedPcmSegment> => {
    const ctx = createDecodeContext()
    try {
        const decoded = await ctx.decodeAudioData(toArrayBuffer(concatBytes([initSegment, mediaSegment])))
        const channelCount = Math.max(1, decoded.numberOfChannels)
        const samples = new Float32Array(decoded.length)

        for (let channel = 0; channel < channelCount; channel += 1) {
            const data = decoded.getChannelData(channel)
            for (let i = 0; i < data.length; i += 1) {
                samples[i] += data[i] / channelCount
            }
        }

        return {
            sampleRate: decoded.sampleRate,
            samples,
            duration: decoded.duration,
        }
    } finally {
        void ctx.close().catch(() => undefined)
    }
}

export const decodeCompleteAudioData = async (data: ArrayBuffer): Promise<DecodedPcmSegment> => {
    const ctx = createDecodeContext()
    try {
        const decoded = await ctx.decodeAudioData(data.slice(0))
        const channelCount = Math.max(1, decoded.numberOfChannels)
        const samples = new Float32Array(decoded.length)

        for (let channel = 0; channel < channelCount; channel += 1) {
            const channelData = decoded.getChannelData(channel)
            for (let i = 0; i < channelData.length; i += 1) {
                samples[i] += channelData[i] / channelCount
            }
        }

        return {
            sampleRate: decoded.sampleRate,
            samples,
            duration: decoded.duration,
        }
    } finally {
        void ctx.close().catch(() => undefined)
    }
}
