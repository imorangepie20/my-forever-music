import { ExternalLink, Newspaper } from 'lucide-react'
import { useMagazineArticles } from '@/hooks/useMagazineArticles'
import type { MagazineArticleResponse } from '@/types/api'

const MAGAZINE_LIMIT = 2

const formatCapturedAt = (value: string | null) => {
    if (!value) {
        return null
    }
    try {
        return new Date(value).toLocaleDateString(undefined, {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
        })
    } catch {
        return null
    }
}

const MagazineSection = () => {
    const state = useMagazineArticles(MAGAZINE_LIMIT)

    if (state.status === 'loading') {
        return (
            <section className="space-y-3">
                <header className="flex items-baseline justify-between">
                    <h2 className="text-lg font-semibold text-hud-text-primary">매거진 픽</h2>
                    <span className="text-xs text-hud-text-muted">Loading…</span>
                </header>
                <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                    {Array.from({ length: MAGAZINE_LIMIT }).map((_, index) => (
                        <div
                            key={index}
                            className="h-56 animate-pulse rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/60"
                        />
                    ))}
                </div>
            </section>
        )
    }

    if (state.status === 'empty' || state.status === 'error') {
        return null
    }

    return (
        <section className="space-y-4">
            <header className="flex items-baseline justify-between">
                <h2 className="text-lg font-semibold text-hud-text-primary">매거진 픽</h2>
                <span className="text-xs text-hud-text-muted">최신 {state.articles.length}건</span>
            </header>
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                {state.articles.map((article) => (
                    <MagazineCard key={article.article_url} article={article} />
                ))}
            </div>
        </section>
    )
}

const MagazineCard = ({ article }: { article: MagazineArticleResponse }) => {
    const captured = formatCapturedAt(article.captured_at)
    const headline = article.article_title_ko ?? article.article_title
    const supportingCopy = article.rationale_ko ?? article.rationale
    return (
        <a
            href={article.article_url}
            target="_blank"
            rel="noreferrer noopener"
            className="group flex h-full flex-col overflow-hidden rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 transition-hud hover:border-hud-border-primary hover:bg-hud-bg-primary/90"
        >
            <div className="relative aspect-[16/9] w-full overflow-hidden bg-hud-bg-primary">
                {article.image_url ? (
                    <img
                        src={article.image_url}
                        alt={headline}
                        loading="lazy"
                        className="h-full w-full object-cover transition-transform duration-200 group-hover:scale-105"
                    />
                ) : (
                    <div className="flex h-full w-full items-center justify-center text-hud-text-muted">
                        <Newspaper size={28} />
                    </div>
                )}
                <div className="absolute left-3 top-3 flex items-center gap-1.5 rounded-full bg-black/60 px-2.5 py-1 text-[11px] uppercase tracking-[0.18em] text-white backdrop-blur">
                    <Newspaper size={11} />
                    <span className="truncate max-w-[160px]">{article.source_name}</span>
                </div>
            </div>
            <div className="flex flex-1 flex-col justify-between gap-3 p-4">
                <div className="space-y-2">
                    <p className="line-clamp-3 text-base font-semibold leading-6 text-hud-text-primary">
                        {headline}
                    </p>
                    {article.article_title_ko && article.article_title_ko !== article.article_title && (
                        <p className="line-clamp-2 text-xs leading-5 text-hud-text-muted">
                            {article.article_title}
                        </p>
                    )}
                    {supportingCopy && (
                        <p className="line-clamp-2 text-sm leading-5 text-hud-text-secondary">{supportingCopy}</p>
                    )}
                </div>
                <div className="flex items-center justify-between text-xs text-hud-text-muted">
                    <span>{captured ?? ''}</span>
                    <span className="inline-flex items-center gap-1 text-hud-text-secondary transition-hud group-hover:text-hud-accent-primary">
                        원문 보기
                        <ExternalLink size={12} />
                    </span>
                </div>
            </div>
        </a>
    )
}

export default MagazineSection
