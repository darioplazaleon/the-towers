package org.nanii.minigame.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.nanii.minigame.Minigame;
import org.nanii.minigame.lang.LangManager;

import java.util.List;

public class TeamSelectorItem {

    public static final int SLOT = 4;
    private static NamespacedKey key;

    private TeamSelectorItem() {
    }

    public static void setup(Minigame minigame) {
        key = new NamespacedKey(minigame, "team-selector");
    }

    public static ItemStack create() {
        ItemStack item = new ItemStack(Material.WHITE_WOOL);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(LangManager.render(Component.translatable("gui.selector.item.name"))
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                LangManager.render(Component.translatable("gui.selector.item.lore"))
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    public static boolean is(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    public static void give(Player player) {
        player.getInventory().setItem(SLOT, create());
    }

    public static void remove(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getSize(); i++) {
            if (is(inventory.getItem(i))) {
                inventory.setItem(i, null);
            }
        }
    }
}
