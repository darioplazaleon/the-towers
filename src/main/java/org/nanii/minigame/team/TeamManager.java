package org.nanii.minigame.team;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nanii.minigame.manager.ConfigManager;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class TeamManager {

    private final Map<UUID, Team> teams = Collections.synchronizedMap(new LinkedHashMap<>());
    private final NameTags nameTags = new NameTags();

    public Team get(UUID id) {
        return teams.get(id);
    }

    public int count(Team team) {
        int amount = 0;
        for (Team t : teams.values()) {
            if (t == team) amount++;
        }
        return amount;
    }

    public List<UUID> members(Team team) {
        List<UUID> result = new ArrayList<>();
        for (Map.Entry<UUID, Team> entry : teams.entrySet()) {
            if (entry.getValue() == team) result.add(entry.getKey());
        }
        return result;
    }

    public void set(Player player, Team team) {
        teams.remove(player.getUniqueId());
        teams.put(player.getUniqueId(), team);
        nameTags.apply(player, team);
    }

    public void remove(Player player) {
        if (teams.remove(player.getUniqueId()) != null) {
            nameTags.reset(player);
        }
    }

    public void clear() {
        for (UUID id : new ArrayList<>(teams.keySet())) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) nameTags.reset(player);
        }
        teams.clear();
    }

    //LIMITS

    public JoinResult canJoin(UUID id, Team team) {
        Team current = teams.get(id);
        if (current == team) return JoinResult.ALREADY_IN_TEAM;

        Team other = team.opposite();

        int target = count(team) + 1;
        int rival = count(other) - (current == other ? 1 : 0);

        if (target > ConfigManager.getTeamSize()) {
            return JoinResult.TEAM_FULL;
        }

        if (target - rival > ConfigManager.getMaxTeamDifference()) {
            return JoinResult.WOULD_UNBALANCE;
        }

        return JoinResult.OK;
    }

    public JoinResult tryJoin(Player player, Team team) {
        JoinResult result = canJoin(player.getUniqueId(), team);
        if (result == JoinResult.OK) {
            set(player, team);
        }
        return result;
    }

    //BALANCE
    public Team assignBalanced(Player player) {
        Team current = teams.get(player.getUniqueId());
        if (current != null) return current;

        int red = count(Team.RED);
        int blue = count(Team.BLUE);

        Team team;
        if (red < blue) {
            team = Team.RED;
        } else if (blue < red) {
            team = Team.BLUE;
        } else {
            team = ThreadLocalRandom.current().nextBoolean() ? Team.RED : Team.BLUE;
        }

        set(player, team);
        return team;
    }

    public boolean rebalance() {
        int maxDiff = Math.max(1, ConfigManager.getMaxTeamDifference());
        boolean moved = false;

        while (true) {
            int red = count(Team.RED);
            int blue = count(Team.BLUE);
            if (Math.abs(red - blue) <= maxDiff) break;

            Team bigger = red > blue ? Team.RED : Team.BLUE;
            List<UUID> members = members(bigger);
            UUID lastJoined = members.get(members.size() - 1);

            Player player = Bukkit.getPlayer(lastJoined);
            if (player == null) {
                teams.remove(lastJoined);
                continue;
            }

            Team destination = bigger.opposite();
            set(player, destination);
            player.sendMessage(Component.translatable("team.moved",
                    Argument.component("team", destination.displayName())));
            moved = true;
        }

        return moved;
    }
}

