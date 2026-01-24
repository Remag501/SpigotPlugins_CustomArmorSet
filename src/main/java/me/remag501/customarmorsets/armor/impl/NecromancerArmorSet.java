package me.remag501.customarmorsets.armor.impl;

import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes;
import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.compatibility.CompatibilityManager;
import io.lumine.mythic.bukkit.compatibility.LibsDisguisesSupport;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.*;
import me.libraryaddict.disguise.disguisetypes.watchers.AreaEffectCloudWatcher;
import me.libraryaddict.disguise.disguisetypes.watchers.PlayerWatcher;
import me.remag501.bgscore.api.TaskHelper;
import me.remag501.customarmorsets.CustomArmorSets;
import me.remag501.customarmorsets.armor.ArmorSet;
import me.remag501.customarmorsets.armor.ArmorSetType;
import me.remag501.customarmorsets.armor.TargetCategory;
import me.remag501.customarmorsets.listener.MythicMobsListener;
import me.remag501.customarmorsets.manager.ArmorManager;
import me.remag501.customarmorsets.manager.CooldownBarManager;
import me.remag501.customarmorsets.manager.DamageStatsManager;
import me.remag501.customarmorsets.manager.PlayerSyncManager;
import me.remag501.customarmorsets.service.AttributesService;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class NecromancerArmorSet extends ArmorSet implements Listener {

    private static final Long RESURRECTION_COOLDOWN = 120 * 1000L;
    private static final Map<UUID, Long> resurrectionCooldowns = new HashMap<>();
    private static final Map<ArmorStand, MythicMob> killedMobs = new HashMap<>();
    private static final Map<UUID, List<ActiveMob>> summonedMobs = new HashMap<>();
    private static final Map<UUID, BukkitTask> summonsTask = new HashMap<>();
    private static final Map<ActiveMob, Long> summonTime = new HashMap<>();
    private static final Map<UUID, ActiveMob> controlledMobs = new HashMap<>();
    private static final Map<UUID, BukkitTask> controlTasks = new HashMap<>();
    private static final Map<UUID, ArmorStand> decoys = new HashMap<>();
    private static final Map<UUID, BukkitRunnable> decoyTasks = new HashMap<>();

    private static final String RESURRECTED_MOB_PREFIX = MythicMobsListener.getPrefix();

    private final TaskHelper api;
    private final ArmorManager armorManager;
    private final DamageStatsManager damageStatsManager;
    private final AttributesService attributesService;
    private final PlayerSyncManager playerSyncManager;

    public NecromancerArmorSet(TaskHelper api, ArmorManager armorManager, DamageStatsManager damageStatsManager, AttributesService attributesService, PlayerSyncManager playerSyncManager) {
        super(ArmorSetType.NECROMANCER);
        this.api = api;
        this.armorManager = armorManager;
        this.damageStatsManager = damageStatsManager;
        this.attributesService = attributesService;
        this.playerSyncManager = playerSyncManager;
    }

    @Override
    public void applyPassive(Player player) {
//        player.sendMessage("You equipped the Necromancer set");
        damageStatsManager.setMobMultiplier(player.getUniqueId(), 1.5f, TargetCategory.UNDEAD);
        UUID uuid = player.getUniqueId();
        summonedMobs.put(uuid, new ArrayList<>());
        summonsTask.put(uuid, new BukkitRunnable() {
            private boolean notified = false;
            private long lastStartTime = 0L;
            @Override
            public void run() {
                List<ActiveMob> mobs = summonedMobs.get(uuid);

                // Skip if cooldown is not running
                if (!resurrectionCooldowns.containsKey(uuid)) {
                    notified = false; // Reset when cooldown not present
                    lastStartTime = 0L;
                } else {
                    long startTime = resurrectionCooldowns.get(uuid);
                    long now = System.currentTimeMillis();

                    // Detect passive reset: start time changed (new cooldown started)
                    if (startTime > lastStartTime) {
                        notified = false; // Allow next notification
                        lastStartTime = startTime;
                    }

                    // If cooldown has expired and player wasn't notified yet
                    if (!notified && now - startTime >= RESURRECTION_COOLDOWN) {
                        player.sendMessage( "§c§l(!) §cYour resurrection is ready!");
                        notified = true; // Prevent spam until reset
                    }
                }


                if (mobs == null || mobs.isEmpty()) return;

                for (int i = 0; i < mobs.size(); i++) {
                    ActiveMob activeMob = mobs.get(i);
                    if (activeMob == null || activeMob.getEntity() == null || !activeMob.getEntity().isValid())
                        continue;

                    AbstractEntity abstractEntity = activeMob.getEntity();
                    if (abstractEntity == null) continue;

                    // Check time entity is alive
                    long oldTime = summonTime.get(activeMob);
                    long newTime = System.currentTimeMillis();
                    int secondsPassed = (int) ((newTime - oldTime) / 1000);

                    if (secondsPassed > 30) {
                        despawnMob(activeMob);
                        i--;
                        continue;
                    }

                    Entity entity = abstractEntity.getBukkitEntity();
                    // Check if player is controlling entity
                    boolean isControlledByPlayer = false;
                    ActiveMob controlledMob = controlledMobs.get(player.getUniqueId());
                    if (controlledMob != null && controlledMob.equals(activeMob))
                        isControlledByPlayer = true;

                    // Slowly kill off mobs over 5 seconds
                    if (secondsPassed >= 25) {
                        //
                        if (isControlledByPlayer) {
                            player.sendMessage("§c§l(!) §cThis summon is decaying, it cannot be controlled.");
                            stopControlling(player);
                        }
                        // Hurt gradually
                        double newHealth = Math.max(0, abstractEntity.getHealth() * 0.8); // heal 0.5 heart per tick
                        abstractEntity.setHealth(newHealth);

                        abstractEntity.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 2, false, false));
                        abstractEntity.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 2, false, false));
                        abstractEntity.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 100, 2, false, false));
                        entity.getWorld().spawnParticle(
                                Particle.SMOKE_LARGE,
                                entity.getLocation().add(0, 0.5, 0),
                                10, 0.3, 0.3, 0.3, 0.01
                        );
                        entity.addScoreboardTag("isDecaying");
                    } else {
                        double maxHealth = abstractEntity.getMaxHealth();
                        double newHealth = Math.min(maxHealth, abstractEntity.getHealth() + 1.0); // heal 0.5 heart per tick
                        // Heal gradually
                        if (isControlledByPlayer)
                            player.setHealth(newHealth);
                        else
                            abstractEntity.setHealth(newHealth);

                        // Normal Particle trail
                        entity.getWorld().spawnParticle(
                                Particle.SOUL,
                                entity.getLocation().add(0, 0.5, 0),
                                5, 0.3, 0.3, 0.3, 0.01
                        );
                    }

                    // Teleport if too far
                    if (entity.getLocation().distanceSquared(player.getLocation()) > 400) { // 20 blocks squared
                        Location safeLoc = player.getLocation().clone().add(0, 1, 0);
                        entity.teleport(safeLoc);
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 10));

        // Register listener(s)
        UUID id = player.getUniqueId();
        // 1. Flight Control (Stop controlling mobs)
        api.subscribe(PlayerToggleFlightEvent.class)
                .owner(id)
                .namespace(type.getId())
                .filter(e -> e.getPlayer().getUniqueId().equals(id))
                .handler(this::onPlayerCancelFlight);

        // 2. Soul Harvest (Killing MythicMobs)
        api.subscribe(EntityDeathEvent.class)
                .owner(id)
                .namespace(type.getId())
                .filter(e -> e.getEntity().getKiller() != null && e.getEntity().getKiller().getUniqueId().equals(id))
                .handler(this::onPlayerKillMob);

        // 3. Life & Death Logic (Resurrection / Controlled Death)
        // We use a lighter filter here because the player is often the Victim
        api.subscribe(EntityDamageEvent.class)
                .owner(id)
                .namespace(type.getId())
                .filter(e -> e.getEntity() instanceof Player || MythicBukkit.inst().getMobManager().isActiveMob(e.getEntity().getUniqueId()))
                .handler(this::onEntityDamage);

        // 4. Combat & Decoy Logic
        api.subscribe(EntityDamageByEntityEvent.class)
                .owner(id)
                .namespace(type.getId())
                .handler(this::onEntityHit);

        // 5. Restriction: No Sprinting while controlling
        api.subscribe(PlayerToggleSprintEvent.class)
                .owner(id)
                .namespace(type.getId())
                .filter(e -> e.getPlayer().getUniqueId().equals(id))
                .handler(this::onSprint);

        // 6. Restriction: No Item Pickup while "Dead" or Controlling
        api.subscribe(EntityPickupItemEvent.class)
                .owner(id)
                .namespace(type.getId())
                .filter(e -> e.getEntity().getUniqueId().equals(id))
                .handler(this::onPickup);
    }

    @Override
    public void removePassive(Player player) {
        damageStatsManager.clearAll(player.getUniqueId());
        List<ActiveMob> mobs = summonedMobs.get(player.getUniqueId());
        while (!mobs.isEmpty()) {
            despawnMob(mobs.get(0));
        }
        summonedMobs.remove(player.getUniqueId());
        summonsTask.get(player.getUniqueId()).cancel();

        api.unregisterListener(player.getUniqueId(), type.getId());
    }

    @Override
    public void triggerAbility(Player player) {
        // Relevant variables
        List<ActiveMob> mobs = summonedMobs.get(player.getUniqueId());

        if (resurrectionCooldowns.get(player.getUniqueId()) != null && resurrectionCooldowns.get(player.getUniqueId()) == -1) {
            player.sendMessage("§c§l(!) §cYou cannot use abilities while dead");
            return;
        }

        // Let player stop controlling their mob
        if (controlledMobs.containsKey(player.getUniqueId())) {
            stopControlling(player);
            return;
        }


        // Check if player is trying to control mob
        if (player.isSneaking() && mobs != null && !mobs.isEmpty()) {
            ActiveMob selectedMob;
            Location eyeLoc = player.getEyeLocation();
            Vector direction = eyeLoc.getDirection().normalize();

            double maxDistance = 20; // configurable range
            double threshold = 1.5;  // max distance from ray to mob center

            ActiveMob closestMob = null;
            double closestDistance = maxDistance;

            for (ActiveMob mob : mobs) {
                if (mob == null || mob.getEntity() == null || !mob.getEntity().isValid()) continue;

                Location mobLoc = mob.getEntity().getBukkitEntity().getLocation().add(0, mob.getEntity().getBukkitEntity().getHeight() / 2, 0);
                Vector toMob = mobLoc.toVector().subtract(eyeLoc.toVector());

                double projection = toMob.dot(direction); // how far along the ray
                if (projection < 0 || projection > maxDistance) continue; // behind or too far

                Vector closestPoint = eyeLoc.toVector().add(direction.multiply(projection));
                double distanceToRay = mobLoc.toVector().distance(closestPoint);

                if (distanceToRay <= threshold && projection < closestDistance) {
                    closestDistance = projection;
                    closestMob = mob;
                }
            }

            selectedMob = closestMob;
            if (selectedMob != null) {
                player.sendMessage("§a§l(!) §aYou will control " + selectedMob.getDisplayName());
                controlMob(player, selectedMob);
            }
        }

        // Check player has capacity for revival
        if (mobs.size() >= 5) {
            player.sendMessage("§c§l(!) §cMax summons capacity reached!");
            return;
        }

        // Check for vampire orb in 5 block radius
        final int RADIUS = 4;
        for (Entity entity : player.getNearbyEntities(RADIUS, RADIUS, RADIUS)) {
            if (entity instanceof ArmorStand stand) {
                NamespacedKey key = new NamespacedKey(plugin, "necromancer_" + player.getUniqueId()); // Check PDC lines up
                if (stand.getPersistentDataContainer().has(key, PersistentDataType.BYTE)
                        && stand.getLocation().distanceSquared(player.getLocation()) <= RADIUS * RADIUS) {
                    stand.remove(); // destroy the orb
//                    player.sendMessage("§a§l(!) §aBorn Again!");
                    // Add mob to set
                    ActiveMob revivedMob = reviveMob(player, killedMobs.get(entity));
                    mobs.add(revivedMob);
                    long now = System.currentTimeMillis();
                    summonTime.put(revivedMob, now);
                }
            }
        }
    }

    private void controlMob(Player player, ActiveMob controlledMob) {
        UUID uuid = player.getUniqueId();

        // Prevent duplicates
        if (controlledMobs.containsKey(uuid)) {
            player.sendMessage("§c§l(!) §cYou are already controlling a mob!");
            return;
        }

        // Check if trying to control decaying mob
        if (controlledMob.getEntity().getBukkitEntity().getScoreboardTags().contains("isDecaying")) {
            player.sendMessage("§c§l(!) §cYou cannot control a decaying summon");
            return;
        }

        // 1. Store the mob
        controlledMobs.put(uuid, controlledMob);

        // 2. Make the mob invisible and disable its AI
        Entity mobEntity = controlledMob.getEntity().getBukkitEntity();
        mobEntity.setInvulnerable(true);
        mobEntity.setSilent(true); // Just being safe, disguise should this

        // Disguise api stuff
        LibsDisguisesSupport support = CompatibilityManager.LibsDisguises;
        String disguiseStr = "Block_Display barrier setInvisible true setBurning false setReplaceSounds false setPlayIdleSounds false setCustomNameVisible false";
        support.setDisguise(controlledMob, disguiseStr);

        // 3. Apply disguise to player (visual)
        MobDisguise disguise = new MobDisguise(DisguiseType.getType(controlledMob.getEntity().getBukkitEntity()));
//        MobDisguise disguise = new MobDisguise(DisguiseType.CAVE_SPIDER);
        disguise.setHearSelfDisguise(true);
        disguise.setViewSelfDisguise(true); // Let player see themselves
        disguise.setHideArmorFromSelf(true);
        disguise.getWatcher().setCustomName(controlledMob.getDisplayName());
        disguise.getWatcher().setCustomNameVisible(true);
        disguise.setNotifyBar(null); // Hide "currently disguised as"
        DisguiseAPI.disguiseToAll(player, disguise);

        // Setup decoy
        Long cooldown = resurrectionCooldowns.get(player.getUniqueId());
        if (cooldown == null || cooldown != -1)
            createDecoy(player);

        // First sync player with the mob's location
        player.teleport(mobEntity.getLocation());
        // Get attribute values
        double speed = controlledMob.getType().getMovementSpeed(controlledMob);
        // MythicMobs config didn’t define speed
        if (speed == -1.0) {
            if (mobEntity instanceof LivingEntity living) {
                AttributeInstance attr = living.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
                if (attr != null) {
                    speed = attr.getValue() / 0.1;
                } else {
                    speed = 1.0; // Default speed
                }
            } else {
                speed = 1.0; // Non-living mobs default to normal speed
            }
        }

        // Set up player effects before controlling (flying from creative, etc.)
        player.setAllowFlight(false);
        player.setInvulnerable(false);

        // Handle flight case
        if (controlledMob.getEntity().isFlyingMob()) {
            player.setAllowFlight(true);
            player.setFlying(true);
            float flySpeed = (float) controlledMob.getType().getFlyingSpeed(controlledMob);
            if (flySpeed == -1) { // Not setup in config
                if (mobEntity instanceof LivingEntity living) {
                    AttributeInstance attr = living.getAttribute(Attribute.GENERIC_FLYING_SPEED);
                    if (attr != null) {
                        flySpeed = (float) (attr.getValue());
                    } else {
                        flySpeed = (float) 0.1; // Default speed
//                        player.sendMessage("Default Speed");
                    }
                } else {
                    flySpeed = (float) 0.1; // Non-living mobs default to normal speed
                }
            }
            player.setFlySpeed(flySpeed);
            player.sendMessage(" " + flySpeed);
        }
        // Apply attributes
        attributesService.applyHealthDirect(player, controlledMob.getEntity().getMaxHealth() / 20.0);
        attributesService.applySpeedDirect(player, speed); // get speed here
        attributesService.applyDamageDirect(player, controlledMob.getType().getDamage(controlledMob) / 1.0);
        // Util functions for major syncs
        if (mobEntity instanceof LivingEntity livingEntity) {
            playerSyncManager.syncPotionEffects(player, livingEntity);
            playerSyncManager.syncInventory(player, livingEntity);
            playerSyncManager.syncHealth(player, livingEntity);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.setHealth(controlledMob.getEntity().getHealth()); // Prevent bugs where health doesn't sync in time
        }, 2L);

        // 4. Start sync task
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !controlledMobs.containsKey(uuid)) {
                    cancel();
                }

                // Teleport mob to player's position
                Vector velocity = player.getLocation().toVector()
                        .subtract(mobEntity.getLocation().toVector())
                        .multiply(0.5); // follow speed
                mobEntity.setVelocity(velocity);

//                 Sync health (player HP → mob HP)
                controlledMob.getEntity().setHealth(player.getHealth());

                // Optional: Sync mob health back to player (technically won't work due to multiple threads overriding)
//                 player.setHealth(controlledMob.getEntity().getHealth());
            }
        }.runTaskTimer(plugin, 0L, 2L); // update every 2 ticks

        controlTasks.put(uuid, task);

        player.sendMessage("§a§l(!) §aYou are now controlling " + controlledMob.getDisplayName() + "!");
    }

    private void stopControlling(Player player) {
        UUID uuid = player.getUniqueId();

        if (!controlledMobs.containsKey(uuid)) {
            player.sendMessage("§c§l(!) §cYou are not controlling any mob.");
            return;
        }

        // 1. Cancel control loop
        if (controlTasks.containsKey(uuid)) {
            controlTasks.get(uuid).cancel();
            controlTasks.remove(uuid);
        }

        // 2. Remove disguise
        DisguiseAPI.undisguiseToAll(player);

        // 3. Restore mob visibility & AI
        ActiveMob mob = controlledMobs.remove(uuid);
        if (mob != null && mob.getEntity() != null) {
            Entity mobEntity = mob.getEntity().getBukkitEntity();
            DisguiseAPI.undisguiseToAll(mobEntity);
            mobEntity.setSilent(false);

            // Disguise api stuff
            Disguise currentDisguise = DisguiseAPI.getDisguise(mobEntity);
            if (currentDisguise != null) {
                FlagWatcher watcher = currentDisguise.getWatcher();
                watcher.setInvisible(false);
                watcher.setCustomNameVisible(false); // Also good practice to hide custom names
                DisguiseAPI.disguiseEntity(mobEntity, currentDisguise);
            }
            DisguiseAPI.disguiseEntity(mobEntity, currentDisguise);
        }

        // Reset attributes and remove decoy
        attributesService.restoreDefaults(player);

        // Remove flight if not in creative or coming back from dead
        if (player.getGameMode() != GameMode.CREATIVE || resurrectionCooldowns.getOrDefault(player.getUniqueId(), 0L) == -1)
            player.setAllowFlight(false);

        // Setup decoy
        Long cooldown = resurrectionCooldowns.get(player.getUniqueId());
        if (cooldown == null || cooldown != -1)
            removeDecoy(player);

        // Make mob vulnerable again
        Entity mobEntity = mob.getEntity().getBukkitEntity();
        mobEntity.setInvulnerable(false);
        mobEntity.setSilent(false);

        // Major util syncs
        playerSyncManager.restorePotionEffects(player);
        playerSyncManager.restoreInventory(player);
        playerSyncManager.restoreHealth(player);

        player.sendMessage("§c§l(!) §cYou are no longer controlling a mob.");
    }

    public ActiveMob reviveMob(Player player, MythicMob originalMythicMob) {
        if (!MythicBukkit.inst().isEnabled()) {
            player.sendMessage("§cError: MythicMobs is not enabled!");
            return null;
        }

        if (originalMythicMob == null) {
            player.sendMessage("§cError: Invalid original mob definition provided.");
            return null;
        }

        // --- 1. Determine the name of the resurrected mob type ---
        // This relies on the convention established by your YAML generator (e.g., "Resurrected_ZombieKing")
        String resurrectedMobInternalName = RESURRECTED_MOB_PREFIX + originalMythicMob.getInternalName();

        // --- 2. Attempt to get the MythicMob definition for the resurrected version ---
        Optional<MythicMob> optResurrectedMobType = MythicBukkit.inst().getMobManager().getMythicMob(resurrectedMobInternalName);

        if (optResurrectedMobType.isEmpty()) {
            player.sendMessage("§c§l(!) §cResurrection has failed!");
            return null;
        }

        MythicMob resurrectedMobType = optResurrectedMobType.get();
        Location spawnLocation = player.getLocation();
        UUID ownerUUID = player.getUniqueId();

        // --- 3. Prepare Spawn Data (including owner) ---
        HashMap<String, String> spawnData = new HashMap<>();
        spawnData.put("owner", ownerUUID.toString());
        // Faction, AITargetSelectors, and AIGoals are now defined within resurrectedMobType's YAML

        // --- 4. Spawn the Resurrected Mob ---
        ActiveMob activeMob = resurrectedMobType.spawn(
                BukkitAdapter.adapt(spawnLocation),
                1.0 // Mob level (adjust as needed, 1.0 is default)
        );

        if (activeMob == null) {
            player.sendMessage("§c§l(!) §cFailed to revive " + resurrectedMobType.getDisplayName() + "!");
            return null;
        }

        // --- 5. Explicitly set owner (redundant with spawnData but harmless and adds robustness) ---
        activeMob.setOwner(ownerUUID);

        // Set name of the mob
        String mobName = activeMob.getDisplayName();
        if (mobName == null)
            mobName = activeMob.getEntity().getName();
        activeMob.setDisplayName(player.getDisplayName() + "'s Resurrected " + ChatColor.BOLD + mobName);

        // 6 flashy ligtning effect
        Location playerLocation = player.getLocation();
        playerLocation.getWorld().strikeLightningEffect(playerLocation);

        player.sendMessage("§a§l(!) §aYou have successfully revived a loyal " + activeMob.getDisplayName() + "!");
        return activeMob;
    }

    @EventHandler
    public void onPlayerCancelFlight(PlayerToggleFlightEvent event) {
        ActiveMob activeMob = controlledMobs.get(event.getPlayer().getUniqueId());
        if (resurrectionCooldowns.getOrDefault(event.getPlayer().getUniqueId(), 0L) == -1) {
            event.setCancelled(true);
            return;
        }
        if (activeMob == null)
            return; // We can assume if a player is controlling a mob they have the set
        if (activeMob.getEntity().isFlyingMob() && event.getPlayer().isFlying()) // Player quit flight while controlling flight mob
            stopControlling(event.getPlayer()); // Stop player from controlling mob
    }

    @EventHandler
    public void onPlayerKillMob(EntityDeathEvent event) {
        // Basic checks
        Player player = event.getEntity().getKiller();
        if (player == null || !(armorManager.getArmorSet(player) instanceof NecromancerArmorSet)) return; // Not killed by a player or player not wearing the set
        Optional<ActiveMob> optActiveMob = MythicBukkit.inst().getMobManager().getActiveMob(event.getEntity().getUniqueId());
        if (optActiveMob.isEmpty()) return; // Not a mythic mob
        // Logic for mythic mob
        optActiveMob.ifPresent(activeMob -> {
            spawnCosmeticHead(event.getEntity().getLocation(), activeMob, String.valueOf(player.getUniqueId()));
        });
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        // Handle all MythicMobs deaths (ActiveMobs)
        if (event.getEntity() instanceof LivingEntity living) {
            if (event.getFinalDamage() >= living.getHealth()) {
                Optional<ActiveMob> mob = MythicBukkit.inst().getMobManager().getActiveMob(living.getUniqueId());
                if (mob.isPresent() && despawnMob(mob.get())) {
                    event.setCancelled(true);
                    return; // Exit early to avoid double-handling
                }
            }
        }

        // Handle player death while controlling a mob and passive
        if (event.getEntity() instanceof Player player) {
            ActiveMob controlled = controlledMobs.get(player.getUniqueId());
            if (controlled != null && event.getFinalDamage() >= player.getHealth()) {
                event.setCancelled(true);
                despawnMob(controlled);
            } else if (armorManager.getArmorSet(player) instanceof NecromancerArmorSet && event.getFinalDamage() >= player.getHealth()) { // Ressurection Passive: final hit to player
                    event.setCancelled(resurrectionPassive(player));
            }
        }
    }

    private boolean resurrectionPassive(Player player) {
        // Check if cooldown for resurrection exists
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (resurrectionCooldowns.containsKey(uuid) && now - resurrectionCooldowns.get(uuid) < RESURRECTION_COOLDOWN) {
            long timeLeft = (RESURRECTION_COOLDOWN - (now - resurrectionCooldowns.get(uuid))) / 1000;
            player.sendMessage("§c§l(!) §cAbility is on cooldown for " + timeLeft + " more seconds!");
            return false;
        }

        resurrectionCooldowns.put(uuid, -1L); // Let rest of plugin know that player is in dead state
        player.sendMessage( "§c§l(!) §cYour body has crumbled but you can mold a new one quickly!");
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setInvulnerable(true);
        player.setFireTicks(0);
        player.teleport(player.getLocation().add(0, 3, 0));
        // Add disguise so particles look clean
        MiscDisguise disguise = new MiscDisguise(DisguiseType.AREA_EFFECT_CLOUD);
        AreaEffectCloudWatcher watcher = (AreaEffectCloudWatcher) disguise.getWatcher();
        watcher.setParticle(new com.github.retrooper.packetevents.protocol.particle.Particle(ParticleTypes.SOUL_FIRE_FLAME));
        watcher.setRadius((float) 0.75);
        disguise.setWatcher(watcher);
        DisguiseAPI.disguiseToAll(player, disguise);
        // Start scheduler to handle lots revival logic
        new BukkitRunnable() {
            int timeLeft = 10;
            int timeControl = 5;
            @Override
            public void run() {
                if (timeControl <= 0) {
                    cancel();
                    despawnMob(controlledMobs.get(player.getUniqueId()));
                    resurrectionCooldowns.put(uuid, now);
                    // Play a bunch of particles, sound and lightning
                    World world = player.getWorld();
                    Location playerLocation = player.getLocation();
                    world.spawnParticle(Particle.PORTAL, playerLocation, 50, 0.5, 1.5, 0.5, 0.1);
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, playerLocation, 25, 0.5, 1.0, 0.5, 0.1);
                    world.spawnParticle(Particle.SMOKE_NORMAL, playerLocation, 100, 0.5, 1.5, 0.5, 0.1);
                    world.spawnParticle(Particle.FLASH, playerLocation, 1);
                    world.playSound(player, Sound.ENTITY_WITHER_SPAWN, 1, 2); // Sound
                    playerLocation.getWorld().strikeLightningEffect(playerLocation);
                    // Remove potion effects
                    for (PotionEffect effect : player.getActivePotionEffects()) {
                        if (effect.getDuration() != PotionEffect.INFINITE_DURATION) { // Prevent perk potions from getting removed
                            player.removePotionEffect(effect.getType());
                        }
                    }
                    // Set to half hp
                    player.setHealth(10);
                    player.sendMessage("§a§l(!) §aBack from the dead!");
                    return;
                } else if (timeLeft <= 0) {
                    cancel();
                    player.sendMessage( "§c§l(!) §cYour soul decays away without a host!");
                    player.setHealth(0.0);
                    player.setAllowFlight(false);
                    player.setInvulnerable(false);
                    DisguiseAPI.undisguiseToAll(player);
                    resurrectionCooldowns.remove(player.getUniqueId());
                    return;
                } else if (controlledMobs.get(player.getUniqueId()) != null) { // Player is possesing mob
                    player.sendMessage("§a§l(!) §aPossess this host for " + timeControl + " to resurrect yourself.");
                    timeControl--;
                } else {
                    player.sendMessage("§c§l(!) §cFind a host for your soul to possess. You will decay in " + timeLeft);
                    timeLeft--;
                    timeControl = 5; // Reset if player left host

                    // Check for nearby MythicMobs to possess
                    for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
                        // Ensure the entity is a MythicMob
                        Optional<ActiveMob> activeMob = MythicBukkit.inst().getMobManager().getActiveMob(entity.getUniqueId());
                        if (activeMob.isPresent() && summonedMobs.get(player.getUniqueId()).contains(activeMob.get())) {
                            // Call your method to control the mob
                            controlMob(player, activeMob.get());
                            break; // Stop after finding the first mob
                        }
                    }

                }

            }
        }.runTaskTimer(plugin, 0, 20); // Every second
        return true;
    }

    @EventHandler
    public void onEntityHit(EntityDamageByEntityEvent event) {
        // Prevent dead players from attacking
        if (event.getDamager() instanceof Player damager) {
            if (resurrectionCooldowns.getOrDefault(damager.getUniqueId(), 0L) == -1) { // Player is dead
                event.setCancelled(true);
                return;
            }
        }

        // Handle case of decoy getting attacked
        if (!(event.getEntity() instanceof ArmorStand armorStand)) return;
        String armorStandName = armorStand.getCustomName();
        if (armorStandName == null) return;
        UUID uuid = UUID.fromString(armorStandName);
        ArmorStand decoy = decoys.get(uuid);
        Player player = Bukkit.getPlayer(uuid);
        if (decoy == null || player == null) return;
        event.setCancelled(true);
        if (event.getDamager().equals(controlledMobs.get(uuid).getEntity().getBukkitEntity()))
            return;
        stopControlling(player);
        player.sendMessage("§c§l(!) §cYou are being attacked. Mental connection broke.");
    }

    @EventHandler
    public void onSprint(PlayerToggleSprintEvent event){ // Don't let players get faster when controlling mobs
        if (controlledMobs.get(event.getPlayer().getUniqueId()) != null)
            event.setCancelled(true);
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event){
        if (!(event.getEntity() instanceof Player player)) return;
        if (controlledMobs.get(player.getUniqueId()) != null || resurrectionCooldowns.getOrDefault(player.getUniqueId(), 0L) == -1)
            event.setCancelled(true);
    }

    private boolean despawnMob(ActiveMob activeMob) {
        // To remove 'activeMob' from the map and get the Player UUID before removal:
        for (Map.Entry<UUID, ActiveMob> entry : controlledMobs.entrySet()) {
            if (entry.getValue().equals(activeMob)) { // Check if the ActiveMob object matches
                UUID playerUUID = entry.getKey(); // Get the Player UUID (the key)
                Player player = Bukkit.getPlayer(playerUUID); // Get the Bukkit Player object (can be null if offline)

                if (player != null) {
                    stopControlling(player); // Call your stopControlling method with the Player object
                }
//                iterator.remove(); // Safely remove the entry (Player UUID -> ActiveMob)
                break; // Assuming each ActiveMob instance is only mapped once
            }
        }
        // Iterate through the summonedMobs map to find and remove the mob
        boolean foundAndRemoved = false;
        // Use an iterator to safely remove elements from the map if a player's list becomes empty
        Iterator<Map.Entry<UUID, List<ActiveMob>>> mapIterator = summonedMobs.entrySet().iterator();

        while (mapIterator.hasNext()) {
            Map.Entry<UUID, List<ActiveMob>> entry = mapIterator.next();
            List<ActiveMob> mobsOwnedByPlayer = entry.getValue();

            // Attempt to remove the current activeMob from this player's list.
            // CopyOnWriteArrayList's .remove() is thread-safe for this operation.
            if (mobsOwnedByPlayer.remove(activeMob)) {
                foundAndRemoved = true;
//                if (mobsOwnedByPlayer.isEmpty()) {
//                    mapIterator.remove(); // Remove player's entry from the main map if their list is now empty
//                    Bukkit.getLogger().info("Player " + entry.getKey() + " no longer has active summoned mobs.");
//                }
                break; // Mob found and removed, no need to check other players' lists
            }
        }

        if (foundAndRemoved) {
            // Despawn the mob gracefully
            activeMob.despawn();

            // Leave a particle effect to signal its disappearance
            Entity entity = activeMob.getEntity().getBukkitEntity();
            Location mobLoc = entity.getLocation();
            mobLoc.getWorld().spawnParticle(
                    Particle.CLOUD, // A cloud or smoke effect is good for despawning
                    mobLoc.add(0, entity.getHeight() / 2.0, 0), // Spawn particles at the center of the mob's height
                    50, // Number of particles
                    0.4, 0.4, 0.4, // Offset (spread) on X, Y, Z axes
                    0.01 // Speed of particles (often low for static effects like smoke/cloud)
            );

            // Add a sound effect for better feedback
            mobLoc.getWorld().playSound(mobLoc, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 0.7f, 1.2f); // Example sound

            Bukkit.getLogger().info("Summoned mob " + activeMob.getDisplayName() + " (" + activeMob.getUniqueId() + ") despawned due to lethal damage.");
            return true;
        }
        return false;
    }

    private void createDecoy(Player player) {
        Location location = player.getLocation();
        World world = location.getWorld();
        UUID uuid = player.getUniqueId();

        ArmorStand decoy = (ArmorStand) world.spawnEntity(location, EntityType.ARMOR_STAND);
        decoy.setCustomName(player.getUniqueId().toString());
        decoys.put(uuid, decoy);

        // Apply disguise (optional)
        PlayerDisguise disguise = new PlayerDisguise(player.getName());
        PlayerWatcher watcher = disguise.getWatcher();

        // Copy each armor slot
        watcher.setHelmet(player.getInventory().getHelmet());
        watcher.setChestplate(player.getInventory().getChestplate());
        watcher.setLeggings(player.getInventory().getLeggings());
        watcher.setBoots(player.getInventory().getBoots());

        // Optional: copy held item
        watcher.setItemInMainHand(player.getInventory().getItemInMainHand());
        watcher.setItemInOffHand(player.getInventory().getItemInOffHand());

        // Apply disguise
        DisguiseAPI.disguiseToAll(decoy, disguise);

        // 2. Run AI task
        BukkitRunnable task = new BukkitRunnable() {

            @Override
            public void run() {
                if (!decoy.isValid()) {
                    decoy.remove();
                    cancel();
                    return;
                }

                for (Entity nearby : decoy.getNearbyEntities(20, 20, 20)) {
                    if (nearby instanceof Mob mob) {
                        if (mob.getTarget() == null || mob.getTarget().equals(player)) {
                            mob.setTarget(decoy); // force target only if free/targeting player
                        }
                    }
                }

            }
        };

        task.runTaskTimer(plugin, 0L, 20L); // check every second
        decoyTasks.put(uuid, task);
    }

    private void removeDecoy(Player player) {
        ArmorStand decoy = decoys.get(player.getUniqueId());
        if (decoy == null) return;
        player.teleport(decoy.getLocation());
        decoy.remove();
        decoyTasks.get(player.getUniqueId()).cancel();

    }

    public void spawnCosmeticHead(Location loc, ActiveMob activeMob, String uuid) {
        Location headLoc = loc.clone().add(0, 1.2, 0); // Adjust Y for head height
        ArmorStand stand = (ArmorStand) loc.getWorld().spawnEntity(headLoc, EntityType.ARMOR_STAND);

        stand.setVisible(false);
        stand.setSmall(true);
        stand.setGravity(false);
        stand.setMarker(true); // Prevents interaction and collision
        stand.setBasePlate(false);
        stand.setInvulnerable(true);
        stand.setSilent(true); // No armor stand sounds

        // Set the custom name to the MythicMob's display name
        stand.setCustomNameVisible(true);
        String mobName = activeMob.getDisplayName();
        if (mobName == null)
            mobName = activeMob.getEntity().getName();
        mobName = ChatColor.BOLD + mobName;
        stand.setCustomName(mobName);

        // Add texture to skull
        String textureUrl = "http://textures.minecraft.net/texture/15378267b72a33618c8c9d8ff4be2d452a26509a9964b080b19d7c308ec79605";
        stand.setHelmet(getCustomSkull(textureUrl));

        // Mark with PDC so it can be tracked/removed
        stand.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "necromancer_" + uuid), // Use your plugin's specific key
                PersistentDataType.BYTE,
                (byte) 1
        );

        // Link stand to mythic mob
        killedMobs.put(stand, activeMob.getType());

        // Remove the armor stand after 5 seconds (100 ticks)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!stand.isDead() && stand.isValid()) {
                stand.remove();
            }
        }, 100L);
    }

    public static ItemStack getCustomSkull(String textureUrl) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        if (meta != null) {
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
            PlayerTextures textures = profile.getTextures();

            try {
                URL url = new URL(textureUrl);
                textures.setSkin(url);
                profile.setTextures(textures);
                meta.setOwnerProfile(profile);
            } catch (MalformedURLException e) {
                Bukkit.getLogger().severe("Invalid texture URL: " + textureUrl);
                e.printStackTrace();
            }

            skull.setItemMeta(meta);
        }

        return skull;
    }

}