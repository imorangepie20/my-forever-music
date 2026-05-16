import type { TidalAudioAnalyserHandle } from '@/hooks/useTidalAudioAnalyser'

export interface VisualizerAnimationProps {
    analyser: TidalAudioAnalyserHandle
    accentHex: string
    isPlaying: boolean
}
