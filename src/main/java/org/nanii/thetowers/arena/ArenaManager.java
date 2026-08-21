package org.nanii.thetowers.arena;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.nanii.thetowers.TheTowers;
import org.nanii.thetowers.config.ConfigManager;

import java.util.ArrayList;
import java.util.List;

public class ArenaManager {

    private List<Arena> arenas = new ArrayList<>();

    public ArenaManager(TheTowers theTowers) {
        for (String key : ConfigManager.getArenaIds()) {
            int id = Integer.parseInt(key);

            String worldName = ConfigManager.getArenaWorld(id);
            World world = Bukkit.createWorld(new WorldCreator(worldName));
            world.setAutoSave(false);

            arenas.add(new Arena(
                    theTowers,
                    id,
                    worldName,
                    ConfigManager.getWaitRoom(id),
                    ConfigManager.getTeamSpawn(id, "blue"),
                    ConfigManager.getTeamSpawn(id, "red"),
                    ConfigManager.getPointZones(id, "blue"),
                    ConfigManager.getPointZones(id, "red")
            ));
        }
    }

    public List<Arena> getArenas() {
        return arenas;
    }

    public Arena getArena(Player player) {
        for (Arena arena : arenas) {
            if (arena.contains(player.getUniqueId())) {
                return arena;
            }
        }

        return null;
    }

    public Arena getArena(int id) {
        for (Arena arena : arenas) {
            if (arena.getId() == id) {
                return arena;
            }
        }

        return null;
    }

    public ArenaJoinResult join(Player player, Arena arena) {
        if (arena == null) return ArenaJoinResult.ARENA_NOT_FOUND;
        if (getArena(player) != null) {
            return ArenaJoinResult.ALREADY_IN_ARENA;
        }
        if (arena.getState() != GameState.RECRUITING && arena.getState() != GameState.COUNTDOWN) {
            return ArenaJoinResult.IN_PROGRESS;
        }
        if (arena.getPlayers().size() >= arena.getMaxPlayers()) {
            return ArenaJoinResult.FULL;
        }

        arena.addPlayer(player);
        return ArenaJoinResult.OK;
    }

    public SpectateResult spectate(Player player, Arena arena) {
        if (arena == null) return SpectateResult.ARENA_NOT_FOUND;
        if (getArena(player) != null) {
            return SpectateResult.ALREADY_IN_ARENA;
        }
        if (arena.getState() == GameState.RESETTING) {
            return SpectateResult.NOT_AVAILABLE;
        }
        if (arena.getSpectators().size() >= arena.getMaxSpectators()) {
            return SpectateResult.FULL;
        }

        arena.addSpectator(player);
        return SpectateResult.OK;
    }


    public boolean leave(Player player) {
        Arena arena = getArena(player);
        if (arena == null) return false;

        if (arena.isSpectator(player.getUniqueId())) {
            arena.removeSpectator(player);
        } else {
            arena.removePlayer(player);

        }
        return true;
    }

}
