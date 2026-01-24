package me.remag501.customarmorsets.armor.impl;

import me.remag501.bgscore.api.TaskHelper;
import me.remag501.customarmorsets.armor.ArmorSet;
import me.remag501.customarmorsets.armor.ArmorSetType;
import me.remag501.customarmorsets.manager.ArmorManager;
import me.remag501.customarmorsets.service.AttributesService;
import me.remag501.customarmorsets.manager.CooldownBarManager;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ArcherArmorSet extends ArmorSet {

    private static final Map<UUID, Long> abilityCooldowns = new HashMap<>();
    private static final long COOLDOWN = 5 * 1000; // 5 seconds

    private final TaskHelper api;
    private final CooldownBarManager cooldownBarManager;;
    private final AttributesService attributesService;

    public ArcherArmorSet(TaskHelper api, CooldownBarManager cooldownBarManager, AttributesService attributesService) {
        super(ArmorSetType.ARCHER);
        this.api = api;
        this.armorManager = armorManager;
        this.cooldownBarManager = cooldownBarManager;
        this.attributesService = attributesService;
    }

    @Override
    public void applyPassive(Player player) {
        attributesService.applySpeed(player, 1.5);
        attributesService.applyHealth(player, 0.5);
        UUID id = player.getUniqueId();

        api.subscribe(EntityDamageByEntityEvent.class)
                .owner(id)
                .namespace(type.getId())
                .filter(e -> {
                    // Case 1: Direct Melee
                    if (e.getDamager() instanceof Player p) {
                        return p.getUniqueId().equals(id);
                    }
                    // Case 2: Bow/Projectile
                    if (e.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player shooter) {
                        return shooter.getUniqueId().equals(id);
                    }
                    return false;
                })
                .handler(this::onEntityDamageByEntity);
    }

    @Override
    public void removePassive(Player player) {
        attributesService.removeSpeed(player);
        attributesService.removeHealth(player);

        api.unregisterListener(player.getUniqueId(), type.getId());
    }

    @Override
    public void triggerAbility(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (abilityCooldowns.containsKey(uuid) && now - abilityCooldowns.get(uuid) < COOLDOWN) {
            long timeLeft = (COOLDOWN - (now - abilityCooldowns.get(uuid))) / 1000;
            player.sendMessage("§c§l(!) §cAbility on cooldown for " + timeLeft + " seconds!");
            return;
        }

        Location loc = player.getLocation();
        double radius = 5.0;

        // Damage and knockback nearby entities
        for (Entity e : player.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof LivingEntity living && !e.equals(player)) {
                living.damage(10, player); // Slash damage
                Vector knockback = e.getLocation().toVector().subtract(loc.toVector()).normalize().add(new Vector(0, 1.25, 0)).multiply(0.75);
                living.setVelocity(knockback);
            }
        }

        // Give player jump boost
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 3*20, 1), false);

        // Play sound globally
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.6f, 1.2f);
        }

        // Play local sweep sound
        player.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);

        // Vertical slash particles
        World world = player.getWorld();
        Vector dir = player.getLocation().getDirection().normalize();
        Location center = player.getEyeLocation().add(dir.clone().multiply(1.5));

        for (double y = -1.5; y <= 1.5; y += 0.25) {
            Location particleLoc = center.clone().add(0, y, 0);
            world.spawnParticle(Particle.SWEEP_ATTACK, particleLoc, 1, 0, 0, 0, 0);
        }

        abilityCooldowns.put(uuid, now);
        cooldownBarManager.startCooldownBar(player, (int) COOLDOWN / 1000);
    }

    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // We know the damager (or shooter) is the owner of this listener
        boolean isProjectile = event.getDamager() instanceof Projectile;

        // We can safely grab the shooter or the damager as the 'player'
        Player player = (isProjectile)
                ? (Player) ((Projectile) event.getDamager()).getShooter()
                : (Player) event.getDamager();

        if (isProjectile) {
            // 1. Headshot Logic
            if (event.getDamager() instanceof Arrow arrow && event.getEntity() instanceof LivingEntity target) {
                if (isHeadshot(arrow, target)) {
                    event.setDamage(event.getDamage() * 1.5);
                    player.sendMessage("§a§l(!) §aHeadshot!");
                }
            }
            // 2. Global Bow Bonus
            event.setDamage(event.getDamage() * 1.25);
        } else {
            // 3. Melee Penalty
            event.setDamage(event.getDamage() * 0.5);
        }
    }

    private boolean isHeadshot(Arrow arrow, LivingEntity target) {
        // Keep your humanoid list here or in a static set
        double hitY = arrow.getLocation().getY() - target.getLocation().getY();
        return hitY > target.getHeight() * 0.75;
    }


}

