package org.nanii.minigame.instance;

public enum SpectateResult {
    OK(null),
    ARENA_NOT_FOUND("Esa arena no existe."),
    ALREADY_IN_ARENA("Ya estas en una arena. Usa /arena leave para salir primero."),
    NOT_AVAILABLE("Esa arena se esta reiniciando. Proba en unos segundos."),
    FULL("No hay lugares de espectador libres en esa arena.");

    private final String message;

    SpectateResult(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
