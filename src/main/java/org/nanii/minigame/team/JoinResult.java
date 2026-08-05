package org.nanii.minigame.team;

public enum JoinResult {

    OK(null),
    ALREADY_IN_TEAM("Ya estas en ese equipo."),
    TEAM_FULL("Ese equipo esta lleno."),
    WOULD_UNBALANCE("No podes unirte: dejarias los equipos desbalanceados.");

    private final String message;

    JoinResult(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
