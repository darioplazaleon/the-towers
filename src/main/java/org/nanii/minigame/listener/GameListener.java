package org.nanii.minigame.listener;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.nanii.minigame.GameState;
import org.nanii.minigame.Minigame;
import org.nanii.minigame.instance.Arena;
import org.nanii.minigame.instance.PointZone;
import org.nanii.minigame.team.Team;

public class GameListener implements Listener {

    private final Minigame minigame;

    public GameListener(Minigame minigame) {
        this.minigame = minigame;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Location to = e.getTo();

        if (e.getFrom().getBlockX() == to.getBlockX()
                && e.getFrom().getBlockY() == to.getBlockY()
                && e.getFrom().getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = e.getPlayer();
        Arena arena = minigame.getArenaManager().getArena(player);
        if (arena == null || arena.getState() != GameState.LIVE) return;

        Team team = arena.getTeam(player.getUniqueId());
        if (team == null) return;

        PointZone zone = arena.getScoringZone(team);
        if (zone != null && zone.contains(to)) {
            arena.getGame().scorePoint(player, team);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();

        Arena arena = minigame.getArenaManager().getArena(player);
        if (arena == null || arena.getState() != GameState.LIVE) return;

        Team team = arena.getTeam(player.getUniqueId());
        if (team != null) return;

        e.setRespawnLocation(arena.getTeamSpawn(team));
    }
}
