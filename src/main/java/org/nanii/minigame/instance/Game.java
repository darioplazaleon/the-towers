package org.nanii.minigame.instance;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRespawnEvent;
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

    public void start() {
        arena.setState(GameState.LIVE);

        for (UUID id : arena.getPlayers()) {
            Player player = Bukkit.getPlayer(id);
            if (player == null) continue;

            Team team = arena.getTeam(id);
            
            player.teleport(arena.getTeamSpawn(team));
        }

        arena.startGenerators();

        arena.sendMessage(ChatColor.GREEN + "The game has started!");
    }

    private void end(Team winner) {

        arena.sendTitle(winner.getDisplay() + ChatColor.GOLD + "gana la partida!", "");
        arena.sendMessage(ChatColor.GOLD + "El equipo " + winner.getDisplay() + ChatColor.GOLD + " ha ganado la partida!");


        arena.reset(true);
    }
}
