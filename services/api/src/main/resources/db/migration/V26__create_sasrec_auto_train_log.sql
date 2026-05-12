create table sasrec_auto_train_log (
    sasrec_auto_train_log_id bigserial primary key,
    user_id varchar(100) not null,
    trained_at timestamptz not null,
    event_count_at_train bigint not null default 0,
    model_version varchar(200),
    qualified boolean not null default false,
    promoted boolean not null default false,
    summary varchar(1000)
);

create index ix_sasrec_auto_train_log_user_trained_at
    on sasrec_auto_train_log (user_id, trained_at desc);
