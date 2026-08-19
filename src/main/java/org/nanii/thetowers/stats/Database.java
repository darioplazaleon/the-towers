package org.nanii.thetowers.stats;

import org.nanii.thetowers.TheTowers;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.logging.Level;

public class Database {

    private static final List<String> SCHEMA = List.of("""
                    CREATE TABLE IF NOT EXISTS players(
                        uuid TEXT PRIMARY KEY,
                        last_name TEXT NOT NULL,
                        first_seen INTEGER NOT NULL,
                        last_seen INTEGER NOT NULL
                    )
                    """, """
                    CREATE TABLE IF NOT EXISTS matches (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        arena_id INTEGER NOT NULL,
                        started_at INTEGER NOT NULL,
                        ended_at INTEGER NOT NULL,
                        duration_s INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        winner_team TEXT,
                        red_score INTEGER NOT NULL,
                        blue_score INTEGER NOT NULL
                    )
                    """, """
                    CREATE TABLE IF NOT EXISTS match_players (
                        match_id INTEGER NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
                        player_uuid TEXT NOT NULL REFERENCES players(uuid),
                        team TEXT NOT NULL,
                        result TEXT NOT NULL,
                        kills INTEGER NOT NULL DEFAULT 0,
                        deaths INTEGER NOT NULL DEFAULT 0,
                        points INTEGER NOT NULL DEFAULT 0,
                        playtime_s INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY (match_id, player_uuid)
                    )
                    """, "CREATE INDEX IF NOT EXISTS idx_mp_player ON match_players(player_uuid)",
            "CREATE INDEX IF NOT EXISTS idx_matches_ended ON matches(ended_at)"
    );

    private final TheTowers plugin;
    private Connection connection;

    public Database(TheTowers plugin) {
        this.plugin = plugin;
    }

    public void open() throws SQLException {
        File folder = plugin.getDataFolder();
        if (!folder.exists() && !folder.mkdirs()) {
            throw new SQLException("Can't create folder " + folder.getAbsolutePath());
        }

        File file = new File(folder, "stats.db");
        boolean isNew = !file.exists();

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite driver not found. Check 'libraries' in plugin.yml", e);
        }

        connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA journal_mode = WAL");
            statement.execute("PRAGMA synchronous = NORMAL");

            for (String ddl : SCHEMA) {
                statement.execute(ddl);
            }
        }

        plugin.getLogger().info(isNew
                ? "Database created in " + file.getName()
                : "Database found from " + file.getName()
        );
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            throw new SQLException("Database is not open");
        }
        return connection;
    }

    public void close() {
        if (connection == null) return;

        try {
            connection.close();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Cannot close the database", e);
        }
        connection = null;
    }
}
