package org.nanii.minigame.tab;

import com.mojang.authlib.GameProfile;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Entry;
import net.minecraft.world.level.GameType;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;


import java.util.*;

public class TabBoard {

    public static final int ROWS = 20;
    public static final int COLUMNS = 2;
    public static final int SLOTS = ROWS * COLUMNS;

    private final Component[] lastSent = new Component[SLOTS];

    private static final EnumSet<Action> CREATE = EnumSet.of(
            Action.ADD_PLAYER,
            Action.UPDATE_LISTED,
            Action.UPDATE_LATENCY,
            Action.UPDATE_GAME_MODE,
            Action.UPDATE_LIST_ORDER,
            Action.UPDATE_DISPLAY_NAME
    );

    private static final EnumSet<Action> REFRESH = EnumSet.of(Action.UPDATE_DISPLAY_NAME);

    private final GameProfile[] profiles = new GameProfile[SLOTS];
    private final Component[] lines = new Component[SLOTS];

    public TabBoard(){
        for (int slot = 0; slot < SLOTS; slot++) {
            profiles[slot] = new GameProfile(UUID.randomUUID(), String.format("%02d", slot));
            lines[slot] = Component.empty();
        }
    }

    public static int slot(int column, int row) {
        return column * ROWS + row;
    }

    public void set(int column, int row, Component text) {
        lines[slot(column, row)] = text;
    }

    public void clearLines() {
        Arrays.fill(lines, Component.empty());
    }

    public boolean isDirty() {
        return !Arrays.equals(lines, lastSent);
    }

    public void markClean() {
        System.arraycopy(lines, 0, lastSent, 0, SLOTS);
    }

        // ENVIO

    public void show(Player viewer) {
        send(viewer, new ClientboundPlayerInfoUpdatePacket(CREATE, buildEntries()));
        for (Player online : Bukkit.getOnlinePlayers()) {
            hidePlayer(viewer, online.getUniqueId());
        }
    }

    public void refresh(Player viewer) {
        send(viewer, new ClientboundPlayerInfoUpdatePacket(REFRESH, buildEntries()));
    }

    public void hide(Player viewer) {
        List<UUID> ids = new ArrayList<>(SLOTS);
        for (GameProfile profile : profiles) {
            ids.add(profile.id());
        }
        send(viewer, new ClientboundPlayerInfoRemovePacket(ids));
    }


    private List<Entry> buildEntries() {
        List<Entry> entries = new ArrayList<>(SLOTS);
        for (int slot = 0; slot < SLOTS; slot++) {
            entries.add(new Entry(
                    profiles[slot].id(),
                    profiles[slot],
                    true,
                    0,
                    GameType.SURVIVAL,
                    PaperAdventure.asVanilla(lines[slot]),
                    false,
                    0,
                    null
            ));
        }

        return entries;
    }

    public void hidePlayer(Player viewer, UUID target) {
        send(viewer, ClientboundPlayerInfoUpdatePacket.updateListed(target, false));
    }

    public void showPlayer(Player viewer, UUID target) {
        send(viewer, ClientboundPlayerInfoUpdatePacket.updateListed(target, true));
    }

    private static void send(Player viewer, net.minecraft.network.protocol.Packet<?> packet) {
        ((CraftPlayer) viewer).getHandle().connection.send(packet);
    }

}
