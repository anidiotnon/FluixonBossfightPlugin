package Locations;

import org.bukkit.Location;

// represents a location in the boss arena.
public class ArenaLocation {
    private int x;
    private int z;
    public boolean isBroken; // whether this arena location is a solid block or not

    public ArenaLocation (int x, int z) {
        this.x = x;
        this.z = z;
        isBroken = false;
    }
    public double distance (Location location) {
        return Math.sqrt((x - location.getX()) * (x - location.getX()) + (z - location.getZ()) * (z - location.getZ()));
    }
    public int getX () {
        return x;
    }
    public int getZ () {
        return z;
    }
    public boolean equals(ArenaLocation other) {
        return this.x == other.x && this.z == other.z;
    }
}
