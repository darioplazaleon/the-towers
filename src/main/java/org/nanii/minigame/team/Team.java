package org.nanii.minigame.team;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.Material;

public enum Team {
    RED(ChatColor.RED + "Red", Material.RED_WOOL, NamedTextColor.RED),
    BLUE(ChatColor.BLUE + "Blue", Material.BLUE_WOOL, NamedTextColor.BLUE);

    private String display;
    private Material material;
    private final NamedTextColor color;

    Team(String display, Material material, NamedTextColor color) {
        this.display = display;
        this.material = material;
        this.color = color;
    }

    public Team opposite() {
        return this == RED ? BLUE : RED;
    }

    public String getDisplay() {
        return display;
    }

    public Material getMaterial() {
        return material;
    }
    public NamedTextColor getColor() {return color;}
}
