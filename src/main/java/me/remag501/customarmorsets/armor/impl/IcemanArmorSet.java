package me.remag501.customarmorsets.armor.impl;

import me.remag501.bgscore.api.TaskHelper;
import me.remag501.customarmorsets.CustomArmorSets;
import me.remag501.customarmorsets.armor.ArmorSet;
import me.remag501.customarmorsets.armor.ArmorSetType;
import me.remag501.customarmorsets.manager.ArmorManager;
import me.remag501.customarmorsets.service.AttributesService;
import me.remag501.customarmorsets.manager.CooldownBarManager;
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

public class IcemanArmorSet extends ArmorSet {

    // Constants for balancing and readability
    private static final int ULT_DURATION_SECONDS = 10;
    private static final int SNOW_CHARGE_MAX = 100;
    private static final int FREEZE_CHARGE_MAX = 5;
    private static final double ICE_BEAM_LENGTH = 20.0;
    private static final double ICE_BEAM_HIT_RADIUS = 1.0;
    private static final int MOB_CHARGE_COOLDOWN_MS = 2500; // 2.5 seconds
    private static final int ICE_BRIDGE_TRAIL_RADIUS = 1;

    // Maps to store player-specific state, using UUID for keying
    private static final List<UUID> runningTime = new ArrayList<>();
    private static final List<UUID> ultTask = new ArrayList<>();
    private static final Map<UUID, Boolean> iceMode = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> freezeCharges = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> domeCharge = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<Block, BlockState>> playerBlocksToReset = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> playerIceBeam = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> playerInUlt = new ConcurrentHashMap<>();

    // Maps for mob-specific state
    private static final List<UUID> decayTasks = new ArrayList<>();
    private static final List<UUID> particleTasks = new ArrayList<>();
    private static final Map<UUID, Long> mobChargeCooldowns = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> freezeStacks = new ConcurrentHashMap<>();

    private final TaskHelper api;
    private final ArmorManager armorManager;
    private final CooldownBarManager cooldownBarManager;
    private final AttributesService attributesService;

    public IcemanArmorSet(TaskHelper api, ArmorManager armorManager, CooldownBarManager cooldownBarManager, AttributesService attributesService) {
        super(ArmorSetType.ICEMAN);
        this.api = api;
        this.armorManager = armorManager;
        this.cooldownBarManager = cooldownBarManager;
        this.attributesService = attributesService;
    }

    @Override
    public void applyPassive(Player player) {
        attributesService.applySpeed(player, 1.25);

        UUID uuid = player.getUniqueId();
        cooldowns.put(uuid, System.currentTimeMillis());
        domeCharge.put(uuid, 0);
        freezeCharges.put(uuid, 0);
        playerBlocksToReset.put(uuid, new HashMap<>());
        playerIceBeam.put(uuid, false);
        playerInUlt.put(uuid, false);

        // Start task for ult
        startTask(player);

        // Register listener(s)
        UUID id = player.getUniqueId();
        // 1. Sprinting Listener (Ice Mode Entry)
        api.subscribe(PlayerToggleSprintEvent.class)
                .owner(id)
                .namespace(type.getId())
                .filter(e -> e.getPlayer().getUniqueId().equals(id))
                .handler(this::onSpint);

        // 2. Flight Listener (Ice Bridge Activation)
        api.subscribe(PlayerToggleFlightEvent.class)
                .owner(id)
                .namespace(type.getId())
                .filter(e -> e.getPlayer().getUniqueId().equals(id))
                .handler(this::onToggleFlight);

        // 3. Move Listener (Bridge Construction)
        api.subscribe(PlayerMoveEvent.class)
                .owner(id)
                .namespace(type.getId())
                .filter(e -> e.getPlayer().getUniqueId().equals(id))
                .handler(this::onPlayerMove);

        // 4. Melee Combat (Freeze Stacks)
        api.subscribe(EntityDamageByEntityEvent.class)
                .owner(id)
                .namespace(type.getId())
                .filter(e -> e.getDamager().getUniqueId().equals(id))
                .handler(this::onEntityDamageByEntity);

        // 5. Thaw Reaction (Player Fire Aspect / Direct Fire Hit)
        api.subscribe(EntityDamageByEntityEvent.class)
                .owner(id)
                .namespace(type.getId())
                .filter(e -> e.getDamager().getUniqueId().equals(id))
                .handler(this::onPlayerFireDamageMob);

        // 6. Global Thaw Reaction (Environmental Fire/Lava)
        // Note: We don't filter for 'id' here because this logic applies to MOBS
        // that were frozen by the player. The task cleanup relies on the mob's UUID.
        api.subscribe(EntityDamageEvent.class)
                .owner(id)
                .namespace(type.getId())
                .handler(this::onEntityFireDamage);
    }

    @Override
    public void removePassive(Player player) {
        attributesService.removeSpeed(player);
        UUID uuid = player.getUniqueId();
        resetIceBridgeBlocks(player);
//        ultTask.remove(uuid).cancel();
//        BukkitTask runningTask = runningTime.remove(uuid);
//        if (runningTask != null)
//            runningTask.cancel();
        cooldowns.remove(uuid);
        domeCharge.remove(uuid);
        freezeCharges.remove(uuid);
        playerBlocksToReset.remove(uuid);
        playerIceBeam.remove(uuid);
        playerIceBeam.remove(uuid);
        cooldownBarManager.restorePlayerBar(player);

        api.unregisterListener(player.getUniqueId(), type.getId());
        api.stopTask(player.getUniqueId(), "iceman_run");
        api.stopTask(player.getUniqueId(), "iceman_charge");
        api.stopTask(player.getUniqueId(), "iceman_ability");
        api.stopTask(player.getUniqueId(), "iceman_ult");
        api.stopTask(player.getUniqueId(), "iceman_ult_dmg");
        api.stopTask(player.getUniqueId(), "iceman_globe");
    }

    @Override
    public void triggerAbility(Player player) {
        UUID uuid = player.getUniqueId();
        if (player.isSneaking() && domeCharge.get(uuid) >= 100) {
            resetIceBridgeBlocks(player); // Clean up blocks that interfere globe
            leaveIceMode(player);
            spawnGlobe(player);
            domeCharge.put(uuid, 0);
            return;
        } else if (player.isSneaking()) {
            player.sendMessage("§c§l(!) §cNot enough snow charge");
            return;
        }
        // First check if player is using ice beam
        if (playerIceBeam.get(uuid)) {
            playerIceBeam.put(uuid, false);
//            player.sendMessage("Paused ts");
            return;
        }
        // Check if player has enough charges
        int charges = freezeCharges.getOrDefault(uuid, 0);
        if (charges > 0) {
            triggerIceBeam(player);
            cooldowns.put(uuid, System.currentTimeMillis());
            domeCharge.put(uuid, Math.min(domeCharge.get(uuid) + 3 * charges, 100));
        } else {
            player.sendMessage("§c§l(!) §cYou don't have enough freeze charges!");
        }
    }

    @EventHandler
    public void onSpint(PlayerToggleSprintEvent event) {
        // Handle ice bridge passive
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!(armorManager.getArmorSet(player) instanceof IcemanArmorSet)) return;
        // Now we start

        if (!player.isSprinting() && runningTime.contains(uuid)) {
            // checks if player started sprinting and no task is running (for safety)
            api.subscribe(player.getUniqueId(), "iceman_run", 0, 1, (ticks) -> {
                if (freezeCharges.get(uuid) <= 0) {
                    leaveIceMode(player);
                } else if (iceMode.getOrDefault(uuid, false)) {
                    if (ticks % 100 == 0) // Runs every time: five second (Could enter ice mode moment so drop is random between 0-time)
                        freezeCharges.put(uuid, freezeCharges.get(uuid) - 1);
                }
                else if (!playerInUlt.get(uuid) && ticks > 100) {
                    // Give player option to enter ice mode option
                    player.setFreezeTicks(10);
                    iceMode.put(uuid, false);
                    player.setAllowFlight(true);
                }
                return false;
            });

            attributesService.removeSpeed(player);
            runningTime.add(uuid);
        }
        else {
            leaveIceMode(player);
        }
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (!(armorManager.getArmorSet(player) instanceof IcemanArmorSet)) return;
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
        if (!(armorManager.getArmorSet(player) instanceof IcemanArmorSet)) return;
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

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(armorManager.getArmorSet(player) instanceof IcemanArmorSet)) return; // Corrected this line

        if (event.getEntity() instanceof LivingEntity target) {
            // Check if the player's attack is a fully charged melee hit (cooldown is >= 1.0)
            if (player.getAttackCooldown() >= 1.0) {
                UUID playerUUID = player.getUniqueId();
                UUID mobUUID = target.getUniqueId();
                // Handle ICD First
                long currentTime = System.currentTimeMillis();
                long lastChargeTime = mobChargeCooldowns.getOrDefault(mobUUID, 0L);
                if (currentTime - lastChargeTime < MOB_CHARGE_COOLDOWN_MS) { // 2.5 second freeze invulnerable
                    return;
                }
                // Add ICD if its over
                mobChargeCooldowns.put(target.getUniqueId(), currentTime);
                // Freeze the enemy and give players snow charge + freeze charge
                freezeCharges.put(playerUUID, Math.min(freezeCharges.getOrDefault(playerUUID, 0) + 1, 5));
                domeCharge.put(playerUUID, Math.min(domeCharge.get(playerUUID) + 3, 100)); // Add snow dome charge
                manageMobFreezeTask(target, 1);
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

            if (stacks == 0)
                return;
            if (stacks > 3)
                mob.setAI(true);

            // If the mob has any freeze stacks, it takes extra damage
            if (stacks > 0) {
                // Damage calculation: The original damage + a bonus based on freeze stacks
                double extraDamage = 1 + stacks * 1.5;
                event.setDamage(event.getDamage() * extraDamage);

                // Spawn a visual melting effect and play a sound
                mob.getWorld().spawnParticle(Particle.LAVA, mob.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
                mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 1.0f, 1.0f);

                // Reset freeze stacks, cooldown, and AI to prevent further freezing
                freezeStacks.put(mobUUID, 0);
                mobChargeCooldowns.remove(mobUUID);
                if (decayTasks.contains(mobUUID)) {
                    decayTasks.remove(mobUUID);
                    api.stopTask(mobUUID, "decay");
                }
                if (particleTasks.contains(mobUUID)) {
                    particleTasks.remove(mobUUID);
                    api.stopTask(mobUUID, "particle");
                }

                // Remove fire from mob
                mob.setFireTicks(0);
            }
        }
    }

    @EventHandler
    public void onPlayerFireDamageMob(EntityDamageByEntityEvent event) {
        // Check if the damager is a player
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(armorManager.getArmorSet(player) instanceof IcemanArmorSet)) return;
        // Check if the damaged entity is a mob and if the cause is an entity attack
        if (!(event.getEntity() instanceof LivingEntity mob) || event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;

        // This is a crucial check. After the event, is the mob on fire?
        // If so, it's likely from an effect like Fire Aspect.
        if (mob.getFireTicks() > 0) {
            UUID mobUUID = mob.getUniqueId();
            int stacks = freezeStacks.getOrDefault(mobUUID, 0);

            // Apply extra damage and logic here
            if (stacks > 0) {
                double extraDamage = 1 + stacks * 1.5;
                event.setDamage(event.getDamage() + extraDamage);

                // Give player dome charge
                UUID playerUUID = player.getUniqueId();
                domeCharge.put(playerUUID, Math.min(domeCharge.getOrDefault(playerUUID, 0) + 10, SNOW_CHARGE_MAX));

                // Your existing visual and sound effects
                mob.getWorld().spawnParticle(Particle.LAVA, mob.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
                mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 1.0f, 1.0f);

                // Set freeze stack to 1
                freezeStacks.put(mobUUID, 1);

                player.sendMessage("§a§l(!) §aYou thawed you're opponent");
            }
        }
    }

    private void manageMobFreezeTask(LivingEntity mob, int stacksToAdd) {
        UUID mobUUID = mob.getUniqueId();
        int previousStacks = freezeStacks.getOrDefault(mobUUID, 0);

        int currentStacks = Math.min(previousStacks + stacksToAdd, FREEZE_CHARGE_MAX);
        freezeStacks.put(mobUUID, currentStacks);

        // Apply effects based on stacks
        if (currentStacks >= FREEZE_CHARGE_MAX) {
            mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 5));
            mob.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 1));
            mob.setAI(false);
        } else if (currentStacks >= 3) {
            mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 2));
            mob.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20, 1));
        } else {
            mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20, 0));
        }

        // Cancel existing tasks to prevent conflicts
//        Optional.of(decayTasks.remove(mobUUID));
//        Optional.of(particleTasks.remove(mobUUID));

        // Schedule decay task
        api.subscribe(mobUUID, "decay", 20, 20, (tick) -> {
            int stacks = freezeStacks.getOrDefault(mobUUID, 0);
            if (stacks <= 3 && !mob.isDead()) {
                mob.setAI(true);
            }
            if (stacks > 0) {
                freezeStacks.put(mobUUID, stacks - 1);
            }
            else {
                freezeStacks.remove(mobUUID);
                Optional.ofNullable(particleTasks.remove(mobUUID));
                decayTasks.remove(mobUUID);
                return true;
            }
            return false;
        });
        decayTasks.add(mobUUID);

        // Schedule particle task
        api.subscribe(mobUUID, "particle", 0, 10, (ticks) -> {
            int stacks = freezeStacks.getOrDefault(mobUUID, 0);
            if (stacks >= FREEZE_CHARGE_MAX) {
                mob.getWorld().spawnParticle(Particle.BLOCK_CRACK, mob.getLocation().add(0, 1, 0), 200, 0.5, 0.5, 0.5, Material.ICE.createBlockData());
            } else if (stacks >= 3) {
                mob.getWorld().spawnParticle(Particle.CLOUD, mob.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
            } else if (stacks >= 1) {
                mob.getWorld().spawnParticle(Particle.SNOWFLAKE, mob.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5);
            }
            return false;
        });
        particleTasks.add(mobUUID);
    }

    private void startTask(Player player) {

        // Run while set is active
        UUID uuid = player.getUniqueId();
        api.subscribe(player.getUniqueId(), "iceman_charge", 0, 1, (ticks) -> {
            if (ticks % 40 == 0) // Add charge every two seconds
                domeCharge.put(uuid, Math.min(domeCharge.get(uuid) + 50, 100));

            cooldownBarManager.setLevel(player, domeCharge.get(uuid));
            int charges = freezeCharges.getOrDefault(uuid, 0);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§bFreeze Charge ❄ " + charges + " / 5"));
            return false;
        });

        ultTask.add(uuid);
    }

    private void leaveIceMode(Player player) {
        UUID uuid = player.getUniqueId();
        if (iceMode.containsKey(uuid)) {
            if (runningTime.remove(uuid))
                api.stopTask(uuid, "iceman_run");
            iceMode.remove(uuid);
            if (player.getGameMode().equals(GameMode.SURVIVAL) || player.getGameMode().equals(GameMode.ADVENTURE))
                player.setAllowFlight(false);
        }
    }

    public void triggerIceBeam(Player player) {
        final World world = player.getWorld();
        final double beamLength = ICE_BEAM_LENGTH;
        final double hitRadius = ICE_BEAM_HIT_RADIUS;

        UUID playerUUID = player.getUniqueId();
        world.playSound(player.getLocation(), Sound.BLOCK_SNOW_BREAK, 1.5f, 1.0f);
        playerIceBeam.put(player.getUniqueId(), true);
        int initialCharges = freezeCharges.get(playerUUID);

        api.subscribe(player.getUniqueId(), "iceman_ability", 0, 1, (ticksLived) -> {
            int currentCharges = freezeCharges.getOrDefault(playerUUID, 0);
            boolean usingIceBeam = playerIceBeam.getOrDefault(playerUUID, false);

            // Stop the beam if the player has no charges or is no longer online
            if (currentCharges <= 0 || !player.isOnline() || !usingIceBeam) {
                playerIceBeam.put(player.getUniqueId(), false);
                freezeCharges.put(playerUUID, Math.max(0, currentCharges - 1));
                world.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
                return true;
            }

            // A set to track mobs hit in THIS specific tick
            final Set<UUID> mobsProcessedThisTick = new HashSet<>();

            // Drain 1 charge per second (20 ticks)
            if (ticksLived % 20 == 0) {
                freezeCharges.put(playerUUID, Math.max(0, currentCharges - 1));
            }

            Location startLocation = player.getEyeLocation();
            Vector direction = startLocation.getDirection().normalize();

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

                            // Add this crucial check to see if the mob has already been processed this tick
                            if (mobsProcessedThisTick.contains(mob.getUniqueId())) {
                                return; // Skip if we've already handled this mob in this tick
                            }

                            mobsProcessedThisTick.add(mob.getUniqueId()); // Add the mob to the set

                            if (ticksLived % 20 == 0) {
                                // Your existing logic for applying freeze and damage
                                manageMobFreezeTask(mob, currentCharges);
                            }
                            if (ticksLived % 10 == 0) {
                                mob.damage(initialCharges * 1.5);
                            }
                        });
            }
            return false;
        });
    }

    private void spawnGlobe(Player player) {
        final Location center = player.getLocation();
        final World world = center.getWorld();
        final int radius = 10; // A good size for a dome.
        final long durationTicks = 20L * ULT_DURATION_SECONDS; // 10 seconds.
        final Random random = new Random();

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

        // Mention player is in ult
        playerInUlt.put(player.getUniqueId(), true);

        // Schedule a repeating task to place blocks at a faster rate.
        api.subscribe(player.getUniqueId(), "iceman_globe", 0, 1, (ticks) -> {
            for (int i = 0; i < 35 && !blockStatesToChange.isEmpty(); i++) {
                BlockState originalState = blockStatesToChange.poll();
                originalStates.add(originalState);

                Block block = originalState.getBlock();
                Material blockType = blockPalette[random.nextInt(blockPalette.length)];
                block.setType(blockType);
            }

            if (blockStatesToChange.isEmpty()) {
                return true;
            }
            return false;
        });

        // --- Step 2: Trap, Freeze, Damage, and Heal ---
        Set<LivingEntity> trappedMobs = new HashSet<>();
        for (Entity entity : world.getNearbyEntities(center, radius, radius, radius)) {
            // Check if the entity is a LivingEntity and not the player
            if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                LivingEntity mob = (LivingEntity) entity;
                trappedMobs.add(mob);

                // Add 1 freeze stack
                UUID mobUUID = mob.getUniqueId();
                int currentStacks = freezeStacks.getOrDefault(mobUUID, 0);
                freezeStacks.put(mobUUID, Math.min(currentStacks + 1, 5));

                // Restart decay task for this mob
                if (decayTasks.contains(mobUUID)) {
                    if (decayTasks.remove(mobUUID))
                        api.stopTask(mobUUID, "decay");
                }
                api.subscribe(player.getUniqueId(), "iceman_ult_dmg", 20, 20, (ticks) -> {
                    int stacks = freezeStacks.getOrDefault(mobUUID, 0);
                    if (stacks <= 3 && !mob.isDead()) {
                        mob.setAI(true);
                    }
                    if (stacks > 0) {
                        freezeStacks.put(mobUUID, stacks - 1);
                    }
                    else {
                        freezeStacks.remove(mobUUID);
                        if (particleTasks.contains(mobUUID)) {
                            if (particleTasks.remove(mobUUID))
                                api.stopTask(mobUUID, "particle");
                        }
                        decayTasks.remove(mobUUID);
                        return true;
                    }
                    return false;
                });
                decayTasks.add(mobUUID);

                // Start or restart a particle task for this mob
                if (!particleTasks.contains(mobUUID)) {
                    api.subscribe(mobUUID, "particle", 0, 10, (ticks) -> {
                        int stacks = freezeStacks.getOrDefault(mobUUID, 0);
                        if (stacks >= 5) {
                            mob.getWorld().spawnParticle(Particle.BLOCK_CRACK, mob.getLocation().add(0, 1, 0), 200, 0.5, 0.5, 0.5, Material.ICE.createBlockData());
                        } else if (stacks >= 3) {
                            mob.getWorld().spawnParticle(Particle.CLOUD, mob.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
                        } else if (stacks >= 1) {
                            mob.getWorld().spawnParticle(Particle.SNOWFLAKE, mob.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5);
                        }
                        return false;
                    });
                    particleTasks.add(mobUUID);
                }
            }
        }

        // Apply DoT and healing in a separate task
        api.subscribe(player.getUniqueId(),"iceman_ult", 0, 20, (ticks) -> {
            if (!playerInUlt.get(player.getUniqueId())) {
                return true;
            }
            // Damage mobs and heal the player
            for (LivingEntity mob : trappedMobs) {
                if (mob.isDead() || !mob.isValid()) {
                    continue;
                }
                // Damage over time
                mob.damage(5.0, player);
                // Make sure mob has at least one freeze stack
                if (freezeStacks.get(mob.getUniqueId()) < 3)
                    manageMobFreezeTask(mob, 1);
                // Add a chilling effect particle
                mob.getWorld().spawnParticle(Particle.DRIP_WATER, mob.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5);
            }
            // Apply a buff to the player.
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 25, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 25, 1));
            return false;
        });

        // --- Step 3: Schedule the Dome's Disappearance ---
        api.delay((int) durationTicks, () -> {
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
            playerInUlt.put(player.getUniqueId(), false);
        });
    }

    private void resetIceBridgeBlocks(Player player) {
        UUID uuid = player.getUniqueId();
        Map<Block, BlockState> blocksToReset = playerBlocksToReset.get(uuid);

        if (blocksToReset != null) {
            for (BlockState originalState : blocksToReset.values()) {
                originalState.update(true);
            }
            blocksToReset.clear(); // Clear the map after resetting all blocks
        }
    }

    private void updateIceBridge(Player player, boolean allowAir) {
        // Places the 3x3 ice bridge under the player's feet.
        Location playerLocation = player.getLocation();
        int trailRadius = ICE_BRIDGE_TRAIL_RADIUS;
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

                        api.delay(20 * 3, () -> {
                            BlockState oldState = blocksToReset.get(blockToPlaceIceOn);
                            if (oldState != null) {
                                oldState.update(true);
                                blocksToReset.remove(blockToPlaceIceOn);
                            }
                        });
                    }
            }
        }
    }

}

