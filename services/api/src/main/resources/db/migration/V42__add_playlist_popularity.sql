alter table ems_collected_playlist
    add column followers_count integer,
    add column popularity_refreshed_at timestamptz;

create index ix_ems_collected_playlist_popularity
    on ems_collected_playlist (followers_count desc nulls last, track_count desc, collected_at desc);
