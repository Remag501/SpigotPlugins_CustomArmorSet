package me.remag501.customarmorsets.armor.impl;

import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import me.remag501.bgscore.api.ability.AbilityService;
import me.remag501.bgscore.api.combat.AttributeService;
import me.remag501.bgscore.api.combat.CombatStatsService;
import me.remag501.bgscore.api.combat.TargetCategory;
import me.remag501.bgscore.api.event.EventService;
import me.remag501.bgscore.api.task.TaskService;
import me.remag501.bgscore.api.util.BGSColor;
import me.remag501.customarmorsets.armor.ArmorSet;
import me.remag501.customarmorsets.armor.ArmorSetType;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;


public class GolemBusterArmorSet extends ArmorSet {

    private static final Map<UUID, Boolean> playerIsGolem = new HashMap<>();
    private final List<UUID> particleTasks = new ArrayList<>();
    private static final Map<UUID, Long> stunCooldown = new HashMap<>();

    private final EventService eventService;
    private final TaskService taskService;
    private final AbilityService abilityService;
    private final AttributeService attributeService;
    private final CombatStatsService combatStatsService;

    public GolemBusterArmorSet(EventService eventService, TaskService taskService, AbilityService abilityService,
                               AttributeService attributeService, CombatStatsService combatStatsService) {
        super(ArmorSetType.GOLEM_BUSTER);
        this.eventService = eventService;
        this.taskService = taskService;
        this.abilityService = abilityService;
        this.attributeService = attributeService;
        this.combatStatsService = combatStatsService;
    }

    @Override
    public void applyPassive(Player player) {
        UUID id = player.getUniqueId();
        playerIsGolem.put(id, false);

        // Apply Damage Stats
        combatStatsService.setTargetDamageMod(player.getUniqueId(), type.getId(), 1.5f, TargetCategory.NON_PLAYER);
        combatStatsService.setSourceDefenseMod(player.getUniqueId(), type.getId(), 0.75f, TargetCategory.NON_PLAYER);

        // Start energy loop timer
        taskService.subscribe(player.getUniqueId(), "golem_energy_loop", 0, 20, (ticks) -> {
            int energy;
            if (playerIsGolem.get(id)) {
                energy = consumePlayerEnergy(player, -5);
                if (energy <= 0) {
                    transformBack(player);
                    return false;
                }
            }
            else
                energy = consumePlayerEnergy(player, 1);

            cooldownBarManager.setLevel(player, energy);
            return false;
        });

        // Register listener(s)
        eventService.subscribe(EntityDeathEvent.class)
                .owner(id)
                .namespace(type.getId())
                // Filter: The killer exists and is the owner of this armor set
                .filter(e -> e.getEntity().getKiller() != null && e.getEntity().getKiller().getUniqueId().equals(id))
                .handler(this::onEntityDeath);
    }

    @Override
    public void removePassive(Player player) {
        transformBack(player);
        cooldownBarManager.restorePlayerBar(player);
        attributeService.resetSource(player, type.getId());
        combatStatsService.removeAllMods(player.getUniqueId(), getType().getId());

        eventService.unregisterListener(player.getUniqueId(), type.getId());
        taskService.stopTask(player.getUniqueId(), "golem_energy_loop");
        taskService.stopTask(player.getUniqueId(), "golem_particle");
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
                    player.sendMessage(BGSColor.NEGATIVE + "Ability on cooldown!");
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
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2));

                        // Particles
                        player.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3);
                        startTargetEffect(target);
                    }
                }

                // Optional confirmation
                player.sendMessage(BGSColor.POSITIVE + "You slam the ground, stunning enemies ahead!");
                return;
            }
        }
        // Check if player is trying to transform
        if (player.isSneaking()) {
//            battery = consumePlayerEnergy(player, -50);
            if (battery >= 50) {
                player.sendMessage(BGSColor.POSITIVE + "Golem Smash");
                cooldownBarManager.setLevel(player, battery);
                golemTransform(player);
                return;
            }
        }
        // Check if player has enough battery
        battery = consumePlayerEnergy(player, -5);
        if (battery < 0) {
            player.sendMessage(BGSColor.NEGATIVE + "Not enough battery");
            return;
        }

        cooldownBarManager.setLevel(player, battery);

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
        player.sendMessage(BGSColor.POSITIVE + "Battery gun discharged!");
    }

    public void startTargetEffect(LivingEntity target) {
        taskService.subscribe(target.getUniqueId(), (ticks) -> {
            if (ticks >= 60 || target.isDead()) {
                return true;
            }

            // Location slightly above the target (head level)
            Location loc = target.getLocation().add(0, 1.2, 0);

            // Redstone-like particle effect
            Particle.DustOptions redDust = new Particle.DustOptions(Color.RED, 1.2F);
            target.getWorld().spawnParticle(Particle.FIREWORK, loc, 10, 0.3, 0.4, 0.3, 0, redDust);

            return false;
        });
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
        attributeService.applyMaxHealth(player, type.getId(), 2.0);
        attributeService.applySpeed(player, type.getId(), 0.5);
        combatStatsService.setTargetDamageMod(player.getUniqueId(), getType().getId(),2, TargetCategory.NON_PLAYER);
        combatStatsService.setSourceDefenseMod(player.getUniqueId(), getType().getId(),0.25f, TargetCategory.NON_PLAYER);

        taskService.delay(2, () -> {
            player.setHealth(40.0);
        });

    }

    private void transformBack(Player player)  {
        // Remove attributes and damage stats
        attributeService.resetSource(player, type.getId());
        combatStatsService.setTargetDamageMod(player.getUniqueId(), type.getId(), 1.5f, TargetCategory.NON_PLAYER);
        combatStatsService.setSourceDefenseMod(player.getUniqueId(), type.getId(), 0.75f, TargetCategory.NON_PLAYER);

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

        // Cancel existing task if one exists
        if (particleTasks.contains(uuid)) {
            taskService.stopTask(player.getUniqueId(), "golem_particle");
        }

        // Remove disguise
        DisguiseAPI.undisguiseToAll(player);

        player.sendMessage(BGSColor.NEGATIVE + "Golem tired.");
    }

    private void startParticleTrail(Player player) {
        UUID uuid = player.getUniqueId();

        // Cancel existing task if one exists
        if (particleTasks.contains(uuid)) {
            taskService.stopTask(player.getUniqueId(), "golem_particle");
        }

        // Create a new particle task
        taskService.subscribe(player.getUniqueId(), "golem_particle", 0, 10, (ticks) -> {
            if (!player.isOnline() || !playerIsGolem.getOrDefault(uuid, false)) {
                particleTasks.remove(uuid);
                return true;
            }

            // Spawn particles around the player's body
            Location loc = player.getLocation().add(0, 1, 0); // Adjust Y as needed
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 3, 0.3, 0.5, 0.3, 0.01);
            return false;
        });

        particleTasks.add(uuid);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player player = event.getEntity().getKiller();

        // Get all attributes about entity killed
        double mobMaxHealth = 0;
        double mobBaseDamage = 0;
        AttributeInstance maxHealthAttribute = event.getEntity().getAttribute(Attribute.MAX_HEALTH);
        AttributeInstance attackDamageAttribute = event.getEntity().getAttribute(Attribute.ATTACK_DAMAGE);
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
        player.sendMessage(BGSColor.POSITIVE + "Battery +" + energy + " from kill!");
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

                world.spawnParticle(Particle.SMOKE, particleLoc, 0, 0, 0.1, 0, 0.01);
            }
        }
    }

}
