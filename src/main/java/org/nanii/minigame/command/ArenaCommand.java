package org.nanii.minigame.command;

import org.bukkit.ChatColor;
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

import java.util.ArrayList;
import java.util.List;

public class ArenaCommand implements CommandExecutor, TabCompleter {

    private Minigame minigame;
    private static final List<String> SUBCOMMANDS = List.of("list", "join", "spectate", "leave");

    public ArenaCommand(Minigame minigame) {
        this.minigame = minigame;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
                player.sendMessage(ChatColor.GREEN + "Lista de arenas disponibles:");

                for (Arena arena : minigame.getArenaManager().getArenas()) {
                    player.sendMessage(ChatColor.YELLOW + "- Arena " + arena.getId() + " (Estado: " + arena.getState() + ")");
                }
            } else if (args.length == 1 && args[0].equalsIgnoreCase("leave")) {
                boolean result = minigame.getArenaManager().leave(player);
                if (result) {
                    player.sendMessage(ChatColor.GREEN + "Has salido de la arena.");
                } else {
                    player.sendMessage(ChatColor.RED + "No estás en ninguna arena.");
                }

            } else if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
                int id;
                try {
                    id = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Por favor, introduce un número válido.");
                    return true;
                }
                Arena arena = minigame.getArenaManager().getArena(id);

                ArenaJoinResult result = minigame.getArenaManager().join(player, arena);
                if (result == ArenaJoinResult.OK) {
                    player.sendMessage(ChatColor.GREEN + "Te uniste a la arena " + arena.getId() + ".");
                } else {
                    player.sendMessage(ChatColor.RED + result.getMessage());
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("spectate")) {
                int id;
                try {
                    id = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Por favor, introduce un numero valido.");
                    return true;
                }

                Arena arena = minigame.getArenaManager().getArena(id);

                SpectateResult result = minigame.getArenaManager().spectate(player, arena);
                if (result != SpectateResult.OK) {
                    player.sendMessage(ChatColor.RED + result.getMessage());
                }
            } else {
                player.sendMessage(ChatColor.RED + "Uso: /arena <list|join|spectate|leave>");
            }
        }

        return true;
    }


    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], SUBCOMMANDS, new ArrayList<>());
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
