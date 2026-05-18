import { ArrowLeft, Brain, Library, Newspaper, Radar, ShieldCheck, Sparkles, Workflow } from 'lucide-react'
import { Link } from 'react-router-dom'
import HudCard from '@/components/common/HudCard'

const RecommendationAlgorithmPage = () => {
    return (
        <div className="space-y-6">
            <header className="flex items-center justify-between gap-3">
                <Link
                    to="/"
                    className="inline-flex items-center gap-2 text-sm text-hud-text-secondary transition-hud hover:text-hud-text-primary"
                >
                    <ArrowLeft size={16} />
                    메인으로 돌아가기
                </Link>
                <span className="text-xs uppercase tracking-[0.28em] text-hud-text-muted">
                    추천 파이프라인
                </span>
            </header>

            <section className="relative overflow-hidden rounded-3xl border border-hud-border-secondary bg-gradient-to-br from-hud-bg-primary/95 via-hud-bg-primary/80 to-hud-bg-primary/95 px-8 py-10">
                <p className="text-[11px] uppercase tracking-[0.32em] text-hud-accent-primary">
                    세 갈래 신호, 하나의 피드
                </p>
                <h1 className="mt-3 max-w-3xl text-3xl font-semibold tracking-tight text-hud-text-primary sm:text-4xl">
                    내 라이브러리, 매거진 리딩리스트, 그리고 투명한 6축 랭커.
                </h1>
                <p className="mt-4 max-w-3xl text-sm leading-7 text-hud-text-secondary sm:text-base">
                    My Forever Music 의 추천은 세 단계로 나뉘어 입력이 명확하게 보이도록 설계돼 있습니다 —
                    내가 가져온 라이브러리(<strong>PMS</strong>), 매거진에서 큐레이션된 풀(<strong>EMS</strong>),
                    그리고 그 둘을 연결하는 개인화 랭커(<strong>GMS</strong>). 메인에 노출되는 모든 플레이리스트는
                    단일 인기도 숫자가 아니라 6개 축으로 분해된 점수로 매겨져 있어 블랙박스가 아닙니다.
                </p>
            </section>

            <section className="grid gap-4 md:grid-cols-3">
                <HudCard
                    title="PMS · Personal Music Space"
                    subtitle="내 음악 공간 — 취향의 기준선"
                >
                    <div className="flex items-start gap-3">
                        <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-hud-accent-primary/10 text-hud-accent-primary">
                            <Library size={20} />
                        </span>
                        <p className="text-sm leading-6 text-hud-text-secondary">
                            Spotify·TIDAL 에서 가져온 내 플레이리스트가 그대로 취향의 기준선이 됩니다. 각 트랙의
                            에너지·댄서빌리티·무드 같은 오디오 특성을 함께 저장해 두기 때문에, 다음 단계가
                            <strong> "내가 좋아하는 음악"</strong> 의 형태를 안정적으로 비교할 수 있습니다.
                        </p>
                    </div>
                </HudCard>

                <HudCard
                    title="EMS · External Music Space"
                    subtitle="바깥 음악 공간 — 매거진이 큐레이션한 후보"
                >
                    <div className="flex items-start gap-3">
                        <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-hud-accent-info/10 text-hud-accent-info">
                            <Newspaper size={20} />
                        </span>
                        <p className="text-sm leading-6 text-hud-text-secondary">
                            Pitchfork·Stereogum·BrooklynVegan·FACT·The FADER·NME 같은 매거진의 RSS 를
                            주기적으로 읽어들여 등장한 트랙을 추출하고, Spotify·TIDAL 카탈로그와 매칭해
                            <strong> 에디터가 검증한 후보</strong> 로 EMS 풀을 채웁니다. 알고리즘이 임의로 만든
                            가짜 데이터가 아니라 실제 매체가 다룬 음악이 들어옵니다.
                        </p>
                    </div>
                </HudCard>

                <HudCard
                    title="GMS · Gateway Music Space"
                    subtitle="게이트웨이 — PMS ↔ EMS 를 잇는 랭커"
                >
                    <div className="flex items-start gap-3">
                        <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-amber-400/10 text-amber-300">
                            <Sparkles size={20} />
                        </span>
                        <p className="text-sm leading-6 text-hud-text-secondary">
                            사용자별로 EMS 후보를 끌어와 PMS 프로필에 맞춰 다시 정렬하는 관문 단계입니다.
                            메인의 <strong>"추천"</strong> 영역은 단일 인기도 숫자가 아니라 6개 축으로 분해된
                            종합 점수에서 나오기 때문에, 왜 이 플레이리스트가 떴는지 축 단위로 추적할 수 있습니다.
                        </p>
                    </div>
                </HudCard>
            </section>

            <HudCard
                title="개인 AI 모델"
                subtitle="GMS 안에서 매 사용자마다 따로 학습되는 추천 엔진"
            >
                <div className="space-y-4">
                    <p className="text-sm leading-7 text-hud-text-secondary">
                        GMS 가 6개 축으로 점수를 매기기 전에, <strong>사용자별로 따로 학습된 개인 AI 모델</strong> 이
                        먼저 후보 순서를 손봅니다. 모든 사용자가 같은 점수표를 쓰는 게 아니라, 내가 실제로 어떤
                        음악에 시간을 썼는지가 그대로 모델 안에 반영됩니다. 모델은 사용자별로 격리돼 있어 다른
                        사람의 행동과 섞이지 않습니다.
                    </p>
                    <div className="grid gap-3 md:grid-cols-2">
                        <div className="flex items-start gap-3 rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                            <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-cyan-400/10 text-cyan-300">
                                <Radar size={20} />
                            </span>
                            <div className="space-y-1.5">
                                <p className="text-sm font-semibold text-hud-text-primary">개인화 프로필 (Personalization Profile)</p>
                                <p className="text-xs leading-5 text-hud-text-secondary">
                                    좋아요·저장·완청·중간 정지·조기 스킵·거부 같은 행동마다 가중치(+2.0 ~ -2.0)를
                                    매겨 합산합니다. 결과는 <strong>"내가 자주 듣는 아티스트"</strong> 와
                                    <strong>"내가 자주 쓰는 플랫폼"</strong> 점수로 누적되어, GMS 가 추천 후보에
                                    소프트 부스트를 거는 1차 신호로 쓰입니다.
                                </p>
                            </div>
                        </div>
                        <div className="flex items-start gap-3 rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                            <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-400/10 text-indigo-300">
                                <Brain size={20} />
                            </span>
                            <div className="space-y-1.5">
                                <p className="text-sm font-semibold text-hud-text-primary">시퀀스 모델 (SASRec)</p>
                                <p className="text-xs leading-5 text-hud-text-secondary">
                                    내 청취 이력을 <strong>"트랙 → 다음 트랙"</strong> 시퀀스로 보고, 어떤 곡 다음에
                                    어떤 곡으로 자연스럽게 넘어가는 패턴을 사용자별 트랜스포머가 학습합니다. 정기
                                    재학습 시 단순 최신성(recency) 기준 베이스라인과 Hit@K · MRR · nDCG 로 비교해
                                    퇴보가 보이면 즉시 가시화됩니다.
                                </p>
                            </div>
                        </div>
                    </div>
                    <p className="text-xs leading-5 text-hud-text-muted">
                        데이터가 적은 신규 사용자는 cold-start 가드로 EMS 풀에서 audio feature 가 채워진 트랙을
                        후보로 받으므로, 모델이 본격적으로 학습되기 전에도 빈 화면을 보지 않습니다.
                    </p>
                </div>
            </HudCard>

            <HudCard
                title="6개 축 (Six axes)"
                subtitle="추천 하나하나가 명시적 근거를 들고 옵니다"
            >
                <ul className="grid gap-3 sm:grid-cols-2">
                    {SIX_AXES.map((axis) => (
                        <li key={axis.name} className="rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                            <p className="text-sm font-semibold text-hud-text-primary">{axis.name}</p>
                            <p className="mt-1 text-xs leading-5 text-hud-text-secondary">{axis.description}</p>
                        </li>
                    ))}
                </ul>
            </HudCard>

            <section className="grid gap-4 md:grid-cols-2">
                <HudCard
                    title="가짜 데이터는 쓰지 않습니다"
                    subtitle="실제 프로바이더 호출 · 실제 PCM · 실제 에러 메시지"
                >
                    <div className="flex items-start gap-3">
                        <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-400/10 text-emerald-300">
                            <ShieldCheck size={20} />
                        </span>
                        <p className="text-sm leading-6 text-hud-text-secondary">
                            실제 스트리밍 프로바이더 호출이 실패할 때, 합성 데이터로 조용히 대체하지
                            않습니다. 플랫폼별 안내 메시지가 그대로 화면에 떠 시스템이 실제로 어떤
                            동작을 했는지 항상 확인할 수 있습니다.
                        </p>
                    </div>
                </HudCard>

                <HudCard
                    title="피드백이 루프를 닫습니다"
                    subtitle="좋아요·저장·완청이 다시 내 프로필로 돌아옵니다"
                >
                    <div className="flex items-start gap-3">
                        <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-fuchsia-400/10 text-fuchsia-300">
                            <Workflow size={20} />
                        </span>
                        <p className="text-sm leading-6 text-hud-text-secondary">
                            Dock 의 좋아요 토글, 추천 카드의 like/dislike, 트랙 완청 같은 행동이 모두
                            PMS 프로필과 GMS 랭커로 되돌아갑니다. 추천은 한 번 정해지면 끝나는 게 아니라
                            사용자와 함께 움직입니다.
                        </p>
                    </div>
                </HudCard>
            </section>

            <div className="flex flex-wrap items-center gap-3">
                <Link
                    to="/ems"
                    className="inline-flex items-center gap-2 rounded-full border border-hud-border-secondary bg-hud-bg-primary/80 px-4 py-2 text-sm text-hud-text-secondary transition-hud hover:border-hud-border-primary hover:text-hud-text-primary"
                >
                    EMS 작업 공간 열기
                </Link>
                <Link
                    to="/gms-preview"
                    className="inline-flex items-center gap-2 rounded-full border border-hud-border-secondary bg-hud-bg-primary/80 px-4 py-2 text-sm text-hud-text-secondary transition-hud hover:border-hud-border-primary hover:text-hud-text-primary"
                >
                    GMS 미리보기 실행
                </Link>
                <Link
                    to="/platforms"
                    className="inline-flex items-center gap-2 rounded-full border border-hud-border-secondary bg-hud-bg-primary/80 px-4 py-2 text-sm text-hud-text-secondary transition-hud hover:border-hud-border-primary hover:text-hud-text-primary"
                >
                    스트리밍 플랫폼 연결
                </Link>
            </div>
        </div>
    )
}

const SIX_AXES = [
    {
        name: 'Affinity · 적합도',
        description: '후보가 내 PMS 라이브러리의 에너지·무드·장르 지문과 얼마나 가까운지를 측정합니다.',
    },
    {
        name: 'Novelty · 새로움',
        description: '아직 만나지 않은 트랙·플레이리스트로 약간 기울이는 가중치 — 에코 챔버에 갇히지 않게 합니다.',
    },
    {
        name: 'Coherence · 일관성',
        description: '플레이리스트 자체가 한 분위기를 유지하는지, 여러 무드가 잡탕으로 섞여 있는지 평가합니다.',
    },
    {
        name: 'Diversity · 다양성',
        description: '내가 지금 듣고 있는 풀을 넓혀주는 후보인지, 이미 있는 것과 겹치는 후보인지 구분합니다.',
    },
    {
        name: 'Redundancy · 중복도',
        description: '이번 세션에서 이미 추천한 것과 너무 겹치는 후보에는 페널티를 줍니다.',
    },
    {
        name: 'Confidence · 신뢰도',
        description: '오디오 특성처럼 채워진 신호가 적은 경우, 적합도가 높아도 신뢰도는 낮게 잡힙니다.',
    },
]

export default RecommendationAlgorithmPage
