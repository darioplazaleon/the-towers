package org.nanii.minigame.team;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

public enum Team {
    RED("Red", Material.RED_WOOL, NamedTextColor.RED),
    BLUE("Blue", Material.BLUE_WOOL, NamedTextColor.BLUE);

    private final String display;
    private final Material material;
    private final NamedTextColor color;

    Team(String display, Material material, NamedTextColor color) {
        this.display = display;
        this.material = material;
        this.color = color;
    }

    public Team opposite() {
        return this == RED ? BLUE : RED;
    }

    public Component displayName() {
        return Component.text(display, color);
    }

    public Material getMaterial() {
        return material;
    }

    public NamedTextColor getColor() {
        return color;
    }
}
