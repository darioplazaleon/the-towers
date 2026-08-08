package org.nanii.minigame.instance;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitTask;
import org.nanii.minigame.GameState;
import org.nanii.minigame.Minigame;
import org.nanii.minigame.manager.ConfigManager;

public class Countdown {

    private Minigame minigame;
    private Arena arena;

    private int secondsLeft;
    private BukkitTask task;

    public Countdown(Minigame minigame, Arena arena) {
        this.minigame = minigame;
        this.arena = arena;
        this.secondsLeft = ConfigManager.getCountdownSeconds();
    }

    public void start() {
        if (task != null) return;
        arena.setState(GameState.COUNTDOWN);
        task = Bukkit.getScheduler().runTaskTimer(minigame, this::tick, 0L, 20L); // Run every 20 ticks (1 second)
    }

    public void cancel() {
        if (task == null) return;
        task.cancel();
        task = null;
    }

    private void tick() {
        if (arena.getPlayers().size() < ConfigManager.getRequiredPlayers()) {
            cancel();
            arena.sendMessage(ChatColor.RED + "No hay suficientes jugadores. Cuenta regresiva cancelada.");
            arena.reset();
            return;
        }

        if (secondsLeft == 0) {
            cancel();
            arena.prepareTeams();   // ← autoassign + balance before start
            arena.start();
            arena.sendTitle("", "");
            return;
        }

        if (secondsLeft <= 10 || secondsLeft % 15 == 0) {
            arena.sendMessage(ChatColor.GREEN + "El juego empieza en " + secondsLeft
                    + " segundo" + (secondsLeft == 1 ? "" : "s") + ".");
        }

        arena.sendTitle(ChatColor.GREEN.toString() + secondsLeft + " segundo" + (secondsLeft == 1 ? "" : "s"),
                ChatColor.GRAY + "para empezar");

        secondsLeft--;
    }

    public int getSecondsLeft() {
        return secondsLeft;
    }
}