package org.nanii.thetowers.instance;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.nanii.thetowers.GameState;
import org.nanii.thetowers.gui.TeamSelectorItem;
import org.nanii.thetowers.config.ConfigManager;
import org.nanii.thetowers.stats.MatchRecord;
import org.nanii.thetowers.stats.MatchResult;
import org.nanii.thetowers.stats.MatchStatus;
import org.nanii.thetowers.stats.PlayerMatchRecord;
import org.nanii.thetowers.tab.TabBoard;
import org.nanii.thetowers.team.Team;

import java.time.Duration;
import java.util.*;

public class Game {

    private Arena arena;
    private final Map<Team, Integer> teamScores = new EnumMap<>(Team.class);
    private final Map<UUID, Integer> playerScores = new HashMap<>();

    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Map<UUID, Integer> deaths = new HashMap<>();

    private final TabBoard board = new TabBoard();

    private long startTime;
    private BukkitTask tabListTask;
    private BukkitTask endTask;
    private boolean tabActive;

    private final List<PlayerMatchRecord> abandoned = new ArrayList<>();
    private boolean persisted;

    public Game(Arena arena) {
        this.arena = arena;
        for (Team t : Team.values()) {
            teamScores.put(t, 0);
        }
    }

    public void scorePoint(Player player, Team team) {
        teamScores.merge(team, 1, Integer::sum);
        playerScores.merge(player.getUniqueId(), 1, Integer::sum);

        player.teleport(arena.getTeamSpawn(team));

        arena.sendMessage(Component.translatable("game.score",
                Argument.component("player", player.name()),
                Argument.component("team", team.displayName())));

        if (teamScores.get(team) >= ConfigManager.getRequiredPoints()) {
            end(team);
        }
    }

    public void addKill(Player p) {
        kills.merge(p.getUniqueId(), 1, Integer::sum);
    }

    public void addDeath(Player p) {
        deaths.merge(p.getUniqueId(), 1, Integer::sum);
    }

    public void renderTab() {
        board.clearLines();

        renderColumn(0, Team.RED);
        renderColumn(1, Team.BLUE);

        boolean gridChanged = board.isDirty();

        long secs = (System.currentTimeMillis() - startTime) / 1000L;
        String time = String.format("%02d:%02d", secs / 60, secs % 60);

        Component header = Component.translatable("game.tab.header", Argument.string("time", time));

        for (Player p : arena.getOnlineMembers()) {
            if (gridChanged) board.refresh(p);
            p.sendPlayerListHeader(header);
        }

        if (gridChanged) board.markClean();
    }

    private void renderColumn(int column, Team team) {
        board.set(column, 0, Component.text()
                .append(Component.text("● ", team.getColor()))
                .append(team.displayName().decorate(TextDecoration.BOLD))
                .append(Component.text("  " + teamScores.get(team), NamedTextColor.WHITE))
                .build()
        );

        board.set(column, 1, Component.text("─────────────", NamedTextColor.DARK_GRAY));

        int row = 2;
        for (UUID id : arena.getPlayers()) {
            if (row >= TabBoard.ROWS) break;
            if (arena.getTeam(id) != team) continue;

            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;

            board.set(column, row++, Component.text()
                    .append(Component.text(p.getName(), NamedTextColor.WHITE))
                    .append(Component.text(" " + kills.getOrDefault(id, 0), NamedTextColor.GREEN))
                    .append(Component.text("/", NamedTextColor.DARK_GRAY))
                    .append(Component.text(String.valueOf(deaths.getOrDefault(id, 0)), NamedTextColor.RED))
                    .build()
            );
        }
    }

    private void clearTab(Player viewer) {
        board.hide(viewer);
        for (Player online : Bukkit.getOnlinePlayers()) {
            board.showPlayer(viewer, online.getUniqueId());
        }
        viewer.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
    }

    public void addViewer(Player player) {
        if (!tabActive) return;
        board.show(player);
    }

    public void removeViewer(Player player) {
        if (tabActive) {
            clearTab(player);
        }
    }

    public void onPlayerLeft(Player player) {
        if (persisted) return;

        UUID id = player.getUniqueId();
        Team team = arena.getTeam(id);
        if (team == null) return;

        long playtime = (System.currentTimeMillis() - startTime) / 1000L;
        abandoned.add(snapshot(id, player.getName(), team, MatchResult.ABANDON, playtime));
    }

    public void flushAborted() {
        flush(MatchStatus.ABORTED, null);
    }

    private void flush(MatchStatus status, Team winner) {
        if (startTime == 0L) return;
        if (persisted) return;
        persisted = true;

        long now = System.currentTimeMillis();
        long duration = (now - startTime) / 1000L;

        List<PlayerMatchRecord> records = new ArrayList<>(abandoned);

        for (UUID id : arena.getPlayers()) {
            Player player = Bukkit.getPlayer(id);
            if (player == null) continue;

            Team team = arena.getTeam(id);
            if (team == null) continue;

            MatchResult result = status == MatchStatus.FINISHED
                    ? (team == winner ? MatchResult.WIN : MatchResult.LOSS)
                    : MatchResult.NONE;

            records.add(snapshot(id, player.getName(), team, result, duration));
        }

        if (records.isEmpty()) return;

        arena.getPlugin().getStatsService().recordMatch(new MatchRecord(
                arena.getId(),
                startTime,
                now,
                duration,
                status,
                winner,
                teamScores.getOrDefault(Team.RED, 0),
                teamScores.getOrDefault(Team.BLUE, 0),
                records
        ));
    }

    private PlayerMatchRecord snapshot(UUID id, String name, Team team, MatchResult result, long playtime) {
        return new PlayerMatchRecord(
                id,
                name,
                team,
                result,
                kills.getOrDefault(id, 0),
                deaths.getOrDefault(id, 0),
                playerScores.getOrDefault(id, 0),
                playtime
        );
    }

    public void start() {
        arena.setState(GameState.LIVE);
        tabActive = true;

        for (UUID id : arena.getPlayers()) {
            Player player = Bukkit.getPlayer(id);
            if (player == null) continue;

            player.closeInventory();
            TeamSelectorItem.remove(player);

            Team team = arena.getTeam(id);
            if (team == null) continue;

            board.show(player);
            player.teleport(arena.getTeamSpawn(team));
        }

        for (UUID id : arena.getSpectators()) {
            Player spectator = Bukkit.getPlayer(id);
            if (spectator != null) board.show(spectator);
        }

        arena.startGenerators();

        startTime = System.currentTimeMillis();
        tabListTask = Bukkit.getScheduler().runTaskTimer(
                arena.getPlugin(), this::renderTab, 0L, 20L
        );

        arena.sendMessage(Component.translatable("game.started"));
    }

    private void end(Team winner) {
        if (arena.getState() == GameState.ENDING) return;

        arena.setState(GameState.ENDING);
        arena.stopGenerators();

        flush(MatchStatus.FINISHED, winner);

        // Congelamos el tab con el marcador final
        if (tabListTask != null) {
            tabListTask.cancel();
            tabListTask = null;
        }

        int seconds = ConfigManager.getEndDelaySeconds();

        arena.showTitle(
                Component.translatable("game.end.title", Argument.component("team", winner.displayName())),
                Component.translatable("game.end.subtitle", Argument.numeric("seconds", seconds)),
                Duration.ofMillis(500),
                Duration.ofSeconds(seconds),
                Duration.ofSeconds(1)
        );
        arena.sendMessage(Component.translatable("game.end.broadcast", Argument.component("team", winner.displayName())));

        endTask = Bukkit.getScheduler().runTaskLater(arena.getPlugin(), arena::reset, seconds * 20L);
    }

    public void stop() {
        if (endTask != null) {
            endTask.cancel();
            endTask = null;
        }

        if (tabListTask != null) {
            tabListTask.cancel();
            tabListTask = null;
        }

        if (!tabActive) return;
        tabActive = false;

        for (Player player : arena.getOnlineMembers()) {
            clearTab(player);
        }
    }
}
