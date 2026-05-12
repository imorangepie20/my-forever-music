create table track_identity_candidate (
    track_identity_candidate_id bigserial primary key,
    query_title varchar(500) not null,
    query_artist varchar(500),
    source varchar(50) not null,
    candidate_kind varchar(50) not null,
    candidate_value varchar(500) not null,
    candidate_score double precision,
    metadata text,
    status varchar(20) not null default 'pending',
    created_by varchar(100),
    created_at timestamptz not null,
    resolved_by varchar(100),
    resolved_at timestamptz,
    notes varchar(1000)
);

create index ix_track_identity_candidate_status_created_at
    on track_identity_candidate (status, created_at desc);

create index ix_track_identity_candidate_query
    on track_identity_candidate (query_title, query_artist);
