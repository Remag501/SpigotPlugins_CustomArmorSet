package me.remag501.customarmorsets.armorsets;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.compatibility.CompatibilityManager;
import io.lumine.mythic.bukkit.compatibility.LibsDisguisesSupport;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.Disguise;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.FlagWatcher;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import me.remag501.customarmorsets.CustomArmorSets;
import me.remag501.customarmorsets.core.ArmorSet;
import me.remag501.customarmorsets.core.ArmorSetType;
import me.remag501.customarmorsets.core.CustomArmorSetsCore;
import me.remag501.customarmorsets.listeners.MythicMobsYamlGenerator;
import me.remag501.customarmorsets.utils.AttributesUtil;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
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

import static me.remag501.customarmorsets.utils.PlayerSyncUtil.*;

public class NecromancerArmorSet extends ArmorSet implements Listener {

    private static final Map<UUID, Long> resurrectionCooldowns = new HashMap<>();
    private static final long RESURRECTION_COOLDOWN = 120 * 1000; // 2 minutes
    private static final Map<ArmorStand, MythicMob> killedMobs = new HashMap<>();
    private static final Map<UUID, List<ActiveMob>> summonedMobs = new HashMap<>();
    private static final Map<UUID, BukkitTask> summonsTask = new HashMap<>();
    private static final Map<ActiveMob, Long> summonTime = new HashMap<>();
    private static final Map<UUID, ActiveMob> controlledMobs = new HashMap<>();
    private static final Map<UUID, BukkitTask> controlTasks = new HashMap<>();

    private static final String RESURRECTED_MOB_PREFIX = MythicMobsYamlGenerator.getPrefix();

    public NecromancerArmorSet() {
        super(ArmorSetType.NECROMANCER);
    }

    @Override
    public void applyPassive(Player player) {
        player.sendMessage("You equipped the Necromancer set");
        summonedMobs.put(player.getUniqueId(), new ArrayList<>());
        summonsTask.put(player.getUniqueId(), new BukkitRunnable() {
            @Override
            public void run() {
                UUID uuid = player.getUniqueId();
                List<ActiveMob> mobs = summonedMobs.get(uuid);
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

                    if (secondsPassed > 20) {
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
                    if (secondsPassed >= 15) {
                        //
                        if (isControlledByPlayer) {
                            player.sendMessage("You cannot control decaying mobs");
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
        }.runTaskTimer(CustomArmorSets.getInstance(), 0, 10));
    }

    @Override
    public void removePassive(Player player) {
        player.sendMessage("You removed the Necromancer set");
        List<ActiveMob> mobs = summonedMobs.get(player.getUniqueId());
        while (!mobs.isEmpty()) {
            despawnMob(mobs.get(0));
        }
        summonedMobs.remove(player.getUniqueId());
        summonsTask.get(player.getUniqueId()).cancel();
    }

    @Override
    public void triggerAbility(Player player) {
        // Relevant variables
        Plugin plugin = CustomArmorSets.getInstance();
        List<ActiveMob> mobs = summonedMobs.get(player.getUniqueId());
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
                player.sendMessage("You will control " + selectedMob.getDisplayName());
                controlMob(player, selectedMob);
            }
        }

        // Check player has capacity for revival
        if (mobs.size() >= 5) {
            player.sendMessage("Max summons capacity reached!");
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
                    player.sendMessage(ChatColor.GOLD + "Born Again!");
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
            player.sendMessage(ChatColor.RED + "You are already controlling a mob!");
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
                        player.sendMessage("Default Speed");
                    }
                } else {
                    flySpeed = (float) 0.1; // Non-living mobs default to normal speed
                }
            }
            player.setFlySpeed(flySpeed);
            player.sendMessage(" " + flySpeed);
        }
        // Apply attributes
        AttributesUtil.applyHealthDirect(player, controlledMob.getEntity().getMaxHealth() / 20.0);
        AttributesUtil.applySpeedDirect(player, speed); // get speed here
        AttributesUtil.applyDamageDirect(player, controlledMob.getType().getDamage(controlledMob) / 1.0);
        // Util functions for major syncs
        if (mobEntity instanceof LivingEntity livingEntity) {
            syncPotionEffects(player, livingEntity);
            syncInventory(player, livingEntity);
            syncHealth(player, livingEntity);
        }

        Bukkit.getScheduler().runTaskLater(CustomArmorSets.getInstance(), () -> {
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
        }.runTaskTimer(CustomArmorSets.getInstance(), 0L, 2L); // update every 2 ticks

        controlTasks.put(uuid, task);

        player.sendMessage(ChatColor.GREEN + "You are now controlling " + controlledMob.getDisplayName() + "!");
    }

    private void stopControlling(Player player) {
        UUID uuid = player.getUniqueId();

        if (!controlledMobs.containsKey(uuid)) {
            player.sendMessage(ChatColor.RED + "You are not controlling any mob.");
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

        // Reset attributes
        AttributesUtil.restoreDefaults(player);

        // Make mob vulnerable again
        Entity mobEntity = mob.getEntity().getBukkitEntity();
        mobEntity.setInvulnerable(false);
        mobEntity.setSilent(false); // Just being safe, disguise should this

        // Major util syncs
        restorePotionEffects(player);
        restoreInventory(player);
        restoreHealth(player);

        player.sendMessage(ChatColor.YELLOW + "You are no longer controlling a mob.");
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
            player.sendMessage("§cResurrection has failed!");
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
            player.sendMessage("§cFailed to revive " + resurrectedMobType.getDisplayName() + "!");
            return null;
        }

        // --- 5. Explicitly set owner (redundant with spawnData but harmless and adds robustness) ---
        activeMob.setOwner(ownerUUID);

        // Set name of the mob
        String mobName = activeMob.getDisplayName();
        if (mobName == null)
            mobName = activeMob.getEntity().getName();
        activeMob.setDisplayName(player.getDisplayName() + "'s Resurrected " + ChatColor.BOLD + mobName);

        player.sendMessage("§aYou have successfully revived a loyal " + activeMob.getDisplayName() + "!");
        return activeMob;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
    }

    @EventHandler
    public void onPlayerCancelFlight(PlayerToggleFlightEvent event) {
        ActiveMob activeMob = controlledMobs.get(event.getPlayer().getUniqueId());
        if (activeMob == null)
            return; // We can assume if a player is controlling a mob they have the set
        if (activeMob.getEntity().isFlyingMob() && event.getPlayer().isFlying()) // Player quit flight while controlling flight mob
            stopControlling(event.getPlayer()); // Stop player from controlling mob
    }

    @EventHandler
    public void onPlayerKillMob(EntityDeathEvent event) {
        // Basic checks
        Player player = event.getEntity().getKiller();
        if (player == null || !(CustomArmorSetsCore.getArmorSet(player) instanceof NecromancerArmorSet)) return; // Not killed by a player or player not wearing the set
        Optional<ActiveMob> optActiveMob = MythicBukkit.inst().getMobManager().getActiveMob(event.getEntity().getUniqueId());
        if (optActiveMob.isEmpty()) return; // Not a mythic mob
        // Logic for mythic mob
        optActiveMob.ifPresent(activeMob -> {
            spawnCosmeticHead(event.getEntity().getLocation(), activeMob, String.valueOf(player.getUniqueId()));
        });
    }

    @EventHandler
    public void beforeEntityDeath(EntityDamageEvent event) {
        // First, check if the entity is actually dying from this damage.
        // We only intervene if the final damage is lethal (would bring health to 0 or less).
        if (!(event.getEntity() instanceof LivingEntity livingEntity)) return; // Just to be safe
        if (event.getFinalDamage() < livingEntity.getHealth()) {
            return; // Not a lethal blow, let the event proceed normally
        }

        // 1. Check if the entity is an ActiveMythicMob
        Optional<ActiveMob> optActiveMob =MythicBukkit.inst().getMobManager().getActiveMob(event.getEntity().getUniqueId());
        if (optActiveMob.isEmpty()) {
            return; // Not a MythicMob, do not interfere
        }

        ActiveMob activeMob = optActiveMob.get();
        if (despawnMob(activeMob)) event.setCancelled(true);

    }

    @EventHandler
    public void beforePlayerDeath(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ActiveMob controlledMob = controlledMobs.get(player.getUniqueId());
        if (controlledMob == null) return;
        if (event.getFinalDamage() > player.getHealth()) {
            // Player is about to die
            event.setCancelled(true);
            despawnMob(controlledMob);
        }

    }

    @EventHandler
    public void onSprint(PlayerToggleSprintEvent event){ // Don't let players get faster when controlling mobs
        if (controlledMobs.get(event.getPlayer().getUniqueId()) != null)
            event.setCancelled(true);
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event){
        if (!(event.getEntity() instanceof Player player)) return;
        if (controlledMobs.get(player.getUniqueId()) != null)
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

        Plugin plugin = CustomArmorSets.getInstance();
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