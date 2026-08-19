package org.nanii.thetowers.stats;

import org.nanii.thetowers.team.Team;

import java.util.UUID;

public record PlayerMatchRecord(
        UUID uuid,
        String name,
        Team team,
        MatchResult result,
        int kills,
        int deaths,
        int points,
        long playtimeSeconds
) {
}
