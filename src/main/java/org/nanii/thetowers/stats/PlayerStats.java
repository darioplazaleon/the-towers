package org.nanii.thetowers.stats;

public record PlayerStats(
        int games,
        int wins,
        int losses,
        int abandons,
        int kills,
        int deaths,
        int points,
        long playtimeSeconds
) {

    public static PlayerStats empty() {
        return new PlayerStats(0, 0, 0, 0, 0, 0, 0, 0L);
    }

    public double winrate() {
        return games == 0 ? 0.0 : (double) wins / games;
    }

    public double kd() {
        return deaths == 0 ? kills : (double) kills / deaths;
    }
}
