package org.nanii.thetowers.team;

import net.kyori.adventure.translation.Translatable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum TeamJoinResult implements Translatable {

    OK,
    ALREADY_IN_TEAM,
    TEAM_FULL,
    WOULD_UNBALANCE;

    private final String translationKey = "team.join." + name().toLowerCase(Locale.ROOT).replace('_', '-');

    @Override
    public @NotNull String translationKey() {
        return translationKey;
    }
}
