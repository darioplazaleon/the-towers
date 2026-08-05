package org.nanii.minigame.listener;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
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
        if (arena == null || (arena.getState() != GameState.LIVE && arena.getState() != GameState.ENDING)) return;

        Team team = arena.getTeam(player.getUniqueId());
        if (team == null) return;

        e.setRespawnLocation(arena.getTeamSpawn(team));
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;

        Player attacker = resolveAttacker(e.getDamager());
        if (attacker == null) return;

        if (attacker.equals(victim)) return;

        Arena arena = minigame.getArenaManager().getArena(victim);
        if (arena == null) return;

        if (arena.getState() != GameState.LIVE && arena.getState() != GameState.ENDING) {
            e.setCancelled(true);
            return;
        }


        if (minigame.getArenaManager().getArena(attacker) != arena) return;

        Team victimTeam = arena.getTeam(victim.getUniqueId());
        Team attackerTeam = arena.getTeam(attacker.getUniqueId());
        if (victimTeam == null || attackerTeam == null) return;

        if (victimTeam == attackerTeam) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();

        Arena arena = minigame.getArenaManager().getArena(victim);
        if (arena == null || arena.getState() != GameState.LIVE) return;

        arena.getGame().addDeath(victim);

        Player killer = victim.getKiller();
        if (killer != null && !killer.equals(victim) && minigame.getArenaManager().getArena(killer) == arena) {
            arena.getGame().addKill(killer);
        }
    }


    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player p) {
            return p;
        }

        if (damager instanceof Projectile proj && proj.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }
}
