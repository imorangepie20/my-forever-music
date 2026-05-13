alter table ems_acquisition_run
    add column skipped_article_count integer not null default 0,
    add column skipped_seed_count integer not null default 0;
