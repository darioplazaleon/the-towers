package org.nanii.thetowers.sign;

import org.bukkit.block.Block;

public record SignKey(String world, int x, int y, int z) {
    public static SignKey of(Block block) {
        return new SignKey(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }
}
