package org.nanii.minigame.instance;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.nanii.minigame.GameState;
import org.nanii.minigame.Minigame;
import org.nanii.minigame.manager.ConfigManager;
import org.nanii.minigame.team.NameTags;
import org.nanii.minigame.team.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class Arena {

    private Minigame minigame;

    private int id;
    private String worldName;
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
    private final NameTags nameTags = new NameTags();

    public Arena(Minigame minigame, int id, String worldName, Location waitRoom, Location blueTeamSpawn,
                 Location redTeamSpawn, PointZone blueScoreZone, PointZone redScoreZone
    ) {
        this.minigame = minigame;
        this.id = id;
        this.worldName = worldName;

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

    public void reset() {
        GameState previous = state;
        state = GameState.RESETTING;
        game.stop();
        stopGenerators();

        sendTitle("", "");

        if (previous == GameState.LIVE || previous == GameState.ENDING) {
            Location lobby = ConfigManager.getLobby();
            for (UUID uuid : players) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    nameTags.reset(player);
                    player.teleport(lobby);
                }
            }
            players.clear();
            teams.clear();

            Bukkit.unloadWorld(worldName, false);
            World world = Bukkit.createWorld(new WorldCreator(worldName));
            world.setAutoSave(false);
        }

        state = GameState.RECRUITING;
        countdown.cancel();
        countdown = new Countdown(minigame, this);
        game = new Game(this);
    }

    //TOOLS

    public void sendMessage(String message) {
        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(message);
            }
        }
    }

    public void sendTitle(String title, String subtitle) {
        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendTitle(title, subtitle);
            }
        }
    }

    public void sendTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
            }
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
        if (state == GameState.LIVE || state == GameState.ENDING) {
            game.removeViewer(player);
        }

        players.remove(player.getUniqueId());
        player.teleport(ConfigManager.getLobby());
        player.sendTitle("", "");

        removeTeam(player);

        if (state == GameState.COUNTDOWN && players.size() < ConfigManager.getRequiredPlayers()) {
            sendMessage(ChatColor.RED + "No hay suficientes jugadores para iniciar el juego. Se cancela la cuenta regresiva.");
            reset();
            return;
        }

        if (state == GameState.LIVE && players.size() < ConfigManager.getRequiredPlayers()) {
            sendMessage(ChatColor.RED + "No hay suficientes jugadores para continuar el juego. Se reinicia la arena.");
            reset();
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

    public Minigame getMinigame() {
        return minigame;
    }

    //TEAMS
    public void setTeam(Player player, Team team) {
        removeTeam(player);
        teams.put(player.getUniqueId(), team);
        nameTags.apply(player, team);
    }

    public void removeTeam(Player player) {
        if (teams.containsKey(player.getUniqueId())) {
            teams.remove(player.getUniqueId());
            nameTags.reset(player);
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
