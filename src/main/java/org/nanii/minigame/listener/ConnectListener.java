package org.nanii.minigame.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.nanii.minigame.Minigame;
import org.nanii.minigame.manager.ConfigManager;

public class ConnectListener implements Listener {

    private final Minigame minigame;

    public ConnectListener(Minigame minigame) {
        this.minigame = minigame;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        e.getPlayer().teleport(ConfigManager.getLobby());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        minigame.getArenaManager().leave(e.getPlayer());
    }
}
