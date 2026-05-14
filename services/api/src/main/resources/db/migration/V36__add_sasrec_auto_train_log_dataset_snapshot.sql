alter table sasrec_auto_train_log
    add column dataset_version varchar(100),
    add column dataset_fingerprint varchar(100),
    add column sequence_item_count_at_train bigint not null default 0,
    add column recommendation_snapshot_count_at_train bigint not null default 0;

create index ix_sasrec_auto_train_log_dataset_fingerprint
    on sasrec_auto_train_log (user_id, dataset_fingerprint);
