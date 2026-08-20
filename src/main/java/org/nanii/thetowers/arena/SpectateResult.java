package org.nanii.thetowers.arena;

import net.kyori.adventure.translation.Translatable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum SpectateResult implements Translatable {
    OK,
    ARENA_NOT_FOUND,
    ALREADY_IN_ARENA,
    NOT_AVAILABLE,
    FULL;

    private final String translationKey = "arena.spectate." + name().toLowerCase(Locale.ROOT).replace('_', '-');

    @Override
    public @NotNull String translationKey() {
        return translationKey;
    }
}
