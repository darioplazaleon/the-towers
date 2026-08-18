package org.nanii.thetowers.lang;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.nanii.thetowers.TheTowers;
import org.nanii.thetowers.manager.ConfigManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LangManager {

    private static final Key STORE_KEY = Key.key("thetowers", "messages");
    private static final List<String> LANGUAGES = List.of("es", "en");

    private static MiniMessageTranslationStore store;
    private static Locale defaultLocale = Locale.of("es");

    private LangManager() {
    }

    public static int load(TheTowers plugin) {
        String configured = ConfigManager.getLanguage();
        if (!LANGUAGES.contains(configured)) {
            plugin.getLogger().warning("Idioma '" + configured + "' desconocido, se usa 'es'.");
            configured = "es";
        }
        defaultLocale = Locale.of(configured);

        MiniMessageTranslationStore newStore = MiniMessageTranslationStore.create(STORE_KEY);
        newStore.defaultLocale(defaultLocale);

        int total = 0;
        for (String language : LANGUAGES) {
            Map<String, String> messages = readMessages(plugin, language);
            newStore.registerAll(Locale.of(language), messages);
            total += messages.size();
        }

        if (store != null) GlobalTranslator.translator().removeSource(store);
        GlobalTranslator.translator().addSource(newStore);
        store = newStore;

        plugin.getLogger().info("Cargados " + total + " mensajes en " + LANGUAGES.size() + " idiomas.");
        return total;
    }

    private static Map<String, String> readMessages(TheTowers plugin, String language) {
        String path = "lang/" + language + ".yml";
        Map<String, String> messages = new HashMap<>();

        InputStream in = plugin.getResource(path);
        if (in == null) {
            plugin.getLogger().severe(path + " no esta dentro del jar");
        } else {
            try (Reader defaults = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                flattenInto(YamlConfiguration.loadConfiguration(defaults), messages);
            } catch (IOException e) {
                plugin.getLogger().warning("No se pudo leer " + path + ": " + e.getMessage());
            }
        }

        File file = new File(plugin.getDataFolder(), path);
        if (!file.exists()) plugin.saveResource(path, false);
        flattenInto(YamlConfiguration.loadConfiguration(file), messages);

        return messages;
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

    public static Component render(Component component) {
        return GlobalTranslator.render(component, Locale.ROOT);
    }

    public static Component render(Component component, Locale locale) {
        return GlobalTranslator.render(component, locale);
    }
}
