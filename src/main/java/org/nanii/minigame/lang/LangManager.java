package org.nanii.minigame.lang;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.nanii.minigame.Minigame;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LangManager {

    private static final Key STORE_KEY = Key.key("minigame", "messages");
    private static final String FILE_NAME = "messages.yml";

    private static MiniMessageTranslationStore store;

    private LangManager() {
    }

    public static void load(Minigame plugin) {
        Map<String, String> messages = new HashMap<>();

        InputStream in = plugin.getResource(FILE_NAME);
        if (in == null) {
            plugin.getLogger().severe(FILE_NAME + " no esta dentro del jar");
        } else {
            try (Reader defaults = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    flattenInto(YamlConfiguration.loadConfiguration(defaults), messages);
            } catch (IOException e) {
                plugin.getLogger().warning("No se pudieron leer los mensajes por defecto: " + e.getMessage());
            }
        }

        // Admin file
        File file =  new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) plugin.saveResource(FILE_NAME, false);
        flattenInto(YamlConfiguration.loadConfiguration(file), messages);

        MiniMessageTranslationStore newStore = MiniMessageTranslationStore.create(STORE_KEY);
        newStore.defaultLocale(Locale.ROOT);
        newStore.registerAll(Locale.ROOT, messages);

        if (store != null) GlobalTranslator.translator().removeSource(store);
        GlobalTranslator.translator().addSource(newStore);
        store = newStore;

        plugin.getLogger().info("Cargados " + messages.size() + " mensajes.");
    }

    public static void unload() {
        if (store == null) return;
        GlobalTranslator.translator().removeSource(store);
        store = null;
    }

    private static void flattenInto(FileConfiguration config, Map<String, String> out) {
        for (String key : config.getKeys(true)) {
            if (config.isString(key)) {
                out.put(key, config.getString(key));
            }
        }
    }
}
