package org.nanii.thetowers.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nanii.thetowers.TheTowers;
import org.nanii.thetowers.chat.ChatChannel;
import org.nanii.thetowers.instance.Arena;
import org.nanii.thetowers.team.Team;

public class ChatListener implements Listener {

    private static final String GLOBAL_PREFIX = "!";

    private final TheTowers theTowers;

    public ChatListener(TheTowers theTowers) {
        this.theTowers = theTowers;
    }

    @EventHandler
    public void onChat(AsyncChatEvent e) {
        Player sender = e.getPlayer();
        Arena arena = theTowers.getArenaManager().getArena(sender);
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

        Arena viewerArena = theTowers.getArenaManager().getArena(target);

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
        return switch (channel) {
            case TEAM -> Component.translatable("chat.team",
                    Argument.tag("team_color", Tag.styling(team.getColor())),
                    Argument.string("player", name),
                    Argument.string("message", body));
            case ARENA -> Component.translatable("chat.arena",
                    Argument.component("bullet", team == null
                            ? Component.text("○ ", NamedTextColor.GRAY)
                            : Component.text("● ", team.getColor())),
                    Argument.string("player", name),
                    Argument.string("message", body));
            case LOBBY -> Component.translatable("chat.lobby",
                    Argument.string("player", name),
                    Argument.string("message", body));
            case SPECTATOR -> Component.translatable("chat.spectator",
                    Argument.string("player", name),
                    Argument.string("message", body));
        };
    }
}
