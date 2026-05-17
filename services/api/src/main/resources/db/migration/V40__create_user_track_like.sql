create table user_track_like (
    user_track_like_id bigserial primary key,
    user_id varchar(100) not null,
    source_platform varchar(50) not null,
    external_track_id varchar(200) not null,
    title varchar(500),
    artist_name varchar(500),
    album_title varchar(500),
    image_url varchar(800),
    spotify_track_id varchar(100),
    platform_external_url varchar(800),
    liked_at timestamptz not null
);

create unique index uk_user_track_like_user_platform_track
    on user_track_like (user_id, source_platform, external_track_id);

create index ix_user_track_like_user_liked_at
    on user_track_like (user_id, liked_at desc);
