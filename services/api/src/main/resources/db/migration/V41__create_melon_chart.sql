create table melon_chart_track (
    melon_chart_track_id bigserial primary key,
    rank integer not null,
    melon_song_id varchar(80),
    title varchar(500) not null,
    artist_name varchar(500) not null,
    album_title varchar(500),
    image_url varchar(800),
    song_external_url varchar(800),
    snapshot_at timestamptz not null
);

create unique index uk_melon_chart_track_rank on melon_chart_track (rank);
create index ix_melon_chart_track_snapshot_at on melon_chart_track (snapshot_at desc);
