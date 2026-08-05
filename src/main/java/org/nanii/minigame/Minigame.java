package org.nanii.minigame;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.nanii.minigame.command.ArenaCommand;
import org.nanii.minigame.gui.TeamSelectorItem;
import org.nanii.minigame.instance.Arena;
import org.nanii.minigame.listener.ConnectListener;
import org.nanii.minigame.listener.GameListener;
import org.nanii.minigame.listener.TeamSelectorListener;
import org.nanii.minigame.manager.ArenaManager;
import org.nanii.minigame.manager.ConfigManager;

public final class Minigame extends JavaPlugin {

    private ArenaManager arenaManager;

    @Override
    public void onEnable() {
        ConfigManager.setupConfig(this);
        TeamSelectorItem.setup(this);
        arenaManager = new ArenaManager(this);

        Bukkit.getPluginManager().registerEvents(new GameListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ConnectListener(this), this);
        Bukkit.getPluginManager().registerEvents(new TeamSelectorListener(this), this);

        getCommand("arena").setExecutor(new ArenaCommand(this));
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    @Override
    public void onDisable() {
        if (arenaManager == null) return;

        for (Arena arena : arenaManager.getArenas()) {
            arena.getGame().stop();
        }
    }
}
