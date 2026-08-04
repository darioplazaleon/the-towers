package org.nanii.minigame.tab;

import com.mojang.authlib.GameProfile;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Entry;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action;
import net.minecraft.world.level.GameType;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public class TabDemo {

    public static void sendFakeEntry(Player viewer) {
        UUID fakeId = UUID.randomUUID();
        GameProfile profile = new GameProfile(fakeId, "FakePlayer");

        net.minecraft.network.chat.Component displayName = PaperAdventure.asVanilla(
                Component.text("§aFake Player", NamedTextColor.LIGHT_PURPLE)
        );

        Entry entry = new Entry(
                fakeId,
                profile,
                true,
                0,
                GameType.SURVIVAL,
                displayName,
                false,
                0,
                null
        );

        ClientboundPlayerInfoUpdatePacket packet = new ClientboundPlayerInfoUpdatePacket(
                EnumSet.of(
                        Action.ADD_PLAYER,
                        Action.UPDATE_LISTED,
                        Action.UPDATE_LATENCY,
                        Action.UPDATE_GAME_MODE,
                        Action.UPDATE_DISPLAY_NAME
                ),
                List.of(entry)
        );

        ((CraftPlayer) viewer).getHandle().connection.send(packet);
    }
}
