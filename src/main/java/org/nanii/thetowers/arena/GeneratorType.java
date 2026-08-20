package org.nanii.thetowers.arena;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ThrownExpBottle;
import org.bukkit.inventory.ItemStack;

public enum GeneratorType {
    IRON {
        public void spawn(Location loc, int amount) {
            loc.getWorld().dropItem(loc, new ItemStack(Material.IRON_INGOT, amount));
        }
    },
    XP {
        public void spawn(Location loc, int amount) {
            for (int i = 0; i < amount; i++) {
                loc.getWorld().spawn(loc, ThrownExpBottle.class);
            }
        }
    };

    public abstract void spawn(Location loc, int amount);
}
