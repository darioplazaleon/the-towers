package org.nanii.minigame.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.nanii.minigame.instance.Arena;
import org.nanii.minigame.manager.ConfigManager;
import org.nanii.minigame.team.JoinResult;
import org.nanii.minigame.team.Team;
import org.nanii.minigame.team.TeamManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TeamSelectorMenu implements InventoryHolder {

    public static final int RED_SLOT = 11;
    public static final int BLUE_SLOT = 15;

    public static final int SIZE = 27;

    private final Arena arena;
    private final Player viewer;
    private final Inventory inventory;

    public TeamSelectorMenu(Arena arena, Player viewer) {
        this.arena = arena;
        this.viewer = viewer;
        this.inventory = Bukkit.createInventory(this, SIZE,
                Component.text("Elegi tu equipo", NamedTextColor.DARK_GRAY)
        );
        render();
    }


    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public Arena getArena() {
        return arena;
    }

    public static Team teamAt(int slot) {
        if (slot == RED_SLOT) return Team.RED;
        if (slot == BLUE_SLOT) return Team.BLUE;
        return null;
    }

    public static void refresh(Arena arena) {
        for (UUID id : arena.getPlayers()) {
            Player player = Bukkit.getPlayer(id);
            if (player == null) continue;

            if (player.getOpenInventory().getTopInventory().getHolder() instanceof TeamSelectorMenu menu) {
                menu.render();
            }
        }
    }

    public void render() {
        ItemStack filler = buildFiller();
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }

        inventory.setItem(RED_SLOT, buildTeamItem(Team.RED));
        inventory.setItem(BLUE_SLOT, buildTeamItem(Team.BLUE));
    }

    private ItemStack buildTeamItem(Team team) {
        TeamManager teams = arena.getTeams();
        JoinResult result = teams.canJoin(viewer.getUniqueId(), team);


        ItemStack item = new ItemStack(team.getMaterial());
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text()
                .append(Component.text(team.name(), team.getColor(), TextDecoration.BOLD))
                .append(Component.text(" " + teams.count(team) + "/" + ConfigManager.getTeamSize(), NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false)

                .build());

        List<Component> lore = new ArrayList<>();
        lore.add(line("─────────────", NamedTextColor.DARK_GRAY));

        List<UUID> members = teams.members(team);
        if (members.isEmpty()) {
            lore.add(line(" (vacio)", NamedTextColor.DARK_GRAY));
        } else {
            for (UUID id : members) {
                Player member = Bukkit.getPlayer(id);
                if (member == null) continue;
                lore.add(line(" " + member.getName(), NamedTextColor.GRAY));
            }
        }

        lore.add(Component.empty());
        lore.add(statusLine(result));
        meta.lore(lore);

        if (result == JoinResult.ALREADY_IN_TEAM) {
            meta.setEnchantmentGlintOverride(true);
        }

        item.setItemMeta(meta);
        return item;
    }

    private Component statusLine(JoinResult result) {
        return switch (result) {
            case OK -> line("▸ Click para unirte", NamedTextColor.GREEN);
            case ALREADY_IN_TEAM -> line("✔ Tu equipo actual", NamedTextColor.AQUA);
            case TEAM_FULL -> line("✖ Equipo lleno", NamedTextColor.RED);
            case WOULD_UNBALANCE -> line("✖ Desbalancearía los equipos", NamedTextColor.RED);
        };
    }

    private Component line(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    private ItemStack buildFiller() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.empty());
        item.setItemMeta(meta);
        return item;
    }
}
