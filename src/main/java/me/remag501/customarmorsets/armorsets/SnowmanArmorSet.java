package me.remag501.customarmorsets.armorsets;

import com.github.retrooper.packetevents.protocol.particle.type.ParticleType;
import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes;
import kernitus.plugin.OldCombatMechanics.ModuleLoader;
import kernitus.plugin.OldCombatMechanics.utilities.storage.PlayerData;
import kernitus.plugin.OldCombatMechanics.utilities.storage.PlayerStorage;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.*;
import me.libraryaddict.disguise.disguisetypes.watchers.AreaEffectCloudWatcher;
import me.libraryaddict.disguise.disguisetypes.watchers.ArmorStandWatcher;
import me.libraryaddict.disguise.disguisetypes.watchers.FallingBlockWatcher;
import me.libraryaddict.disguise.disguisetypes.watchers.PlayerWatcher;
import me.remag501.customarmorsets.CustomArmorSets;
import me.remag501.customarmorsets.core.*;
import me.remag501.customarmorsets.utils.AttributesUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.w3c.dom.Attr;

import javax.management.Attribute;
import java.lang.annotation.Target;
import java.util.*;

public class SnowmanArmorSet extends ArmorSet implements Listener {

//    private final HashMap<UUID, Long> cooldowns = new HashMap<>();
    private long cooldown;

    public SnowmanArmorSet() {
        super(ArmorSetType.SNOWMAN);
    }

    @Override
    public void applyPassive(Player player) {
        player.sendMessage("✅ You equipped the snowman set");
//        DamageStats.setMobMultiplier(player.getUniqueId(),2000, TargetCategory.ALL);
//        DamageStats.setMobMultiplier(player.getUniqueId(),1, TargetCategory.UNDEAD);
        DefenseStats.setSourceReduction(player.getUniqueId(), 0.02f, TargetCategory.ALL);
//        OCMMain.getInstance()

        PlayerData playerData = PlayerStorage.getPlayerData(player.getUniqueId());
        UUID worldId = player.getWorld().getUID();
        String modesetName = "old";
        playerData.setModesetForWorld(worldId, modesetName);
        PlayerStorage.setPlayerData(player.getUniqueId(), playerData);
        PlayerStorage.scheduleSave();

        ModuleLoader.getModules().forEach(module -> module.onModesetChange(player));

    }

    @Override
    public void removePassive(Player player) {
        DamageStats.clearAll(player.getUniqueId());
        DefenseStats.clearAll(player.getUniqueId());
        player.sendMessage("❌ You removed the snowman set");

        PlayerData playerData = PlayerStorage.getPlayerData(player.getUniqueId());
        UUID worldId = player.getWorld().getUID();
        String modesetName = "new";
        playerData.setModesetForWorld(worldId, modesetName);
        PlayerStorage.setPlayerData(player.getUniqueId(), playerData);
        PlayerStorage.scheduleSave();

        ModuleLoader.getModules().forEach(module -> module.onModesetChange(player));

    }

    @Override
    public void triggerAbility(Player player) {

    }

//    private void spawnGlobe(Player player) {
//        final Location center = player.getLocation();
//        final World world = center.getWorld();
//        final int radius = 10; // A good size for a dome.
//        final long durationTicks = 20L * 10; // 10 seconds.
//        final Random random = new Random();
//        final Plugin plugin = CustomArmorSets.getInstance();
//
//        // A set to store the original state of blocks that make up the dome so we can remove them later.
//        Set<BlockState> originalStates = new HashSet<>();
//
//        // Play a dramatic sound and particle effect at the start.
//        world.playSound(center, Sound.ENTITY_POLAR_BEAR_WARNING, 1.5f, 1.0f);
//        world.spawnParticle(Particle.DRAGON_BREATH, center, 50, 0, 0, 0, 0.5);
//
//        // --- Step 1: Create the Dome with a growing pattern ---
//        final Material[] blockPalette = {
//                Material.ICE,
//                Material.PACKED_ICE,
//                Material.SNOW_BLOCK
//        };
//
//        Queue<BlockState> blockStatesToChange = new LinkedList<>();
//        Map<Integer, List<BlockState>> blocksByDistance = new TreeMap<>();
//
//        for (int x = -radius; x <= radius; x++) {
//            for (int y = -radius; y <= radius; y++) {
//                for (int z = -radius; z <= radius; z++) {
//                    // Calculate the squared distance from the center
//                    double distanceSquared = x * x + y * y + z * z;
//                    Location loc = new Location(world, center.getX() + x, center.getY() + y, center.getZ() + z);
//                    Block block = loc.getBlock();
//
//                    // Condition for blocks on the perimeter
//                    if (distanceSquared <= radius * radius && distanceSquared >= (radius - 1) * (radius - 1)) {
//                        blocksByDistance
//                                .computeIfAbsent((int) distanceSquared, k -> new LinkedList<>())
//                                .add(block.getState());
//                    }
//                    // Condition for blocks strictly inside the sphere (and not air)
//                    else if (distanceSquared < (radius - 1) * (radius - 1) && !block.getType().isAir()) {
//                        blocksByDistance
//                                .computeIfAbsent((int) distanceSquared, k -> new LinkedList<>())
//                                .add(block.getState());
//                    }
//                }
//            }
//        }
//
//// Shuffle the blocks within each distance group
//        for (List<BlockState> blockList : blocksByDistance.values()) {
//            Collections.shuffle(blockList);
//            blockStatesToChange.addAll(blockList);
//        }
//
//        // Schedule a repeating task to place blocks at a faster rate.
//        new BukkitRunnable() {
//            @Override
//            public void run() {
//                // Place 10 blocks per tick for a faster effect.
//                for (int i = 0; i < 35 && !blockStatesToChange.isEmpty(); i++) {
//                    BlockState originalState = blockStatesToChange.poll();
//                    originalStates.add(originalState);
//
//                    Block block = originalState.getBlock();
//                    Material blockType = blockPalette[random.nextInt(blockPalette.length)];
//                    block.setType(blockType);
//                }
//
//                if (blockStatesToChange.isEmpty()) {
//                    this.cancel();
//                }
//            }
//        }.runTaskTimer(plugin, 0L, 1L); // Runs every tick.
//
//        // --- Step 2: Trap and Debuff Mobs ---
//        Set<LivingEntity> trappedMobs = new HashSet<>();
//        for (Entity entity : world.getNearbyEntities(center, radius, radius, radius)) {
//            // Check if the entity is a LivingEntity and not the player
//            if (entity instanceof LivingEntity && !(entity instanceof Player)) {
//                LivingEntity livingEntity = (LivingEntity) entity;
//
//                // Apply debuffs to mobs.
//                livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) durationTicks, 2));
//                livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, (int) durationTicks, 1));
//                trappedMobs.add(livingEntity);
//            }
//        }
//
//        // Apply a buff to the player.
//        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) durationTicks, 1));
//
//        // --- Step 3: Schedule the Dome's Disappearance ---
//        Bukkit.getScheduler().runTaskLater(plugin, () -> {
//            // Remove the dome blocks by restoring their original state.
//            for (BlockState originalState : originalStates) {
//                originalState.update(true);
//            }
//
//            // Remove potion effects from trapped mobs and the player.
//            for (LivingEntity mob : trappedMobs) {
//                mob.removePotionEffect(PotionEffectType.SLOW);
//                mob.removePotionEffect(PotionEffectType.BLINDNESS);
//            }
//            player.removePotionEffect(PotionEffectType.SPEED);
//
//            // Play a final sound and particle effect.
//            world.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
//            world.spawnParticle(Particle.CLOUD, center, 30, 0, 0, 0, 0.5);
//
//        }, durationTicks);
//    }
//
//    public void triggerAbility(Player player) {
//        // spawnGlobe(player);
//        triggerIceBeam(player);
//
//
//    }
//
//    public void triggerIceBeam(Player player) {
//        final World world = player.getWorld();
//        final double beamLength = 20.0; // The maximum length of the ice beam.
//        final double damage = 6.0; // The damage dealt by the beam.
//        final double hitRadius = 1.0; // The radius to check for mobs around each beam particle.
//        final double durationInSeconds = 1;
//        final int durationInTicks = (int) (durationInSeconds * 20);
//
//        // Play a sound effect when the ability is triggered.
//        world.playSound(player.getLocation(), Sound.BLOCK_SNOW_BREAK, 1.5f, 1.0f);
//
//        new BukkitRunnable() {
//            private int ticksLived = 0;
//
//            @Override
//            public void run() {
//                // Cancel the task if the duration has passed or the player is no longer valid.
//                if (ticksLived >= durationInTicks || !player.isOnline()) {
//                    world.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
//                    this.cancel();
//                    return;
//                }
//
//                Location startLocation = player.getEyeLocation();
//                Vector direction = startLocation.getDirection().normalize();
//
//                // Loop to trace the path of the beam for this single tick.
//                for (double d = 0; d < beamLength; d += 0.5) {
//                    Location currentLocation = startLocation.clone().add(direction.clone().multiply(d));
//
//                    // Check if the beam hits a solid block.
//                    if (currentLocation.getBlock().getType().isSolid()) {
//                        // Spawn a bigger particle effect at the point of impact.
//                        world.spawnParticle(Particle.BLOCK_CRACK, currentLocation, 20, 0.5, 0.5, 0.5, Material.ICE.createBlockData());
//                        break; // Stop the beam when a block is hit.
//                    }
//
//                    // Spawn a particle to represent the continuous beam.
//                    world.spawnParticle(Particle.SNOWFLAKE, currentLocation, 1, 0, 0, 0, 0);
//
//                    // Check for mobs near the current beam location.
//                    world.getNearbyEntities(currentLocation, hitRadius, hitRadius, hitRadius)
//                            .stream()
//                            .filter(entity -> entity instanceof LivingEntity)
//                            .forEach(entity -> {
//                                LivingEntity mob = (LivingEntity) entity;
//                                // Don't hit the player who fired the beam.
//                                if (mob.getUniqueId().equals(player.getUniqueId())) {
//                                    return;
//                                }
//
//                                // Apply damage and slow effect.
//                                mob.damage(damage, player);
//                                mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 3, 2)); // 3 seconds of Slowness 2.
//
//                                // Spawn particles on the mob to show a hit.
//                                mob.getWorld().spawnParticle(Particle.BLOCK_CRACK, mob.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, Material.SNOW_BLOCK.createBlockData());
//                            });
//                }
//                ticksLived++;
//            }
//        }.runTaskTimer(CustomArmorSets.getInstance(), 0L, 1L);
//    }
//
////    @EventHandler
////    public void onEntityHit(EntityDamageByEntityEvent event) {
//////        if (!(event.getDamager() instanceof Player player)) return;
//////        if (!(event.getEntity() instanceof LivingEntity target)) return;
//////        if (event.getEntity() instanceof ArmorStand) {
//////            event.getEntity().playEffect(EntityEffect.TOTEM_RESURRECT);
//////            CustomArmorSets.getInstance().getLogger().info("Somthing");
//////            event.setCancelled(true);
//////        }
////    }
//
//    @EventHandler
//    public void onMove(PlayerMoveEvent event) {
//        if (true)
//            return;
//
//        Player player = event.getPlayer();
//        ArmorSet set = CustomArmorSetsCore.getArmorSet(player);
//        if (!(set instanceof SnowmanArmorSet)) return;
//
//        Location from = event.getFrom();
//        Location to = event.getTo();
//
//        // Ignore head turns
//        if (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ()) return;
//
//        // Leave a snowy trail!
//        Location blockBelow = player.getLocation().clone().subtract(0, 1, 0);
//        if (blockBelow.getBlock().getType().isAir()) return;
//
//        // Only on solid ground
//        if (!player.isOnGround()) return;
//
//        // Make trail block (snow layer or frosted ice)
//        Material oldType = blockBelow.getBlock().getType();
//        blockBelow.getBlock().setType(Material.SNOW_BLOCK);
//
//        // Remove after 3 seconds
//        Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("CustomArmorSets"), () -> {
//            if (blockBelow.getBlock().getType() == Material.SNOW_BLOCK) {
//                blockBelow.getBlock().setType(oldType);
//            }
//        }, 200L); // 60 ticks = 3 seconds
//
//        // Speed boost while on snow
//        if (blockBelow.getBlock().getType() == Material.SNOW_BLOCK) {
//            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 0, true, false));
//            player.spawnParticle(Particle.SNOW_SHOVEL, player.getLocation(), 8, 0.2, 0.2, 0.2, 0.01);
//        }
//    }
//
//    private static final Map<UUID, BukkitTask> runningTime = new HashMap<>();
//    private static final Map<UUID, Boolean> iceMode = new HashMap<>();
//    private final HashMap<Block, BlockState> blocksToReset = new HashMap<>();
//
//
//    @EventHandler
//    public void onSpint(PlayerToggleSprintEvent event) {
//        Player player = event.getPlayer();
//        if (!player.isSprinting() && runningTime.get(player.getUniqueId()) == null) {
//            // checks if player started sprinting and no task is running (for safety)
//            BukkitTask runnable = new BukkitRunnable() {
//                int seconds;
//                @Override
//                public void run() {
//                    seconds++;
//                    if (seconds <= 100) { // give speed under 3 seconds
//                        AttributesUtil.applySpeed(player, 1.15); // slowly increase speed
//                    }
//                    else if (iceMode.getOrDefault(player.getUniqueId(), false)) {
//
//
//                    } else {
//                        // Give player ice mode option
//                        player.setFreezeTicks(10);
//                        iceMode.put(player.getUniqueId(), false);
//                        player.setAllowFlight(true);
//                    }
//
//                }
//            }.runTaskTimer(CustomArmorSets.getInstance(), 0, 1);
//            AttributesUtil.removeSpeed(player);
//            runningTime.put(player.getUniqueId(), runnable);
//        }
//        else {
//            runningTime.remove(player.getUniqueId()).cancel();
//            iceMode.remove(player.getUniqueId());
//            if (player.getGameMode().equals(GameMode.SURVIVAL) || player.getGameMode().equals(GameMode.ADVENTURE))
//                player.setAllowFlight(false);
//        }
//    }
//
//    @EventHandler
//    public void onToggleFlight(PlayerToggleFlightEvent event) {
//        Player player = event.getPlayer();
//        Boolean canEnterIceMode = iceMode.get(player.getUniqueId());
//        if (canEnterIceMode == null)
//            return;
//        if (canEnterIceMode) { // Place ice under them
//            // Prevent horizontal movement during a jump
//            updateIceBridge(player, true);
//            event.setCancelled(true);
//            return;
//        }
//        // Check players that have not entered ice mode
//        iceMode.put(player.getUniqueId(), true);
//        event.setCancelled(true);
////        if (player.getGameMode().equals(GameMode.SURVIVAL) || player.getGameMode().equals(GameMode.ADVENTURE))
////            player.setAllowFlight(false);
//    }
//
//    @EventHandler
//    public void onPlayerMove(PlayerMoveEvent event) {
//        Player player = event.getPlayer();
//
//        if (!iceMode.getOrDefault(player.getUniqueId(), false)) {
//            return;
//        }
//
//        // Call the reusable function to update the ice bridge
//        if (player.isOnGround())
//            updateIceBridge(player, true);
//        else
//            updateIceBridge(player, false);
//
////        updateIceBridge(player, false);
//    }
//
//    // --- Helper Functions ---
//
//    /**
//     * Places the 3x3 ice bridge under the player's feet.
//     */
//    private void updateIceBridge(Player player, boolean allowAir) {
//        Location playerLocation = player.getLocation();
//        int trailRadius = 1;
//        int yOffset = player.isSneaking() ? -2 : -1;
//
//        // Logic to remove the blocks on top when sneaking
//        if (player.isSneaking()) {
//            for (int x = -trailRadius; x <= trailRadius; x++) {
//                for (int z = -trailRadius; z <= trailRadius; z++) {
//                    Block blockToRemove = playerLocation.clone().add(x, -1, z).getBlock();
//                    // Only remove the block if it's ice to avoid unexpected behavior
//                    if (blockToRemove.getType() == Material.ICE) {
//                        blockToRemove.setType(Material.AIR);
//                        blocksToReset.remove(blockToRemove); // Clean up the map
//                    }
//                }
//            }
//        }
//
//        // Original logic to place the new ice blocks
//        for (int x = -trailRadius; x <= trailRadius; x++) {
//            for (int z = -trailRadius; z <= trailRadius; z++) {
//                Block blockToPlaceIceOn = playerLocation.clone().add(x, yOffset, z).getBlock();
//                if (blockToPlaceIceOn.getType() != Material.AIR || allowAir)
////                    continue;
//
//                // The condition `blockToPlaceIceOn.getType() != Material.AIR || allowAir`
//                // is not needed in a simple case and can be simplified.
//                if (blockToPlaceIceOn.getType() != Material.ICE) {
//                    BlockState originalState = blockToPlaceIceOn.getState();
//                    blocksToReset.put(blockToPlaceIceOn, originalState);
//                    blockToPlaceIceOn.setType(Material.ICE);
//
//                    Bukkit.getScheduler().runTaskLater(CustomArmorSets.getInstance(), () -> {
//                        BlockState oldState = blocksToReset.get(blockToPlaceIceOn);
//                        if (oldState != null) {
//                            oldState.update(true);
//                            blocksToReset.remove(blockToPlaceIceOn);
//                        }
//                    }, 20L * 3);
//                }
//            }
//        }
//    }
//
//    /**
//     * Clears all remaining ice blocks for a player.
//     * This is useful when the ice mode is turned off.
//     */
//    private void clearIceBridge(Player player) {
//        // Find and clear any remaining ice blocks in the vicinity
//        Location playerLocation = player.getLocation();
//        int radius = 3;
//
//        for (int x = -radius; x <= radius; x++) {
//            for (int y = -radius; y <= radius; y++) {
//                for (int z = -radius; z <= radius; z++) {
//                    Block block = playerLocation.clone().add(x, y, z).getBlock();
//                    if (blocksToReset.containsKey(block)) {
//                        BlockState originalState = blocksToReset.get(block);
//                        originalState.update(true);
//                        blocksToReset.remove(block);
//                    }
//                }
//            }
//        }
//    }

}
