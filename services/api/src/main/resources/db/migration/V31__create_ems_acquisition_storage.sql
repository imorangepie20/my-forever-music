alter table ems_pool_ingest_run
    add column collection_source varchar(50) not null default 'search_pool';

create index idx_ems_pool_ingest_run_collection_source
    on ems_pool_ingest_run (collection_source, created_at desc);

create table ems_acquisition_run (
    ems_acquisition_run_id bigserial primary key,
    trigger_type varchar(50) not null,
    requested_by_user_id varchar(100) not null,
    status varchar(50) not null,
    source_count integer not null default 0,
    article_count integer not null default 0,
    signal_count integer not null default 0,
    seed_count integer not null default 0,
    pool_run_count integer not null default 0,
    failed_source_count integer not null default 0,
    failed_seed_count integer not null default 0,
    message varchar(1000),
    last_error varchar(1000),
    started_at timestamptz not null,
    completed_at timestamptz,
    updated_at timestamptz not null
);

create index idx_ems_acquisition_run_status_started
    on ems_acquisition_run (status, started_at desc);

create table ems_acquisition_signal (
    ems_acquisition_signal_id bigserial primary key,
    ems_acquisition_run_id bigint not null
        references ems_acquisition_run (ems_acquisition_run_id) on delete cascade,
    source_name varchar(160) not null,
    source_url varchar(500) not null,
    article_url varchar(500),
    article_title varchar(300),
    signal_type varchar(50) not null,
    query varchar(200) not null,
    confidence_score numeric(5, 4) not null,
    rationale varchar(500),
    status varchar(50) not null,
    created_at timestamptz not null
);

create index idx_ems_acquisition_signal_run
    on ems_acquisition_signal (ems_acquisition_run_id, ems_acquisition_signal_id);

create index idx_ems_acquisition_signal_query
    on ems_acquisition_signal (query, created_at desc);

create table ems_acquisition_seed (
    ems_acquisition_seed_id bigserial primary key,
    ems_acquisition_run_id bigint not null
        references ems_acquisition_run (ems_acquisition_run_id) on delete cascade,
    ems_acquisition_signal_id bigint
        references ems_acquisition_signal (ems_acquisition_signal_id) on delete set null,
    platform_id varchar(50) not null,
    query varchar(200) not null,
    status varchar(50) not null,
    ems_pool_ingest_run_id bigint
        references ems_pool_ingest_run (ems_pool_ingest_run_id) on delete set null,
    result_playlist_count integer not null default 0,
    result_track_count integer not null default 0,
    last_error varchar(1000),
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create index idx_ems_acquisition_seed_run
    on ems_acquisition_seed (ems_acquisition_run_id, ems_acquisition_seed_id);

create index idx_ems_acquisition_seed_pool_run
    on ems_acquisition_seed (ems_pool_ingest_run_id);
