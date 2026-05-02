package io.myforevermusic.api.modules.ems.application;

import io.myforevermusic.api.modules.ems.presentation.EmsWorkspaceAnalysisRequest;
import io.myforevermusic.api.modules.ems.presentation.EmsWorkspaceAnalysisResponse;
import io.myforevermusic.api.modules.pms.infrastructure.persistence.PmsCatalogPlaylistTrackEntity;
import io.myforevermusic.api.modules.pms.infrastructure.persistence.PmsCatalogPlaylistTrackRepository;
import io.myforevermusic.api.modules.pms.infrastructure.persistence.PmsCatalogTrackEntity;
import io.myforevermusic.api.modules.pms.infrastructure.persistence.PmsCatalogTrackRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class EmsWorkspaceAnalysisService {

    private static final String ANALYSIS_STRATEGY = "catalog-signal-analysis-v1";

    private final Optional<PmsCatalogTrackRepository> trackRepository;
    private final Optional<PmsCatalogPlaylistTrackRepository> playlistTrackRepository;

    public EmsWorkspaceAnalysisService(
        Optional<PmsCatalogTrackRepository> trackRepository,
        Optional<PmsCatalogPlaylistTrackRepository> playlistTrackRepository
    ) {
        this.trackRepository = trackRepository;
        this.playlistTrackRepository = playlistTrackRepository;
    }

    public EmsWorkspaceAnalysisResponse analyzeWorkspace(EmsWorkspaceAnalysisRequest request) {
        List<String> seedTrackIds = normalizeList(request.seedTrackIds());
        List<String> seedArtistNames = normalizeList(request.seedArtistNames());
        List<String> seedGenres = normalizeList(request.seedGenres());
        List<String> warnings = new ArrayList<>();
        List<String> notes = new ArrayList<>();

        List<PmsCatalogTrackEntity> matchedTracks = resolveCatalogTracks(request.playlistId(), seedTrackIds, warnings);
        LinkedHashMap<String, Double> genreSignals = new LinkedHashMap<>();
        LinkedHashMap<String, Double> artistSignals = new LinkedHashMap<>();

        addSignals(genreSignals, seedGenres, 1.0);
        addSignals(
            genreSignals,
            matchedTracks.stream()
                .map(PmsCatalogTrackEntity::getPrimaryGenre)
                .filter(Objects::nonNull)
                .toList(),
            0.8
        );

        addSignals(artistSignals, seedArtistNames, 1.0);
        addSignals(
            artistSignals,
            matchedTracks.stream()
                .map(PmsCatalogTrackEntity::getArtistName)
                .filter(Objects::nonNull)
                .toList(),
            0.7
        );

        if (genreSignals.isEmpty() && artistSignals.isEmpty() && seedTrackIds.isEmpty()) {
            warnings.add("No PMS seeds were provided, so EMS analysis is using a safe default profile.");
        }

        if (matchedTracks.isEmpty() && !seedTrackIds.isEmpty()) {
            warnings.add("Catalog matching was unavailable for the provided seed track ids, so typed seeds were weighted more heavily.");
        }

        EnumMap<Mood, Double> moodScores = new EnumMap<>(Mood.class);
        for (Mood mood : Mood.values()) {
            moodScores.put(mood, 0.0);
        }

        double weightedEnergy = 0.0;
        double energyWeight = 0.0;

        for (Map.Entry<String, Double> entry : genreSignals.entrySet()) {
            GenreProfile genreProfile = genreProfileFor(entry.getKey());
            double weight = entry.getValue();

            for (Map.Entry<Mood, Double> moodContribution : genreProfile.moodWeights().entrySet()) {
                moodScores.compute(
                    moodContribution.getKey(),
                    (ignored, current) -> current + (moodContribution.getValue() * weight)
                );
            }

            weightedEnergy += genreProfile.energyHint() * weight;
            energyWeight += weight;
        }

        Mood recommendedMood = moodScores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(Mood.DISCOVERY);

        int recommendedEnergy = resolveEnergyLevel(recommendedMood, weightedEnergy, energyWeight);
        int recommendedFamiliarityBias = resolveFamiliarityBias(
            recommendedMood,
            matchedTracks,
            artistSignals,
            genreSignals
        );
        double confidenceScore = resolveConfidenceScore(
            seedTrackIds.size(),
            seedArtistNames.size(),
            seedGenres.size(),
            matchedTracks.size()
        );

        notes.add(buildMoodNote(recommendedMood, genreSignals));
        notes.add(buildEnergyNote(recommendedEnergy, genreSignals));
        if (matchedTracks.size() >= 2) {
            notes.add("Matched catalog tracks give this EMS pass a stronger anchor than text-only seed analysis.");
        } else if (!seedTrackIds.isEmpty()) {
            notes.add("Only a small portion of the seed catalog matched locally, so the EMS profile favors broad genre intent.");
        }

        return new EmsWorkspaceAnalysisResponse(
            "api",
            "ok",
            Instant.now(),
            new EmsWorkspaceAnalysisResponse.AnalysisContext(
                ANALYSIS_STRATEGY,
                request.playlistId(),
                seedTrackIds.size(),
                seedArtistNames.size(),
                seedGenres.size(),
                matchedTracks.size()
            ),
            new EmsWorkspaceAnalysisResponse.WorkspaceRecommendation(
                recommendedMood.apiValue(),
                recommendedEnergy,
                recommendedFamiliarityBias,
                confidenceScore
            ),
            buildSignalCards(genreSignals, artistSignals),
            List.copyOf(notes),
            List.copyOf(warnings)
        );
    }

    private List<PmsCatalogTrackEntity> resolveCatalogTracks(
        String playlistId,
        List<String> seedTrackIds,
        List<String> warnings
    ) {
        if (trackRepository.isEmpty()) {
            if (playlistTrackRepository.isEmpty() && !seedTrackIds.isEmpty()) {
                warnings.add("Catalog lookup is disabled in the current profile, so EMS analysis is using explicit seed text only.");
            }
            return List.of();
        }

        List<String> trackIdsToResolve = !seedTrackIds.isEmpty()
            ? seedTrackIds
            : resolveSeedTracksFromPlaylist(playlistId);

        if (trackIdsToResolve.isEmpty()) {
            return List.of();
        }

        List<PmsCatalogTrackEntity> matchedTracks = trackRepository.get().findAllById(trackIdsToResolve);
        Map<String, PmsCatalogTrackEntity> trackById = matchedTracks.stream()
            .collect(Collectors.toMap(PmsCatalogTrackEntity::getId, track -> track));

        if (matchedTracks.size() < trackIdsToResolve.size()) {
            warnings.add("Some seed tracks were not found in the local PMS catalog and were skipped during EMS analysis.");
        }

        return trackIdsToResolve.stream()
            .map(trackById::get)
            .filter(Objects::nonNull)
            .toList();
    }

    private List<String> resolveSeedTracksFromPlaylist(String playlistId) {
        if (playlistId == null || playlistId.isBlank() || playlistTrackRepository.isEmpty()) {
            return List.of();
        }

        List<PmsCatalogPlaylistTrackEntity> playlistTracks = playlistTrackRepository.get()
            .findByPlaylist_IdOrderBySortOrderAscIdAsc(playlistId);

        List<String> explicitSeeds = playlistTracks.stream()
            .filter(PmsCatalogPlaylistTrackEntity::isSeed)
            .map(playlistTrack -> playlistTrack.getTrack().getId())
            .toList();

        if (!explicitSeeds.isEmpty()) {
            return explicitSeeds;
        }

        return playlistTracks.stream()
            .limit(2)
            .map(playlistTrack -> playlistTrack.getTrack().getId())
            .toList();
    }

    private void addSignals(Map<String, Double> signals, Collection<String> values, double weight) {
        for (String rawValue : values) {
            if (rawValue == null || rawValue.isBlank()) {
                continue;
            }

            String value = rawValue.trim();
            signals.merge(value, weight, Double::sum);
        }
    }

    private List<EmsWorkspaceAnalysisResponse.SignalCard> buildSignalCards(
        Map<String, Double> genreSignals,
        Map<String, Double> artistSignals
    ) {
        List<EmsWorkspaceAnalysisResponse.SignalCard> signalCards = new ArrayList<>();

        genreSignals.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
            .limit(3)
            .map(entry -> new EmsWorkspaceAnalysisResponse.SignalCard(
                "genre",
                entry.getKey(),
                roundWeight(entry.getValue()),
                genreProfileFor(entry.getKey()).signalReason()
            ))
            .forEach(signalCards::add);

        artistSignals.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
            .limit(2)
            .map(entry -> new EmsWorkspaceAnalysisResponse.SignalCard(
                "artist",
                entry.getKey(),
                roundWeight(entry.getValue()),
                "Recurring artist affinity suggests this name should remain visible during EMS shaping."
            ))
            .forEach(signalCards::add);

        return List.copyOf(signalCards);
    }

    private int resolveEnergyLevel(Mood recommendedMood, double weightedEnergy, double energyWeight) {
        if (energyWeight <= 0.0) {
            return switch (recommendedMood) {
                case CALM, MELANCHOLY -> 2;
                case FOCUS, DISCOVERY -> 3;
                case UPBEAT -> 4;
            };
        }

        return clampToFive((int) Math.round(weightedEnergy / energyWeight));
    }

    private int resolveFamiliarityBias(
        Mood recommendedMood,
        List<PmsCatalogTrackEntity> matchedTracks,
        Map<String, Double> artistSignals,
        Map<String, Double> genreSignals
    ) {
        int bias = 3;

        if (matchedTracks.size() >= 2) {
            bias += 1;
        }

        long repeatedArtistCount = matchedTracks.stream()
            .collect(Collectors.groupingBy(PmsCatalogTrackEntity::getArtistName, Collectors.counting()))
            .values()
            .stream()
            .filter(count -> count >= 2)
            .count();

        if (repeatedArtistCount > 0 || artistSignals.size() <= 1) {
            bias += 1;
        }

        if (genreSignals.size() >= 3 || recommendedMood == Mood.DISCOVERY) {
            bias -= 1;
        }

        return clampToFive(bias);
    }

    private double resolveConfidenceScore(
        int seedTrackCount,
        int seedArtistCount,
        int seedGenreCount,
        int matchedCatalogTrackCount
    ) {
        double rawScore = 0.42
            + (Math.min(seedTrackCount, 4) * 0.06)
            + (Math.min(seedArtistCount, 3) * 0.05)
            + (Math.min(seedGenreCount, 3) * 0.05)
            + (Math.min(matchedCatalogTrackCount, 4) * 0.04);

        return Math.min(0.98, roundWeight(rawScore));
    }

    private String buildMoodNote(Mood recommendedMood, Map<String, Double> genreSignals) {
        if (genreSignals.isEmpty()) {
            return "EMS is holding a balanced discovery posture until stronger PMS genre signals arrive.";
        }

        String dominantGenre = genreSignals.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("current seeds");

        return switch (recommendedMood) {
            case CALM -> dominantGenre + " is pulling the session toward a calmer pacing profile.";
            case DISCOVERY -> dominantGenre + " widens the style spread, so EMS is leaning into discovery.";
            case FOCUS -> dominantGenre + " keeps the session structured, so EMS is tightening focus.";
            case MELANCHOLY -> dominantGenre + " adds softer tonal gravity, so EMS is preserving that reflective edge.";
            case UPBEAT -> dominantGenre + " carries the strongest lift, so EMS is biasing toward upbeat motion.";
        };
    }

    private String buildEnergyNote(int recommendedEnergy, Map<String, Double> genreSignals) {
        if (genreSignals.size() >= 3 && recommendedEnergy <= 3) {
            return "Genre diversity is fairly wide, so the energy setting is staying moderate to preserve transitions.";
        }

        if (recommendedEnergy >= 4) {
            return "The current seed mix supports faster pacing, so EMS is nudging energy toward the upper range.";
        }

        return "The current seed mix favors softer pacing, so EMS is keeping energy restrained.";
    }

    private List<String> normalizeList(List<String> values) {
        Set<String> normalizedValues = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null) {
                continue;
            }

            String trimmed = value.trim();
            if (!trimmed.isEmpty()) {
                normalizedValues.add(trimmed);
            }
        }

        return List.copyOf(normalizedValues);
    }

    private GenreProfile genreProfileFor(String genre) {
        String normalizedGenre = genre.trim().toLowerCase(Locale.ROOT);

        return switch (normalizedGenre) {
            case "ambient-pop" -> new GenreProfile(
                2.0,
                "Ambient-pop softens transitions and lowers session intensity.",
                Map.of(Mood.CALM, 1.0, Mood.DISCOVERY, 0.3)
            );
            case "art-pop" -> new GenreProfile(
                3.0,
                "Art-pop widens the palette and supports more adventurous sequencing.",
                Map.of(Mood.DISCOVERY, 1.0, Mood.MELANCHOLY, 0.4)
            );
            case "downtempo" -> new GenreProfile(
                2.0,
                "Downtempo signals a slower pulse and longer breathing room between peaks.",
                Map.of(Mood.CALM, 0.9, Mood.MELANCHOLY, 0.7)
            );
            case "dream-pop" -> new GenreProfile(
                3.0,
                "Dream-pop keeps the session airy and emotionally soft.",
                Map.of(Mood.CALM, 0.9, Mood.MELANCHOLY, 0.5)
            );
            case "indietronica" -> new GenreProfile(
                4.0,
                "Indietronica adds motion and supports sharper focus without going fully club-driven.",
                Map.of(Mood.FOCUS, 0.8, Mood.DISCOVERY, 0.7)
            );
            case "night-drive" -> new GenreProfile(
                4.0,
                "Night-drive hints at forward momentum with a controlled, late-session glow.",
                Map.of(Mood.UPBEAT, 0.8, Mood.FOCUS, 0.7, Mood.MELANCHOLY, 0.2)
            );
            case "synth-pop" -> new GenreProfile(
                4.0,
                "Synth-pop pushes the session toward repeatable uplift and bright momentum.",
                Map.of(Mood.UPBEAT, 1.0, Mood.FOCUS, 0.6)
            );
            default -> new GenreProfile(
                3.0,
                "Unmapped genres are treated as broad discovery hints until we model them explicitly.",
                Map.of(Mood.DISCOVERY, 0.5, Mood.FOCUS, 0.2)
            );
        };
    }

    private double roundWeight(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private int clampToFive(int value) {
        return Math.max(1, Math.min(5, value));
    }

    private enum Mood {
        CALM("calm"),
        DISCOVERY("discovery"),
        FOCUS("focus"),
        MELANCHOLY("melancholy"),
        UPBEAT("upbeat");

        private final String apiValue;

        Mood(String apiValue) {
            this.apiValue = apiValue;
        }

        public String apiValue() {
            return apiValue;
        }
    }

    private record GenreProfile(
        double energyHint,
        String signalReason,
        Map<Mood, Double> moodWeights
    ) {
    }
}
