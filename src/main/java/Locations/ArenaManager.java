package Locations;

import anidiotnon.fluix.fluixonBoss.FluixonBossPlugin;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.ArrayList;

import static anidiotnon.fluix.fluixonBoss.FluixonBossPlugin.world;

// manages the arena locations, and also creates new arenas in case there are multiple players on one server.
public class ArenaManager {
    private static ArrayList<ArenaLocation> arenaLocations;
    public static final int yHeight = 100;
    public static final Location fluixonSpawnLocation = new Location(world, 150.5, 105, 140.5);
    public static final Location playerSpawnLocation = new Location(world, 150.5, 101, 160.5, 180, 0);


    public static void initArenaManager () {
        arenaLocations = new ArrayList<>();

        for (int x = 100; x <= 200; x++) {
            for (int z = 100; z <= 200; z++) {
                if (world.getBlockAt(x, yHeight, z).getBlockData().getMaterial().equals(Material.WHITE_CONCRETE)) {
                    arenaLocations.add(new ArenaLocation(x, z));
                }
            }
        }

        FluixonBossPlugin.print("Arena Size: " + arenaLocations.size());
    }
    public static ArrayList<ArenaLocation> getArenaLocations () {
        return arenaLocations;
    }
}
