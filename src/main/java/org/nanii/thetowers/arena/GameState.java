package org.nanii.thetowers.arena;

import net.kyori.adventure.translation.Translatable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum GameState implements Translatable {
    RECRUITING,
    COUNTDOWN,
    LIVE,
    ENDING,
    RESETTING;

    private final String translationKey = "game.state." + name().toLowerCase(Locale.ROOT);

    @Override
    public @NotNull String translationKey() {
        return translationKey;
    }
}
