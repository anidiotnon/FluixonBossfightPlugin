package Projectiles;

import Util.Util;
import anidiotnon.fluix.fluixonBoss.FluixonBossPlugin;
import me.libraryaddict.disguise.disguisetypes.PlayerDisguise;
import me.libraryaddict.disguise.disguisetypes.watchers.PlayerWatcher;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import static Locations.ArenaManager.yHeight;
import static anidiotnon.fluix.fluixonBoss.FluixonBossPlugin.world;

// a class for the dripstone projectile that fluixon fires during the bossfight.
public class DripstoneProjectile {
    private final int damage = 7;
    private final int explodeDamage = 5;
    Vector velocity;
    Location startLocation;
    Item projectile;
    private final double hitRadius = 0.15;
    private final double explodeRadius = 1.5;
    private BukkitTask projectileTask;
    private PlayerDisguise playerDisguise;
    private PlayerWatcher projectileWatcher;
    private boolean isBlockBreaking = false;
    private final int maxAliveTicks = 60;

    public DripstoneProjectile (Location startLocation, Vector velocity) {
        this.velocity = velocity;

        // spawn in the projectile
        projectile = (Item) world.spawnEntity(startLocation, EntityType.ITEM);
        projectile.setItemStack(new ItemStack(Material.POINTED_DRIPSTONE));
        // projectile is an item, so prevent mobs and players from picking it up.
        projectile.setCanMobPickup(false);
        projectile.setCanPlayerPickup(false);

        // continuously updates the projectile.
        projectileTask = new BukkitRunnable() {
            int aliveTicks = 0;

            @Override
            public void run () {
                updateProjectile();

                if (projectile.isDead()) {
                    this.cancel();
                }
                aliveTicks++;

                if (aliveTicks >= maxAliveTicks) {
                    projectileExplode();
                }
            }
        }.runTaskTimer(FluixonBossPlugin.getPlugin(), 0, 1);
    }
    // this function sets the projectile to be block breaking, meaning it can break blocks in the arena.
    // block breaking projectiles will have a black particle trail following it.
    public void setBlockBreaking () {
        isBlockBreaking = true;
    }

    private void updateProjectile () {
        // checks if a player is nearby and damages the player.
        for (Player player : world.getNearbyPlayers(projectile.getLocation(), hitRadius)) {
            player.damage(damage);

            projectile.remove();

            projectileTask.cancel();
        }
        if (isBlockBreaking) {
            world.spawnParticle(Particle.SQUID_INK, projectile.getLocation(), 1);
        }
        // if the projectile hits the ground, explode.
        if (world.getBlockAt(projectile.getLocation().subtract(0, 0.5, 0)).getBlockData().getMaterial() != Material.AIR) {
            projectileExplode();
        }
        // moves the projectile
        projectile.setVelocity(velocity);
    }
    private void projectileExplode () {
        // damage the player if nearby explode radius.
        for (Player player : world.getNearbyPlayers(projectile.getLocation(), explodeRadius)) {
            player.damage(explodeDamage);
        }
        world.spawnParticle(Particle.EXPLOSION, projectile.getLocation(), 1);

        world.playSound(projectile.getLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1, 1);

        // breaks all nearby blocks hit by the projectile's explode radius.
        if (isBlockBreaking) {
            int projectileX = (int)projectile.getX();
            int projectileZ = (int)projectile.getZ();
            int xStart = projectileX - 2;
            int xEnd = projectileX + 2;
            int zStart = projectileZ - 2;
            int zEnd = projectileZ + 2;

            for (int x = xStart; x <= xEnd; x++) {
                for (int z = zStart; z <= zEnd; z++) {
                    if (Util.distance(x, projectileX, z, projectileZ) <= explodeRadius) {
                        if (world.getBlockData(x, yHeight, z).getMaterial() == Material.WHITE_CONCRETE &&
                            world.getBlockData(x, yHeight + 1, z).getMaterial() == Material.AIR) {

                            world.setBlockData(x, yHeight, z, Material.AIR.createBlockData());
                        }
                    }
                }
            }
        }
        projectile.remove();
        projectileTask.cancel();
    }
}
