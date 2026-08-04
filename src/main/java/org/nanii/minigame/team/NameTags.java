package org.nanii.minigame.team;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

public class NameTags {

    private final Scoreboard scoreboard;

    public NameTags() {
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

        for (Team team : Team.values()) {
            org.bukkit.scoreboard.Team entry = scoreboard.registerNewTeam(team.name());

            entry.color(team.getColor());
            entry.prefix(Component.text("● ", team.getColor()));
            entry.setAllowFriendlyFire(false);
            entry.setCanSeeFriendlyInvisibles(true);
        }
    }

    public void apply(Player player, Team team) {
        scoreboard.getTeam(team.name()).addEntry(player.getName());
        player.setScoreboard(scoreboard);
    }

    public void reset(Player player) {
        org.bukkit.scoreboard.Team entry = scoreboard.getEntryTeam(player.getName());
        if (entry != null) {
            entry.removeEntry(player.getName());
        }
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }
}
