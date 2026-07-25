package org.nanii.minigame.instance;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitTask;
import org.nanii.minigame.GameState;
import org.nanii.minigame.manager.ConfigManager;
import org.nanii.minigame.team.Team;

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

    public void updateTabList() {
        long secs = (System.currentTimeMillis() - startTime) / 1000L;
        String time = String.format("%02d:%02d", secs / 60, secs % 60);

        String header = ChatColor.GOLD + "" + ChatColor.BOLD + "TOWERS\n"
                + ChatColor.GRAY + "Time: " + ChatColor.WHITE + time + "\n"
                + Team.RED.getDisplay() + ChatColor.WHITE + " " + teamScores.get(Team.RED)
                + ChatColor.GRAY + " - "
                + ChatColor.WHITE + teamScores.get(Team.BLUE) + " " + Team.BLUE.getDisplay();

        StringBuilder footer = new StringBuilder();
        for (Team team : Team.values()) {
            footer.append("\n").append(team.getDisplay());
            for (UUID id : arena.getPlayers()) {
                if (arena.getTeam(id) != team) continue;
                Player p = Bukkit.getPlayer(id);
                if (p == null) continue;
                footer.append("\n").append(ChatColor.WHITE).append("  ").append(p.getName())
                        .append(ChatColor.GRAY).append("  ")
                        .append(ChatColor.GREEN).append(kills.getOrDefault(id, 0))
                        .append(ChatColor.GRAY).append(" / ")
                        .append(ChatColor.RED).append(deaths.getOrDefault(id, 0));
            }
        }

        for (UUID id : arena.getPlayers()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                p.setPlayerListHeaderFooter(header, footer.toString());
            }
        }

    }


    public void start() {
        arena.setState(GameState.LIVE);

        for (UUID id : arena.getPlayers()) {
            Player player = Bukkit.getPlayer(id);
            if (player == null) continue;

            Team team = arena.getTeam(id);
            
            player.teleport(arena.getTeamSpawn(team));
        }

        arena.startGenerators();

        startTime = System.currentTimeMillis();
        tabListTask = Bukkit.getScheduler().runTaskTimer(
                arena.getMinigame(), this::updateTabList, 0L, 20L
        );

        arena.sendMessage(ChatColor.GREEN + "The game has started!");
    }

    private void end(Team winner) {

        arena.sendTitle(winner.getDisplay() + ChatColor.GOLD + "gana la partida!", "");
        arena.sendMessage(ChatColor.GOLD + "El equipo " + winner.getDisplay() + ChatColor.GOLD + " ha ganado la partida!");


        arena.reset(true);
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
