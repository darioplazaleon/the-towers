package org.nanii.thetowers.sign;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;
import org.nanii.thetowers.GameState;
import org.nanii.thetowers.TheTowers;
import org.nanii.thetowers.arena.Arena;
import org.nanii.thetowers.lang.LangManager;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;

public class ArenaSignManager {

    private final TheTowers theTowers;
    private final Map<SignKey, Integer> signs = new HashMap<>();
    private File file;
    private YamlConfiguration config;

    private BukkitTask countdownTask;

    public ArenaSignManager(TheTowers theTowers) {
        this.theTowers = theTowers;
        this.file = new File(theTowers.getDataFolder(), "signs.yml");
    }

    public void load() {
        signs.clear();
        config = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection section = config.getConfigurationSection("signs");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) continue;

            String world = entry.getString("world");
            if (world == null) {
                theTowers.getLogger().warning("[TT Signs] Cartel '" + key + "' sin mundo, se descarta.");
                continue;
            }

            int arenaId = entry.getInt("arena", -1);
            if (theTowers.getArenaManager().getArena(arenaId) == null) {
                theTowers.getLogger().warning("[TT Signs] Cartel '" + key + "' apunta a la arena " + arenaId + ", que no existe. Se descarta.");
                continue;
            }


            signs.put(new SignKey(world, entry.getInt("x"), entry.getInt("y"), entry.getInt("z")), arenaId);
        }

        for (SignKey key : signs.keySet()) {
            addTicket(key);
        }

        theTowers.getLogger().info("[TT Signs] " + signs.size() + " cartel(es) cargado(s).");
    }

    public void save() {
        if (config == null) config = new YamlConfiguration();
        config.set("signs", null);

        int index = 0;
        for (Map.Entry<SignKey, Integer> register : signs.entrySet()) {
            SignKey key = register.getKey();
            String path = "signs." + index++;

            config.set(path + ".world", key.world());
            config.set(path + ".x", key.x());
            config.set(path + ".y", key.y());
            config.set(path + ".z", key.z());
            config.set(path + ".arena", register.getValue());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            theTowers.getLogger().log(Level.SEVERE, "[TT Signs] No se pudo guardar signs.yml", e);
        }
    }

    //REGISTER
    public void register(Block block, int arenaId) {
        SignKey key = SignKey.of(block);
        signs.put(key, arenaId);
        addTicket(key);
        save();

        Arena arena = theTowers.getArenaManager().getArena(arenaId);
        theTowers.getServer().getScheduler().runTask(theTowers, () -> refresh(arena));
    }

    public void unregister(Block block) {
        SignKey key = SignKey.of(block);
        if (signs.remove(key) != null) {
            removeTicket(key);
            save();
        }
    }

    public Integer getArenaId(Block block) {
        return signs.get(SignKey.of(block));
    }

    //RENDER
    public void refreshAll() {
        for (Arena arena : theTowers.getArenaManager().getArenas()) {
            refresh(arena);
        }
    }

    public void refresh(Arena arena) {
        Iterator<Map.Entry<SignKey, Integer>> iterator = signs.entrySet().iterator();
        boolean removed = false;

        while (iterator.hasNext()) {
            Map.Entry<SignKey, Integer> entry = iterator.next();
            if (entry.getValue() != arena.getId()) continue;

            SignKey key = entry.getKey();
            if (!render(key, arena)) {
                iterator.remove();
                removeTicket(key);
                removed = true;
            }
        }

        if (removed) save();
    }

    private boolean render(SignKey key, Arena arena) {
        World world = Bukkit.getWorld(key.world());
        if (world == null) return true;

        if (!world.isChunkLoaded(key.x() >> 4, key.z() >> 4)) return true;

        Block block = world.getBlockAt(key.x(), key.y(), key.z());
        if (!(block.getState(false) instanceof Sign sign)) return false;

        SignSide side = sign.getSide(Side.FRONT);
        line(side, 0, Component.translatable("sign.title"));
        line(side, 1, Component.translatable("sign.arena", Argument.numeric("arena", arena.getId())));
        line(side, 2, statusLine(arena));
        line(side, 3, countLine(arena));

        sign.setWaxed(true);
        sign.update();
        return true;
    }

    private Component statusLine(Arena arena) {
        return switch (arena.getState()) {
            case RECRUITING -> Component.translatable("sign.status.recruiting");
            case COUNTDOWN -> Component.translatable("sign.status.countdown",
                    Argument.numeric("seconds", arena.getCountdownSeconds()));
            case LIVE -> Component.translatable("sign.status.live");
            case ENDING -> Component.translatable("sign.status.ending");
            case RESETTING -> Component.translatable("sign.status.resetting");
        };
    }

    private Component countLine(Arena arena) {
        Component count = Component.translatable("sign.count",
                Argument.numeric("players", arena.getPlayers().size()),
                Argument.numeric("max", arena.getMaxPlayers()));

        int watching = arena.getSpectators().size();
        if (watching == 0) return count;

        return count.append(Component.translatable("sign.spectators",
                Argument.numeric("count", watching)));
    }


    public void startCountdownTask() {
        if (countdownTask != null) return;

        countdownTask = Bukkit.getScheduler().runTaskTimer(theTowers, () -> {
            for (Arena arena : theTowers.getArenaManager().getArenas()) {
                if (arena.getState() == GameState.COUNTDOWN) {
                    refresh(arena);
                }
            }
        }, 20L, 20L);
    }

    public void shutdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        save();
    }

    private void line(SignSide side, int index, Component component) {
        side.line(index, LangManager.render(component));
    }

    private void addTicket(SignKey key) {
        World world = Bukkit.getWorld(key.world());
        if (world == null) return;
        world.addPluginChunkTicket(key.x() >> 4, key.z() >> 4, theTowers);
    }

    private void removeTicket(SignKey key) {
        World world = Bukkit.getWorld(key.world());
        if (world == null) return;

        int cx = key.x() >> 4, cz = key.z() >> 4;

        for (SignKey other : signs.keySet()) {
            if  (other.world().equals(key.world()) && (other.x() >> 4) == cx && (other.z() >> 4) == cz) return;
        }
        world.removePluginChunkTicket(cx, cz, theTowers);
    }
}
