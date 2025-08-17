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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class IcemanArmorSet extends ArmorSet implements Listener {

    private static final int COOLDOWN = 5;
    private static final Map<UUID, BukkitTask> runningTime = new ConcurrentHashMap<>();
    private static final Map<UUID, BukkitTask> ultTask = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> iceMode = new HashMap<>();
    private static final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final Map<UUID, Integer> freezeCharges = new HashMap<>();
    private static final Map<UUID, Integer> snowCharge = new HashMap<>();
    private static final Map<UUID, Map<Block, BlockState>> playerBlocksToReset = new HashMap<>();
    // Decay task for mobs
    private static final Map<UUID, BukkitTask> decayTasks = new HashMap<>();
    private static final Map<UUID, BukkitTask> particleTasks = new HashMap<>();
    private static final Map<UUID, Long> mobChargeCooldowns = new HashMap<>();
    // Map to store freeze stacks for each mob
    private static final Map<UUID, Integer> freezeStacks = new HashMap<>();
    private static final Map<UUID, Boolean> playerIceBeam = new HashMap<>();



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
        playerIceBeam.put(uuid, false);
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
        playerIceBeam.remove(uuid, false);
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
//                if (ticks % 5 == 0) {// temp
//                    freezeCharges.put(uuid, Math.min(5, freezeCharges.get(uuid) + 1));
////                    player.sendTitle("Freeze Charge ❄ " + freezeCharges.get(uuid) + "/ 5", "subtitle");
//                }
                int charges = freezeCharges.getOrDefault(uuid, 0);
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§bFreeze Charge ❄ " + charges + " / 5"));
            }
        }.runTaskTimer(CustomArmorSets.getInstance(), 0, 1);
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
        // First check if player is using ice beam
        if (playerIceBeam.get(uuid)) {
            playerIceBeam.put(uuid, false);
            player.sendMessage("Paused ts");
            return;
        }
        // Check if player has enough charges
        int charges = freezeCharges.getOrDefault(uuid, 0);
        if (charges > 0) {
            triggerIceBeam(player, charges);
            cooldowns.put(uuid, System.currentTimeMillis());
        } else {
            player.sendMessage("§cYou don't have enough freeze charges!");
        }
    }

    public void triggerIceBeam(Player player, int initialCharges) {
        final World world = player.getWorld();
        final double beamLength = 20.0;
        final double hitRadius = 1.0;

        world.playSound(player.getLocation(), Sound.BLOCK_SNOW_BREAK, 1.5f, 1.0f);
        playerIceBeam.put(player.getUniqueId(), true);

        new BukkitRunnable() {
            private int ticksLived = 0;

            @Override
            public void run() {
                UUID playerUUID = player.getUniqueId();
                int currentCharges = freezeCharges.getOrDefault(playerUUID, 0);
                boolean usingIceBeam = playerIceBeam.getOrDefault(playerUUID, false);

                // Stop the beam if the player has no charges or is no longer online
                if (currentCharges <= 0 || !player.isOnline() || !usingIceBeam) {
                    playerIceBeam.put(player.getUniqueId(), false);
                    freezeCharges.put(playerUUID, Math.max(0, currentCharges - 1));
                    world.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
                    this.cancel();
                    return;
                }

                // Drain 1 charge per second (20 ticks)
                if (ticksLived % 20 == 0) {
                    freezeCharges.put(playerUUID, Math.max(0, currentCharges - 1));
                }

                Location startLocation = player.getEyeLocation();
                Vector direction = startLocation.getDirection().normalize();

                // Ray-trace to check for blocks, so the beam stops
                RayTraceResult result = world.rayTraceBlocks(startLocation, direction, beamLength);
                double actualBeamLength = beamLength;
                if (result != null && result.getHitBlock() != null) {
                    actualBeamLength = result.getHitBlock().getLocation().distance(startLocation);
                }

                for (double d = 0; d < actualBeamLength; d += 0.5) {
                    Location currentLocation = startLocation.clone().add(direction.clone().multiply(d));

                    world.spawnParticle(Particle.SNOWFLAKE, currentLocation, 1, 0, 0, 0, 0);

                    world.getNearbyEntities(currentLocation, hitRadius, hitRadius, hitRadius)
                            .stream()
                            .filter(entity -> entity instanceof LivingEntity)
                            .forEach(entity -> {
                                LivingEntity mob = (LivingEntity) entity;
                                if (mob.getUniqueId().equals(player.getUniqueId())) {
                                    return;
                                }

                                // Apply freeze stacks based on initial charges
                                int currentStacks = freezeStacks.getOrDefault(mob.getUniqueId(), 0);
                                int newStacks = Math.min(currentStacks + initialCharges, 5);
                                freezeStacks.put(mob.getUniqueId(), newStacks);
                                mob.damage(initialCharges * 1.5, player); // Damage scales with charge

                                if (newStacks >= 5) {
                                    mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 3, 5));
                                    mob.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 3, 1));
                                } else if (newStacks >= 3) {
                                    mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 3, 2));
                                } else {
                                    mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 3, 0));
                                }

                                mob.getWorld().spawnParticle(Particle.BLOCK_CRACK, mob.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, Material.SNOW_BLOCK.createBlockData());
                            });
                }
                ticksLived++;
            }
        }.runTaskTimer(CustomArmorSets.getInstance(), 0L, 1L);
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

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(CustomArmorSetsCore.getArmorSet(player) instanceof IcemanArmorSet)) return; // Corrected this line

        if (event.getEntity() instanceof LivingEntity target) {
            // Check if the player's attack is a fully charged melee hit (cooldown is >= 1.0)
            if (player.getAttackCooldown() >= 1.0) {
                UUID mobUUID = target.getUniqueId();
                UUID playerUUID = player.getUniqueId();
                int previousStacks = freezeStacks.getOrDefault(mobUUID, 0);

                // Increment freeze stacks on the target
                int currentStacks = previousStacks + 1;
                freezeStacks.put(mobUUID, Math.min(currentStacks, 5));

                // Add 1 freeze charge to the player (max 5) only if the cooldown has passed
                long currentTime = System.currentTimeMillis();
                long lastChargeTime = mobChargeCooldowns.getOrDefault(mobUUID, 0L);
                if (currentTime - lastChargeTime < 2500) { // 2.5 second freeze invulnerable
                    return;
                }

                freezeCharges.put(playerUUID, Math.min(freezeCharges.getOrDefault(playerUUID, 0) + 1, 5));
                mobChargeCooldowns.put(mobUUID, currentTime);

                // Apply effects based on freeze stacks
                if (currentStacks >= 5) { // Tier 3: Completely Frozen
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 5));
                    target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 1));
                    target.setAI(false); // Disable AI to prevent attack animation
                    player.sendMessage("§bYour target is completely frozen!");
                } else if (currentStacks >= 3) { // Tier 2: Frozen
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 2));
                    target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20, 1));
                    player.sendMessage("§bYou froze your target!");
                } else { // Tier 1: Chilled
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20, 0));
                    player.sendMessage("§bYou chilled your target!");
                }

                // Restart decay task for this mob
                if (decayTasks.containsKey(mobUUID)) {
                    decayTasks.remove(mobUUID).cancel();
                }
                BukkitTask decayTask = new BukkitRunnable() {
                    @Override
                    public void run() {
                        int stacks = freezeStacks.getOrDefault(mobUUID, 0);
                        if (stacks <= 3 && !target.isDead()) {
                            // Only re-enable AI if the mob is still alive
                            target.setAI(true);
                        }
                        if (stacks > 0) {
                            freezeStacks.put(mobUUID, stacks - 1);
                        }
                        else {
                            freezeStacks.remove(mobUUID);
                            // Re-enable AI and cancel particle task when stacks fall to 0
                            if (particleTasks.containsKey(mobUUID)) {
                                particleTasks.remove(mobUUID).cancel();
                            }
                            decayTasks.remove(mobUUID);
                            this.cancel();
                        }
                    }
                }.runTaskTimer(CustomArmorSets.getInstance(), 20, 20); // Decay one stack per second
                decayTasks.put(mobUUID, decayTask);

                // Start or restart a particle task for this mob
                if (!particleTasks.containsKey(mobUUID)) {
                    BukkitTask particleTask = new BukkitRunnable() {
                        @Override
                        public void run() {
                            int stacks = freezeStacks.getOrDefault(mobUUID, 0);
                            if (stacks >= 5) {
                                target.getWorld().spawnParticle(Particle.BLOCK_CRACK, target.getLocation().add(0, 1, 0), 200, 0.5, 0.5, 0.5, Material.ICE.createBlockData());
                            } else if (stacks >= 3) {
                                target.getWorld().spawnParticle(Particle.CLOUD, target.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
                            } else if (stacks >= 1) {
                                target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5);
                            }
                        }
                    }.runTaskTimer(CustomArmorSets.getInstance(), 0, 10); // Run every half second
                    particleTasks.put(mobUUID, particleTask);
                }
            }
        }
    }

    // Thaw reaction
    @EventHandler
    public void onEntityFireDamage(EntityDamageEvent event) {
        // Ensure the damaged entity is a mob and not a player
        if (!(event.getEntity() instanceof LivingEntity mob)) return;
        // Check if the damage cause is related to fire
        if (event.getCause() == EntityDamageEvent.DamageCause.FIRE || event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK || event.getCause() == EntityDamageEvent.DamageCause.LAVA) {
            UUID mobUUID = mob.getUniqueId();
            int stacks = freezeStacks.getOrDefault(mobUUID, 0);

            if (stacks > 3)
                mob.setAI(true);

            // If the mob has any freeze stacks, it takes extra damage
            if (stacks > 0) {
                // Damage calculation: The original damage + a bonus based on freeze stacks
                double extraDamage = stacks * 1.5;
                event.setDamage(event.getDamage() + extraDamage);

                // Spawn a visual melting effect and play a sound
                mob.getWorld().spawnParticle(Particle.LAVA, mob.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
                mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 1.0f, 1.0f);

                // Reset freeze stacks, cooldown, and AI to prevent further freezing
                freezeStacks.remove(mobUUID);
                mobChargeCooldowns.remove(mobUUID);
                if (decayTasks.containsKey(mobUUID)) {
                    decayTasks.remove(mobUUID).cancel();
                }
                if (particleTasks.containsKey(mobUUID)) {
                    particleTasks.remove(mobUUID).cancel();
                }
            }

            // Remove fire from mob
            mob.setFireTicks(0);
        }
    }

}

