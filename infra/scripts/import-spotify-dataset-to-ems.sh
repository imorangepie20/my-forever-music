#!/usr/bin/env bash
set -euo pipefail

CSV_PATH="${1:-}"
POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-my-forever-music-local-postgres}"
POSTGRES_USER="${POSTGRES_USER:-postgres}"
POSTGRES_DB="${POSTGRES_DB:-my_forever_music}"
PLAYLIST_CHUNK_SIZE="${PLAYLIST_CHUNK_SIZE:-100}"

if [[ -z "${CSV_PATH}" ]]; then
  echo "usage: $0 /absolute/path/to/dataset.csv" >&2
  exit 2
fi

if [[ ! -f "${CSV_PATH}" ]]; then
  echo "dataset csv not found: ${CSV_PATH}" >&2
  exit 2
fi

if ! [[ "${PLAYLIST_CHUNK_SIZE}" =~ ^[0-9]+$ ]] || [[ "${PLAYLIST_CHUNK_SIZE}" -lt 10 ]]; then
  echo "PLAYLIST_CHUNK_SIZE must be an integer >= 10" >&2
  exit 2
fi

{
  cat <<'SQL'
begin;

create temp table spotify_dataset_stage (
    csv_index integer,
    track_id text,
    artists text,
    album_name text,
    track_name text,
    popularity integer,
    duration_ms integer,
    explicit boolean,
    danceability double precision,
    energy double precision,
    musical_key integer,
    loudness double precision,
    mode integer,
    speechiness double precision,
    acousticness double precision,
    instrumentalness double precision,
    liveness double precision,
    valence double precision,
    tempo double precision,
    time_signature integer,
    track_genre text
) on commit drop;

\copy spotify_dataset_stage from stdin with (format csv, header true)
SQL

  sed 's/\r$//' "${CSV_PATH}"
  printf '\\.\n'

  cat <<'SQL'
create temp table spotify_dataset_ranked as
with cleaned as (
    select distinct on (
        lower(trim(coalesce(nullif(track_genre, ''), 'unknown'))),
        track_id
    )
        csv_index,
        track_id,
        nullif(trim(artists), '') as artists,
        nullif(trim(album_name), '') as album_name,
        nullif(trim(track_name), '') as track_name,
        popularity,
        duration_ms,
        danceability,
        energy,
        musical_key,
        loudness,
        mode,
        speechiness,
        acousticness,
        instrumentalness,
        liveness,
        valence,
        tempo,
        time_signature,
        lower(trim(coalesce(nullif(track_genre, ''), 'unknown'))) as genre
    from spotify_dataset_stage
    where track_id is not null
      and track_id <> ''
      and nullif(trim(track_name), '') is not null
      and nullif(trim(artists), '') is not null
    order by
        lower(trim(coalesce(nullif(track_genre, ''), 'unknown'))),
        track_id,
        popularity desc nulls last,
        csv_index asc
),
ranked as (
    select
        cleaned.*,
        row_number() over (
            partition by genre
            order by popularity desc nulls last, csv_index asc, track_id asc
        ) as genre_rank
    from cleaned
)
select
    *,
    ((genre_rank - 1) / (:'playlist_chunk_size')::integer + 1)::integer as chunk_index
from ranked;

insert into ems_collected_track (
    external_track_id,
    title,
    artist_name,
    source_platform,
    isrc,
    album_title,
    album_image_url,
    platform_external_url,
    spotify_uri,
    preview_url,
    duration_ms,
    collection_source,
    collected_at,
    audio_feature_track_id,
    audio_feature_source,
    audio_features_filled,
    audio_analysis_url,
    audio_track_href,
    audio_track_uri,
    audio_feature_type,
    audio_duration_ms,
    audio_key,
    audio_mode,
    audio_time_signature,
    audio_acousticness,
    audio_danceability,
    audio_energy,
    audio_instrumentalness,
    audio_liveness,
    audio_loudness,
    audio_speechiness,
    audio_tempo,
    audio_valence,
    audio_resolved_at
)
select
    track_id,
    left(track_name, 200),
    left(artists, 200),
    'spotify',
    null,
    left(album_name, 200),
    null,
    'https://open.spotify.com/track/' || track_id,
    'spotify:track:' || track_id,
    null,
    duration_ms,
    'spotify_audio_dataset',
    now(),
    track_id,
    'spotify_dataset_csv',
    true,
    null,
    'https://api.spotify.com/v1/tracks/' || track_id,
    'spotify:track:' || track_id,
    'audio_features',
    duration_ms,
    musical_key,
    mode,
    time_signature,
    acousticness,
    danceability,
    energy,
    instrumentalness,
    liveness,
    loudness,
    speechiness,
    tempo,
    valence,
    now()
from (
    select distinct on (track_id) *
    from spotify_dataset_ranked
    order by track_id, popularity desc nulls last, csv_index asc
) tracks
on conflict (source_platform, external_track_id) do update set
    title = excluded.title,
    artist_name = excluded.artist_name,
    album_title = excluded.album_title,
    platform_external_url = excluded.platform_external_url,
    spotify_uri = excluded.spotify_uri,
    duration_ms = excluded.duration_ms,
    collection_source = excluded.collection_source,
    collected_at = excluded.collected_at,
    audio_feature_track_id = excluded.audio_feature_track_id,
    audio_feature_source = excluded.audio_feature_source,
    audio_features_filled = excluded.audio_features_filled,
    audio_track_href = excluded.audio_track_href,
    audio_track_uri = excluded.audio_track_uri,
    audio_feature_type = excluded.audio_feature_type,
    audio_duration_ms = excluded.audio_duration_ms,
    audio_key = excluded.audio_key,
    audio_mode = excluded.audio_mode,
    audio_time_signature = excluded.audio_time_signature,
    audio_acousticness = excluded.audio_acousticness,
    audio_danceability = excluded.audio_danceability,
    audio_energy = excluded.audio_energy,
    audio_instrumentalness = excluded.audio_instrumentalness,
    audio_liveness = excluded.audio_liveness,
    audio_loudness = excluded.audio_loudness,
    audio_speechiness = excluded.audio_speechiness,
    audio_tempo = excluded.audio_tempo,
    audio_valence = excluded.audio_valence,
    audio_resolved_at = excluded.audio_resolved_at;

with chunks as (
    select
        genre,
        regexp_replace(genre, '[^a-z0-9]+', '-', 'g') as genre_slug,
        chunk_index,
        min(genre_rank) as start_rank,
        max(genre_rank) as end_rank,
        count(*) as track_count
    from spotify_dataset_ranked
    group by genre, chunk_index
)
insert into ems_collected_playlist (
    external_playlist_id,
    title,
    source_platform,
    curator,
    description,
    cover_image_url,
    platform_external_url,
    spotify_uri,
    track_count,
    collection_source,
    search_query,
    collected_at
)
select
    left(format('dataset:spotify:%s:%s', nullif(genre_slug, ''), lpad(chunk_index::text, 3, '0')), 160),
    left(format('Spotify Dataset · %s · %s', initcap(replace(genre, '-', ' ')), lpad(chunk_index::text, 3, '0')), 200),
    'spotify',
    'Spotify Audio Dataset',
    left(format('Imported from dataset.csv track_genre=%s, popularity rank %s-%s.', genre, start_rank, end_rank), 1000),
    null,
    null,
    null,
    track_count,
    'spotify_audio_dataset',
    genre,
    now()
from chunks
on conflict (source_platform, external_playlist_id) do update set
    title = excluded.title,
    curator = excluded.curator,
    description = excluded.description,
    track_count = excluded.track_count,
    collection_source = excluded.collection_source,
    search_query = excluded.search_query,
    collected_at = excluded.collected_at;

insert into ems_collected_playlist_track (
    ems_collected_playlist_id,
    ems_collected_track_id,
    sort_order
)
select
    playlist.ems_collected_playlist_id,
    track.ems_collected_track_id,
    ((ranked.genre_rank - 1) % (:'playlist_chunk_size')::integer)::integer
from spotify_dataset_ranked ranked
join ems_collected_track track
  on track.source_platform = 'spotify'
 and track.external_track_id = ranked.track_id
join ems_collected_playlist playlist
  on playlist.source_platform = 'spotify'
 and playlist.external_playlist_id = left(
     format(
         'dataset:spotify:%s:%s',
         nullif(regexp_replace(ranked.genre, '[^a-z0-9]+', '-', 'g'), ''),
         lpad(ranked.chunk_index::text, 3, '0')
     ),
     160
 )
on conflict (ems_collected_playlist_id, ems_collected_track_id) do update set
    sort_order = excluded.sort_order;

select
    (select count(*) from spotify_dataset_stage) as csv_rows,
    (select count(*) from spotify_dataset_ranked) as genre_track_rows,
    (select count(*) from ems_collected_track where collection_source = 'spotify_audio_dataset') as imported_tracks,
    (select count(*) from ems_collected_playlist where collection_source = 'spotify_audio_dataset') as imported_playlists,
    (
        select count(*)
        from ems_collected_playlist_track link
        join ems_collected_playlist playlist
          on playlist.ems_collected_playlist_id = link.ems_collected_playlist_id
        where playlist.collection_source = 'spotify_audio_dataset'
    ) as imported_playlist_links;

commit;
SQL
} | docker exec -i "${POSTGRES_CONTAINER}" psql \
  -v ON_ERROR_STOP=1 \
  -v playlist_chunk_size="${PLAYLIST_CHUNK_SIZE}" \
  -U "${POSTGRES_USER}" \
  -d "${POSTGRES_DB}"
