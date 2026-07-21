package org.nanii.minigame.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.nanii.minigame.Minigame;
import org.nanii.minigame.instance.Arena;
import org.nanii.minigame.manager.ConfigManager;

public class ConnectListener implements Listener {

    private final Minigame minigame;

    public ConnectListener(Minigame minigame) {
        this.minigame = minigame;
    }

    @EventHandler
    public void onJoin (PlayerJoinEvent e) {
        e.getPlayer().teleport(ConfigManager.getLobby());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Arena arena = minigame.getArenaManager().getArena(e.getPlayer());
        if (arena != null) {
            arena.removePlayer(e.getPlayer());
        }
    }
}
