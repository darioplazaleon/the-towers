package org.nanii.minigame.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nanii.minigame.Minigame;
import org.nanii.minigame.instance.Arena;
import org.nanii.minigame.instance.ArenaJoinResult;
import org.nanii.minigame.instance.SpectateResult;
import org.nanii.minigame.lang.LangManager;

import java.util.ArrayList;
import java.util.List;

public class ArenaCommand implements CommandExecutor, TabCompleter {

    private Minigame minigame;
    private static final List<String> SUBCOMMANDS = List.of("list", "join", "spectate", "leave");
    private static final List<String> ADMIN_SUBCOMMANDS = List.of("reload");

    public ArenaCommand(Minigame minigame) {
        this.minigame = minigame;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
                player.sendMessage(Component.translatable("command.arena.list.header"));

                for (Arena arena : minigame.getArenaManager().getArenas()) {
                    player.sendMessage(Component.translatable("command.arena.list.entry",
                            Argument.numeric("arena", arena.getId()),
                            Argument.component("state", Component.translatable(arena.getState()))));
                }
            } else if (args.length == 1 && args[0].equalsIgnoreCase("leave")) {
                boolean result = minigame.getArenaManager().leave(player);
                if (result) {
                    player.sendMessage(Component.translatable("command.arena.leave.success"));
                } else {
                    player.sendMessage(Component.translatable("command.arena.leave.not-in-arena"));
                }

            } else if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
                int id;
                try {
                    id = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.translatable("command.arena.invalid-number"));
                    return true;
                }
                Arena arena = minigame.getArenaManager().getArena(id);

                ArenaJoinResult result = minigame.getArenaManager().join(player, arena);
                if (result == ArenaJoinResult.OK) {
                    player.sendMessage(Component.translatable("command.arena.join.success",
                            Argument.numeric("arena", arena.getId())));
                } else {
                    player.sendMessage(Component.translatable(result));
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("spectate")) {
                int id;
                try {
                    id = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.translatable("command.arena.invalid-number"));
                    return true;
                }

                Arena arena = minigame.getArenaManager().getArena(id);

                SpectateResult result = minigame.getArenaManager().spectate(player, arena);
                if (result != SpectateResult.OK) {
                    player.sendMessage(Component.translatable(result));
                }
            } else if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                if  (!player.hasPermission("minigame.admin")) {
                    player.sendMessage(Component.translatable("command.arena.no-permission"));
                    return true;
                }

                int count = LangManager.load(minigame);
                minigame.getSignManager().refreshAll();

                player.sendMessage(Component.translatable("command.arena.reload",
                        Argument.numeric("count", count)));
            } else {
                player.sendMessage(Component.translatable("command.arena.usage"));
            }
        }

        return true;
    }


    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(SUBCOMMANDS);
            if (sender.hasPermission("minigame.admin")) options.addAll(ADMIN_SUBCOMMANDS);
            return StringUtil.copyPartialMatches(args[0], options, new ArrayList<>());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("join") || args[0].equalsIgnoreCase("spectate"))) {
            List<String> ids = new ArrayList<>();
            for (Arena arena : minigame.getArenaManager().getArenas()) {
                ids.add(String.valueOf(arena.getId()));
            }
            return StringUtil.copyPartialMatches(args[1], ids, new ArrayList<>());
        }

        return List.of();
    }
}
