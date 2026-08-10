package org.nanii.minigame.listener;

import io.papermc.paper.event.player.PlayerOpenSignEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
            player.sendMessage(Component.text("No tenes permiso para crear carteles de arena.", NamedTextColor.RED));
            return;
        }

        int id;
        try {
            id = Integer.parseInt(plain(e.line(1)));
        } catch (NumberFormatException ex) {
            player.sendMessage(Component.text("La segunda linea tiene que ser el id de la arena.", NamedTextColor.RED));
            return;
        }

        Arena arena = minigame.getArenaManager().getArena(id);
        if (arena == null) {
            player.sendMessage(Component.text("No existe la arena " + id + ".", NamedTextColor.RED));
            return;
        }

        minigame.getSignManager().register(e.getBlock(), id);
        player.sendMessage(Component.text("Cartel vinculado a la arena " + id + ".", NamedTextColor.GREEN));
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
            player.sendMessage(Component.text("Te uniste a la arena " + arenaId + ".", NamedTextColor.GREEN));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);
        } else {
            player.sendMessage(Component.text(result.getMessage(), NamedTextColor.RED));
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
            player.sendMessage(Component.text("No podes romper un cartel de arena.", NamedTextColor.RED));
            return;
        }

        minigame.getSignManager().unregister(block);
        player.sendMessage(Component.text("Cartel de arena eliminado.", NamedTextColor.YELLOW));
    }

    private String plain(Component component) {
        if (component == null) return "";
        return PlainTextComponentSerializer.plainText().serialize(component).trim();
    }
}
