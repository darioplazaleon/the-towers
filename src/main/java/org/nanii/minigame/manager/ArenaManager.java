package org.nanii.minigame.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.nanii.minigame.instance.Arena;
import org.nanii.minigame.Minigame;

import java.util.ArrayList;
import java.util.List;

public class ArenaManager {

    private List<Arena> arenas = new ArrayList<>();

    public ArenaManager(Minigame minigame) {
        for (String key : ConfigManager.getArenaIds()) {
            int id = Integer.parseInt(key);

            String worldName = ConfigManager.getArenaWorld(id);
            World world = Bukkit.createWorld(new WorldCreator(worldName));
            world.setAutoSave(false);

            arenas.add(new Arena(
                    minigame,
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
            if (arena.getPlayers().contains(player.getUniqueId())) {
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
}
