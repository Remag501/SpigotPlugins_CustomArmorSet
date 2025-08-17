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
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Snow;
import org.bukkit.entity.*;
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
import org.w3c.dom.Attr;

import javax.management.Attribute;
import java.lang.annotation.Target;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SnowmanArmorSet extends ArmorSet implements Listener {

    private static final Map<UUID, BukkitTask> ultTask = new HashMap<>();
    private static final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final Map<UUID, Integer> freezeCharges = new HashMap<>();
    private static final Map<UUID, Integer> snowCharge = new HashMap<>();

    // Map to store freeze stacks for each mob
    private static final Map<UUID, Integer> freezeStacks = new ConcurrentHashMap<>();
    // Decay task for mobs
    private static final Map<UUID, BukkitTask> decayTasks = new ConcurrentHashMap<>();
    private static final Map<UUID, BukkitTask> particleTasks = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> mobChargeCooldowns = new ConcurrentHashMap<>();

    public SnowmanArmorSet() {
        super(ArmorSetType.SNOWMAN);
    }

    @Override
    public void applyPassive(Player player) {
//        player.sendMessage("✅ You equipped the snowman set");
//        DefenseStats.setSourceReduction(player.getUniqueId(), 0.02f, TargetCategory.ALL);
//
//        PlayerData playerData = PlayerStorage.getPlayerData(player.getUniqueId());
//        UUID worldId = player.getWorld().getUID();
//        String modesetName = "old";
//        playerData.setModesetForWorld(worldId, modesetName);
//        PlayerStorage.setPlayerData(player.getUniqueId(), playerData);
//        PlayerStorage.scheduleSave();
//
//        ModuleLoader.getModules().forEach(module -> module.onModesetChange(player));


        // Populate maps with initial values
        UUID uuid = player.getUniqueId();
        freezeCharges.put(uuid, 0);

        // Start a task to display freeze charges on the action bar
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                int charges = freezeCharges.getOrDefault(uuid, 0);
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§bFreeze Charge ❄ " + charges + " / 5"));
            }
        }.runTaskTimer(CustomArmorSets.getInstance(), 0, 5); // Display every quarter second
        ultTask.put(uuid, task);


    }

    @Override
    public void removePassive(Player player) {
//        DamageStats.clearAll(player.getUniqueId());
//        DefenseStats.clearAll(player.getUniqueId());
//        player.sendMessage("❌ You removed the snowman set");
//
//        PlayerData playerData = PlayerStorage.getPlayerData(player.getUniqueId());
//        UUID worldId = player.getWorld().getUID();
//        String modesetName = "new";
//        playerData.setModesetForWorld(worldId, modesetName);
//        PlayerStorage.setPlayerData(player.getUniqueId(), playerData);
//        PlayerStorage.scheduleSave();
//
//        ModuleLoader.getModules().forEach(module -> module.onModesetChange(player));

        AttributesUtil.removeSpeed(player);
        UUID uuid = player.getUniqueId();
        if (ultTask.containsKey(uuid)) {
            ultTask.remove(uuid).cancel();
        }
        freezeCharges.remove(uuid);
        player.sendMessage("❄ You removed the Iceman set");


    }

    @Override
    public void triggerAbility(Player player) {
        UUID uuid = player.getUniqueId();
        int charges = freezeCharges.getOrDefault(uuid, 0);
        if (charges > 0) {
            triggerIceBeam(player, charges);
        } else {
            player.sendMessage("§cYou don't have enough freeze charges!");
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(CustomArmorSetsCore.getArmorSet(player) instanceof SnowmanArmorSet)) return; // Corrected this line

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

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
//        if (!(event.getDamager() instanceof LivingEntity target)) return;
//        UUID mobUUID = target.getUniqueId();
//        // Check if the mob is "Completely Frozen"
//        if (freezeStacks.getOrDefault(mobUUID, 0) >= 1) {
//            event.setCancelled(true);
//            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation(), 10, 0.5, 0.5, 0.5, 0.1);
//        }
    }

    public void triggerIceBeam(Player player, int initialCharges) {
        final World world = player.getWorld();
        final double beamLength = 20.0;
        final double hitRadius = 1.0;

        world.playSound(player.getLocation(), Sound.BLOCK_SNOW_BREAK, 1.5f, 1.0f);

        new BukkitRunnable() {
            private int ticksLived = 0;

            @Override
            public void run() {
                UUID playerUUID = player.getUniqueId();
                int currentCharges = freezeCharges.getOrDefault(playerUUID, 0);

                // Stop the beam if the player has no charges or is no longer online
                if (currentCharges <= 0 || !player.isOnline()) {
                    freezeCharges.put(playerUUID, 0);
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

}
