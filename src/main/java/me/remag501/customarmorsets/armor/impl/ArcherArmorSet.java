package me.remag501.customarmorsets.armor.impl;

import me.remag501.customarmorsets.armor.ArmorSet;
import me.remag501.customarmorsets.armor.ArmorSetType;
import me.remag501.customarmorsets.manager.ArmorManager;
import me.remag501.customarmorsets.util.AttributesUtil;
import me.remag501.customarmorsets.util.CooldownBarUtil;
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

public class ArcherArmorSet extends ArmorSet implements Listener {

    private static final Map<UUID, Long> abilityCooldowns = new HashMap<>();
    private static final long COOLDOWN = 5 * 1000; // 5 seconds

    public ArcherArmorSet() {
        super(ArmorSetType.ARCHER);
    }

    @Override
    public void applyPassive(Player player) {
        // Halve max HP (set max health to 10) and give Speed 1.25x
        AttributesUtil.applySpeed(player, 1.5);
        AttributesUtil.applyHealth(player, 0.5);
//        player.sendMessage("✅ You equipped the Archer set");
    }

    @Override
    public void removePassive(Player player) {
        AttributesUtil.removeSpeed(player);
        AttributesUtil.removeHealth(player);
//        player.sendMessage("❌ You removed the Archer set");
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
        CooldownBarUtil.startCooldownBar(Bukkit.getPluginManager().getPlugin("CustomArmorSets"), player, (int) COOLDOWN / 1000);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Player player = null;
        boolean isProjectile = false;

        // Determine damager and shooter
        if (event.getDamager() instanceof Arrow arrow && arrow.getShooter() instanceof Player p) {
            player = p;
            isProjectile = true;

            // Headshot logic
            if (event.getEntity() instanceof LivingEntity target) {
                // Check for humanoid target
                Set<Class<?>> humanoidTypes = Set.of(
                        Player.class, Zombie.class, Skeleton.class, Villager.class,
                        Piglin.class, Vindicator.class, Evoker.class, Pillager.class
                );

                boolean isHumanoid = humanoidTypes.stream().anyMatch(type -> type.isInstance(target));
                if (isHumanoid) {
                    double targetHeight = target.getHeight();
                    double hitY = arrow.getLocation().getY() - target.getLocation().getY();

                    if (hitY > targetHeight * 0.75) {
                        event.setDamage(event.getDamage() * 1.5); // Headshot bonus
                        player.sendMessage("§a§l(!) §aHeadshot!");
                    }
                }
            }
        } else if (event.getDamager() instanceof Player p) {
            player = p;
        }

        if (player == null) return;

        // Check if player is wearing Archer armor
        if (!(ArmorManager.getArmorSet(player) instanceof ArcherArmorSet)) return;

        // Apply Archer bonuses
        if (isProjectile) {
            event.setDamage(event.getDamage() * 1.25); // Bow bonus
        } else if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            event.setDamage(event.getDamage() * 0.5);  // Melee penalty
        }
    }


}

