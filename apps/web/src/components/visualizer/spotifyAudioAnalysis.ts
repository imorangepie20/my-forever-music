/**
 * Spotify audio-analysis JSON 의 최소 형식 + Visualizer 용 순수 변환 함수들.
 *
 * 외부 API 호출(token, fetch)은 hook 쪽에서 처리하고, 이 파일은 정적으로 가져온
 * analysis 객체와 현재 재생 위치(ms)만 받아 bar 배열을 만든다. 그래야 jest/vitest
 * 없이도 손쉽게 검증 가능하고, hook 은 transport 만 책임진다.
 */

export interface SpotifyAudioSegment {
    start: number
    duration: number
    confidence: number
    loudness_start: number
    loudness_max: number
    loudness_max_time: number
    loudness_end?: number
    pitches: number[]
    timbre?: number[]
}

export interface SpotifyAudioBeat {
    start: number
    duration: number
    confidence: number
}

export interface SpotifyAudioAnalysis {
    segments: SpotifyAudioSegment[]
    beats: SpotifyAudioBeat[]
}

const PITCH_CLASSES = 12

/**
 * positionSeconds 시점에 활성화된 segment 의 index 를 binary search 로 찾는다.
 * 마지막 segment 이후면 마지막 index, segment 가 비어 있으면 -1.
 */
export function findSegmentIndex(segments: SpotifyAudioSegment[], positionSeconds: number): number {
    if (segments.length === 0) {
        return -1
    }
    if (positionSeconds <= segments[0].start) {
        return 0
    }
    let lo = 0
    let hi = segments.length - 1
    while (lo < hi) {
        const mid = (lo + hi + 1) >>> 1
        if (segments[mid].start <= positionSeconds) {
            lo = mid
        } else {
            hi = mid - 1
        }
    }
    return lo
}

/**
 * 현재 시점 기준 가장 가까운 beat 까지의 거리(seconds)를 반환.
 * beat 가 없으면 양수 무한대.
 */
export function timeSinceNearestBeat(beats: SpotifyAudioBeat[], positionSeconds: number): number {
    if (beats.length === 0) {
        return Number.POSITIVE_INFINITY
    }
    let lo = 0
    let hi = beats.length - 1
    while (lo < hi) {
        const mid = (lo + hi + 1) >>> 1
        if (beats[mid].start <= positionSeconds) {
            lo = mid
        } else {
            hi = mid - 1
        }
    }
    return Math.max(0, positionSeconds - beats[lo].start)
}

/**
 * dB scale loudness 값(-60 ~ 0 정도)을 0..1 로 정규화.
 */
export function normalizeLoudness(loudnessDb: number, floorDb = -45): number {
    if (!Number.isFinite(loudnessDb)) {
        return 0.4
    }
    const clamped = Math.max(floorDb, Math.min(0, loudnessDb))
    return (clamped - floorDb) / -floorDb
}

/**
 * pitch 12 차원 값을 count 길이의 bar 배열로 선형 보간한다.
 * pitches 가 비어 있으면 균등 0.5 로 채운다.
 */
export function pitchesToBars(pitches: number[], count: number): number[] {
    const safePitches = pitches.length === PITCH_CLASSES
        ? pitches
        : new Array(PITCH_CLASSES).fill(0.5)
    const bars = new Array(count)
    for (let i = 0; i < count; i++) {
        const x = (i / Math.max(1, count - 1)) * (PITCH_CLASSES - 1)
        const lo = Math.floor(x)
        const hi = Math.min(PITCH_CLASSES - 1, lo + 1)
        const frac = x - lo
        bars[i] = safePitches[lo] * (1 - frac) + safePitches[hi] * frac
    }
    return bars
}

export interface BuildBarsOptions {
    analysis: SpotifyAudioAnalysis
    positionSeconds: number
    count: number
    /** 0..1 (default 0.45) — 작을수록 bass-only beat envelope 좁아짐 */
    beatHalfLifeSeconds?: number
}

/**
 * 현재 시점의 pitches × loudness × beat envelope 를 합성해 bar heights 를 만든다.
 * 반환값은 대략 0..1 범위이며 음악적 일관성을 위해 bar 마다 가벼운 노이즈는
 * 의도적으로 안 섞었다 (real signal 이 충분히 흔들림).
 */
export function buildBars(options: BuildBarsOptions): number[] {
    const { analysis, positionSeconds, count } = options
    const halfLife = options.beatHalfLifeSeconds ?? 0.18
    const segmentIndex = findSegmentIndex(analysis.segments, positionSeconds)
    if (segmentIndex < 0) {
        return new Array(count).fill(0.12)
    }
    const segment = analysis.segments[segmentIndex]
    const loudnessGain = normalizeLoudness(segment.loudness_max ?? segment.loudness_start ?? -30)
    const pitchBars = pitchesToBars(segment.pitches ?? [], count)

    const beatDelta = timeSinceNearestBeat(analysis.beats, positionSeconds)
    const beatStrength = Number.isFinite(beatDelta) ? Math.exp(-beatDelta / halfLife) : 0

    const bars = new Array(count)
    for (let i = 0; i < count; i++) {
        const pitched = pitchBars[i]
        const base = pitched * (0.45 + 0.55 * loudnessGain)
        const bassBoost = i < count * 0.25 ? beatStrength * 0.35 : 0
        bars[i] = Math.max(0.06, Math.min(1.0, base + bassBoost))
    }
    return bars
}
