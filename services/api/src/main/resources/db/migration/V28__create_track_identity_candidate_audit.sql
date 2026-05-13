create table track_identity_candidate_audit (
    track_identity_candidate_audit_id bigserial primary key,
    track_identity_candidate_id bigint not null references track_identity_candidate(track_identity_candidate_id) on delete cascade,
    action varchar(40) not null,
    ems_collected_track_id bigint,
    candidate_value varchar(500),
    previous_isrc varchar(32),
    new_isrc varchar(32),
    status varchar(40) not null,
    message varchar(1000),
    acted_by varchar(100),
    acted_at timestamptz not null
);

create index ix_track_identity_candidate_audit_candidate_action
    on track_identity_candidate_audit (track_identity_candidate_id, action, acted_at desc);

create index ix_track_identity_candidate_audit_track
    on track_identity_candidate_audit (ems_collected_track_id);
