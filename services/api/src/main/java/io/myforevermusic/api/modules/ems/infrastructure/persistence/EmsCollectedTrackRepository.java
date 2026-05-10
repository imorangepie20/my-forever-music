package io.myforevermusic.api.modules.ems.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmsCollectedTrackRepository extends JpaRepository<EmsCollectedTrackEntity, Long> {
    List<EmsCollectedTrackEntity> findBySourcePlatformOrderByCollectedAtDesc(String platformId);
    Optional<EmsCollectedTrackEntity> findBySourcePlatformAndExternalTrackId(String platformId, String externalTrackId);
    long countBySourcePlatform(String platformId);
    long countBySourcePlatformAndAudioFeaturesAudioFeaturesFilled(String platformId, boolean filled);

    @Query("select distinct track.sourcePlatform from EmsCollectedTrackEntity track order by track.sourcePlatform")
    List<String> findDistinctSourcePlatforms();

    @Modifying
    @Query(value = """
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
        ) values (
            :externalTrackId,
            :title,
            :artistName,
            :sourcePlatform,
            :isrc,
            :albumTitle,
            :albumImageUrl,
            :platformExternalUrl,
            :spotifyUri,
            :previewUrl,
            :durationMs,
            :collectionSource,
            :collectedAt,
            :audioFeatureTrackId,
            :audioFeatureSource,
            :audioFeaturesFilled,
            :audioAnalysisUrl,
            :audioTrackHref,
            :audioTrackUri,
            :audioFeatureType,
            :audioDurationMs,
            :audioKey,
            :audioMode,
            :audioTimeSignature,
            :audioAcousticness,
            :audioDanceability,
            :audioEnergy,
            :audioInstrumentalness,
            :audioLiveness,
            :audioLoudness,
            :audioSpeechiness,
            :audioTempo,
            :audioValence,
            :audioResolvedAt
        )
        on conflict (source_platform, external_track_id) do update set
            title = excluded.title,
            artist_name = excluded.artist_name,
            isrc = excluded.isrc,
            album_title = excluded.album_title,
            album_image_url = excluded.album_image_url,
            platform_external_url = excluded.platform_external_url,
            spotify_uri = excluded.spotify_uri,
            preview_url = excluded.preview_url,
            duration_ms = excluded.duration_ms,
            collection_source = excluded.collection_source,
            collected_at = excluded.collected_at,
            audio_feature_track_id = excluded.audio_feature_track_id,
            audio_feature_source = excluded.audio_feature_source,
            audio_features_filled = excluded.audio_features_filled,
            audio_analysis_url = excluded.audio_analysis_url,
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
            audio_resolved_at = excluded.audio_resolved_at
        """, nativeQuery = true)
    void upsertByExternalTrack(
        @Param("externalTrackId") String externalTrackId,
        @Param("title") String title,
        @Param("artistName") String artistName,
        @Param("sourcePlatform") String sourcePlatform,
        @Param("isrc") String isrc,
        @Param("albumTitle") String albumTitle,
        @Param("albumImageUrl") String albumImageUrl,
        @Param("platformExternalUrl") String platformExternalUrl,
        @Param("spotifyUri") String spotifyUri,
        @Param("previewUrl") String previewUrl,
        @Param("durationMs") Integer durationMs,
        @Param("collectionSource") String collectionSource,
        @Param("collectedAt") Instant collectedAt,
        @Param("audioFeatureTrackId") String audioFeatureTrackId,
        @Param("audioFeatureSource") String audioFeatureSource,
        @Param("audioFeaturesFilled") boolean audioFeaturesFilled,
        @Param("audioAnalysisUrl") String audioAnalysisUrl,
        @Param("audioTrackHref") String audioTrackHref,
        @Param("audioTrackUri") String audioTrackUri,
        @Param("audioFeatureType") String audioFeatureType,
        @Param("audioDurationMs") Integer audioDurationMs,
        @Param("audioKey") Integer audioKey,
        @Param("audioMode") Integer audioMode,
        @Param("audioTimeSignature") Integer audioTimeSignature,
        @Param("audioAcousticness") Double audioAcousticness,
        @Param("audioDanceability") Double audioDanceability,
        @Param("audioEnergy") Double audioEnergy,
        @Param("audioInstrumentalness") Double audioInstrumentalness,
        @Param("audioLiveness") Double audioLiveness,
        @Param("audioLoudness") Double audioLoudness,
        @Param("audioSpeechiness") Double audioSpeechiness,
        @Param("audioTempo") Double audioTempo,
        @Param("audioValence") Double audioValence,
        @Param("audioResolvedAt") Instant audioResolvedAt
    );
}
