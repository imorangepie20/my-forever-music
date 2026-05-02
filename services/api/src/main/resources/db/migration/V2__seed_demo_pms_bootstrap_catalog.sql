INSERT INTO pms_playlist (
    playlist_id,
    owner_user_id,
    title,
    source_platform,
    track_count,
    curator,
    highlight,
    display_order
) VALUES
    (
        'playlist-001',
        'user-001',
        'Forever Midnight Drive',
        'spotify',
        4,
        'system',
        'High replay consistency and strong synth-pop overlap.',
        1
    ),
    (
        'playlist-002',
        'user-001',
        'Soft Signal Bloom',
        'apple-music',
        4,
        'editorial',
        'Good candidate for calm and discovery-focused sessions.',
        2
    ),
    (
        'playlist-003',
        'user-002',
        'Velvet Motion Archive',
        'tidal',
        4,
        'user',
        'Dense artist affinity and useful late-night reference tracks.',
        3
    );

INSERT INTO pms_track (
    track_id,
    title,
    artist_name,
    source_platform,
    primary_genre
) VALUES
    ('track-alpha', 'Track Alpha', 'Artist One', 'spotify', 'synth-pop'),
    ('track-beta', 'Track Beta', 'Artist Two', 'apple-music', 'dream-pop'),
    ('track-gamma', 'Track Gamma', 'Artist One', 'spotify', 'synth-pop'),
    ('track-delta', 'Track Delta', 'Artist Three', 'tidal', 'indietronica'),
    ('track-epsilon', 'Track Epsilon', 'Artist Four', 'apple-music', 'ambient-pop'),
    ('track-zeta', 'Track Zeta', 'Artist Five', 'spotify', 'downtempo'),
    ('track-eta', 'Track Eta', 'Artist Three', 'tidal', 'art-pop'),
    ('track-theta', 'Track Theta', 'Artist Six', 'spotify', 'dream-pop'),
    ('track-iota', 'Track Iota', 'Artist Seven', 'tidal', 'synth-pop'),
    ('track-kappa', 'Track Kappa', 'Artist Eight', 'apple-music', 'indietronica'),
    ('track-lambda', 'Track Lambda', 'Artist Nine', 'tidal', 'downtempo'),
    ('track-mu', 'Track Mu', 'Artist Ten', 'spotify', 'night-drive');

INSERT INTO pms_playlist_track (
    playlist_id,
    track_id,
    sort_order,
    is_seed
) VALUES
    ('playlist-001', 'track-alpha', 1, TRUE),
    ('playlist-001', 'track-beta', 2, TRUE),
    ('playlist-001', 'track-gamma', 3, FALSE),
    ('playlist-001', 'track-delta', 4, FALSE),
    ('playlist-002', 'track-epsilon', 1, TRUE),
    ('playlist-002', 'track-zeta', 2, FALSE),
    ('playlist-002', 'track-eta', 3, FALSE),
    ('playlist-002', 'track-theta', 4, TRUE),
    ('playlist-003', 'track-iota', 1, TRUE),
    ('playlist-003', 'track-kappa', 2, FALSE),
    ('playlist-003', 'track-lambda', 3, TRUE),
    ('playlist-003', 'track-mu', 4, FALSE);
