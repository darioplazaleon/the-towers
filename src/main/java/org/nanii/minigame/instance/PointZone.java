package org.nanii.minigame.instance;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;

public class PointZone {
    private final World world;
    private final BoundingBox box;

    public PointZone(World world, double x, double y, double z, int radius, int height) {
        this.world = world;
        this.box = new BoundingBox(
                x - radius, y, z - radius,
                x + radius, y + height,z + radius
        );
    }

    public boolean contains(Location location) {
        if (location.getWorld() != world) {
            return false;
        }
        return box.contains(location.toVector());
    }

    public BoundingBox getBox() {
        return box;
    }

    public World getWorld() {
        return world;
    }
}
