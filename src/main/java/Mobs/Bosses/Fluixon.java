package Mobs.Bosses;

import Dialogue.FluixonDialogue;
import Locations.ArenaLocation;
import Locations.ArenaManager;
import TitleSender.TitleSender;
import Util.Util;
import Projectiles.DripstoneProjectile;
import anidiotnon.fluix.fluixonBoss.FluixonBossPlugin;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.PlayerDisguise;
import me.libraryaddict.disguise.disguisetypes.watchers.PlayerWatcher;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.*;
import org.bukkit.block.data.type.PointedDripstone;
import org.bukkit.damage.DamageType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;

import static Locations.ArenaManager.*;
import static anidiotnon.fluix.fluixonBoss.FluixonBossPlugin.print;
import static anidiotnon.fluix.fluixonBoss.FluixonBossPlugin.world;

public class Fluixon implements Listener {
    private ArrayList<ArenaLocation> arenaLocations; // manages and stores all blocks within the arena.
    private Mob fluixon; // the boss
    private PlayerDisguise fluixonDisguise; // the "disguise" so that fluixon isn't just a regular zombie...
    private PlayerWatcher fluixonWatcher; // "responsible for the appearance of fluixon, e.g. what armor, weapons he's holding"
    private final int maxHP = 100;
    private int hp = 100;
    private double healthPercentage;
    private String displayName;
    private BukkitTask bossAI; // task responsible for the AI
    private Player player;
    private final int ticksPerDialogue = 40;
    private FluixonDialogue fluixonDialogue;
    private int phase;
    private int aliveTicks = 0;

    // this variable toggles whether fluixon is vulnerable or not to regular attacks For example, in the intro fluixon should be invulnerable.
    private boolean invulnerable = false;

    // downtimes: all in ticks, for all attacks and abilities
    private int attackDowntime = 0;
    private int teleportDowntime = 100;
    private int dripstoneDropDowntime = 100;

    private int dripstoneShotDowntimeMin = 40;
    private int dripstoneShotDowntimeMax = 50;
    private int teleportDowntimeMin = 40;
    private int teleportDowntimeMax = 80;

    private int axeSlamDowntimeMin = 40;
    private int axeSlamDowntimeMax = 50;

    private int slashChainDowntimeMin = 100;
    private int slashChainDowntimeMax = 120;

    private int dripstoneDropDowntimeMin = 400;
    private int dripstoneDropDowntimeMax = 500;

    private double axeSlamChance = 0.3;

    private BossBar fluixonBossbar;

    public Fluixon (Player player) {
        // Initializing the boss. Everything from the fluixon skin, to setting up the watchers, and whatever else.
        this.player = player;
        // grabs the player model.
        fluixonDisguise = new PlayerDisguise("Fluixon");

        // Spawns in both the player and the boss for the duel.
        fluixon = (Mob) world.spawnEntity(fluixonSpawnLocation, EntityType.ZOMBIE);
        player.teleport(playerSpawnLocation);

        // here using LibsDisguises, I send a packet to the player that "Fluixon is the person you are fighting, not a zombie".
        DisguiseAPI.disguiseToAll(fluixon, fluixonDisguise);
        fluixonWatcher = fluixonDisguise.getWatcher();

        // add the weapons and armor for fluixon
        fluixonWatcher.setItemInMainHand(new ItemStack(Material.POINTED_DRIPSTONE));
        fluixonWatcher.setItemInOffHand(new ItemStack(Material.DIAMOND_AXE));

        ItemStack helmet = new ItemStack(Material.DIAMOND_HELMET);
        ItemStack chestplate = new ItemStack(Material.DIAMOND_CHESTPLATE);
        ItemStack leggings = new ItemStack(Material.DIAMOND_LEGGINGS);
        ItemStack boots = new ItemStack(Material.DIAMOND_BOOTS);

        ArmorMeta armorMeta = (ArmorMeta)helmet.getItemMeta();
        armorMeta.setTrim(new ArmorTrim(TrimMaterial.LAPIS, TrimPattern.SILENCE));

        helmet.setItemMeta(armorMeta);
        chestplate.setItemMeta(armorMeta);
        leggings.setItemMeta(armorMeta);
        boots.setItemMeta(armorMeta);

        fluixonWatcher.setHelmet(helmet);
        fluixonWatcher.setChestplate(chestplate);
        fluixonWatcher.setLeggings(leggings);
        fluixonWatcher.setBoots(boots);

        // Manipulate the base health of the boss and sets it to 50.
        fluixon.getAttribute(Attribute.MAX_HEALTH).setBaseValue(maxHP);
        fluixon.setHealth(maxHP);

        // set the speed and damage for the melee phase
        fluixon.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(8);
        fluixon.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.55);

        // the arenaLocations stores all important locations in the arena for random selection from future attacks.
        this.arenaLocations = ArenaManager.getArenaLocations();

        // using the watcher, gives a good custom name tag.
        fluixonWatcher.setCustomName(ChatColor.BOLD + "" + ChatColor.DARK_PURPLE + "Fluixon " + ChatColor.GREEN + hp + "/" + (int)fluixon.getAttribute(Attribute.MAX_HEALTH).getValue());

        // turns off the AI, fluixon uses a custom-coded AI made by me for phase 1 and phase 3.
        fluixon.setAI(false);

        // I stored all the dialogue in a different class for organization.
        fluixonDialogue = new FluixonDialogue();

        fluixonBossbar = BossBar.bossBar(Component.text("Fluixon").color(NamedTextColor.DARK_PURPLE)
                , 1, BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS);

        // starts at phase 1.
        phase = 1;

        startBattle();
        resetArena();

        player.showBossBar(fluixonBossbar);

        Bukkit.getPluginManager().registerEvents(this, FluixonBossPlugin.getPlugin());
    }

    // Starts the battle with the opening dialogue. Once the dialogue is finished, the AI is initialized and the fight begins.
    private void startBattle () {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 10000, 200));
        invulnerable = true;

        new BukkitRunnable() {
            int dialogueIndex = 0;
            @Override
            public void run() {
                player.sendMessage(fluixonDialogue.getIntroDialogue().get(dialogueIndex));
                dialogueIndex++;

                if (dialogueIndex >= fluixonDialogue.getIntroDialogue().size()) {
                    initAI();
                    this.cancel();
                }
            }
        }.runTaskTimer(FluixonBossPlugin.getPlugin(), 0, ticksPerDialogue);
    }
    // Initializes the AI. makes sure to cancel the second fluixon is marked as dead.
    private void initAI () {
       // fluixon.setAI(true);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        bossAI = new BukkitRunnable() {
            @Override
            public void run () {
                if (fluixon.isDead()) {
                    this.cancel();
                }
                updateAI();
            }
        }.runTaskTimer(FluixonBossPlugin.getPlugin(), 0, 1);

        TitleSender.sendTitle("Fluixon", "The Dripstone Guy", player, 1000, 3000, 1000,
                NamedTextColor.DARK_PURPLE, NamedTextColor.DARK_RED);

        invulnerable = false;

        initDowntimes();
        initPlayer();
    }
    // initializes player armor, weapons, and potion effects.
    private void initPlayer () {
        for (PotionEffect potionEffect : player.getActivePotionEffects()) {
            player.removePotionEffect(potionEffect.getType());
        }

        player.getInventory().clear();
        player.getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.DIAMOND_BOOTS));

        ItemStack playerSword = new ItemStack(Material.DIAMOND_SWORD);
        playerSword.addEnchantment(Enchantment.SHARPNESS, 5);
        ItemStack playerCrossbow = new ItemStack(Material.CROSSBOW);
        playerCrossbow.addEnchantment(Enchantment.QUICK_CHARGE, 3);

        player.getInventory().setItem(0, playerSword);
        player.getInventory().setItem(1, playerCrossbow);
        player.getInventory().setItem(2, new ItemStack(Material.ARROW, 64));
        player.getInventory().setItemInOffHand(new ItemStack(Material.GOLDEN_APPLE, 5));

        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 600000, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 600000, 200));

    }
    // called at the start of the battle, so that fluixon doesn't use all of his attacks right away.
    private void initDowntimes () {
        attackDowntime = Util.randInRange(15, 25);
        teleportDowntime = Util.randInRange(40, 80);
        dripstoneDropDowntime = Util.randInRange(400, 500);

    }
    // every tick, updateAI fires. Here attack selection is made.
    private void updateAI () {
        // updates the health bar of fluixon.
        fluixonWatcher.setCustomName(ChatColor.BOLD + "" + ChatColor.DARK_PURPLE + "Fluixon "
                + ChatColor.GREEN + (int)(fluixon.getHealth()) + "/" + (int)fluixon.getAttribute(Attribute.MAX_HEALTH).getValue());
        checkSpikeHit();
        fluixonBossbar.progress((float) (fluixon.getHealth() / fluixon.getAttribute(Attribute.MAX_HEALTH).getValue()));
        aliveTicks++;

        updateArena();

        // gets the health percentage, and checks if the phase has changed.
        healthPercentage = fluixon.getHealth() * 100 / maxHP;

        if (healthPercentage < 66) {
            if (phase < 2) {
                phase = 2;
                updatePhase2();
            }
        }
        if (healthPercentage < 33) {
            if (phase < 3) {
                phase = 3;
                updatePhase3();
            }
        }
        // phase 2 has a different AI compared to 1 and 3.
        if (phase == 2) {
            updateMeleeAI();

            return;
        }
        attackDowntime--;
        teleportDowntime--;
        dripstoneDropDowntime--;

        if (attackDowntime <= 0) {
            if (Math.random() < axeSlamChance) {
                axeSlam();
            }
            else {
                dripstoneShot();
            }
        }
        // teleport AI: 50% chance for an offensive teleport (near the player) and a 50% chance for a defensive one (away from player).
        if (teleportDowntime <= 0) {
            if (Math.random() < 0.5) {
                teleport(0, offensiveTeleportMaxDistance);
            }
            else {
                teleport(defensiveTeleportMinDistance, 40);
            }
        }
        if (dripstoneDropDowntime <= 0) {
            dripstoneDrop();
            attackDowntime = Math.max(attackDowntime, 60);
        }
    }
    private void updateMeleeAI () {
        attackDowntime--;
        dripstoneDropDowntime--;

        if (attackDowntime <= 0) {
            slashChain();
        }
        if (dripstoneDropDowntime <= 0) {
            dripstoneDrop();
        }
    }

    // updates the arena.
    private void updateArena () {
        for (ArenaLocation arenaLocation : arenaLocations) {
            if (world.getBlockAt(arenaLocation.getX(), yHeight, arenaLocation.getZ()).getBlockData().getMaterial() == Material.AIR) {
                arenaLocation.isBroken = true;
            }
        }
    }
    // set the blocks back to original.
    private void resetArena () {
        for (ArenaLocation arenaLocation : arenaLocations) {
            world.setBlockData(new Location(world, arenaLocation.getX(), yHeight, arenaLocation.getZ()), Material.WHITE_CONCRETE.createBlockData());
            arenaLocation.isBroken = false;
            world.setBlockData(new Location(world, arenaLocation.getX(), yHeight + 1, arenaLocation.getZ()), Material.AIR.createBlockData());
            world.setBlockData(new Location(world, arenaLocation.getX(), yHeight + 2, arenaLocation.getZ()), Material.AIR.createBlockData());
        }
        player.hideBossBar(fluixonBossbar);
    }
    private void updatePhase2 () {
        TitleSender.sendTitle("Phase 2", "Speed Demon", player, 1000, 3000, 1000,
                NamedTextColor.DARK_PURPLE, NamedTextColor.DARK_RED);
    }
    private void updatePhase3 () {
        TitleSender.sendTitle("Phase 3", "Trapper", player, 1000, 3000, 1000,
                NamedTextColor.DARK_PURPLE, NamedTextColor.DARK_RED);
    }


    // ATTACKS AND ABILITIES DOWN BELOW

    // note about downtimes: Some attacks will not just set a cooldown for its own attack, but also other attacks as well.
    // this is done to prevent near impossible to dodge attack combinations because two deadly attacks triggered at the same time.

    // checks if the player has been hit by a dripstone spike
    private void checkSpikeHit () {
        final int spikeDamage = 8;

        if (world.getBlockAt(player.getLocation()).getBlockData().getMaterial() == Material.POINTED_DRIPSTONE) {
            // despite proccing multiple times, I-frames prevent the player from taking too much damage.
            player.damage(spikeDamage);
            // due to this proccing multiple times due to checking being after every tick, this will actually launch the player
            // 8-10 blocks into the air.
            player.setVelocity(new Vector(0, 1, 0));
            world.playSound(player, Sound.BLOCK_POINTED_DRIPSTONE_HIT, SoundCategory.MASTER, 1, 1);
        }
    }

    // look and teleport to the desired yHeight.
    private void lookAtPlayer (int yHeight) {
        // first calculate the differences.
        double xDif = player.getX() - fluixon.getX();
        double zDif = player.getZ() - fluixon.getZ();

        // use trigonometry to find the right yaw to look at
        double lookYaw = Util.getYaw(-xDif, -zDif);
        FluixonBossPlugin.print(String.valueOf(lookYaw));

        // make fluixon look at the player
        Location moveRotation = fluixon.getLocation();
        moveRotation.setYaw((float)lookYaw);
        moveRotation.setY(yHeight);
        fluixon.teleport(moveRotation);
    }

    private final int ticksBeforeShot = 5; // warning time for attack
    private final double displacement = 1; // displacement of dripstone projectile
    private final double spread = 20; // spread of projectiles
    private void dripstoneShot () {
        // Dripstone shot is the first main attack. First, it grabs a random target location within 2 blocks of the player.
        // Then, with a 20 degree spread two other blocks are selected and dripstone is fired at them.
        lookAtPlayer((int) Math.max(fluixon.getY(), yHeight + 5));

        world.playSound(player, Sound.ITEM_CROSSBOW_LOADING_START, 1, 1);
        new BukkitRunnable() {
            @Override
            public void run () {
                WrapperPlayServerEntityAnimation swingAnimation = new WrapperPlayServerEntityAnimation(fluixon.getEntityId(),
                        WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM);
                PacketEvents.getAPI().getPlayerManager().sendPacket(player, swingAnimation);

                // create a dripstone projectile and fire it at the player.

                Vector targetDirection = Util.getRandomLocationNear(player.getLocation().add(0, 0.4, 0), displacement).subtract(fluixon.getLocation()).toVector();


                // launch two extra projectiles.
                Vector leftSpread = Util.rotate(targetDirection, -30);
                Vector rightSpread = Util.rotate(targetDirection, 30);

                if (phase == 3) {
                    new DripstoneProjectile(fluixon.getLocation().add(0, 1, 0),
                            targetDirection.normalize().multiply(0.8)).setBlockBreaking();
                    new DripstoneProjectile(fluixon.getLocation().add(0, 1, 0),
                            leftSpread.normalize().multiply(0.8)).setBlockBreaking();

                    new DripstoneProjectile(fluixon.getLocation().add(0, 1, 0),
                            rightSpread.normalize().multiply(0.8)).setBlockBreaking();
                }
                else {
                    new DripstoneProjectile(fluixon.getLocation().add(0, 1, 0),
                            targetDirection.normalize().multiply(0.8))/*.setBlockBreaking()*/;
                    new DripstoneProjectile(fluixon.getLocation().add(0, 1, 0),
                            leftSpread.normalize().multiply(0.8))/*.setBlockBreaking()*/;

                    new DripstoneProjectile(fluixon.getLocation().add(0, 1, 0),
                            rightSpread.normalize().multiply(0.8))/*.setBlockBreaking()*/;
                }

                world.playSound(player, Sound.ENTITY_ARROW_SHOOT, 1, 1);
            }
        }.runTaskLater(FluixonBossPlugin.getPlugin(), ticksBeforeShot);

        attackDowntime = Util.randInRange(dripstoneShotDowntimeMin, dripstoneShotDowntimeMax);
        dripstoneDropDowntime = Math.max(dripstoneDropDowntime, 40);
    }
    // axe slam attack.
    // Fluixon: Swings his axe
    // then the impact leaves a trail of dripstone following behind.
    private void axeSlam () {
        final int axeSlamDelayTicks = 10;
        final int axeSlamDamage = 10;

        // fluixon teleports low to signal his intention to slam his axe
        Location slamTeleportLocation = fluixon.getLocation();
        slamTeleportLocation.setY(yHeight + 3);
       // fluixon.teleport(slamTeleportLocation);
        lookAtPlayer(yHeight + 3);

        teleportDowntime = Math.max(teleportDowntime, 30);
        attackDowntime = Util.randInRange(axeSlamDowntimeMin, axeSlamDowntimeMax);
        dripstoneDropDowntime = Math.max(dripstoneDropDowntime, 60);

        // after a delay, slam the axe.
        new BukkitRunnable() {
            @Override
            public void run() {
                // send an animation packet to the player
                WrapperPlayServerEntityAnimation swingAnimation = new WrapperPlayServerEntityAnimation(fluixon.getEntityId(),
                        WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_OFF_HAND);

                PacketEvents.getAPI().getPlayerManager().sendPacket(player, swingAnimation);

                // finding the aim direction and where the sweep particles should be spawned in.
                Vector aimDirection = fluixon.getLocation().getDirection();

                // makes sure that the y-height does not change.
                aimDirection.setY(0);

                // the following sets the starting point of the slam location and the trail.
                // This way, no dripstone spikes will spawn behind fluixon.
                aimDirection.normalize().multiply(3);

                Location axeSlamLocation = fluixon.getLocation().add(aimDirection);
                axeSlamLocation.add(0, 1, 0);

                // animation using a sweep attack alongside the attack packet
                world.spawnParticle(Particle.SWEEP_ATTACK, axeSlamLocation, 4);
                //world.spawnParticle(Particle.SOUL_FIRE_FLAME, axeSlamLocation, 4);
                world.playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1);

                if (player.getLocation().add(0, 2, 0).distance(axeSlamLocation) <= 2) {
                    player.damage(axeSlamDamage);
                }

                dripstoneTrail(axeSlamLocation.clone(), aimDirection, 2.5);

                // in phase 2 and 3, two additional trails spawn in.
                if (phase > 1) {
                    Vector leftAimDirection = Util.rotate(aimDirection, -45);
                    Vector rightAimDirection = Util.rotate(aimDirection, 45);

                    dripstoneTrail(axeSlamLocation.clone(), leftAimDirection, 1.5);
                    dripstoneTrail(axeSlamLocation.clone(), rightAimDirection, 1.5);
                }
            }
        }.runTaskLater(FluixonBossPlugin.getPlugin(), axeSlamDelayTicks);
    }

    // trail of dripstone coming from the axe slam.
    private void dripstoneTrail (Location start, Vector direction, double affectRadius) {
        // max travel ticks: Max amount of ticks that the trail will travel for.
        final int maxTravelTicks = 60;

        // delay time between axe slam and trail creation.
        final int trailDelayTicks = 10;
        direction.normalize().multiply(1.2);

        new BukkitRunnable() {
            int runTicks = 0;
            @Override
            public void run() {
                if (runTicks == 0) {
                    world.playSound(player, Sound.BLOCK_ANVIL_LAND, 1, 1);
                }
                // checks all candidate blocks to replace.
                for (int x = (int)(start.getX() - affectRadius); x <= (int)(start.getX() + affectRadius); x++) {
                    for (int z = (int)(start.getZ() - affectRadius); z <= (int)(start.getZ() + affectRadius); z++) {

                        // if within distance of the trail direction, create a dripstone spike.
                        if (Util.distance(x, (int) start.getX(), z, (int) start.getZ()) <= affectRadius + 0.05) {
                            if (world.getBlockData(x, yHeight + 1, z).getMaterial() == Material.AIR) {
                                createDripstoneSpike(new Location(world, x, yHeight + 1, z));
                            }
                        }
                    }
                }
                start.add(direction);
                runTicks++;

                if (runTicks >= maxTravelTicks) {
                    this.cancel();
                }
            }
        }.runTaskTimer(FluixonBossPlugin.getPlugin(), trailDelayTicks, 1);
    }

    // this method handles the creation of one singular dripstone spike.
    private void createDripstoneSpike (Location spikeLocation) {
        final int spikeStayTicks = 60;

//        PointedDripstone dripstoneBase = (PointedDripstone) Material.POINTED_DRIPSTONE.createBlockData();
//        dripstoneBase.setVerticalDirection(BlockFace.UP);
//        dripstoneBase.setThickness(PointedDripstone.Thickness.BASE

        PointedDripstone dripstoneTip = (PointedDripstone) Material.POINTED_DRIPSTONE.createBlockData();
        dripstoneTip.setVerticalDirection(BlockFace.UP);
        dripstoneTip.setThickness(PointedDripstone.Thickness.TIP_MERGE);

        world.setBlockData(spikeLocation, dripstoneTip);
        world.setBlockData(spikeLocation.add(0, 1, 0), dripstoneTip);

        //world.playSound(player, Sound.BLOCK_POINTED_DRIPSTONE_PLACE, 1, 1);

        new BukkitRunnable() {
            int runTicks = 0;
            @Override
            public void run() {
                runTicks++;

                // time to remove the spike by setting it to air.
                if (runTicks >= spikeStayTicks) {
                    world.setBlockData(spikeLocation, Material.AIR.createBlockData());
                    world.setBlockData(spikeLocation.subtract(0, 1, 0), Material.AIR.createBlockData());
                    this.cancel();
                }
            }
        }.runTaskTimer(FluixonBossPlugin.getPlugin(), 0, 1);
    }

    // Drop attack drops dripstone from the sky, dealing massive damage if the player gets hit.

    private void dripstoneDrop () {
        final int dropPercentage = 60; // percentage of blocks which will be affected by a dripstone drop.
        final int dripstoneDropHeight = 185;
        final int dripstoneDropDelay = 5; // delay before the dripstone drops

        ArrayList<ArenaLocation> curRemainingValidArenaLocations = new ArrayList<>();

        // initialize dripstone data
        PointedDripstone dripstone = (PointedDripstone) Material.POINTED_DRIPSTONE.createBlockData();

        dripstone.setThickness(PointedDripstone.Thickness.BASE);
        dripstone.setVerticalDirection(BlockFace.DOWN);

        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 1));

        // warning
        TitleSender.sendTitle("LOOK OUT!", "Dripstone falling from the sky", player, 200, 500, 200,
                NamedTextColor.DARK_PURPLE, NamedTextColor.DARK_RED);

        // all non-broken arena locations are usable as target
        for (ArenaLocation arenaLocation : arenaLocations) {
            if (!arenaLocation.isBroken) {
                curRemainingValidArenaLocations.add(arenaLocation);
            }
        }

        final int dropCount = curRemainingValidArenaLocations.size() * dropPercentage / 100;
        ArenaLocation dropLocation;

        // time complexity of O(N^2) is okay as arenaSize is small enough
        // drop algorithm: spawn dripstone at the drop location supported by stone
        // after a set delay time, remove the supporting stone and drop the dripstone.
        for (int i = 0; i < dropCount; i++) {
            dropLocation = curRemainingValidArenaLocations.remove((int)(Math.random() * curRemainingValidArenaLocations.size()));

            world.setBlockData((int) dropLocation.getX(), dripstoneDropHeight, (int)dropLocation.getZ(), dripstone);
            world.setBlockData((int) dropLocation.getX(), dripstoneDropHeight + 1, (int)dropLocation.getZ(), Material.STONE.createBlockData());

            ArenaLocation finalDropLocation = dropLocation;

            new BukkitRunnable() {
                @Override
                public void run() {
                    world.setBlockData(finalDropLocation.getX(), dripstoneDropHeight + 1, finalDropLocation.getZ(), Material.AIR.createBlockData());
                }
            }.runTaskLater(FluixonBossPlugin.getPlugin(), dripstoneDropDelay);
        }

        dripstoneDropDowntime = Util.randInRange(dripstoneDropDowntimeMin, dripstoneDropDowntimeMax);
        attackDowntime = Math.max(attackDowntime, 120);
    }
    private final double defensiveTeleportMinDistance = 15; // min teleportation distance away from player for defensive teleport
    private final double offensiveTeleportMaxDistance = 6; // max teleportation distance away from player for offensive teleport
    private final int yMinTP = 3;
    private final int yMaxTP = 9;

    // teleport function responsible for fluixon's movement in the bossfight.
    private void teleport (double minDistance, double maxDistance) {
       // Location teleportLocation;
        int teleportDelay = 25; // teleport delay in ticks
        int yPos = yHeight + Util.randInRange(yMinTP, yMaxTP);

        // phase 2 has a different teleport AI / location.
        if (phase == 2) {
            yPos = yHeight + 2;
            teleportDelay = 5;
        }

        ArenaLocation arenaTeleportLocation;

        // in phase 2, the location must be a certain distance from fluixon's current position.
        if (phase == 2) {
            do {
                arenaTeleportLocation = arenaLocations.get((int)(Math.random() * arenaLocations.size()));

            } while (arenaTeleportLocation.distance(fluixon.getLocation()) < minDistance);
        }
        // in the other phases, it must be a within a certain distance of the player.
        else {
            do {
                arenaTeleportLocation = arenaLocations.get((int)(Math.random() * arenaLocations.size()));

            } while (arenaTeleportLocation.distance(player.getLocation()) < minDistance || arenaTeleportLocation.distance(player.getLocation()) > maxDistance);
        }

        Location teleportLocation = new Location(world, arenaTeleportLocation.getX(), yPos, arenaTeleportLocation.getZ());
        world.spawnParticle(Particle.LARGE_SMOKE, teleportLocation, 10);
        world.spawnParticle(Particle.REVERSE_PORTAL, teleportLocation, 30);
        world.spawnParticle(Particle.SOUL_FIRE_FLAME, teleportLocation, 10);

        new BukkitRunnable() {
            @Override
            public void run () {
                if (phase == 2) {
                    // in phase 2, teleportation also triggers the slash attack.
                    performSlash(fluixon.getLocation(), teleportLocation.clone());
                }
                fluixon.teleport(teleportLocation.add(0.5, 0, 0.5));
                attackDowntime = Math.max(attackDowntime, 20);
                teleportDowntime = Util.randInRange(teleportDowntimeMin, teleportDowntimeMax);

                world.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            }
        }.runTaskLater(FluixonBossPlugin.getPlugin(), teleportDelay);

        teleportDowntime = 1000;
    }
    // the main attack of phase 2.
    // fluixon first slashes at the player, then teleports a few times before finishing the attack combo off with an axe slam.

    private void slashChain () {
        int chains = Util.randInRange(1, 4); // extra teleports after the first slash
        int slashWarnTicks = 20;
        int slashTime = 5;

        double xDif = player.getX() - fluixon.getX();
        double zDif = player.getZ() - fluixon.getZ();

        // the first slash location is directly aimed at the player.
        Location firstSlashLocation = fluixon.getLocation().add(xDif * 2, 0, zDif * 2);
        //firstSlashLocation.add(xDif, 0, zDif);
        firstSlashLocation.setY(yHeight + 2);

        // warning via sound and glowing effect. Player has 1 second to react.
        world.playSound(player, Sound.BLOCK_BELL_USE, 7, 0.9f);
        fluixon.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, slashWarnTicks, 1));
        attackDowntime = Util.randInRange(slashChainDowntimeMin, slashChainDowntimeMax);

        // prevent dripstone from dropping while the slash attack is going on.
        dripstoneDropDowntime = Math.max(dripstoneDropDowntime, 60);

        new BukkitRunnable() {
            int runTicks = 0;
            @Override
            public void run () {
                // waits a few iterations before axe slamming, to give a warning
                // if teleportation happens too rapidly some teleportations won't register.
                if (runTicks > chains + 6) {
                    axeSlam();

                    this.cancel();
                }

                // first slash
                if (runTicks == 0) {
                    performSlash(fluixon.getLocation(), firstSlashLocation.clone());

                    print(firstSlashLocation.toString());
                    fluixon.teleport(firstSlashLocation);
                }
                else if (runTicks <= chains) {
                    teleport(8, 40);
                }
                runTicks++;

            }
        }.runTaskTimer(FluixonBossPlugin.getPlugin(), slashWarnTicks, slashTime);
    }
    // checks if the slash has hit any player.
    private void performSlash (Location start, Location end) {
        final double stepSize = 0.1;
        double distance = start.distance(end);
        final double hitRadius = 0.5;
        final int slashDamage = 9;

        double distanceTraveled = 0;

        Vector moveDirection = end.subtract(start).toVector().normalize().multiply(stepSize);

        // spawns the particles for the slash, and checks if any player is within the line of the slash.
        while (distanceTraveled < distance) {
            for (Player player : world.getNearbyPlayers(start, hitRadius)) {
                player.damage(slashDamage);
            }
            distanceTraveled += stepSize;

            world.spawnParticle(Particle.FLAME, start, 0);

            start.add(moveDirection);
        }
    }


    // EVENT HANDLERS


    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        // stops the fight when fluixon dies.
        if (event.getEntity() == fluixon) {
            event.getDrops().clear();
            bossAI.cancel();
            HandlerList.unregisterAll(this);
            resetArena();
        }
        if (event.getEntity() == player) {
//            bossAI.cancel();
//            fluixon.remove();
//            HandlerList.unregisterAll(this);
//            resetArena();
        }
    }

    @EventHandler
    public void onEntityDamaged (EntityDamageEvent event) {
        // prevents fluixon from taking damage when invulnerable (for example during dialogue)
        if (event.getEntity() == fluixon) {
            if (invulnerable) {
                event.setCancelled(true);

                return;
            }
        }
        // prevents fluixon from dying to his own dripstone.
        if (event.getDamageSource().getDamageType() == DamageType.FALLING_STALACTITE) {
            if (event.getEntity() == fluixon) {
                event.setCancelled(true);
            }
        }
    }
    // This event handler prevents the falling dripstone from the dripstone drop attack to drop items.
    @EventHandler
    public void preventDripstoneDropItem (EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof FallingBlock fallingBlock) {
            if (fallingBlock.getBlockData().getMaterial() == Material.POINTED_DRIPSTONE) {
                fallingBlock.setCancelDrop(true);
            }
        }
    }
}
