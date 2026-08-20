package org.nanii.thetowers.config;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.nanii.thetowers.TheTowers;
import org.nanii.thetowers.instance.Generator;
import org.nanii.thetowers.instance.GeneratorType;
import org.nanii.thetowers.instance.PointZone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ConfigManager {

    private static FileConfiguration config;

    public static void setupConfig(TheTowers theTowers) {
        theTowers.saveDefaultConfig();
        ConfigManager.config = theTowers.getConfig();
    }

    public static String getLanguage() {
        return config.getString("language", "es");
    }

    public static boolean isStatsEnabled() {
        return config.getBoolean("stats.enabled", true);
    }

    public static int getMinGamesForTop() {
        return config.getInt("stats.min-games-for-top", 10);
    }

    public static int getRequiredPoints() {
        return config.getInt("required-points");
    }

    public static int getRequiredPlayers() {
        return config.getInt("required-players");
    }

    public static int getCountdownSeconds() {
        return config.getInt("countdown-seconds");
    }

    public static int getEndDelaySeconds() {
        return config.getInt("end-delay-seconds", 10);
    }

    public static int getTeamSize() {
        return config.getInt("team-size", 5);
    }

    public static int getMaxTeamDifference() {
        return config.getInt("max-team-difference", 1);
    }

    public static int getMaxSpectators() {
        return config.getInt("max-spectators", 2);
    }

    public static Location getLobby() {
        return getLocation("lobby");
    }

    public static List<Generator> getGenerators(int arenaId) {
        List<Generator> generators = new ArrayList<>();

        String base = "arenas." + arenaId + ".generators";
        ConfigurationSection section = config.getConfigurationSection(base);
        if (section == null) return generators;

        for (String typeKey : section.getKeys(false)) {
            GeneratorType type = GeneratorType.valueOf(typeKey.toUpperCase());
            String path = base + "." + typeKey;
            long interval = config.getLong(path + ".interval");
            int amount = config.getInt(path + ".amount");

            List<Location> locations = new ArrayList<>();
            ConfigurationSection locs = config.getConfigurationSection(path + ".locations");
            for (String k : locs.getKeys(false)) {
                locations.add(getLocation(path + ".locations." + k));
            }
            generators.add(new Generator(type, locations, interval, amount));
        }
        return generators;
    }

    // Datos Arena

    public static String getArenaWorld(int arenaId) {
        return config.getString("arenas." + arenaId + ".world");
    }

    public static Set<String> getArenaIds() {
        ConfigurationSection section = config.getConfigurationSection("arenas");
        if (section == null) {
            Bukkit.getLogger().warning("[Towers] No hay seccion 'arenas' en el config");
            return Collections.emptySet();
        }
        return section.getKeys(false);
    }

    public static Location getWaitRoom(int arenaId) {
        return getLocation("arenas." + arenaId + ".wait-room");
    }

    public static Location getTeamSpawn(int arenaId, String team) {
        return getLocation("arenas." + arenaId + ".team-spawns." + team);
    }

    public static PointZone getPointZones(int arenaId, String key) {
        String path = "arenas." + arenaId + ".point-zones." + key;

        String worldName = config.getString(path + ".world");
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            Bukkit.getLogger().warning("[Towers] Mundo '" + worldName + "' no encontrado para la zona " + key + " (arena: " + arenaId + ")");
            return null;
        }

        return new PointZone(world, config.getDouble(path + ".x"), config.getDouble(path + ".y"), config.getDouble(path + ".z"), config.getInt(path + ".radius"), config.getInt(path + ".height"));
    }

    // Helper para locations
    private static Location getLocation(String path) {
        return new Location(
                Bukkit.getWorld(config.getString(path + ".world")),
                config.getDouble(path + ".x"),
                config.getDouble(path + ".y"),
                config.getDouble(path + ".z"),
                (float) config.getDouble(path + ".yaw"),
                (float) config.getDouble(path + ".pitch")
        );
    }
}
