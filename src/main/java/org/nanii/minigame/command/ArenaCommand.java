package org.nanii.minigame.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.nanii.minigame.GameState;
import org.nanii.minigame.Minigame;
import org.nanii.minigame.instance.Arena;
import org.nanii.minigame.tab.TabBoard;
import org.nanii.minigame.tab.TabDemo;

public class ArenaCommand implements CommandExecutor {

    private Minigame minigame;

    public ArenaCommand(Minigame minigame) {
        this.minigame = minigame;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
                player.sendMessage("§aLista de arenas disponibles:");

                for (Arena arena : minigame.getArenaManager().getArenas()) {
                    player.sendMessage("§e- Arena " + arena.getId() + " (Estado: " + arena.getState() + ")");
                }
            } else if (args.length == 1 && args[0].equalsIgnoreCase("leave")) {
                Arena arena = minigame.getArenaManager().getArena(player);
                if (arena != null) {
                    arena.removePlayer(player);
                    player.sendMessage("§aHas salido del juego en la arena " + arena.getId() + ".");
                } else {
                    player.sendMessage("§cNo estás en ninguna arena.");
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
                if (minigame.getArenaManager().getArena(player) != null) {
                    player.sendMessage("§cYa estás en una arena. Usa /arena leave para salir primero.");
                    return true;
                }

                int id;
                try {
                    id = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cPor favor, introduce un número válido.");
                    return false;
                }

                if (id >= 0 && id < minigame.getArenaManager().getArenas().size()) {
                    Arena arena = minigame.getArenaManager().getArena(id);
                    if (arena.getState() == GameState.RECRUITING || arena.getState() == GameState.COUNTDOWN) {
                        arena.addPlayer(player);
                        player.sendMessage("§aTe has unido a la arena " + id + ".");
                    } else {
                        player.sendMessage("§cNo puedes unirte a la arena " + id + " porque el juego ya ha comenzado.");
                    }
                } else {
                    player.sendMessage("§cID de arena no válido.");
                }
            } else if (args.length == 1 && args[0].equalsIgnoreCase("tabtest")) {
                TabBoard board = new TabBoard();
                board.set(0,0, Component.text("IZQUIERDA", NamedTextColor.RED));
                board.set(0,1, Component.text("izq fila 1", NamedTextColor.GRAY));
                board.set(1,0, Component.text("DERECHA", NamedTextColor.BLUE));
                board.set(1,1, Component.text("der fila 1", NamedTextColor.GRAY));
                board.show(player);
                player.sendMessage("§aTabBoard enviado.");
            } else {
                player.sendMessage("§cUso: /arena <list|join|leave>");
            }
        }

        return false;
    }
}
