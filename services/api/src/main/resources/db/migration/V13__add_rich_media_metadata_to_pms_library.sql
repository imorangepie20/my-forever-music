ALTER TABLE pms_imported_playlist
    ADD COLUMN cover_image_url VARCHAR(500),
    ADD COLUMN platform_external_url VARCHAR(500),
    ADD COLUMN platform_uri VARCHAR(200);

ALTER TABLE pms_imported_track
    ADD COLUMN album_title VARCHAR(200),
    ADD COLUMN album_image_url VARCHAR(500),
    ADD COLUMN platform_external_url VARCHAR(500),
    ADD COLUMN platform_uri VARCHAR(200),
    ADD COLUMN preview_url VARCHAR(500);

ALTER TABLE pms_user_playlist
    ADD COLUMN cover_image_url VARCHAR(500),
    ADD COLUMN platform_external_url VARCHAR(500),
    ADD COLUMN platform_uri VARCHAR(200);

ALTER TABLE pms_user_track
    ADD COLUMN album_title VARCHAR(200),
    ADD COLUMN album_image_url VARCHAR(500),
    ADD COLUMN platform_external_url VARCHAR(500),
    ADD COLUMN platform_uri VARCHAR(200),
    ADD COLUMN preview_url VARCHAR(500);
