package org.nanii.minigame.instance;

public enum ArenaJoinResult {
    OK(null),
    ALREADY_IN_ARENA("Ya estas en una arena. Usa /arena leave para salir primero."),
    ARENA_NOT_FOUND("Esa arena no existe."),
    IN_PROGRESS("La partida ya empezo. Usa /arena spectate <id> para mirarla."),
    FULL("La arena esta llena.");

    private final String message;

    ArenaJoinResult(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
