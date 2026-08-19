package org.nanii.thetowers.stats;

import org.bukkit.Bukkit;
import org.nanii.thetowers.TheTowers;
import org.nanii.thetowers.manager.ConfigManager;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;

public class StatsService {

    @FunctionalInterface
    private interface SqlTask{
        void run() throws SQLException;
    }

    @FunctionalInterface
    private interface SQlQuery<T> {
        T run() throws SQLException;
    }

    private final TheTowers plugin;
    private final Database database;
    private final StatsRepository repository;
    private final ExecutorService executor;

    private volatile boolean active;

    public StatsService(TheTowers plugin) {
        this.plugin = plugin;
        this.database = new Database(plugin);
        this.repository = new StatsRepository(database);
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "TheTowers-DB");
            thread.setDaemon(false);
            return thread;
        });
    }

    public void start() {
        if (!ConfigManager.isStatsEnabled()) {
            plugin.getLogger().info("Stats disabled by config.");
            return;
        }

        try {
            database.open();
            active = true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Cant open database. Stats disabled.", e);
        }
    }

    public boolean isActive() {
        return active;
    }

    public void recordMatch(MatchRecord match) {
        if (!active || match.players().isEmpty()) return;
        submit(() -> repository.saveMatch(match), "save the match");
    }

    public void touchPlayer(UUID uuid, String name) {
        if (!active) return;
        submit(() -> repository.touchPlayer(uuid, name), "register player " + name);
    }

    public void stats(UUID uuid, Consumer<PlayerStats> callback) {
        if (!active) {
            callback.accept(PlayerStats.empty());
            return;
        }
        query(() -> repository.findStats(uuid), callback, "read stats");
    }

    public void top(TopType type, int limit, Consumer<List<TopEntry>> callback) {
        if (!active) {
            callback.accept(List.of());
            return;
        }
        query(() -> repository.findTop(type, limit), callback, "read rank");
    }

    public void resolveUuid(String name, Consumer<UUID> callback) {
        if (!active) {
            callback.accept(null);
            return;
        }
        query(() -> repository.findUuidByName(name), callback, "search uuid from " + name);
    }

    public void shutdown() {
        executor.shutdown();

        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Unfinished records remained when the database was closed.");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        database.close();
        active = false;
    }

    private void submit(SqlTask task, String what) {
        executor.execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error: " + what, e);
            }
        });
    }

    private <T> void query(SQlQuery<T> sQlQuery, Consumer<T> callback, String what) {
        executor.execute(() -> {
            try {
                T result = sQlQuery.run();

                if (plugin.isEnabled()) {
                    Bukkit.getScheduler().runTask(plugin, () -> callback.accept(result));
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error when " + what, e);
            }
        });
    }
}
