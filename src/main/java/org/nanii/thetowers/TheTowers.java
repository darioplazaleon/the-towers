package org.nanii.thetowers;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.nanii.thetowers.command.ArenaCommand;
import org.nanii.thetowers.command.StatsCommand;
import org.nanii.thetowers.gui.TeamSelectorItem;
import org.nanii.thetowers.arena.Arena;
import org.nanii.thetowers.lang.LangManager;
import org.nanii.thetowers.listener.*;
import org.nanii.thetowers.arena.ArenaManager;
import org.nanii.thetowers.config.ConfigManager;
import org.nanii.thetowers.sign.ArenaSignManager;
import org.nanii.thetowers.stats.StatsService;

public final class TheTowers extends JavaPlugin {

    private ArenaManager arenaManager;
    private ArenaSignManager signManager;
    private StatsService statsService;

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

        statsService = new StatsService(this);
        statsService.start();

        Bukkit.getPluginManager().registerEvents(new GameListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ConnectListener(this), this);
        Bukkit.getPluginManager().registerEvents(new TeamSelectorListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new SignListener(this), this);

        ArenaCommand arenaCommand = new ArenaCommand(this);
        getCommand("arena").setExecutor(arenaCommand);
        getCommand("arena").setTabCompleter(arenaCommand);
        StatsCommand statsCommand = new StatsCommand(this);
        getCommand("stats").setExecutor(statsCommand);
        getCommand("stats").setTabCompleter(statsCommand);
        getCommand("top").setExecutor(statsCommand);
        getCommand("top").setTabCompleter(statsCommand);
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public ArenaSignManager getSignManager() {
        return signManager;
    }

    public StatsService getStatsService() {
        return statsService;
    }

    @Override
    public void onDisable() {
        LangManager.unload();

        if (signManager != null) signManager.shutdown();
        if (arenaManager != null) {
            for (Arena arena : arenaManager.getArenas()) {
                arena.getGame().flushAborted();
                arena.getGame().stop();
            }
        }

        if (statsService != null) statsService.shutdown();
    }
}
