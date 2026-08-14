package org.nanii.minigame.instance;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.nanii.minigame.GameState;
import org.nanii.minigame.Minigame;
import org.nanii.minigame.manager.ConfigManager;

import java.time.Duration;

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
            arena.sendMessage(Component.translatable("countdown.cancelled"));
            arena.reset();
            return;
        }

        if (secondsLeft == 0) {
            cancel();
            arena.prepareTeams();   // ← autoassign + balance before start
            arena.start();
            arena.clearTitles();
            return;
        }

        String key = secondsLeft == 1 ? "coundown.title.one" : "countdown.title.other";

        arena.showTitle(
                Component.translatable(key, Argument.numeric("seconds", secondsLeft)),
                Component.translatable("countdown.subtitle"),
                Duration.ZERO,
                Duration.ofMillis(1500),
                Duration.ZERO
        );
        secondsLeft--;
    }

    public int getSecondsLeft() {
        return secondsLeft;
    }
}