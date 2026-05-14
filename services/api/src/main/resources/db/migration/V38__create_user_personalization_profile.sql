create table user_personalization_profile (
    user_personalization_profile_id bigserial primary key,
    user_id varchar(100) not null,
    top_artists_json text,
    top_source_platforms_json text,
    event_count_at_update bigint not null default 0,
    last_event_at timestamptz,
    recomputed_at timestamptz not null
);

create unique index uk_user_personalization_profile_user
    on user_personalization_profile (user_id);
