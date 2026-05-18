# TIDAL Playlist URL GMS Import Design

Date: 2026-05-19

## Goal

Allow a normal logged-in user to paste a public TIDAL playlist URL such as:

```text
https://tidal.com/playlist/0a3d87d2-27dc-4edc-84b6-9f1eaa567f33
```

The backend must load the real playlist through the user's connected TIDAL credential, persist it into the EMS collected playlist pool, and let the existing GMS playlist preview flow surface it as a recommendation candidate.

## Non-Goals

- Do not create a separate GMS-only playlist store.
- Do not bypass EMS collected playlist storage.
- Do not save the imported playlist directly into PMS.
- Do not use mock, preview-only, or sandbox data if the TIDAL call fails.
- Do not silently fall back to another provider when TIDAL auth, playlist lookup, or track lookup fails.

## User Flow

1. The user opens the GMS playlist page.
2. The user pastes a TIDAL playlist URL.
3. The app validates the URL shape before sending the request.
4. The API extracts the TIDAL playlist UUID.
5. The API verifies that the user has a usable TIDAL credential.
6. The API loads playlist metadata and tracks from TIDAL.
7. The API stores the playlist and tracks in EMS with `collection_source = user_tidal_url_import`.
8. The frontend refreshes the GMS playlist candidates in a way that includes the just-imported TIDAL playlist.
9. The user previews, saves, or dismisses the playlist through the existing GMS playlist UI.

## API Design

Add:

```http
POST /api/v1/gms/playlists/import/tidal-url
```

Request:

```json
{
  "user_id": "user-id",
  "playlist_url": "https://tidal.com/playlist/0a3d87d2-27dc-4edc-84b6-9f1eaa567f33"
}
```

Response:

```json
{
  "service": "api",
  "status": "ok",
  "generated_at": "2026-05-19T00:00:00Z",
  "user_id": "user-id",
  "ems_playlist_id": 123,
  "external_playlist_id": "0a3d87d2-27dc-4edc-84b6-9f1eaa567f33",
  "source_platform": "tidal",
  "title": "Playlist title",
  "track_count": 42,
  "collection_source": "user_tidal_url_import",
  "collected_at": "2026-05-19T00:00:00Z"
}
```

Validation and errors:

- Missing `user_id`: `400`.
- Invalid or unsupported TIDAL URL: `400`.
- User has no usable TIDAL credential: `400` with an explicit reconnect/connect message.
- TIDAL playlist metadata or track request fails: propagate an explicit API error; do not store partial fake data.
- TIDAL returns zero tracks: fail the import instead of leaving an empty EMS playlist.

## Backend Design

Add a small GMS-facing import service that delegates persistence to the existing EMS collection path.

Suggested service boundary:

- `GmsTidalPlaylistUrlImportService`
  - Parses and validates TIDAL playlist URLs.
  - Resolves the user's usable TIDAL credential.
  - Calls an EMS application method to collect one concrete TIDAL playlist.

Add an EMS application method:

- `EmsCollectionService.collectTidalPlaylistFromUrlImport(userId, playlistId)`
  - Uses `TidalWebApiClient` to load playlist metadata and tracks.
  - Upserts `EmsCollectedPlaylistEntity`.
  - Upserts `EmsCollectedTrackEntity` rows.
  - Links tracks to the playlist in order.
  - Resolves audio features through the existing ReccoBeats ISRC flow.
  - Uses `collection_source = user_tidal_url_import`.

The implementation should reuse existing helpers such as:

- `upsertPlaylistFromTidal`
- `upsertTrackFromTidal`
- `linkPlaylistTrack`
- `resolveTidalAudioFeatures`
- `resolveTidalTrackAudioFeatures`

If current TIDAL client methods cannot fetch playlist metadata by id, add the smallest real API method needed in `TidalWebApiClient`. It must call TIDAL, not infer metadata from the URL alone.

## Frontend Design

Update `/gms-playlists` with a compact import form near the top of the page:

- One text input for a TIDAL playlist URL.
- One import button.
- Loading state while the request is in flight.
- Success message naming the imported playlist and track count.
- Error message that shows the real API failure reason.

After a successful import, refresh the existing GMS playlist candidates so the imported EMS playlist appears through the normal preview path. If the user's preferred playback platform is not TIDAL, the refresh must still include this just-imported TIDAL candidate instead of hiding it behind the preferred-platform filter.

## Data Model

No new table is required.

Existing EMS tables remain the source of truth:

- `ems_collected_playlist`
- `ems_collected_track`
- `ems_collected_playlist_track`

Use a new collection source value:

```text
user_tidal_url_import
```

This keeps the origin auditable without branching the GMS candidate model.

## GMS Behavior

The existing GMS playlist preview service continues to select candidates from EMS collected playlists.

The just-imported playlist is treated as an explicit user-requested candidate for the importing user. It must be eligible in the next preview response even when the user's preferred platform is Spotify or another provider. This can be implemented with a narrow request parameter or service option that includes the imported EMS playlist id alongside the normal ranked candidates, without creating a separate GMS storage model.

## Testing

Backend tests:

- Parses valid `https://tidal.com/playlist/{uuid}` URLs.
- Rejects non-TIDAL URLs and malformed playlist ids.
- Fails when no usable TIDAL credential exists.
- Stores a TIDAL playlist with tracks into EMS using `user_tidal_url_import`.
- Fails without leaving an empty playlist when TIDAL returns no tracks.

Frontend checks:

- API wrapper sends `user_id` and `playlist_url`.
- `/gms-playlists` disables submit while importing.
- Success refreshes candidates.
- Error text is visible and not suppressed.

## Success Criteria

- A logged-in user with a connected TIDAL credential can paste a real TIDAL playlist URL and import it.
- Imported playlist and tracks are persisted in EMS.
- The import uses real TIDAL responses and does not create mock playlist or track data.
- Existing GMS preview/save/dismiss behavior continues to work.
- Failure boundaries are explicit: invalid URL, missing TIDAL credential, TIDAL API failure, and empty playlist are all visible errors.
