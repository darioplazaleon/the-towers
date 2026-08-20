package org.nanii.thetowers.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.nanii.thetowers.GameState;
import org.nanii.thetowers.TheTowers;
import org.nanii.thetowers.gui.TeamSelectorItem;
import org.nanii.thetowers.gui.TeamSelectorMenu;
import org.nanii.thetowers.arena.Arena;
import org.nanii.thetowers.team.JoinResult;
import org.nanii.thetowers.team.Team;

public class TeamSelectorListener implements Listener {

    private final TheTowers theTowers;

    public TeamSelectorListener(TheTowers theTowers) {
        this.theTowers = theTowers;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!TeamSelectorItem.is(e.getItem())) return;

        e.setCancelled(true);

        Player player = e.getPlayer();
        Arena arena = theTowers.getArenaManager().getArena(player);
        if (arena == null) return;
        if (arena.getState() != GameState.RECRUITING && arena.getState() != GameState.COUNTDOWN) return;

        player.openInventory(new TeamSelectorMenu(arena, player).getInventory());
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (TeamSelectorItem.is(e.getItemDrop().getItemStack())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof TeamSelectorMenu) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (TeamSelectorItem.is(e.getCurrentItem()) || TeamSelectorItem.is(e.getCursor())) {
            e.setCancelled(true);
        }

        if (!(e.getInventory().getHolder() instanceof TeamSelectorMenu menu)) return;

        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player player)) return;

        Team team = TeamSelectorMenu.teamAt(e.getRawSlot());
        if (team == null) return;

        JoinResult result = menu.getArena().getTeams().tryJoin(player, team);

        if (result == JoinResult.OK) {
            player.sendMessage(Component.translatable("team.join.success", Argument.component("team", team.displayName())));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);
            player.closeInventory();
            TeamSelectorMenu.refresh(menu.getArena());
        } else {
            player.sendMessage(Component.translatable(result));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            menu.render();
        }
    }
}
