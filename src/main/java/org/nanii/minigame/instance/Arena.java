package org.nanii.minigame.instance;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.nanii.minigame.GameState;
import org.nanii.minigame.Minigame;
import org.nanii.minigame.gui.TeamSelectorItem;
import org.nanii.minigame.gui.TeamSelectorMenu;
import org.nanii.minigame.manager.ConfigManager;
import org.nanii.minigame.sign.ArenaSignManager;
import org.nanii.minigame.team.Team;
import org.nanii.minigame.team.TeamManager;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class Arena {

    private Minigame minigame;

    private int id;
    private String worldName;
    private Location waitRoom;

    private List<UUID> players;
    private List<UUID> spectators;

    private final TeamManager teamManager = new TeamManager();
    private Location blueTeamSpawn;
    private Location redTeamSpawn;
    private PointZone blueScoreZone;
    private PointZone redScoreZone;

    private List<Generator> generators;

    private GameState state;
    private Countdown countdown;
    private Game game;


    public Arena(Minigame minigame, int id, String worldName, Location waitRoom, Location blueTeamSpawn,
                 Location redTeamSpawn, PointZone blueScoreZone, PointZone redScoreZone
    ) {
        this.minigame = minigame;
        this.id = id;
        this.worldName = worldName;

        this.waitRoom = waitRoom;
        this.countdown = new Countdown(minigame, this);

        this.players = new CopyOnWriteArrayList<>();
        this.spectators = new CopyOnWriteArrayList<>();

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
        setState(GameState.RESETTING);
        game.stop();
        stopGenerators();

        clearTitles();

        for (UUID uuid : spectators) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) removeSpectator(player);
        }
        spectators.clear();

        if (previous == GameState.LIVE || previous == GameState.ENDING) {
            Location lobby = ConfigManager.getLobby();
            for (UUID uuid : players) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    TeamSelectorItem.remove(player);
                    player.teleport(lobby);
                }
            }
            teamManager.clear();
            players.clear();

            Bukkit.unloadWorld(worldName, false);
            World world = Bukkit.createWorld(new WorldCreator(worldName));
            world.setAutoSave(false);
        }

        setState(GameState.RECRUITING);
        countdown.cancel();
        countdown = new Countdown(minigame, this);
        game = new Game(this);
    }

    //TOOLS

    public void sendMessage(Component message) {
        for (Player player : getOnlineMembers()) {
            player.sendMessage(message);
        }
    }

    public void showTitle(Component title, Component subtitle) {
        Title t = Title.title(title, subtitle, Title.DEFAULT_TIMES);
        for (Player player : getOnlineMembers()) {
            player.showTitle(t);
        }
    }

    public void showTitle(Component title, Component subtitle, Duration in, Duration stay, Duration fadeOut) {
        Title t = Title.title(title, subtitle, Title.Times.times(in, stay, fadeOut));
        for (Player player : getOnlineMembers()) {
            player.showTitle(t);
        }
    }

    public void clearTitles() {
        for (Player player : getOnlineMembers()) {
            player.clearTitle();
        }
    }

    //PLAYERS

    public void addPlayer(Player player) {
        players.add(player.getUniqueId());
        player.teleport(waitRoom);

        TeamSelectorItem.give(player);
        player.sendMessage(Component.translatable("arena.choose-team"));

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
        player.clearTitle();

        teamManager.remove(player);
        TeamSelectorItem.remove(player);
        player.closeInventory();

        if (state != GameState.LIVE && state != GameState.ENDING) {
            teamManager.rebalance();
        }

        TeamSelectorMenu.refresh(this);

        if (state == GameState.COUNTDOWN && players.size() < ConfigManager.getRequiredPlayers()) {
            sendMessage(Component.translatable("arena.not-enough-players.countdown"));
            reset();
            return;
        }

        if (state == GameState.LIVE && players.size() < ConfigManager.getRequiredPlayers()) {
            sendMessage(Component.translatable("arena.not-enough-players.live"));
            reset();
        }
    }

    //SPECTATORS

    public void addSpectator(Player player) {
        spectators.add(player.getUniqueId());

        player.teleport(waitRoom);
        player.setGameMode(GameMode.SPECTATOR);

        if (state == GameState.LIVE || state == GameState.ENDING) {
            game.addViewer(player);
        }

        player.sendMessage(Component.translatable("arena.spectating", Argument.numeric("arena", id)));
    }

    public void removeSpectator(Player player) {
        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.setSpectatorTarget(null);
        }

        spectators.remove(player.getUniqueId());

        if (state == GameState.LIVE || state == GameState.ENDING) {
            game.removeViewer(player);
        }

        player.setGameMode(GameMode.SURVIVAL);
        player.teleport(ConfigManager.getLobby());
        player.clearTitle();
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

    public List<UUID> getSpectators() {
        return spectators;
    }

    public List<Player> getOnlineMembers() {
        List<Player> result = new ArrayList<>(players.size() + spectators.size());

        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) result.add(player);
        }

        for (UUID uuid : spectators) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) result.add(player);
        }

        return result;
    }

    public boolean isSpectator(UUID id) {
        return spectators.contains(id);
    }

    public boolean contains(UUID id) {
        return players.contains(id) || spectators.contains(id);
    }

    public int getMaxSpectators() {
        return ConfigManager.getMaxSpectators();
    }

    public void setState(GameState state) {
        this.state = state;
        refreshSigns();
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

    public int getMaxPlayers() {
        return ConfigManager.getTeamSize() * 2;
    }

    public int getCountdownSeconds() {
        return countdown.getSecondsLeft();
    }

    //TEAMS

    public TeamManager getTeams() {
        return teamManager;
    }

    public Team getTeam(UUID id) {
        return teamManager.get(id);
    }

    public int getTeamCount(Team team) {
        return teamManager.count(team);
    }

    public void prepareTeams() {
        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;

            if (teamManager.get(uuid) == null) {
                Team team = teamManager.assignBalanced(player);
                player.sendMessage(Component.translatable("arena.team-assigned", Argument.component("team", team.displayName())));
            }
        }
        teamManager.rebalance();
    }

    //SIGNS

    private void refreshSigns() {
        ArenaSignManager signs = minigame.getSignManager();
        if (signs != null) signs.refresh(this);
    }

}
