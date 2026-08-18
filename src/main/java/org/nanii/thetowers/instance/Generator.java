package org.nanii.thetowers.instance;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;
import org.nanii.thetowers.TheTowers;

import java.util.List;

public class Generator {

    private final GeneratorType type;
    private final List<Location> locations;
    private final long interval;
    private final int amount;
    private BukkitTask task;

    public Generator(GeneratorType type, List<Location> locations, long interval, int amount) {
        this.type = type;
        this.locations = locations;
        this.interval = interval;
        this.amount = amount;
    }

    public void start(TheTowers plugin) {
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Location loc : locations) {
                type.spawn(loc, amount);
            }
        }, interval, interval);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}


