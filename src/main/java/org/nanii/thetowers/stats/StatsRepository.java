package org.nanii.thetowers.stats;

import org.nanii.thetowers.manager.ConfigManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StatsRepository {

    private static final String INSERT_MATCH = """
                INSERT INTO matches (arena_id, started_at, ended_at, duration_s, status, winner_team, red_score, blue_score)
                VALUES (?,?,?,?,?,?,?,?)
            """;

    private static final String UPSERT_PLAYER = """
                INSERT INTO players (uuid, last_name, first_seen, last_seen)
                VALUES (?,?,?,?)
                ON CONFLICT(uuid) DO UPDATE SET last_name = excluded.last_name, last_seen = excluded.last_seen
            """;

    private static final String INSERT_MATCH_PLAYER = """
                INSERT INTO match_players (match_id, player_uuid, team, result, kills, deaths, points, playtime_s)
                VALUES (?,?,?,?,?,?,?,?)
            """;

    private static final String SELECT_STATS = """
                SELECT
                    COALESCE(SUM(m.status = 'FINISHED' AND mp.result <> 'ABANDON'), 0) AS games,
                    COALESCE(SUM(mp.result = 'WIN'), 0) AS wins,
                    COALESCE(SUM(m.status = 'FINISHED' AND mp.result = 'LOSS'), 0) AS losses,
                    COALESCE(SUM(mp.result = 'ABANDON'), 0) AS abandons,
                    COALESCE(SUM(mp.kills), 0) AS kills,
                    COALESCE(SUM(mp.deaths), 0) AS deaths,
                    COALESCE(SUM(mp.points), 0) AS points,
                    COALESCE(SUM(mp.playtime_s), 0) AS playtime
                FROM match_players mp
                JOIN matches m ON m.id = mp.match_id
                WHERE mp.player_uuid = ?
            """;

    private static final String SELECT_UUID_BY_NAME =
            "SELECT uuid FROM players WHERE last_name = ? COLLATE NOCASE LIMIT 1";

    private static final String SELECT_TOP = """
                SELECT p.last_name AS name, %s AS value
                FROM match_players mp
                JOIN matches m ON m.id = mp.match_id
                JOIN players p ON p.uuid = mp.player_uuid
                WHERE m.status = 'FINISHED' AND mp.result <> 'ABANDON'
                GROUP BY mp.player_uuid
                HAVING COUNT(*) >= ?
                ORDER BY value DESC
                LIMIT ?
            """;

    private final Database database;

    public StatsRepository(Database database) {
        this.database = database;
    }

    public void saveMatch(MatchRecord match) throws SQLException {
        Connection connection = database.getConnection();
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);

        try {
            long matchId = insertMatch(connection, match);
            insertPlayers(connection, match, matchId);
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    private long insertMatch(Connection connection, MatchRecord match) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_MATCH, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, match.arenaId());
            statement.setLong(2, match.startedAt());
            statement.setLong(3, match.endedAt());
            statement.setLong(4, match.durationSeconds());
            statement.setString(5, match.status().name());
            statement.setString(6, match.winner() == null ? null : match.winner().name());
            statement.setInt(7, match.redScore());
            statement.setInt(8, match.blueScore());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("La base no devolvio el id de la partida");
                }
                return keys.getLong(1);
            }
        }
    }

    private void insertPlayers(Connection connection, MatchRecord match, long matchId) throws SQLException {
        try (PreparedStatement players = connection.prepareStatement(UPSERT_PLAYER);
             PreparedStatement rows = connection.prepareStatement(INSERT_MATCH_PLAYER)) {

            for (PlayerMatchRecord player : match.players()) {
                players.setString(1, player.uuid().toString());
                players.setString(2, player.name());
                players.setLong(3, match.endedAt());
                players.setLong(4, match.endedAt());
                players.addBatch();

                rows.setLong(1, matchId);
                rows.setString(2, player.uuid().toString());
                rows.setString(3, player.team().name());
                rows.setString(4, player.result().name());
                rows.setInt(5, player.kills());
                rows.setInt(6, player.deaths());
                rows.setInt(7, player.points());
                rows.setLong(8, player.playtimeSeconds());
                rows.addBatch();
            }

            players.executeBatch();
            rows.executeBatch();
        }
    }

    public void touchPlayer(UUID uuid, String name) throws SQLException {
        long now = System.currentTimeMillis();

        try (PreparedStatement statement = database.getConnection().prepareStatement(UPSERT_PLAYER)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, name);
            statement.setLong(3, now);
            statement.setLong(4, now);
            statement.executeUpdate();
        }
    }

    public PlayerStats findStats(UUID uuid) throws SQLException {
        try (PreparedStatement statement = database.getConnection().prepareStatement(SELECT_STATS)) {
            statement.setString(1, uuid.toString());

            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) return PlayerStats.empty();

                return new PlayerStats(
                        rs.getInt("games"),
                        rs.getInt("wins"),
                        rs.getInt("losses"),
                        rs.getInt("abandons"),
                        rs.getInt("kills"),
                        rs.getInt("deaths"),
                        rs.getInt("points"),
                        rs.getLong("playtime")
                );
            }
        }
    }

    public UUID findUuidByName(String name) throws SQLException {
        try (PreparedStatement statement = database.getConnection().prepareStatement(SELECT_UUID_BY_NAME)){
            statement.setString(1, name);

            try (ResultSet rs = statement.executeQuery()){
                return rs.next() ? UUID.fromString(rs.getString("uuid")) : null;
            }
        }
    }

    public List<TopEntry> findTop(TopType type, int limit) throws SQLException {
        String sql = SELECT_TOP.formatted(type.getExpression());
        int minGames = type == TopType.WINRATE ? ConfigManager.getMinGamesForTop() : 1;

        List<TopEntry> entries = new ArrayList<>();

        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setInt(1, minGames);
            statement.setInt(2, limit);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    entries.add(new TopEntry(rs.getString("name"), rs.getDouble("value")));
                }
            }
        }

        return entries;
    }
}
