package org.nanii.minigame.sign;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.nanii.minigame.Minigame;
import org.nanii.minigame.instance.Arena;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;

public class ArenaSignManager {
    private final Minigame minigame;
    private final Map<SignKey, Integer> signs = new HashMap<>();
    private File file;
    private YamlConfiguration config;

    public ArenaSignManager(Minigame minigame) {
        this.minigame = minigame;
        this.file = new File(minigame.getDataFolder(), "signs.yml");
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
                minigame.getLogger().warning("[TT Signs] Cartel '" + key + "' sin mundo, se descarta.");
                continue;
            }

            int arenaId = entry.getInt("arena", -1);
            if (minigame.getArenaManager().getArena(arenaId) == null) {
                minigame.getLogger().warning("[TT Signs] Cartel '" + key + "' apunta a la arena " + arenaId + ", que no existe. Se descarta.");
                continue;
            }

            signs.put(new SignKey(world, entry.getInt("x"), entry.getInt("y"), entry.getInt("z")), arenaId);
        }

        minigame.getLogger().info("[TT Signs] " + signs.size() + " cartel(es) cargado(s).");
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
            minigame.getLogger().log(Level.SEVERE, "[TT Signs] No se pudo guardar signs.yml", e);
        }
    }

    //REGISTER
    public void register(Block block, int arenaId) {
        signs.put(SignKey.of(block), arenaId);
        save();

        Arena arena = minigame.getArenaManager().getArena(arenaId);
        minigame.getServer().getScheduler().runTask(minigame, () -> refresh(arena));
    }

    public void unregister(Block block) {
        if (signs.remove(SignKey.of(block)) != null) {
            save();
        }
    }

    public Integer getArenaId(Block block) {
        return signs.get(SignKey.of(block));
    }

    //RENDER
    public void refreshAll() {
        for (Arena arena : minigame.getArenaManager().getArenas()) {
            refresh(arena);
        }
    }

    public void refresh(Arena arena) {
        Iterator<Map.Entry<SignKey, Integer>> iterator = signs.entrySet().iterator();
        boolean removed = false;

        while (iterator.hasNext()) {
            Map.Entry<SignKey, Integer> entry = iterator.next();
            if (entry.getValue() != arena.getId()) continue;

            if (!render(entry.getKey(), arena)) {
                iterator.remove();
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
        side.line(0, Component.text("TOWERS", NamedTextColor.GOLD, TextDecoration.BOLD));
        side.line(1, Component.text("Arena " + arena.getId(), NamedTextColor.WHITE));
        side.line(2, statusLine(arena));
        side.line(3, Component.text(arena.getPlayers().size() + "/" + arena.getMaxPlayers(), NamedTextColor.GRAY));

        sign.setWaxed(true);
        sign.update();
        return true;
    }

    private Component statusLine(Arena arena) {
        return switch (arena.getState()) {
            case RECRUITING -> Component.text("Esperando", NamedTextColor.GREEN);
            case COUNTDOWN -> Component.text("Iniciando...", NamedTextColor.YELLOW);
            case LIVE -> Component.text("En partida", NamedTextColor.RED);
            case ENDING -> Component.text("Terminando", NamedTextColor.GOLD);
            case RESETTING -> Component.text("Reiniciando", NamedTextColor.DARK_GRAY);
        };
    }


}
