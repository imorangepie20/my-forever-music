import { ArrowRight, Library, Newspaper, Sparkles } from 'lucide-react'
import { Link } from 'react-router-dom'

const AlgorithmIntroSection = () => {
    return (
        <section className="relative overflow-hidden rounded-3xl border border-hud-border-secondary bg-gradient-to-br from-hud-bg-primary/95 via-hud-bg-primary/80 to-hud-bg-primary/95 px-6 py-7 sm:px-8">
            <div className="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
                <div className="space-y-3">
                    <p className="text-[11px] uppercase tracking-[0.32em] text-hud-accent-primary">
                        추천 알고리즘 작동 방식
                    </p>
                    <h2 className="text-2xl font-semibold text-hud-text-primary sm:text-3xl">
                        내 라이브러리, 매거진 큐레이션, 그 사이를 잇는 점수표.
                    </h2>
                    <p className="max-w-2xl text-sm leading-6 text-hud-text-secondary">
                        가져온 내 플레이리스트(<strong>PMS</strong>)와 음악 매거진에서 자란 외부 풀(<strong>EMS</strong>)을
                        6개 축 점수(<strong>GMS</strong>)로 비교해 진짜로 듣고 싶을 만한 플레이리스트만 메인에 띄웁니다.
                    </p>
                </div>
                <Link
                    to="/about/recommendation"
                    className="inline-flex shrink-0 items-center gap-2 self-start rounded-full bg-hud-accent-primary px-5 py-2.5 text-sm font-semibold text-hud-bg-primary transition-hud hover:bg-hud-accent-primary/90 lg:self-auto"
                >
                    자세히 보기
                    <ArrowRight size={16} />
                </Link>
            </div>
            <div className="mt-6 grid gap-3 sm:grid-cols-3">
                <div className="flex items-start gap-3 rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                    <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-hud-accent-primary/10 text-hud-accent-primary">
                        <Library size={18} />
                    </span>
                    <div>
                        <p className="text-sm font-semibold text-hud-text-primary">PMS · Personal Music Space</p>
                        <p className="mt-1 text-xs leading-5 text-hud-text-secondary">
                            내 음악 공간. 가져온 플레이리스트가 곧 내 취향 기준선이 됩니다.
                        </p>
                    </div>
                </div>
                <div className="flex items-start gap-3 rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                    <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-hud-accent-info/10 text-hud-accent-info">
                        <Newspaper size={18} />
                    </span>
                    <div>
                        <p className="text-sm font-semibold text-hud-text-primary">EMS · External Music Space</p>
                        <p className="mt-1 text-xs leading-5 text-hud-text-secondary">
                            바깥 음악 공간. Pitchfork·Stereogum·NME 같은 매거진에서 자라난 후보 풀.
                        </p>
                    </div>
                </div>
                <div className="flex items-start gap-3 rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                    <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-amber-400/10 text-amber-300">
                        <Sparkles size={18} />
                    </span>
                    <div>
                        <p className="text-sm font-semibold text-hud-text-primary">GMS · Gateway Music Space</p>
                        <p className="mt-1 text-xs leading-5 text-hud-text-secondary">
                            내 공간과 바깥 공간 사이의 관문. 6개 축으로 점수를 매겨 메인에 띄울 후보를 정합니다.
                        </p>
                    </div>
                </div>
            </div>
        </section>
    )
}

export default AlgorithmIntroSection
