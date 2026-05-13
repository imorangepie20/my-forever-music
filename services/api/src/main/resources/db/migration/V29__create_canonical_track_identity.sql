create table canonical_track (
    canonical_track_id bigserial primary key,
    display_title varchar(500) not null,
    display_artist_name varchar(500),
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table canonical_track_identity (
    canonical_track_identity_id bigserial primary key,
    canonical_track_id bigint not null references canonical_track(canonical_track_id) on delete cascade,
    identity_kind varchar(50) not null,
    identity_value varchar(500) not null,
    source varchar(50) not null,
    confidence_score double precision,
    status varchar(40) not null,
    created_from_candidate_id bigint references track_identity_candidate(track_identity_candidate_id) on delete set null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create unique index uk_canonical_track_identity_active
    on canonical_track_identity (source, identity_kind, identity_value)
    where status = 'active';

create index ix_canonical_track_identity_track
    on canonical_track_identity (canonical_track_id);

create index ix_canonical_track_identity_candidate
    on canonical_track_identity (created_from_candidate_id);
