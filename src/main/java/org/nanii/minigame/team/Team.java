package org.nanii.minigame.team;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.translation.Translatable;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum Team implements Translatable {
    RED(Material.RED_WOOL, NamedTextColor.RED),
    BLUE(Material.BLUE_WOOL, NamedTextColor.BLUE);

    private final Material material;
    private final NamedTextColor color;
    private final String translationKey = "team.name." + name().toLowerCase(Locale.ROOT);

    Team(Material material, NamedTextColor color) {
        this.material = material;
        this.color = color;
    }

    public Team opposite() {
        return this == RED ? BLUE : RED;
    }

    public Component displayName() {
        return Component.translatable(this).color(color);
    }

    public Material getMaterial() {
        return material;
    }

    public NamedTextColor getColor() {
        return color;
    }

    @Override
    public @NotNull String translationKey() {
        return translationKey;
    }
}
