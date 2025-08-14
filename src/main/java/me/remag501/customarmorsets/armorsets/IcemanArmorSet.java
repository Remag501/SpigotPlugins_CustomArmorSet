package me.remag501.customarmorsets.armorsets;

import me.remag501.customarmorsets.CustomArmorSets;
import me.remag501.customarmorsets.core.ArmorSet;
import me.remag501.customarmorsets.core.ArmorSetType;
import me.remag501.customarmorsets.core.CustomArmorSetsCore;
import me.remag501.customarmorsets.utils.AttributesUtil;
import me.remag501.customarmorsets.utils.CooldownBarUtil;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
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

import java.util.*;

public class IcemanArmorSet extends ArmorSet implements Listener {

    private static final int COOLDOWN = 5;
    private static final Map<UUID, BukkitTask> runningTime = new HashMap<>();
    private static final Map<UUID, BukkitTask> ultTask = new HashMap<>();
    private static final Map<UUID, Boolean> iceMode = new HashMap<>();
    private static final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final Map<UUID, Integer> freezeCharges = new HashMap<>();
    private static final Map<UUID, Integer> snowCharge = new HashMap<>();
    private static final Map<UUID, Map<Block, BlockState>> playerBlocksToReset = new HashMap<>();

    public IcemanArmorSet() {
        super(ArmorSetType.ICEMAN);
    }

    @Override
    public void applyPassive(Player player) {
        AttributesUtil.applySpeed(player, 1.25);
        player.sendMessage("❄ You equipped the Iceman set");
        // Populate maps
        UUID uuid = player.getUniqueId();
        cooldowns.put(uuid, System.currentTimeMillis());
        snowCharge.put(uuid, 0);
        freezeCharges.put(uuid, 0);
        playerBlocksToReset.put(uuid, new HashMap<>());
        // Start task for ult
        startTask(player);

    }

    @Override
    public void removePassive(Player player) {
        AttributesUtil.removeSpeed(player);
        UUID uuid = player.getUniqueId();
        ultTask.remove(uuid).cancel();
        cooldowns.remove(uuid);
        snowCharge.remove(uuid);
        freezeCharges.remove(uuid);
        playerBlocksToReset.remove(uuid);
        player.sendMessage("❄ You removed the Iceman set");
        CooldownBarUtil.restorePlayerBar(player);
    }

    private void startTask(Player player) {
        UUID uuid = player.getUniqueId();
        // Run while set is active
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                ticks++;
                snowCharge.put(uuid, Math.min(snowCharge.get(uuid) + 1, 100));
                CooldownBarUtil.setLevel(player, snowCharge.get(uuid));
                if (ticks % 5 == 0) {// temp
                    freezeCharges.put(uuid, Math.min(5, freezeCharges.get(uuid) + 1));
//                    player.sendTitle("Freeze Charge ❄ " + freezeCharges.get(uuid) + "/ 5", "subtitle");
                }
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§bFreeze Charge ❄ " + freezeCharges.get(uuid) + " / 5"));
            }
        }.runTaskTimer(CustomArmorSets.getInstance(), 0, 20);
        ultTask.put(uuid, task);
    }

    @Override
    public void triggerAbility(Player player) {
        UUID uuid = player.getUniqueId();
        if (player.isSneaking() && snowCharge.get(uuid) >= 100) {
            spawnGlobe(player);
            snowCharge.put(uuid, 50);
            return;
        } else if (player.isSneaking()) {
            player.sendMessage("Not enough snow charge");
            return;
        }
        if ((System.currentTimeMillis() - cooldowns.get(uuid)) / 1000 > COOLDOWN) {
            triggerIceBeam(player);
            cooldowns.put(uuid, System.currentTimeMillis());
        } else {
            player.sendMessage("Ability on cooldown");
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(CustomArmorSetsCore.getArmorSet(player) instanceof IcemanArmorSet)) return;

        if (event.getEntity() instanceof LivingEntity target) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 1));
            player.sendMessage("§bYou froze your target!");
        }
    }

    private void spawnGlobe(Player player) {
        final Location center = player.getLocation();
        final World world = center.getWorld();
        final int radius = 10; // A good size for a dome.
        final long durationTicks = 20L * 10; // 10 seconds.
        final Random random = new Random();
        final Plugin plugin = CustomArmorSets.getInstance();

        // A set to store the original state of blocks that make up the dome so we can remove them later.
        Set<BlockState> originalStates = new HashSet<>();

        // Play a dramatic sound and particle effect at the start.
        world.playSound(center, Sound.ENTITY_POLAR_BEAR_WARNING, 1.5f, 1.0f);
        world.spawnParticle(Particle.DRAGON_BREATH, center, 50, 0, 0, 0, 0.5);

        // --- Step 1: Create the Dome with a growing pattern ---
        final Material[] blockPalette = {
                Material.ICE,
                Material.PACKED_ICE,
                Material.SNOW_BLOCK
        };

        Queue<BlockState> blockStatesToChange = new LinkedList<>();
        Map<Integer, List<BlockState>> blocksByDistance = new TreeMap<>();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    // Calculate the squared distance from the center
                    double distanceSquared = x * x + y * y + z * z;
                    Location loc = new Location(world, center.getX() + x, center.getY() + y, center.getZ() + z);
                    Block block = loc.getBlock();

                    // Condition for blocks on the perimeter
                    if (distanceSquared <= radius * radius && distanceSquared >= (radius - 1) * (radius - 1)) {
                        blocksByDistance
                                .computeIfAbsent((int) distanceSquared, k -> new LinkedList<>())
                                .add(block.getState());
                    }
                    // Condition for blocks strictly inside the sphere (and not air)
                    else if (distanceSquared < (radius - 1) * (radius - 1) && !block.getType().isAir()) {
                        blocksByDistance
                                .computeIfAbsent((int) distanceSquared, k -> new LinkedList<>())
                                .add(block.getState());
                    }
                }
            }
        }

// Shuffle the blocks within each distance group
        for (List<BlockState> blockList : blocksByDistance.values()) {
            Collections.shuffle(blockList);
            blockStatesToChange.addAll(blockList);
        }

        // Schedule a repeating task to place blocks at a faster rate.
        new BukkitRunnable() {
            @Override
            public void run() {
                // Place 10 blocks per tick for a faster effect.
                for (int i = 0; i < 35 && !blockStatesToChange.isEmpty(); i++) {
                    BlockState originalState = blockStatesToChange.poll();
                    originalStates.add(originalState);

                    Block block = originalState.getBlock();
                    Material blockType = blockPalette[random.nextInt(blockPalette.length)];
                    block.setType(blockType);
                }

                if (blockStatesToChange.isEmpty()) {
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L); // Runs every tick.

        // --- Step 2: Trap and Debuff Mobs ---
        Set<LivingEntity> trappedMobs = new HashSet<>();
        for (Entity entity : world.getNearbyEntities(center, radius, radius, radius)) {
            // Check if the entity is a LivingEntity and not the player
            if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                LivingEntity livingEntity = (LivingEntity) entity;

                // Apply debuffs to mobs.
                livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) durationTicks, 2));
                livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, (int) durationTicks, 1));
                trappedMobs.add(livingEntity);
            }
        }

        // Apply a buff to the player.
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) durationTicks, 1));

        // --- Step 3: Schedule the Dome's Disappearance ---
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Remove the dome blocks by restoring their original state.
            for (BlockState originalState : originalStates) {
                originalState.update(true);
            }

            // Remove potion effects from trapped mobs and the player.
            for (LivingEntity mob : trappedMobs) {
                mob.removePotionEffect(PotionEffectType.SLOW);
                mob.removePotionEffect(PotionEffectType.BLINDNESS);
            }
            player.removePotionEffect(PotionEffectType.SPEED);

            // Play a final sound and particle effect.
            world.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
            world.spawnParticle(Particle.CLOUD, center, 30, 0, 0, 0, 0.5);

        }, durationTicks);
    }

    public void triggerIceBeam(Player player) {
        final World world = player.getWorld();
        final double beamLength = 20.0; // The maximum length of the ice beam.
        final double damage = 6.0; // The damage dealt by the beam.
        final double hitRadius = 1.0; // The radius to check for mobs around each beam particle.
        final double durationInSeconds = 1;
        final int durationInTicks = (int) (durationInSeconds * 20);

        // Play a sound effect when the ability is triggered.
        world.playSound(player.getLocation(), Sound.BLOCK_SNOW_BREAK, 1.5f, 1.0f);

        new BukkitRunnable() {
            private int ticksLived = 0;

            @Override
            public void run() {
                // Cancel the task if the duration has passed or the player is no longer valid.
                if (ticksLived >= durationInTicks || !player.isOnline()) {
                    world.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
                    this.cancel();
                    return;
                }

                Location startLocation = player.getEyeLocation();
                Vector direction = startLocation.getDirection().normalize();

                // Loop to trace the path of the beam for this single tick.
                for (double d = 0; d < beamLength; d += 0.5) {
                    Location currentLocation = startLocation.clone().add(direction.clone().multiply(d));

                    // Check if the beam hits a solid block.
                    if (currentLocation.getBlock().getType().isSolid()) {
                        // Spawn a bigger particle effect at the point of impact.
                        world.spawnParticle(Particle.BLOCK_CRACK, currentLocation, 20, 0.5, 0.5, 0.5, Material.ICE.createBlockData());
                        break; // Stop the beam when a block is hit.
                    }

                    // Spawn a particle to represent the continuous beam.
                    world.spawnParticle(Particle.SNOWFLAKE, currentLocation, 1, 0, 0, 0, 0);

                    // Check for mobs near the current beam location.
                    world.getNearbyEntities(currentLocation, hitRadius, hitRadius, hitRadius)
                            .stream()
                            .filter(entity -> entity instanceof LivingEntity)
                            .forEach(entity -> {
                                LivingEntity mob = (LivingEntity) entity;
                                // Don't hit the player who fired the beam.
                                if (mob.getUniqueId().equals(player.getUniqueId())) {
                                    return;
                                }

                                // Apply damage and slow effect.
                                mob.damage(damage, player);
                                mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 3, 2)); // 3 seconds of Slowness 2.

                                // Spawn particles on the mob to show a hit.
                                mob.getWorld().spawnParticle(Particle.BLOCK_CRACK, mob.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, Material.SNOW_BLOCK.createBlockData());
                            });
                }
                ticksLived++;
            }
        }.runTaskTimer(CustomArmorSets.getInstance(), 0L, 1L);
    }

    // Handle ice bridge passive

    @EventHandler
    public void onSpint(PlayerToggleSprintEvent event) {
        Player player = event.getPlayer();
        if (!(CustomArmorSetsCore.getArmorSet(player) instanceof IcemanArmorSet)) return;
        // Now we start

        if (!player.isSprinting() && runningTime.get(player.getUniqueId()) == null) {
            // checks if player started sprinting and no task is running (for safety)
            BukkitTask runnable = new BukkitRunnable() {
                int seconds;
                @Override
                public void run() {
                    seconds++;
                    if (seconds <= 100) { // give speed under 3 seconds
                        AttributesUtil.applySpeed(player, 1.15); // slowly increase speed
                    }
                    else if (iceMode.getOrDefault(player.getUniqueId(), false)) {


                    } else {
                        // Give player ice mode option
                        player.setFreezeTicks(10);
                        iceMode.put(player.getUniqueId(), false);
                        player.setAllowFlight(true);
                    }

                }
            }.runTaskTimer(CustomArmorSets.getInstance(), 0, 1);
            AttributesUtil.removeSpeed(player);
            runningTime.put(player.getUniqueId(), runnable);
        }
        else {
            runningTime.remove(player.getUniqueId()).cancel();
            iceMode.remove(player.getUniqueId());
            if (player.getGameMode().equals(GameMode.SURVIVAL) || player.getGameMode().equals(GameMode.ADVENTURE))
                player.setAllowFlight(false);
        }
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (!(CustomArmorSetsCore.getArmorSet(player) instanceof IcemanArmorSet)) return;
        // Now we start

        Boolean canEnterIceMode = iceMode.get(player.getUniqueId());
        if (canEnterIceMode == null)
            return;
        if (canEnterIceMode) { // Place ice under them
            // Prevent horizontal movement during a jump
            updateIceBridge(player, true);
            event.setCancelled(true);
            return;
        }
        // Check players that have not entered ice mode
        iceMode.put(player.getUniqueId(), true);
        event.setCancelled(true);
//        if (player.getGameMode().equals(GameMode.SURVIVAL) || player.getGameMode().equals(GameMode.ADVENTURE))
//            player.setAllowFlight(false);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!(CustomArmorSetsCore.getArmorSet(player) instanceof IcemanArmorSet)) return;
        // Now we start

        if (!iceMode.getOrDefault(player.getUniqueId(), false)) {
            return;
        }

        // Call the reusable function to update the ice bridge
        if (player.isOnGround())
            updateIceBridge(player, true);
        else
            updateIceBridge(player, false);
    }


    /**
     * Places the 3x3 ice bridge under the player's feet.
     */
    private void updateIceBridge(Player player, boolean allowAir) {
        Location playerLocation = player.getLocation();
        int trailRadius = 1;
        int yOffset = player.isSneaking() ? -2 : -1;
        Map<Block, BlockState> blocksToReset = playerBlocksToReset.get(player.getUniqueId());

        // Logic to remove the blocks on top when sneaking
        if (player.isSneaking()) {
            for (int x = -trailRadius; x <= trailRadius; x++) {
                for (int z = -trailRadius; z <= trailRadius; z++) {
                    Block blockToRemove = playerLocation.clone().add(x, -1, z).getBlock();
                    // Only remove the block if it's ice to avoid unexpected behavior
                    if (blockToRemove.getType() == Material.ICE) {
                        blockToRemove.setType(Material.AIR);
                        blocksToReset.remove(blockToRemove); // Clean up the map
                    }
                }
            }
        }

        // Original logic to place the new ice blocks
        for (int x = -trailRadius; x <= trailRadius; x++) {
            for (int z = -trailRadius; z <= trailRadius; z++) {
                Block blockToPlaceIceOn = playerLocation.clone().add(x, yOffset, z).getBlock();
                if (blockToPlaceIceOn.getType() != Material.AIR || allowAir)
//                    continue;

                    // The condition `blockToPlaceIceOn.getType() != Material.AIR || allowAir`
                    // is not needed in a simple case and can be simplified.
                    if (blockToPlaceIceOn.getType() != Material.ICE) {
                        BlockState originalState = blockToPlaceIceOn.getState();
                        blocksToReset.put(blockToPlaceIceOn, originalState);
                        blockToPlaceIceOn.setType(Material.ICE);

                        Bukkit.getScheduler().runTaskLater(CustomArmorSets.getInstance(), () -> {
                            BlockState oldState = blocksToReset.get(blockToPlaceIceOn);
                            if (oldState != null) {
                                oldState.update(true);
                                blocksToReset.remove(blockToPlaceIceOn);
                            }
                        }, 20L * 3);
                    }
            }
        }
    }

}

