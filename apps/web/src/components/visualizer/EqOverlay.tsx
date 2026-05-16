import { useEffect, useMemo, useState } from 'react'
import BarsVisualizer from './animations/BarsVisualizer'
import ParticleDriftVisualizer from './animations/ParticleDriftVisualizer'
import RadialBloomVisualizer from './animations/RadialBloomVisualizer'
import type { VisualizerAnimationProps } from './animations/types'

export type AnimationId = 'bars' | 'radial' | 'particle'

const ANIMATIONS: AnimationId[] = ['bars', 'radial', 'particle']

interface EqOverlayProps extends VisualizerAnimationProps {
    trackKey: string
    forcedAnimation?: AnimationId | null
}

const pickRandom = (key: string): AnimationId => {
    let hash = 0
    for (let i = 0; i < key.length; i += 1) {
        hash = (hash * 31 + key.charCodeAt(i)) | 0
    }
    return ANIMATIONS[Math.abs(hash) % ANIMATIONS.length]
}

const EqOverlay = ({ trackKey, forcedAnimation, ...animationProps }: EqOverlayProps) => {
    const initial = useMemo(() => forcedAnimation ?? pickRandom(trackKey), [forcedAnimation, trackKey])
    const [active, setActive] = useState<AnimationId>(initial)

    useEffect(() => {
        setActive(forcedAnimation ?? pickRandom(trackKey))
    }, [trackKey, forcedAnimation])

    if (active === 'bars') {
        return (
            <div className="flex h-full w-full items-end justify-center">
                <BarsVisualizer {...animationProps} />
            </div>
        )
    }
    if (active === 'radial') {
        return <RadialBloomVisualizer {...animationProps} />
    }
    return <ParticleDriftVisualizer {...animationProps} />
}

export default EqOverlay
