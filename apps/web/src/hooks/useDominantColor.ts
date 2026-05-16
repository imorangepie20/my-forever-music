import { useEffect, useState } from 'react'

export interface DominantColor {
    hex: string
    rgb: [number, number, number]
}

const SAMPLE_SIZE = 32

const seedHueFromString = (seed: string) => {
    let total = 0
    for (let i = 0; i < seed.length; i += 1) {
        total += seed.charCodeAt(i)
    }
    return total % 360
}

const hslToRgb = (h: number, s: number, l: number): [number, number, number] => {
    const c = (1 - Math.abs(2 * l - 1)) * s
    const hp = h / 60
    const x = c * (1 - Math.abs((hp % 2) - 1))
    let r1 = 0
    let g1 = 0
    let b1 = 0
    if (hp >= 0 && hp < 1) { r1 = c; g1 = x; b1 = 0 }
    else if (hp < 2) { r1 = x; g1 = c; b1 = 0 }
    else if (hp < 3) { r1 = 0; g1 = c; b1 = x }
    else if (hp < 4) { r1 = 0; g1 = x; b1 = c }
    else if (hp < 5) { r1 = x; g1 = 0; b1 = c }
    else if (hp < 6) { r1 = c; g1 = 0; b1 = x }
    const m = l - c / 2
    return [
        Math.round((r1 + m) * 255),
        Math.round((g1 + m) * 255),
        Math.round((b1 + m) * 255),
    ]
}

const rgbToHex = ([r, g, b]: [number, number, number]) =>
    `#${[r, g, b].map((v) => v.toString(16).padStart(2, '0')).join('')}`

const luminance = ([r, g, b]: [number, number, number]) =>
    (0.299 * r + 0.587 * g + 0.114 * b) / 255

const brighten = (rgb: [number, number, number], target: number): [number, number, number] => {
    if (luminance(rgb) >= target) {
        return rgb
    }
    const factor = target / Math.max(0.01, luminance(rgb))
    return [
        Math.min(255, Math.round(rgb[0] * factor)),
        Math.min(255, Math.round(rgb[1] * factor)),
        Math.min(255, Math.round(rgb[2] * factor)),
    ]
}

const fallbackColor = (seed: string): DominantColor => {
    const hue = seedHueFromString(seed)
    const rgb = hslToRgb(hue, 0.65, 0.55)
    return { rgb, hex: rgbToHex(rgb) }
}

const sampleImagePixels = (image: HTMLImageElement): [number, number, number] | null => {
    const canvas = document.createElement('canvas')
    canvas.width = SAMPLE_SIZE
    canvas.height = SAMPLE_SIZE
    const ctx = canvas.getContext('2d')
    if (!ctx) {
        return null
    }
    try {
        ctx.drawImage(image, 0, 0, SAMPLE_SIZE, SAMPLE_SIZE)
        const { data } = ctx.getImageData(0, 0, SAMPLE_SIZE, SAMPLE_SIZE)
        let r = 0
        let g = 0
        let b = 0
        let count = 0
        for (let i = 0; i < data.length; i += 4) {
            const alpha = data[i + 3]
            if (alpha < 128) {
                continue
            }
            r += data[i]
            g += data[i + 1]
            b += data[i + 2]
            count += 1
        }
        if (count === 0) {
            return null
        }
        return [Math.round(r / count), Math.round(g / count), Math.round(b / count)]
    } catch {
        return null
    }
}

export function useDominantColor(imageUrl: string | null | undefined, seed: string): DominantColor {
    const [color, setColor] = useState<DominantColor>(() => fallbackColor(seed))

    useEffect(() => {
        if (!imageUrl) {
            setColor(fallbackColor(seed))
            return
        }

        let cancelled = false
        const image = new Image()
        image.crossOrigin = 'anonymous'
        image.decoding = 'async'

        const handleLoad = () => {
            if (cancelled) {
                return
            }
            const rgb = sampleImagePixels(image)
            if (!rgb) {
                setColor(fallbackColor(seed))
                return
            }
            const adjusted = brighten(rgb, 0.32)
            setColor({ rgb: adjusted, hex: rgbToHex(adjusted) })
        }

        const handleError = () => {
            if (!cancelled) {
                setColor(fallbackColor(seed))
            }
        }

        image.addEventListener('load', handleLoad)
        image.addEventListener('error', handleError)
        image.src = imageUrl

        return () => {
            cancelled = true
            image.removeEventListener('load', handleLoad)
            image.removeEventListener('error', handleError)
        }
    }, [imageUrl, seed])

    return color
}
