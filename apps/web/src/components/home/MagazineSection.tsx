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
                            className="h-48 animate-pulse rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/60"
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
    const bodyCopy = article.description_ko ?? article.description
    const supportingCopy = article.rationale_ko ?? article.rationale
    return (
        <a
            href={article.article_url}
            target="_blank"
            rel="noreferrer noopener"
            className="group flex h-full flex-col rounded-2xl border border-hud-border-secondary bg-hud-bg-primary/70 p-4 transition-hud hover:border-hud-border-primary hover:bg-hud-bg-primary/90"
        >
            <div>
                {article.image_url ? (
                    <img
                        src={article.image_url}
                        alt={headline}
                        loading="lazy"
                        className="float-right ml-3 mb-2 h-20 w-20 shrink-0 rounded-lg object-cover sm:h-24 sm:w-24"
                    />
                ) : (
                    <div className="float-right ml-3 mb-2 flex h-20 w-20 shrink-0 items-center justify-center rounded-lg bg-hud-bg-primary text-hud-text-muted sm:h-24 sm:w-24">
                        <Newspaper size={22} />
                    </div>
                )}
                <div className="flex items-center gap-1.5 text-[11px] uppercase tracking-[0.18em] text-hud-accent-primary">
                    <Newspaper size={12} />
                    <span className="truncate">{article.source_name}</span>
                </div>
                <p className="mt-2 text-base font-semibold leading-6 text-hud-text-primary">
                    {headline}
                </p>
                {article.article_title_ko && article.article_title_ko !== article.article_title && (
                    <p className="mt-1 text-xs leading-5 text-hud-text-muted">
                        {article.article_title}
                    </p>
                )}
                {bodyCopy && (
                    <p className="mt-2 text-sm leading-6 text-hud-text-secondary">{bodyCopy}</p>
                )}
                {supportingCopy && supportingCopy !== bodyCopy && (
                    <p className="mt-2 text-xs leading-5 text-hud-text-muted">{supportingCopy}</p>
                )}
            </div>
            <div className="mt-3 flex items-center justify-between border-t border-hud-border-secondary/60 pt-3 text-xs text-hud-text-muted clear-both">
                <span>{captured ?? ''}</span>
                <span className="inline-flex items-center gap-1 text-hud-text-secondary transition-hud group-hover:text-hud-accent-primary">
                    원문 보기
                    <ExternalLink size={12} />
                </span>
            </div>
        </a>
    )
}

export default MagazineSection
