create table ems_pool_ingest_run (
    ems_pool_ingest_run_id bigserial primary key,
    requested_by_user_id varchar(100) not null,
    source_platform varchar(50) not null,
    search_query varchar(200) not null,
    status varchar(50) not null,
    total_playlist_entries integer not null default 0,
    total_track_entries integer not null default 0,
    processed_playlist_entries integer not null default 0,
    processed_track_entries integer not null default 0,
    failed_entries integer not null default 0,
    collected_playlist_count integer not null default 0,
    collected_track_count integer not null default 0,
    last_error varchar(1000),
    created_at timestamptz not null,
    started_at timestamptz,
    completed_at timestamptz,
    updated_at timestamptz not null
);

create index idx_ems_pool_ingest_run_status_created
    on ems_pool_ingest_run (status, created_at);

create index idx_ems_pool_ingest_run_query
    on ems_pool_ingest_run (source_platform, search_query, created_at desc);

create table ems_pool_entry (
    ems_pool_entry_id bigserial primary key,
    ems_pool_ingest_run_id bigint not null
        references ems_pool_ingest_run (ems_pool_ingest_run_id) on delete cascade,
    entry_type varchar(50) not null,
    source_platform varchar(50) not null,
    external_id varchar(160) not null,
    title varchar(200) not null,
    artist_name varchar(200),
    curator varchar(120),
    description varchar(1000),
    cover_image_url varchar(500),
    platform_external_url varchar(500),
    platform_uri varchar(200),
    isrc varchar(32),
    album_title varchar(200),
    album_image_url varchar(500),
    preview_url varchar(500),
    duration_ms integer,
    track_count integer not null default 0,
    status varchar(50) not null,
    attempts integer not null default 0,
    last_error varchar(1000),
    created_at timestamptz not null,
    updated_at timestamptz not null,
    processed_at timestamptz,
    constraint uk_ems_pool_entry_run_external
        unique (ems_pool_ingest_run_id, entry_type, source_platform, external_id)
);

create index idx_ems_pool_entry_run_status
    on ems_pool_entry (ems_pool_ingest_run_id, status, ems_pool_entry_id);

create index idx_ems_pool_entry_external
    on ems_pool_entry (entry_type, source_platform, external_id);
