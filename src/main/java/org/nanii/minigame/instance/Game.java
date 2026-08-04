package org.nanii.minigame.instance;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitTask;
import org.nanii.minigame.GameState;
import org.nanii.minigame.manager.ConfigManager;
import org.nanii.minigame.tab.TabBoard;
import org.nanii.minigame.team.Team;

import javax.inject.Named;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Game {
    //GAME LOGIC

    private Arena arena;
    private final Map<Team, Integer> teamScores = new EnumMap<>(Team.class);
    private final Map<UUID, Integer> playerScores = new HashMap<>();

    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Map<UUID, Integer> deaths = new HashMap<>();

    private final TabBoard board = new TabBoard();

    private long startTime;
    private BukkitTask tabListTask;

    public Game(Arena arena)
    {
        this.arena = arena;
        for (Team t : Team.values()) {
            teamScores.put(t, 0);
        }
    }

    public void scorePoint(Player player, Team team) {
        teamScores.merge(team, 1, Integer::sum);
        playerScores.merge(player.getUniqueId(), 1, Integer::sum);

        player.teleport(arena.getTeamSpawn(team));

        arena.sendMessage(ChatColor.GREEN + player.getName() + " scored a point for " + team.name() + "! Total points: " + teamScores.get(team));

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

//    public void updateTabList() {
//        long secs = (System.currentTimeMillis() - startTime) / 1000L;
//        String time = String.format("%02d:%02d", secs / 60, secs % 60);
//
//        String header = ChatColor.GOLD + "" + ChatColor.BOLD + "TOWERS\n"
//                + ChatColor.GRAY + "Time: " + ChatColor.WHITE + time + "\n"
//                + Team.RED.getDisplay() + ChatColor.WHITE + " " + teamScores.get(Team.RED)
//                + ChatColor.GRAY + " - "
//                + ChatColor.WHITE + teamScores.get(Team.BLUE) + " " + Team.BLUE.getDisplay();
//
//        StringBuilder footer = new StringBuilder();
//        for (Team team : Team.values()) {
//            footer.append("\n").append(team.getDisplay());
//            for (UUID id : arena.getPlayers()) {
//                if (arena.getTeam(id) != team) continue;
//                Player p = Bukkit.getPlayer(id);
//                if (p == null) continue;
//                footer.append("\n").append(ChatColor.WHITE).append("  ").append(p.getName())
//                        .append(ChatColor.GRAY).append("  ")
//                        .append(ChatColor.GREEN).append(kills.getOrDefault(id, 0))
//                        .append(ChatColor.GRAY).append(" / ")
//                        .append(ChatColor.RED).append(deaths.getOrDefault(id, 0));
//            }
//        }
//
//        for (UUID id : arena.getPlayers()) {
//            Player p = Bukkit.getPlayer(id);
//            if (p != null) {
//                p.setPlayerListHeaderFooter(header, footer.toString());
//            }
//        }
//    }

    public void renderTab() {
        board.clearLines();

        renderColumn(0, Team.RED);
        renderColumn(1, Team.BLUE);

        long secs = (System.currentTimeMillis() - startTime) / 1000L;
        String time = String.format("%02d:%02d", secs / 60, secs % 60);

        Component header = Component.text()
                .append(Component.text("TOWERS", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(" · ", NamedTextColor.DARK_GRAY))
                .append(Component.text(time, NamedTextColor.WHITE))
                .build();

        for (UUID id : arena.getPlayers()) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;

            board.refresh(p);
            p.sendPlayerListHeader(header);
        }
    }

    private void renderColumn(int column, Team team) {
        board.set(column, 0, Component.text()
                .append(Component.text("● ", team.getColor()))
                .append(Component.text(team.name(), team.getColor(), TextDecoration.BOLD))
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

    public void start() {
        arena.setState(GameState.LIVE);

        for (UUID id : arena.getPlayers()) {
            Player player = Bukkit.getPlayer(id);
            if (player == null) continue;
            board.show(player);

            Team team = arena.getTeam(id);
            
            player.teleport(arena.getTeamSpawn(team));
        }

        arena.startGenerators();

        startTime = System.currentTimeMillis();
        tabListTask = Bukkit.getScheduler().runTaskTimer(
                arena.getMinigame(), this::renderTab, 0L, 20L
        );

        arena.sendMessage(ChatColor.GREEN + "The game has started!");
    }

    private void end(Team winner) {

        arena.sendTitle(winner.getDisplay() + ChatColor.GOLD + " gana la partida!", "");
        arena.sendMessage(ChatColor.GOLD + "El equipo " + winner.getDisplay() + ChatColor.GOLD + " ha ganado la partida!");

        arena.reset();
    }

    public void stop() {
        if (tabListTask != null) {
            tabListTask.cancel();
            tabListTask = null;
        }

        for (UUID id : arena.getPlayers()) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                player.setPlayerListHeaderFooter("", "");
            }
        }
    }
}
