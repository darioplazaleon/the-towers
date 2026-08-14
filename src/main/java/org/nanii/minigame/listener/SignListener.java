package org.nanii.minigame.listener;

import io.papermc.paper.event.player.PlayerOpenSignEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
import org.nanii.minigame.Minigame;
import org.nanii.minigame.instance.Arena;
import org.nanii.minigame.instance.ArenaJoinResult;

public class SignListener implements Listener {

    private static final String MARKER = "[arena]";
    private static final String PERMISSION = "minigame.admin";

    private final Minigame minigame;

    public SignListener(Minigame minigame) {
        this.minigame = minigame;
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

        Arena arena = minigame.getArenaManager().getArena(id);
        if (arena == null) {
            player.sendMessage(Component.translatable("sign.admin.arena-not-found",
                    Argument.numeric("arena", id)));
            return;
        }

        minigame.getSignManager().register(e.getBlock(), id);
        player.sendMessage(Component.translatable("sign.admin.linked",
                Argument.numeric("arena", id)));
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getHand() != EquipmentSlot.HAND) return;

        Block block = e.getClickedBlock();
        if (block == null) return;

        Integer arenaId = minigame.getSignManager().getArenaId(block);
        if (arenaId == null) return;

        e.setCancelled(true);

        Player player = e.getPlayer();
        Arena arena = minigame.getArenaManager().getArena(arenaId);
        ArenaJoinResult result = minigame.getArenaManager().join(player, arena);

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
        if (minigame.getSignManager().getArenaId(e.getSign().getBlock()) == null) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        if (minigame.getSignManager().getArenaId(block) == null) return;

        Player player = e.getPlayer();
        if (!player.hasPermission(PERMISSION)) {
            e.setCancelled(true);
            player.sendMessage(Component.translatable("sign.admin.cannot-break"));
            return;
        }

        minigame.getSignManager().unregister(block);
        player.sendMessage(Component.translatable("sign.admin.removed"));
    }

    private String plain(Component component) {
        if (component == null) return "";
        return PlainTextComponentSerializer.plainText().serialize(component).trim();
    }
}
