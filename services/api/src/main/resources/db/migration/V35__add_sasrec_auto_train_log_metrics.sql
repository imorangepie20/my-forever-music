alter table sasrec_auto_train_log
    add column hit_rate_at_k double precision,
    add column mrr_at_k double precision,
    add column ndcg_at_k double precision,
    add column baseline_hit_rate_at_k double precision,
    add column baseline_mrr_at_k double precision,
    add column baseline_ndcg_at_k double precision,
    add column hit_rate_delta double precision,
    add column mrr_delta double precision,
    add column ndcg_delta double precision;
