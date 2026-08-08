package org.nanii.minigame.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nanii.minigame.Minigame;
import org.nanii.minigame.chat.ChatChannel;
import org.nanii.minigame.instance.Arena;
import org.nanii.minigame.team.Team;

public class ChatListener implements Listener {

    private static final String GLOBAL_PREFIX = "!";

    private final Minigame minigame;

    public ChatListener(Minigame minigame) {
        this.minigame = minigame;
    }

    @EventHandler
    public void onChat(AsyncChatEvent e) {
        Player sender = e.getPlayer();
        Arena arena = minigame.getArenaManager().getArena(sender);
        Team senderTeam = (arena == null) ? null : arena.getTeam(sender.getUniqueId());

        String raw = PlainTextComponentSerializer.plainText().serialize(e.message());

        ChatChannel channel;
        String body;

        if (arena == null) {
            channel = ChatChannel.LOBBY;
            body = raw;
        } else if (arena.isSpectator(sender.getUniqueId())) {
            channel = ChatChannel.SPECTATOR;
            body = raw;
        } else if (raw.startsWith(GLOBAL_PREFIX)) {
            channel = ChatChannel.ARENA;
            body = raw.substring(GLOBAL_PREFIX.length()).trim();
        } else if (senderTeam == null) {
            channel = ChatChannel.ARENA;
            body = raw;
        } else {
            channel = ChatChannel.TEAM;
            body = raw;
        }

        if (body.isEmpty()) {
            e.setCancelled(true);
            return;
        }

        e.viewers().removeIf(viewer -> !canSee(viewer, arena, channel, senderTeam));

        Component formatted = format(channel, senderTeam, sender.getName(), body);
        e.renderer(((source, sourceDisplayName, message, viewer) -> formatted));
    }

    private boolean canSee(Audience viewer, Arena arena, ChatChannel channel, Team senderTeam) {
        if (!(viewer instanceof Player target)) return true;

        Arena viewerArena = minigame.getArenaManager().getArena(target);

        if (channel == ChatChannel.LOBBY) {
            return viewerArena == null;
        }

        if (viewerArena != arena) return false;

        boolean viewersIsSpectator = arena.isSpectator(target.getUniqueId());

        if (channel == ChatChannel.SPECTATOR) return viewersIsSpectator;
        if (channel == ChatChannel.ARENA) return !viewersIsSpectator;

        return arena.getTeam(target.getUniqueId()) == senderTeam;
    }

    private Component format(ChatChannel channel, Team team, String name, String body) {
        Component separator = Component.text(": ", NamedTextColor.DARK_GRAY);
        Component message = Component.text(body, NamedTextColor.WHITE);

        return switch (channel) {
            case TEAM -> Component.text()
                    .append(Component.text("[EQUIPO] ", team.getColor(), TextDecoration.BOLD))
                    .append(Component.text(name, team.getColor()))
                    .append(separator)
                    .append(message)
                    .build();
            case ARENA -> Component.text()
                    .append(team == null
                            ? Component.text("○ ", NamedTextColor.GRAY)
                            : Component.text("● ", team.getColor()))
                    .append(Component.text(name, NamedTextColor.WHITE))
                    .append(separator)
                    .append(message)
                    .build();
            case LOBBY -> Component.text()
                    .append(Component.text(name, NamedTextColor.GRAY))
                    .append(separator)
                    .append(message)
                    .build();
            case SPECTATOR -> Component.text()
                    .append(Component.text("[ESPECTADOR]", NamedTextColor.GRAY, TextDecoration.BOLD))
                    .append(Component.text(name, NamedTextColor.GRAY))
                    .append(separator)
                    .append(message)
                    .build();
        };
    }
}
