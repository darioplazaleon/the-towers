package org.nanii.thetowers;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.nanii.thetowers.command.ArenaCommand;
import org.nanii.thetowers.gui.TeamSelectorItem;
import org.nanii.thetowers.instance.Arena;
import org.nanii.thetowers.lang.LangManager;
import org.nanii.thetowers.listener.*;
import org.nanii.thetowers.manager.ArenaManager;
import org.nanii.thetowers.manager.ConfigManager;
import org.nanii.thetowers.sign.ArenaSignManager;
import org.nanii.thetowers.stats.Database;

import java.sql.SQLException;

public final class TheTowers extends JavaPlugin {

    private ArenaManager arenaManager;
    private ArenaSignManager signManager;

    @Override
    public void onEnable() {
        ConfigManager.setupConfig(this);
        LangManager.load(this);

        TeamSelectorItem.setup(this);
        arenaManager = new ArenaManager(this);

        signManager = new ArenaSignManager(this);
        signManager.load();
        Bukkit.getScheduler().runTask(this, signManager::refreshAll);
        signManager.startCountdownTask();

        try {
            new Database(this).open();
        } catch (SQLException e) {
            getLogger().severe("Fallo" + e.getMessage());
        }

        Bukkit.getPluginManager().registerEvents(new GameListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ConnectListener(this), this);
        Bukkit.getPluginManager().registerEvents(new TeamSelectorListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new SignListener(this), this);

        ArenaCommand arenaCommand = new ArenaCommand(this);
        getCommand("arena").setExecutor(arenaCommand);
        getCommand("arena").setTabCompleter(arenaCommand);
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public ArenaSignManager getSignManager() {
        return signManager;
    }

    @Override
    public void onDisable() {
        LangManager.unload();

        if (signManager != null) signManager.shutdown();
        if (arenaManager == null) return;

        for (Arena arena : arenaManager.getArenas()) {
            arena.getGame().stop();
        }
    }
}
