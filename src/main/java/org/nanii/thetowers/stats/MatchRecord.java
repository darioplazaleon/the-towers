package org.nanii.thetowers.stats;

import org.nanii.thetowers.team.Team;

import java.util.List;

public record MatchRecord(
        int arenaId,
        long startedAt,
        long endedAt,
        long durationSeconds,
        MatchStatus status,
        Team winner,
        int redScore,
        int blueScore,
        List<PlayerMatchRecord> players
) {
    public MatchRecord {
        players = List.copyOf(players);
    }
}
