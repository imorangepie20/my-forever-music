export interface PcmAudioSegment {
    startTime: number
    sampleRate: number
    samples: Float32Array
}

export class AudioRingBuffer {
    private readonly keepSeconds: number
    private segments: PcmAudioSegment[] = []

    constructor(keepSeconds = 12) {
        this.keepSeconds = keepSeconds
    }

    clear() {
        this.segments = []
    }

    append(segment: PcmAudioSegment) {
        if (!Number.isFinite(segment.startTime) || segment.sampleRate <= 0 || segment.samples.length === 0) {
            return
        }

        this.segments.push(segment)
        this.segments.sort((a, b) => a.startTime - b.startTime)

        const latestEnd = this.segments.reduce((latest, item) => Math.max(latest, this.endTime(item)), 0)
        const cutoff = Math.max(0, latestEnd - this.keepSeconds)
        this.segments = this.segments.filter((item) => this.endTime(item) >= cutoff)
    }

    hasData() {
        return this.segments.length > 0
    }

    latestWindow(sampleCount: number) {
        const latest = this.segments[this.segments.length - 1]
        if (!latest) {
            return null
        }
        const output = new Float32Array(sampleCount)
        const start = Math.max(0, latest.samples.length - sampleCount)
        output.set(latest.samples.slice(start, start + sampleCount), sampleCount - (latest.samples.length - start))
        return output
    }

    readWindow(currentTime: number, sampleCount: number) {
        const sampleRate = this.segments[this.segments.length - 1]?.sampleRate
        if (!sampleRate || !Number.isFinite(currentTime)) {
            return this.latestWindow(sampleCount)
        }

        const output = new Float32Array(sampleCount)
        const startTime = currentTime - sampleCount / sampleRate
        let filled = 0

        for (let i = 0; i < sampleCount; i += 1) {
            const time = startTime + i / sampleRate
            const segment = this.findSegment(time)
            if (!segment) {
                continue
            }
            const index = Math.floor((time - segment.startTime) * segment.sampleRate)
            if (index >= 0 && index < segment.samples.length) {
                output[i] = segment.samples[index]
                filled += 1
            }
        }

        if (filled === 0) {
            return this.latestWindow(sampleCount)
        }
        return output
    }

    private findSegment(time: number) {
        return this.segments.find((segment) => time >= segment.startTime && time <= this.endTime(segment)) ?? null
    }

    private endTime(segment: PcmAudioSegment) {
        return segment.startTime + segment.samples.length / segment.sampleRate
    }
}
