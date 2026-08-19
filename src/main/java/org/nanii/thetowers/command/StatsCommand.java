package org.nanii.thetowers.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nanii.thetowers.TheTowers;
import org.nanii.thetowers.stats.TopEntry;
import org.nanii.thetowers.stats.TopType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class StatsCommand implements CommandExecutor, TabCompleter {

    private static final int TOP_LIMIT = 10;
    private static final List<String> TOP_TYPES = List.of("kills", "points", "wins", "winrate");

    private final TheTowers theTowers;

    public StatsCommand(TheTowers theTowers) {
        this.theTowers = theTowers;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) return true;

        if (!theTowers.getStatsService().isActive()) {
            player.sendMessage(Component.translatable("command.stats.disabled"));
            return true;
        }

        if (command.getName().equalsIgnoreCase("stats")) {
            handleStats(player, args);
        } else {
            handleTop(player, args);
        }

        return true;
    }

    private void handleStats(Player player, String[] args) {
        if (args.length == 0) {
            show(player, player.getName(), player.getUniqueId());
            return;
        }

        String name = args[0];

        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            show(player, online.getName(), online.getUniqueId());
            return;
        }

        theTowers.getStatsService().resolveUuid(name, uuid -> {
            if (uuid == null) {
                player.sendMessage(Component.translatable("command.stats.not-found", Argument.string("player", name)));
            }
            show(player, name, uuid);
        });
    }

    private void show(Player viewer, String name, UUID uuid) {
        theTowers.getStatsService().stats(uuid, stats -> {
            viewer.sendMessage(Component.translatable("command.stats.header",
                    Argument.string("player", name)));

            viewer.sendMessage(Component.translatable("command.stats.games",
                    Argument.numeric("games", stats.games()),
                    Argument.numeric("wins", stats.wins()),
                    Argument.numeric("losses", stats.losses()),
                    Argument.string("winrate", percent(stats.winrate()))));

            viewer.sendMessage(Component.translatable("command.stats.combat",
                    Argument.numeric("kills", stats.kills()),
                    Argument.numeric("deaths", stats.deaths()),
                    Argument.string("kd", String.format(Locale.ROOT, "%.2f", stats.kd()))));

            viewer.sendMessage(Component.translatable("command.stats.points",
                    Argument.numeric("points", stats.points())));

            viewer.sendMessage(Component.translatable("command.stats.extra",
                    Argument.numeric("abandons", stats.abandons()),
                    Argument.numeric("minutes", stats.playtimeSeconds() / 60)));
        });
    }

    private void handleTop(Player player, String[] args) {
        if (args.length != 1) {
            player.sendMessage(Component.translatable("command.top.usage"));
            return;
        }

        TopType type;
        try {
            type = TopType.valueOf(args[0].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.translatable("command.top.usage"));
            return;
        }

        theTowers.getStatsService().top(type, TOP_LIMIT, entries -> {
            player.sendMessage(Component.translatable("command.top.header",
                    Argument.string("type", type.name().toLowerCase(Locale.ROOT))));

            if (entries.isEmpty()) {
                player.sendMessage(Component.translatable("command.top.empty"));
                return;
            }

            int position = 1;
            for (TopEntry entry : entries) {
                player.sendMessage(Component.translatable("command.top.entry",
                        Argument.numeric("position", position++),
                        Argument.string("player", entry.name()),
                        Argument.string("value", format(type, entry.value()))));
            }
        });
    }

    private String format(TopType type, double value) {
        return type == TopType.WINRATE ? percent(value) : String.valueOf((long) value);
    }

    private String percent(double ratio) {
        return String.format(Locale.ROOT, "%.1f", ratio * 100) + "%";
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length != 1) return List.of();

        if (command.getName().equalsIgnoreCase("top")) {
            return StringUtil.copyPartialMatches(args[0], TOP_TYPES, new ArrayList<>());
        }

        List<String> names = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            names.add(online.getName());
        }
        return StringUtil.copyPartialMatches(args[0], names, new ArrayList<>());

    }
}
