import {
    ArrowLeft,
    ArrowRight,
    AudioLines,
    Brain,
    CheckCircle2,
    Compass,
    Database,
    Gauge,
    Heart,
    Library,
    ListChecks,
    Newspaper,
    RefreshCcw,
    ShieldCheck,
    Sparkles,
    Workflow,
} from 'lucide-react'
import { Link } from 'react-router-dom'
import HudCard from '@/components/common/HudCard'

const inputPillars = [
    {
        label: 'PMS',
        title: '내 라이브러리',
        description: 'Spotify, TIDAL 등에서 가져온 플레이리스트와 트랙을 개인 취향의 기준선으로 사용합니다.',
        evidence: ['저장한 플레이리스트', '좋아요/스킵/완청', '오디오 특성'],
        icon: <Library size={22} />,
        tone: 'text-hud-accent-primary bg-hud-accent-primary/10 border-hud-accent-primary/25',
    },
    {
        label: 'EMS',
        title: '외부 음악 풀',
        description: '매거진, 블로그, FLO 스페셜, 공개 플레이리스트에서 발견된 후보를 계속 확장합니다.',
        evidence: ['에디토리얼 소스', '공개 플레이리스트', '카탈로그 매칭'],
        icon: <Newspaper size={22} />,
        tone: 'text-hud-accent-info bg-hud-accent-info/10 border-hud-accent-info/25',
    },
    {
        label: 'GMS',
        title: '추천 게이트',
        description: 'PMS와 EMS를 연결해 후보를 고르고, 사용자별 모델과 6축 점수로 다시 정렬합니다.',
        evidence: ['개인 모델', '6축 점수', 'PMS 저장 루프'],
        icon: <Sparkles size={22} />,
        tone: 'text-amber-300 bg-amber-400/10 border-amber-300/25',
    },
]

const flowSteps = [
    {
        title: '1. 수집',
        text: '플랫폼 연결과 외부 소스 수집으로 개인/외부 음악 데이터를 분리해 쌓습니다.',
        icon: <Database size={18} />,
    },
    {
        title: '2. 정규화',
        text: '트랙명, 아티스트, ISRC, 오디오 특성을 맞춰 서로 비교 가능한 형태로 만듭니다.',
        icon: <AudioLines size={18} />,
    },
    {
        title: '3. 후보 생성',
        text: '내 취향과 너무 먼 후보는 줄이고, 새로 발견할 만한 EMS 후보를 앞으로 끌어옵니다.',
        icon: <Compass size={18} />,
    },
    {
        title: '4. 개인화 재정렬',
        text: '최근 행동, 장기 선호, SASRec 시퀀스 모델을 섞어 사용자별 순서를 만듭니다.',
        icon: <Brain size={18} />,
    },
    {
        title: '5. 검증과 피드백',
        text: '추천 이유, 품질 지표, 사용자 반응을 다시 로그로 남겨 다음 추천에 반영합니다.',
        icon: <RefreshCcw size={18} />,
    },
]

const modelBlocks = [
    {
        title: 'Personalization Profile',
        subtitle: '가볍게 자주 갱신되는 취향 벡터',
        text: '좋아요, 저장, 완청, 재생 재개는 긍정 신호로, 조기 스킵과 거부는 부정 신호로 누적합니다. 결과는 아티스트 선호와 플랫폼 선호 점수로 저장되어 GMS 후보에 빠르게 반영됩니다.',
        icon: <Heart size={21} />,
        tone: 'text-rose-300 bg-rose-400/10',
    },
    {
        title: 'SASRec Sequence Model',
        subtitle: '다음에 듣기 좋은 흐름을 학습',
        text: '사용자별 청취 이력을 트랙 시퀀스로 보고, 어떤 곡 다음에 어떤 곡이 자연스러운지 학습합니다. 단순 최신성 기준보다 나아졌는지 Hit@K, MRR, nDCG로 비교한 뒤 승격합니다.',
        icon: <Brain size={21} />,
        tone: 'text-indigo-300 bg-indigo-400/10',
    },
]

const sixAxes = [
    {
        name: 'Affinity',
        ko: '적합도',
        description: '내 PMS 라이브러리와 오디오 특성, 아티스트, 분위기가 얼마나 가까운지 봅니다.',
    },
    {
        name: 'Novelty',
        ko: '새로움',
        description: '이미 아는 음악만 반복하지 않도록 낯선 후보에 적당한 가산점을 줍니다.',
    },
    {
        name: 'Coherence',
        ko: '일관성',
        description: '플레이리스트가 하나의 장면이나 무드를 유지하는지 평가합니다.',
    },
    {
        name: 'Diversity',
        ko: '다양성',
        description: '현재 라이브러리의 빈 공간을 넓히는 후보인지 확인합니다.',
    },
    {
        name: 'Redundancy',
        ko: '중복도',
        description: '최근 추천이나 내 기존 목록과 지나치게 겹치면 페널티를 줍니다.',
    },
    {
        name: 'Confidence',
        ko: '신뢰도',
        description: '오디오 특성, ISRC, canonical link 같은 근거가 충분한지 따로 표시합니다.',
    },
]

const safeguards = [
    '프로바이더 인증 실패나 매칭 실패는 다른 데이터로 조용히 덮지 않습니다.',
    'PMS가 비어 있으면 EMS cold-start 후보를 주되, 개인화가 약하다는 메시지를 같이 보여줍니다.',
    '모델 승격은 baseline 대비 지표가 나아졌을 때만 진행합니다.',
    '추천 생성과 피드백은 audit log로 남겨 나중에 왜 그 결과가 나왔는지 추적합니다.',
]

const RecommendationAlgorithmPage = () => {
    return (
        <div className="space-y-6">
            <header className="flex flex-wrap items-center justify-between gap-3">
                <Link
                    to="/"
                    className="inline-flex items-center gap-2 text-sm text-hud-text-secondary transition-hud hover:text-hud-text-primary"
                >
                    <ArrowLeft size={16} />
                    메인으로 돌아가기
                </Link>
                <span className="text-xs uppercase tracking-[0.28em] text-hud-text-muted">
                    Recommendation Engine
                </span>
            </header>

            <section className="overflow-hidden rounded-lg border border-hud-border-secondary bg-hud-bg-secondary/85 shadow-hud">
                <div className="grid gap-0 xl:grid-cols-[minmax(0,1.15fr)_minmax(360px,0.85fr)]">
                    <div className="px-6 py-8 sm:px-8 sm:py-10">
                        <p className="inline-flex items-center gap-2 rounded-full border border-hud-accent-primary/25 bg-hud-accent-primary/10 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.24em] text-hud-accent-primary">
                            <Gauge size={14} />
                            Transparent Personal Ranking
                        </p>
                        <h1 className="mt-5 max-w-4xl text-3xl font-semibold leading-tight tracking-tight text-hud-text-primary sm:text-4xl">
                            추천은 “인기곡 나열”이 아니라, 내 음악 공간과 외부 음악 세계를 이어주는 게이트입니다.
                        </h1>
                        <p className="mt-4 max-w-3xl text-sm leading-7 text-hud-text-secondary sm:text-base">
                            My Forever Music은 먼저 내가 실제로 들은 음악을 기준선으로 만들고, 그다음 외부에서 수집한
                            후보를 비교합니다. 마지막에는 개인 모델과 6개 축 점수를 거쳐 메인에 보여줄 플레이리스트와
                            트랙만 남깁니다.
                        </p>

                        <div className="mt-6 flex flex-wrap gap-3">
                            <Link
                                to="/gms-playlists"
                                className="inline-flex items-center gap-2 rounded-lg bg-hud-accent-primary px-4 py-2.5 text-sm font-semibold text-hud-bg-primary transition-hud hover:bg-hud-accent-primary/90"
                            >
                                GMS 플레이리스트 보기
                                <ArrowRight size={16} />
                            </Link>
                            <Link
                                to="/recommendations/feature-coverage"
                                className="inline-flex items-center gap-2 rounded-lg border border-hud-border-secondary bg-hud-bg-primary/70 px-4 py-2.5 text-sm font-medium text-hud-text-secondary transition-hud hover:border-hud-border-primary hover:text-hud-text-primary"
                            >
                                품질 대시보드
                            </Link>
                            <Link
                                to="/recommendations/sasrec-admin"
                                className="inline-flex items-center gap-2 rounded-lg border border-hud-border-secondary bg-hud-bg-primary/70 px-4 py-2.5 text-sm font-medium text-hud-text-secondary transition-hud hover:border-hud-border-primary hover:text-hud-text-primary"
                            >
                                모델 관리
                            </Link>
                        </div>
                    </div>

                    <div className="border-t border-hud-border-secondary bg-hud-bg-primary/65 p-5 xl:border-l xl:border-t-0">
                        <div className="space-y-3">
                            {inputPillars.map((pillar, index) => (
                                <div key={pillar.label} className="rounded-lg border border-hud-border-secondary bg-hud-bg-secondary/80 p-4">
                                    <div className="flex items-center justify-between gap-3">
                                        <div className="flex items-center gap-3">
                                            <span className={`flex h-10 w-10 items-center justify-center rounded-lg border ${pillar.tone}`}>
                                                {pillar.icon}
                                            </span>
                                            <div>
                                                <p className="text-[11px] font-semibold uppercase tracking-[0.22em] text-hud-text-muted">
                                                    {pillar.label}
                                                </p>
                                                <p className="text-sm font-semibold text-hud-text-primary">{pillar.title}</p>
                                            </div>
                                        </div>
                                        <span className="text-xs text-hud-text-muted">0{index + 1}</span>
                                    </div>
                                    <p className="mt-3 text-xs leading-5 text-hud-text-secondary">{pillar.description}</p>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>
            </section>

            <section className="grid gap-4 lg:grid-cols-3">
                {inputPillars.map((pillar) => (
                    <HudCard key={pillar.label} title={`${pillar.label} · ${pillar.title}`} subtitle={pillar.description}>
                        <div className="space-y-2">
                            {pillar.evidence.map((item) => (
                                <div key={item} className="flex items-center gap-2 rounded-lg border border-hud-border-secondary bg-hud-bg-primary/70 px-3 py-2">
                                    <CheckCircle2 size={15} className="text-hud-accent-primary" />
                                    <span className="text-sm text-hud-text-secondary">{item}</span>
                                </div>
                            ))}
                        </div>
                    </HudCard>
                ))}
            </section>

            <HudCard title="추천이 만들어지는 순서" subtitle="화면에 보이는 카드 하나가 나오기까지">
                <div className="grid gap-3 lg:grid-cols-5">
                    {flowSteps.map((step) => (
                        <div key={step.title} className="rounded-lg border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                            <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-hud-accent-primary/10 text-hud-accent-primary">
                                {step.icon}
                            </span>
                            <p className="mt-3 text-sm font-semibold text-hud-text-primary">{step.title}</p>
                            <p className="mt-2 text-xs leading-5 text-hud-text-secondary">{step.text}</p>
                        </div>
                    ))}
                </div>
            </HudCard>

            <section className="grid gap-4 lg:grid-cols-[0.9fr_1.1fr]">
                <HudCard title="개인 AI 모델" subtitle="사용자마다 따로 학습되고 따로 검증됩니다">
                    <div className="space-y-3">
                        {modelBlocks.map((block) => (
                            <div key={block.title} className="flex items-start gap-3 rounded-lg border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                                <span className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-lg ${block.tone}`}>
                                    {block.icon}
                                </span>
                                <div>
                                    <p className="text-sm font-semibold text-hud-text-primary">{block.title}</p>
                                    <p className="mt-0.5 text-xs uppercase tracking-[0.16em] text-hud-text-muted">{block.subtitle}</p>
                                    <p className="mt-2 text-xs leading-5 text-hud-text-secondary">{block.text}</p>
                                </div>
                            </div>
                        ))}
                    </div>
                </HudCard>

                <HudCard title="6축 점수판" subtitle="추천 이유를 설명 가능한 단위로 나눕니다">
                    <div className="grid gap-3 sm:grid-cols-2">
                        {sixAxes.map((axis) => (
                            <div key={axis.name} className="rounded-lg border border-hud-border-secondary bg-hud-bg-primary/70 p-4">
                                <div className="flex items-center justify-between gap-3">
                                    <p className="text-sm font-semibold text-hud-text-primary">{axis.name}</p>
                                    <span className="rounded-full border border-hud-border-secondary px-2 py-0.5 text-[11px] text-hud-text-muted">
                                        {axis.ko}
                                    </span>
                                </div>
                                <p className="mt-2 text-xs leading-5 text-hud-text-secondary">{axis.description}</p>
                            </div>
                        ))}
                    </div>
                </HudCard>
            </section>

            <section className="grid gap-4 lg:grid-cols-[1fr_1fr]">
                <HudCard title="안전장치" subtitle="추천 품질이 나빠지는 순간을 숨기지 않습니다">
                    <div className="space-y-3">
                        {safeguards.map((item) => (
                            <div key={item} className="flex items-start gap-3 rounded-lg border border-hud-border-secondary bg-hud-bg-primary/70 p-3">
                                <ShieldCheck size={17} className="mt-0.5 shrink-0 text-emerald-300" />
                                <p className="text-sm leading-6 text-hud-text-secondary">{item}</p>
                            </div>
                        ))}
                    </div>
                </HudCard>

                <HudCard title="운영자가 확인할 수 있는 것" subtitle="좋아 보이는 결과보다 검증 가능한 결과를 우선합니다">
                    <div className="grid gap-3 sm:grid-cols-2">
                        <Link to="/recommendations/feature-coverage" className="rounded-lg border border-hud-border-secondary bg-hud-bg-primary/70 p-4 transition-hud hover:border-hud-border-primary">
                            <ListChecks size={19} className="text-hud-accent-primary" />
                            <p className="mt-3 text-sm font-semibold text-hud-text-primary">Feature Coverage</p>
                            <p className="mt-1 text-xs leading-5 text-hud-text-secondary">PMS/EMS 오디오 특성, ISRC, canonical 연결 상태를 확인합니다.</p>
                        </Link>
                        <Link to="/recommendations/sasrec-admin" className="rounded-lg border border-hud-border-secondary bg-hud-bg-primary/70 p-4 transition-hud hover:border-hud-border-primary">
                            <Brain size={19} className="text-indigo-300" />
                            <p className="mt-3 text-sm font-semibold text-hud-text-primary">SASRec Admin</p>
                            <p className="mt-1 text-xs leading-5 text-hud-text-secondary">모델 학습, 승격, rollback, baseline 대비 지표를 관리합니다.</p>
                        </Link>
                        <Link to="/recommendations/metadata-admin" className="rounded-lg border border-hud-border-secondary bg-hud-bg-primary/70 p-4 transition-hud hover:border-hud-border-primary">
                            <Database size={19} className="text-cyan-300" />
                            <p className="mt-3 text-sm font-semibold text-hud-text-primary">Metadata Normalize</p>
                            <p className="mt-1 text-xs leading-5 text-hud-text-secondary">MusicBrainz, Wikidata, Discogs 후보를 검토해 identity 품질을 올립니다.</p>
                        </Link>
                        <Link to="/gms-preview" className="rounded-lg border border-hud-border-secondary bg-hud-bg-primary/70 p-4 transition-hud hover:border-hud-border-primary">
                            <Workflow size={19} className="text-amber-300" />
                            <p className="mt-3 text-sm font-semibold text-hud-text-primary">GMS Preview</p>
                            <p className="mt-1 text-xs leading-5 text-hud-text-secondary">추천 후보와 feedback 루프를 직접 확인합니다.</p>
                        </Link>
                    </div>
                </HudCard>
            </section>

            <section className="rounded-lg border border-hud-border-secondary bg-hud-bg-secondary/80 p-5">
                <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                    <div>
                        <p className="text-sm font-semibold text-hud-text-primary">다음 추천을 더 좋게 만드는 가장 빠른 방법</p>
                        <p className="mt-1 text-sm leading-6 text-hud-text-secondary">
                            플랫폼 플레이리스트를 PMS로 가져오고, GMS 후보를 들어본 뒤 저장/거부 피드백을 남기면 개인 모델이 더 빨리 선명해집니다.
                        </p>
                    </div>
                    <div className="flex flex-wrap gap-2">
                        <Link
                            to="/pms"
                            className="inline-flex items-center gap-2 rounded-lg border border-hud-border-secondary bg-hud-bg-primary/70 px-4 py-2 text-sm text-hud-text-secondary transition-hud hover:border-hud-border-primary hover:text-hud-text-primary"
                        >
                            PMS 가져오기
                        </Link>
                        <Link
                            to="/gms-playlists"
                            className="inline-flex items-center gap-2 rounded-lg bg-hud-accent-primary px-4 py-2 text-sm font-semibold text-hud-bg-primary transition-hud hover:bg-hud-accent-primary/90"
                        >
                            추천 플레이리스트 보기
                            <ArrowRight size={15} />
                        </Link>
                    </div>
                </div>
            </section>
        </div>
    )
}

export default RecommendationAlgorithmPage
