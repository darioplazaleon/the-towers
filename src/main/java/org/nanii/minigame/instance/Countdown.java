package org.nanii.minigame.instance;

import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;
import org.nanii.minigame.GameState;
import org.nanii.minigame.Minigame;
import org.nanii.minigame.manager.ConfigManager;

public class Countdown extends BukkitRunnable {
    private Minigame minigame;
    private Arena arena;
    private int countdownSeconds;

    public Countdown(Minigame minigame, Arena arena) {
        this.minigame = minigame;
        this.arena = arena;
        this.countdownSeconds = ConfigManager.getCountdownSeconds();
    }

    public void start() {
        arena.setState(GameState.COUNTDOWN);
        runTaskTimer(minigame, 0, 20); // Run every 20 ticks (1 second)
    }

    @Override
    public void run() {
        if  (arena.getPlayers().size() < ConfigManager.getRequiredPlayers()) {
            cancel();
            arena.sendMessage(ChatColor.RED + "Not enough players to start the game. Countdown stopped.");
            arena.reset();
            return;
        }

        if (countdownSeconds == 0) {
            cancel();
            arena.start();
            arena.sendTitle("","");
            return;
        }

        if (countdownSeconds <= 10 || countdownSeconds % 15 == 0) {
            arena.sendMessage(ChatColor.GREEN + "Game will start in " + countdownSeconds + " second" + (countdownSeconds == 1 ? "" : "s") + ".");
        }

        arena.sendTitle(ChatColor.GREEN.toString() + countdownSeconds + " second" + (countdownSeconds == 1 ? "" : "s"), ChatColor.GRAY + "until start");

        countdownSeconds--;
    }
}