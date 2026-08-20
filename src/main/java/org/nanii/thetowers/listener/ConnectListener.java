package org.nanii.thetowers.listener;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.nanii.thetowers.TheTowers;
import org.nanii.thetowers.config.ConfigManager;

public class ConnectListener implements Listener {

    private final TheTowers theTowers;

    public ConnectListener(TheTowers theTowers) {
        this.theTowers = theTowers;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();

        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.setGameMode(GameMode.SURVIVAL);
        }

        theTowers.getStatsService().touchPlayer(player.getUniqueId(), player.getName());

        player.teleport(ConfigManager.getLobby());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        theTowers.getArenaManager().leave(e.getPlayer());
    }
}
