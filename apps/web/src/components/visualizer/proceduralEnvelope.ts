/**
 * Phase 1 옵션 A baseline 신호함수 (docs/architecture/PLAYBACK_VISUALIZER_DESIGN.md §3.1, §4.1).
 *
 * 입력: mode + 시각(t) + 막대 수 + (있으면) BPM / energy / valence.
 * 출력: 0..1 범위로 정규화된 막대 높이 배열.
 *
 * Web Audio API 호출 0회. AudioContext 미생성. 외부 호출 0회.
 * audioFeature 가 없을 때는 mode 별 기본 preset 으로 동작하며, "estimated BPM"
 * 같은 fake 수치를 audioFeature 슬롯에 흘리지 않는다.
 */
import type { VisualizerMode, VisualizerSample } from './Visualizer'

export interface TrackAudioFeatures {
    tempo: number | null
    energy: number | null
    valence: number | null
}

interface EnvelopeOptions {
    features?: TrackAudioFeatures | null
}

const DEFAULT_PRESET_BPM: Record<VisualizerMode, number> = {
    idle: 70,
    spotify: 112,
    tidal: 96,
}

const DEFAULT_ENERGY = 0.6

/** valence 가 없으면 0.5 (중립). bar 색이 아닌 진폭 곡률에만 살짝 영향. */
const DEFAULT_VALENCE = 0.5

const clamp01 = (value: number) => Math.max(0, Math.min(1, value))

const sanitizeTempo = (tempo: number | null | undefined): number | null => {
    if (tempo == null || !Number.isFinite(tempo) || tempo <= 30 || tempo > 300) {
        return null
    }
    return tempo
}

const sanitizeUnit = (value: number | null | undefined): number | null => {
    if (value == null || !Number.isFinite(value)) {
        return null
    }
    return clamp01(value)
}

/**
 * BPM 기반 박자 envelope. attack 0 / decay-release 만 있는 단순 곡선.
 * 정확한 박자 동기가 아니라 "박자가 있는 듯한 펄스" 를 만든다.
 */
const beatEnvelope = (t: number, bpm: number) => {
    const period = 60 / bpm
    const phase = (t % period) / period
    return Math.pow(1 - phase, 2.2)
}

export const buildHeights = (
    sample: VisualizerSample,
    options: EnvelopeOptions = {},
): number[] => {
    const { count, t, mode } = sample
    const features = options.features ?? null

    const tempo = sanitizeTempo(features?.tempo) ?? DEFAULT_PRESET_BPM[mode]
    const energy = sanitizeUnit(features?.energy) ?? DEFAULT_ENERGY
    const valence = sanitizeUnit(features?.valence) ?? DEFAULT_VALENCE

    const pulse = beatEnvelope(t, tempo)
    // valence 가 높을 수록 진폭 곡률이 약간 더 부드러워진다 (긍정/밝은 곡은 댐핑이 덜함).
    const curveBias = 0.4 + 0.2 * (valence - 0.5)

    const out = new Array<number>(count)
    for (let i = 0; i < count; i++) {
        const x = i / count
        const base = preset(mode, x, t)
        const amplitude = base * (curveBias + (1 - curveBias) * pulse) * (0.5 + 0.5 * energy)
        out[i] = Math.max(0.06, clamp01(amplitude))
    }
    return out
}

const preset = (mode: VisualizerMode, x: number, t: number): number => {
    if (mode === 'idle') {
        return 0.12 + 0.05 * Math.sin(t * 1.4 + x * Math.PI * 2.4)
    }
    if (mode === 'spotify') {
        const bass = Math.exp(-Math.pow((x - 0.18) * 4.4, 2)) * 0.85
        const mid = Math.exp(-Math.pow((x - 0.5) * 4.0, 2)) * (0.45 + 0.25 * Math.sin(t * 4 + x * 6))
        const high = Math.exp(-Math.pow((x - 0.85) * 6.0, 2)) * (0.3 + 0.2 * Math.sin(t * 9 + x * 10))
        const wobble = 0.05 * Math.sin(t * 13 + x * 31)
        return bass + mid * 0.8 + high * 0.7 + wobble
    }
    // tidal
    const env = 0.55 + 0.45 * Math.exp(-Math.pow((x - 0.22) * 2.4, 2))
    const f =
        0.35 * Math.sin(t * 7 + x * 28) +
        0.22 * Math.sin(t * 11 + x * 52) +
        0.18 * Math.sin(t * 17 + x * 13) +
        0.1 * Math.sin(t * 3 + x * 6)
    return env * (0.55 + 0.45 * f)
}

/**
 * VisualizerPage 가 features 변화에 맞춰 새 closure 를 만들어 Visualizer 에 prop 으로 넘긴다.
 * Visualizer 는 heightsAt identity 변화를 감지해 rAF 루프를 재구성한다.
 */
export const makeHeightsAt = (features: TrackAudioFeatures | null) =>
    (sample: VisualizerSample) => buildHeights(sample, { features })
