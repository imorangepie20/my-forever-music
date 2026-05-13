alter table ems_collected_track
    add column canonical_track_id bigint references canonical_track(canonical_track_id) on delete set null;

create index ix_ems_collected_track_canonical_track
    on ems_collected_track (canonical_track_id);

create index ix_ems_collected_track_isrc_canonical
    on ems_collected_track (lower(isrc), canonical_track_id)
    where isrc is not null;

alter table pms_imported_track
    add column canonical_track_id bigint references canonical_track(canonical_track_id) on delete set null;

create index ix_pms_imported_track_canonical_track
    on pms_imported_track (canonical_track_id);

create index ix_pms_imported_track_isrc_canonical
    on pms_imported_track (lower(isrc), canonical_track_id)
    where isrc is not null;

alter table pms_user_track
    add column canonical_track_id bigint references canonical_track(canonical_track_id) on delete set null;

create index ix_pms_user_track_canonical_track
    on pms_user_track (canonical_track_id);

create index ix_pms_user_track_isrc_canonical
    on pms_user_track (lower(isrc), canonical_track_id)
    where isrc is not null;
