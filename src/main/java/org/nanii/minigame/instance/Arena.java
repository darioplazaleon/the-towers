package org.nanii.minigame.instance;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.nanii.minigame.GameState;
import org.nanii.minigame.Minigame;
import org.nanii.minigame.manager.ConfigManager;
import org.nanii.minigame.team.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class Arena {

    private Minigame minigame;

    private int id;
    private Location waitRoom;

    private Location blueTeamSpawn;
    private Location redTeamSpawn;
    private PointZone blueScoreZone;
    private PointZone redScoreZone;

    private List<Generator> generators;

    private GameState state;
    private List<UUID> players;
    private Countdown countdown;
    private Game game;
    private HashMap<UUID, Team> teams;

    public Arena(Minigame minigame, int id, Location waitRoom, Location blueTeamSpawn,
                 Location redTeamSpawn, PointZone blueScoreZone, PointZone redScoreZone
    ) {
        this.minigame = minigame;
        this.id = id;

        this.waitRoom = waitRoom;
        this.countdown = new Countdown(minigame, this);

        this.players = new ArrayList<>();
        this.teams = new HashMap<>();
        this.blueTeamSpawn = blueTeamSpawn;
        this.redTeamSpawn = redTeamSpawn;
        this.blueScoreZone = blueScoreZone;
        this.redScoreZone = redScoreZone;

        this.generators = ConfigManager.getGenerators(id);

        this.game = new Game(this);

        this.state = GameState.RECRUITING;
    }

    //GAME

    public void start() {
        game.start();
    }

    public void reset(boolean kickPlayers) {
        stopGenerators();
        if (kickPlayers) {
            for (UUID uuid : players) {
                Location loc = ConfigManager.getLobby();
                Bukkit.getPlayer(uuid).teleport(loc);
            }
            players.clear();
            teams.clear();
        }

        sendTitle("", "");
        state = GameState.RECRUITING;
        countdown.cancel();
        countdown = new Countdown(minigame, this);
        game = new Game(this);
    }

    //TOOLS

    public void sendMessage(String message) {
        for (UUID uuid : players) {
            Bukkit.getPlayer(uuid).sendMessage(message);
        }
    }

    public void sendTitle(String title, String subtitle) {
        for (UUID uuid : players) {
            Bukkit.getPlayer(uuid).sendTitle(title, subtitle);
        }
    }

    //PLAYERS

    public void addPlayer(Player player) {
        players.add(player.getUniqueId());
        player.teleport(waitRoom);

        Team lowest = getTeamCount(Team.RED) <= getTeamCount(Team.BLUE) ? Team.RED : Team.BLUE;
        setTeam(player, lowest);

        player.sendMessage(ChatColor.AQUA + "Fuiste agregado al equipo " + lowest.getDisplay() + ".");

        if (state.equals(GameState.RECRUITING) && players.size() >= ConfigManager.getRequiredPlayers()) {
            countdown.start();
        }
    }

    public void removePlayer(Player player) {
        players.remove(player.getUniqueId());
        player.teleport(ConfigManager.getLobby());
        player.sendTitle("", "");

        removeTeam(player);

        if (state == GameState.COUNTDOWN && players.size() < ConfigManager.getRequiredPlayers()) {
            sendMessage(ChatColor.RED + "No hay suficientes jugadores para iniciar el juego. Se cancela la cuenta regresiva.");
            reset(false);
            return;
        }

        if (state == GameState.LIVE && players.size() < ConfigManager.getRequiredPlayers()) {
            sendMessage(ChatColor.RED + "No hay suficientes jugadores para continuar el juego. Se reinicia la arena.");
            reset(false);
        }
    }

    //INFO

    public int getId() {
        return id;
    }

    public GameState getState() {
        return state;
    }

    public List<UUID> getPlayers() {
        return players;
    }

    public void setState(GameState state) {
        this.state = state;
    }

    public Team getTeam(UUID id) {
        return teams.get(id);
    }

    public PointZone getScoringZone(Team team) {
        return team == Team.RED ? blueScoreZone : redScoreZone;
    }

    public Game getGame() {
        return game;
    }

    public Location getTeamSpawn(Team team) {
        switch (team) {
            case BLUE:
                return blueTeamSpawn;
            case RED:
                return redTeamSpawn;
            default:
                return null;
        }
    }

    public void startGenerators() {
        for (Generator g : generators) g.start(minigame);
    }

    public void stopGenerators() {
        for (Generator g : generators) g.stop();
    }
    //TEAMS

    public void setTeam(Player player, Team team) {
        removeTeam(player);
        teams.put(player.getUniqueId(), team);
    }

    public void removeTeam(Player player) {
        if (teams.containsKey(player.getUniqueId())) {
            teams.remove(player.getUniqueId());
        }
    }

    public int getTeamCount(Team team) {
        int amount = 0;
        for (Team t : teams.values()) {
            if (t == team) {
                amount++;
            }
        }

        return amount;
    }

}
