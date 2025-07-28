package Util;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import static anidiotnon.fluix.fluixonBoss.FluixonBossPlugin.world;

// the math object!
public class Util {

    // gets the yaw equivalent of a certain x and z direction.
    public static double getYaw (double x, double z) {
        return Math.toDegrees(Math.atan2(z, x)) + 90;
    }

    // rotates a vector to a new vector, ignoring y-axis
    public static Vector rotate(Vector vector, double degrees) {
        double x = vector.getX();
        double z = vector.getZ();

        double curAngle = Math.toDegrees(Math.atan2(z, x));
        double newAngle = curAngle + degrees;
        newAngle = Math.toRadians(newAngle);
        double r = Math.sqrt(x * x + z * z);

        return new Vector(Math.cos(newAngle) * r, vector.getY(), Math.sin(newAngle) * r);
    }
    public static double randInRange (double min, double max) {
        if (min > max) {
            throw new IllegalArgumentException("Invalid parameters: min greater than max");
        }
        return Math.random() * (max - min) + min;
    }
    // rand integer from min to max, inclusive
    public static int randInRange (int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("Invalid parameters: min greater than max");
        }
        return (int)(Math.random() * (max - min + 1)) + min;
    }
    // distance between points
    public static double distance (int x1, int x2, int z1, int z2) {
        return Math.sqrt((x1 - x2) * (x1 - x2) + (z1 - z2) * (z1 - z2));
    }

    // returns a random location with:
    // both an x and a z coordinate within displacement blocks of the location's given x and z coordinate.

    public static Location getRandomLocationNear (Location location, double displacement) {
        if (displacement < 0) {
            throw new IllegalArgumentException("Invalid displacement: Must not be negative");
        }
        double xMin = location.getX() - displacement;
        double xMax = location.getX() + displacement;

        double zMin = location.getZ() - displacement;
        double zMax = location.getZ() + displacement;

        double newX = randInRange(xMin, xMax);
        double newZ = randInRange(zMin, zMax);

        return new Location(world, newX, location.getY(), newZ);
    }
}
