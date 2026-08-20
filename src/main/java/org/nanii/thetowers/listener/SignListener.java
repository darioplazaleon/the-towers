package org.nanii.thetowers.listener;

import io.papermc.paper.event.player.PlayerOpenSignEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.nanii.thetowers.TheTowers;
import org.nanii.thetowers.arena.Arena;
import org.nanii.thetowers.arena.ArenaJoinResult;

public class SignListener implements Listener {

    private static final String MARKER = "[arena]";
    private static final String PERMISSION = "thetowers.admin";

    private final TheTowers theTowers;

    public SignListener(TheTowers theTowers) {
        this.theTowers = theTowers;
    }

    @EventHandler
    public void onSignChange(SignChangeEvent e) {
        if (!plain(e.line(0)).equalsIgnoreCase(MARKER)) return;

        Player player = e.getPlayer();
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(Component.translatable("sign.admin.no-permission"));
            return;
        }

        int id;
        try {
            id = Integer.parseInt(plain(e.line(1)));
        } catch (NumberFormatException ex) {
            player.sendMessage(Component.translatable("sign.admin.invalid-id"));
            return;
        }

        Arena arena = theTowers.getArenaManager().getArena(id);
        if (arena == null) {
            player.sendMessage(Component.translatable("sign.admin.arena-not-found",
                    Argument.numeric("arena", id)));
            return;
        }

        theTowers.getSignManager().register(e.getBlock(), id);
        player.sendMessage(Component.translatable("sign.admin.linked",
                Argument.numeric("arena", id)));
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getHand() != EquipmentSlot.HAND) return;

        Block block = e.getClickedBlock();
        if (block == null) return;

        Integer arenaId = theTowers.getSignManager().getArenaId(block);
        if (arenaId == null) return;

        e.setCancelled(true);

        Player player = e.getPlayer();
        Arena arena = theTowers.getArenaManager().getArena(arenaId);
        ArenaJoinResult result = theTowers.getArenaManager().join(player, arena);

        if (result == ArenaJoinResult.OK) {
            player.sendMessage(Component.translatable("command.arena.join.success", Argument.numeric("arena", arenaId)));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);
        } else {
            player.sendMessage(Component.translatable(result));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    @EventHandler
    public void onSignOpen(PlayerOpenSignEvent e) {
        if (theTowers.getSignManager().getArenaId(e.getSign().getBlock()) == null) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        if (theTowers.getSignManager().getArenaId(block) == null) return;

        Player player = e.getPlayer();
        if (!player.hasPermission(PERMISSION)) {
            e.setCancelled(true);
            player.sendMessage(Component.translatable("sign.admin.cannot-break"));
            return;
        }

        theTowers.getSignManager().unregister(block);
        player.sendMessage(Component.translatable("sign.admin.removed"));
    }

    private String plain(Component component) {
        if (component == null) return "";
        return PlainTextComponentSerializer.plainText().serialize(component).trim();
    }
}
