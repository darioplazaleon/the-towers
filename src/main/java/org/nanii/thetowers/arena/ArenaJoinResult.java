package org.nanii.thetowers.arena;

import net.kyori.adventure.translation.Translatable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum ArenaJoinResult implements Translatable {
    OK,
    ALREADY_IN_ARENA,
    ARENA_NOT_FOUND,
    IN_PROGRESS,
    FULL;

    private final String translationKey = "arena.join." + name().toLowerCase(Locale.ROOT).replace('_', '-');

    @Override
    public @NotNull String translationKey() {
        return translationKey;
    }
}
