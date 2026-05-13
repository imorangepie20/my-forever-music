create index idx_ems_acquisition_signal_article_url
    on ems_acquisition_signal (article_url)
    where article_url is not null;

create index idx_ems_acquisition_seed_platform_query_status
    on ems_acquisition_seed (lower(platform_id), lower(query), status);
