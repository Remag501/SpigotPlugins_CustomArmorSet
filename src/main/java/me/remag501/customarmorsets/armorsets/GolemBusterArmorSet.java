package me.remag501.customarmorsets.armorsets;

import io.lumine.mythic.core.mobs.MobType;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import me.remag501.customarmorsets.CustomArmorSets;
import me.remag501.customarmorsets.core.*;
import me.remag501.customarmorsets.utils.AttributesUtil;
import me.remag501.customarmorsets.utils.CooldownBarUtil;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

import static me.remag501.customarmorsets.utils.CooldownBarUtil.setLevel;

public class GolemBusterArmorSet extends ArmorSet implements Listener {

    private static final Map<UUID, Integer> playerEnergy = new HashMap<>();
    private static final Map<UUID, Boolean> playerIsGolem = new HashMap<>();
    private final Map<UUID, BukkitRunnable> particleTasks = new HashMap<>();
    private static final Map<GolemBusterArmorSet, BukkitTask> energyLoop = new HashMap<>();
    private static final Map<UUID, Long> stunCooldown = new HashMap<>();

    public GolemBusterArmorSet() {
        super(ArmorSetType.GOLEM_BUSTER);
    }

    @Override
    public void applyPassive(Player player) {
//        player.sendMessage("✅ You equipped the Golem Buster set");
        UUID uuid = player.getUniqueId();
        playerEnergy.put(uuid, 0);
        playerIsGolem.put(uuid, false);
        // Apply Damage Stats
        DamageStats.setMobMultiplier(player.getUniqueId(), 1.5f, TargetCategory.NON_PLAYER);
        DefenseStats.setSourceReduction(player.getUniqueId(), 0.75f, TargetCategory.NON_PLAYER);
//        DefenseStats.setWeaponReduction(player.getUniqueId(), 0.25f, WeaponType.);
        // Apply 1.8 pvp later
        energyLoop.put(this, new BukkitRunnable() {

            @Override
            public void run() {
                int energy;
                if (playerIsGolem.get(uuid)) {
                    energy = consumePlayerEnergy(player, -5);
                    if (energy <= 0) {
                        transformBack(player);
                        return;
                    }
                }
                else
                    energy = consumePlayerEnergy(player, 1);

                setLevel(player, energy);
            }
        }.runTaskTimer(CustomArmorSets.getInstance(), 0, 20));
    }

    @Override
    public void removePassive(Player player) {
//        player.sendMessage("❌ You removed the Golem Buster set");
        // End the bukkit task
        transformBack(player);
        energyLoop.get(this).cancel();
        CooldownBarUtil.restorePlayerBar(player);
        AttributesUtil.restoreDefaults(player); // Just in case
        DamageStats.clearAll(player.getUniqueId());
        DefenseStats.clearAll(player.getUniqueId());
    }

    @Override
    public void triggerAbility(Player player) {
        UUID uuid = player.getUniqueId();
        int battery = playerEnergy.get(uuid);
        // If player is golem transform them back
        if (playerIsGolem.get(uuid)) {
            if (player.isSneaking()) {
                // Set battery to max of 50 to prevent spam
                playerEnergy.put(uuid, Math.min(battery, 50));
                transformBack(player);
                return;
            } else { // Separate ability
                long now = System.currentTimeMillis();
                long lastUsed = stunCooldown.getOrDefault(uuid, 0L);

                if (now - lastUsed < 1000) {
                    player.sendMessage("§c§l(!) §cAbility on cooldown!");
                    return;
                }

                stunCooldown.put(uuid, now);

                // Play iron golem attack sound
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_DAMAGE, 1.0f, 1.0f);

                // Play attack partciles
                spawnSpiralBeam(player);

                // Stun and knock-up entities in front
                double radius = 5.0;
                Vector direction = player.getLocation().getDirection().normalize();
                Location origin = player.getEyeLocation();

                for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                    if (!(entity instanceof LivingEntity) || entity.equals(player)) continue;

                    Location entityLoc = entity.getLocation();
                    Vector toEntity = entityLoc.toVector().subtract(origin.toVector()).normalize();

                    // Angle check — only affect mobs roughly in front
                    if (direction.dot(toEntity) > 0.5) {
                        LivingEntity target = (LivingEntity) entity;

                        // Apply knock-up
                        Vector knockUp = new Vector(0, 1, 0);
                        target.setVelocity(knockUp);

                        // Apply stun (Weakness II + Slowness III for 2 seconds)
                        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 1));
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 2));

                        // Particles
                        player.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3);
                        startTargetEffect(target);
                    }
                }

                // Optional confirmation
                player.sendMessage("§a§l(!) §aYou slam the ground, stunning enemies ahead!");
                return;
            }
        }
        // Check if player is trying to transform
        if (player.isSneaking()) {
//            battery = consumePlayerEnergy(player, -50);
            if (battery >= 50) {
                player.sendMessage("§a§l(!) §aGolem Smash");
                setLevel(player, battery);
                golemTransform(player);
                return;
            }
        }
        // Check if player has enough battery
        battery = consumePlayerEnergy(player, -5);
        if (battery < 0) {
            player.sendMessage("§c§l(!) §cNot enough battery");
            return;
        }

        setLevel(player, battery);

        World world = player.getWorld();
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection().normalize();

        double range = 20.0;
        double step = 0.5;
        Set<LivingEntity> hitEntities = new HashSet<>();

        for (double i = 0; i < range; i += step) {
            Location point = start.clone().add(direction.clone().multiply(i));
            world.spawnParticle(Particle.ELECTRIC_SPARK, point, 1, 0, 0, 0, 0);

            for (Entity entity : world.getNearbyEntities(point, 1, 1, 1)) {
                if (entity instanceof LivingEntity && entity != player && !hitEntities.contains(entity)) {
                    ((LivingEntity) entity).damage(6.0, player); // Deal 6 damage
                    hitEntities.add((LivingEntity) entity);
                }
            }
        }

        world.playSound(player.getLocation(), Sound.ENTITY_SHULKER_SHOOT, 1f, 2f);
        player.sendMessage("§a§l(!) §aBattery gun discharged!");
    }

    public void startTargetEffect(LivingEntity target) {
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 60 || target.isDead()) {
                    cancel();
                    return;
                }

                // Location slightly above the target (head level)
                Location loc = target.getLocation().add(0, 1.2, 0);

                // Redstone-like particle effect
                Particle.DustOptions redDust = new Particle.DustOptions(Color.RED, 1.2F);
                target.getWorld().spawnParticle(Particle.REDSTONE, loc, 10, 0.3, 0.4, 0.3, 0, redDust);

                ticks++;
            }
        }.runTaskTimer(CustomArmorSets.getInstance(), 0L, 1L); // Run every tick (1L) for 60 ticks = 3 seconds
    }

    private void golemTransform(Player player) {
        playerIsGolem.put(player.getUniqueId(), true);

        // Apply disguise
        MobDisguise disguise = new MobDisguise(DisguiseType.IRON_GOLEM);
        disguise.setViewSelfDisguise(true);
        disguise.setNotifyBar(null); // Disable action bar "Currently disguised as..."
        disguise.getWatcher().setCustomName(player.getDisplayName());
        disguise.getWatcher().setCustomNameVisible(true);
        DisguiseAPI.disguiseToAll(player, disguise);

        // Spawn transformation particle effect
        player.getWorld().spawnParticle(
                Particle.CLOUD, // Choose your effect here
                player.getLocation().add(0, 1, 0), // Slightly above feet
                40, // Particle count
                0.5, 1.0, 0.5, // Spread in x, y, z
                0.01 // Speed
        );

        // Start particle trail
        startParticleTrail(player);

        // Give attributes and damage stats
        AttributesUtil.applyHealth(player, 2.0);
        AttributesUtil.applySpeed(player, 0.5);
        DamageStats.setMobMultiplier(player.getUniqueId(), 2, TargetCategory.NON_PLAYER);
        DefenseStats.setSourceReduction(player.getUniqueId(), 0.25f, TargetCategory.NON_PLAYER);
        Bukkit.getScheduler().runTaskLater(CustomArmorSets.getInstance(), () -> {
            player.setHealth(40.0);
        }, 2L);
    }

    private void transformBack(Player player)  {
        // Remove attributes and damage stats
        AttributesUtil.removeHealth(player);
        AttributesUtil.removeSpeed(player);
        DamageStats.setMobMultiplier(player.getUniqueId(), 1.5f, TargetCategory.NON_PLAYER);
        DefenseStats.setSourceReduction(player.getUniqueId(), 0.75f, TargetCategory.NON_PLAYER);

        // Make player a golem in map
        UUID uuid = player.getUniqueId();
        playerIsGolem.put(uuid, false);

        // Spawn transformation particle effect
        player.getWorld().spawnParticle(
                Particle.CLOUD, // Choose your effect here
                player.getLocation().add(0, 1, 0), // Slightly above feet
                40, // Particle count
                0.5, 1.0, 0.5, // Spread in x, y, z
                0.01 // Speed
        );

        // Cancel particle task
        BukkitRunnable task = particleTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }

        // Remove disguise
        DisguiseAPI.undisguiseToAll(player);

        player.sendMessage("§c§l(!) §cGolem tired.");
    }

    private void startParticleTrail(Player player) {
        UUID uuid = player.getUniqueId();

        // Cancel existing task if one exists
        BukkitRunnable existing = particleTasks.get(uuid);
        if (existing != null) {
            existing.cancel();
        }

        // Create a new particle task
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !playerIsGolem.getOrDefault(uuid, false)) {
                    this.cancel();
                    particleTasks.remove(uuid);
                    return;
                }

                // Spawn particles around the player's body
                Location loc = player.getLocation().add(0, 1, 0); // Adjust Y as needed
                player.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, loc, 3, 0.3, 0.5, 0.3, 0.01);
            }
        };

        task.runTaskTimer(CustomArmorSets.getInstance(), 0L, 10L); // Every 10 ticks (0.5s)
        particleTasks.put(uuid, task);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(CustomArmorSetsCore.getArmorSet(player) instanceof GolemBusterArmorSet)) return;

        if (event.getEntity() instanceof Monster) {
            event.setDamage(event.getDamage() * 0.8); // Reduced damage from mobs
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
//        if (!(event.getEntity().getKiller())) return;
        Player player = event.getEntity().getKiller();
        if (player == null) return;
        if (!(CustomArmorSetsCore.getArmorSet(player) instanceof GolemBusterArmorSet)) return;

        // Get all attributes about entity killed
        double mobMaxHealth = 0;
        double mobBaseDamage = 0;
        AttributeInstance maxHealthAttribute = event.getEntity().getAttribute(Attribute.GENERIC_MAX_HEALTH);
        AttributeInstance attackDamageAttribute = event.getEntity().getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (maxHealthAttribute != null) {
            mobMaxHealth = maxHealthAttribute.getBaseValue(); // Get the base value of the max health
        }
        if (attackDamageAttribute != null) {
            mobBaseDamage = attackDamageAttribute.getBaseValue(); // Get the base value of the attribute
        }
        final double HEALTH_WEIGHT = 0.05;
        final double DAMAGE_POTENTIAL_WEIGHT = 0.2;
        final double BASE_REWARD_WEIGHT = 5;

        // Calculate energy based on mob kill
        double calculatedEnergy = (mobMaxHealth * HEALTH_WEIGHT) +
                (mobBaseDamage * DAMAGE_POTENTIAL_WEIGHT) +
                BASE_REWARD_WEIGHT;
//        player.sendMessage(mobMaxHealth + " " + mobBaseDamage + " " + BASE_REWARD_WEIGHT);

        // Add battery energy
        int energy = (int) calculatedEnergy;
        player.sendMessage("§a§l(!) §aBattery +" + energy + " from kill!");
        consumePlayerEnergy(player, energy);
    }

    private int consumePlayerEnergy(Player player, int amount) {
        UUID uuid = player.getUniqueId();
        int currentEnergy = playerEnergy.get(uuid);
        int newEnergy = currentEnergy + amount;

        if (newEnergy < 0) {
            return -1;
        }
        if (newEnergy > 100) {
            newEnergy = 100;
        }

        playerEnergy.put(uuid, newEnergy);
        return newEnergy;
    }

    public void spawnSpiralBeam(Player player) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        World world = player.getWorld();

        double maxDistance = 3;
        double step = 1;
        double radius = 0.5;

        for (double i = 0; i < maxDistance; i += step) {
            Location center = eye.clone().add(direction.clone().multiply(i));

            for (int j = 0; j < 20; j++) {
                double angle = Math.toRadians(j * (360.0 / 20));
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                Location particleLoc = center.clone().add(x, 0, z);

                world.spawnParticle(Particle.SMOKE_NORMAL, particleLoc, 0, 0, 0.1, 0, 0.01);
            }
        }
    }

}
