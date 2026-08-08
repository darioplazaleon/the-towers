package org.nanii.minigame;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.nanii.minigame.command.ArenaCommand;
import org.nanii.minigame.gui.TeamSelectorItem;
import org.nanii.minigame.instance.Arena;
import org.nanii.minigame.listener.*;
import org.nanii.minigame.manager.ArenaManager;
import org.nanii.minigame.manager.ConfigManager;
import org.nanii.minigame.sign.ArenaSignManager;

public final class Minigame extends JavaPlugin {

    private ArenaManager arenaManager;
    private ArenaSignManager signManager;

    @Override
    public void onEnable() {
        ConfigManager.setupConfig(this);
        TeamSelectorItem.setup(this);
        arenaManager = new ArenaManager(this);

        signManager = new ArenaSignManager(this);
        signManager.load();
        Bukkit.getScheduler().runTask(this, signManager::refreshAll);
        signManager.startCountdownTask();

        Bukkit.getPluginManager().registerEvents(new GameListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ConnectListener(this), this);
        Bukkit.getPluginManager().registerEvents(new TeamSelectorListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new SignListener(this), this);

        getCommand("arena").setExecutor(new ArenaCommand(this));
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public ArenaSignManager getSignManager() {
        return signManager;
    }

    @Override
    public void onDisable() {
        if (signManager != null) signManager.shutdown();
        if (arenaManager == null) return;

        for (Arena arena : arenaManager.getArenas()) {
            arena.getGame().stop();
        }
    }
}
