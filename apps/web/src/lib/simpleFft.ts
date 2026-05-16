const TWO_PI = Math.PI * 2

export const pcmToByteFrequencyData = (samples: Float32Array, target: Uint8Array) => {
    const n = samples.length
    if (n === 0 || target.length === 0) {
        target.fill(0)
        return
    }

    const binLimit = Math.min(target.length, Math.floor(n / 2))
    for (let bin = 0; bin < target.length; bin += 1) {
        if (bin >= binLimit) {
            target[bin] = 0
            continue
        }

        let real = 0
        let imag = 0
        for (let i = 0; i < n; i += 1) {
            const windowed = samples[i] * (0.5 - 0.5 * Math.cos(TWO_PI * i / Math.max(1, n - 1)))
            const angle = TWO_PI * bin * i / n
            real += windowed * Math.cos(angle)
            imag -= windowed * Math.sin(angle)
        }

        const magnitude = Math.sqrt(real * real + imag * imag) / n
        target[bin] = Math.round(Math.min(255, Math.log10(1 + magnitude * 160) * 255))
    }
}
