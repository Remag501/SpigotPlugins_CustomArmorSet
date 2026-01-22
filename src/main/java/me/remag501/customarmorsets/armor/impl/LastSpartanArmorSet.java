package me.remag501.customarmorsets.armor.impl;

import me.remag501.customarmorsets.armor.ArmorSet;
import me.remag501.customarmorsets.armor.ArmorSetType;
import me.remag501.customarmorsets.core.CustomArmorSetsCore;
import me.remag501.customarmorsets.util.AttributesUtil;
import me.remag501.customarmorsets.util.CooldownBarUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

import static me.remag501.customarmorsets.util.LookEntitiesUtil.getNearestEntityInSight;

public class LastSpartanArmorSet extends ArmorSet implements Listener {

    private static final Map<UUID, Long> abilityCooldowns = new HashMap<>();
    private static final long COOLDOWN = 3 * 1000;

    public LastSpartanArmorSet() {
        super(ArmorSetType.LAST_SPARTAN);
    }

    @Override
    public void applyPassive(Player player) {
        AttributesUtil.applyHealth(player, 0.7);
//        player.sendMessage("You equipped the Last Spartan set");
    }

    @Override
    public void removePassive(Player player) {
        AttributesUtil.removeHealth(player);
//        player.sendMessage("You removed the Last Spartan set");
    }

    @Override
    public void triggerAbility(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (abilityCooldowns.containsKey(uuid) && now - abilityCooldowns.get(uuid) < COOLDOWN) {
            long timeLeft = (COOLDOWN - (now - abilityCooldowns.get(uuid))) / 1000;
            player.sendMessage("§c§l(!) §cAbility is on cooldown for " + timeLeft + " more seconds!");
            return;
        }

        LivingEntity nearest = getNearestEntityInSight(player, 10);

        if (nearest != null) {
            Vector leap = nearest.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(1.25).setY(0.5);
            player.setVelocity(leap);

            // Play firework sparks at the player's feet when they leap
            player.getWorld().spawnParticle(
                    Particle.FIREWORKS_SPARK,
                    player.getLocation().add(0, 0.1, 0), // slightly above ground
                    30, // number of particles
                    0.2, 0.1, 0.2, // spread in x, y, z
                    0.05 // speed
            );

            // Tp player to entity if they are close to them
            LivingEntity finalNearest = nearest;
            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    double distance = Math.sqrt(Math.pow(player.getLocation().getX() - finalNearest.getLocation().getX(), 2) + Math.pow(player.getLocation().getZ() - finalNearest.getLocation().getZ(), 2));
                    if (distance < 1) {
                        // Activate spartan sequence here
                        Location landing = finalNearest.getLocation();
                        landing.setDirection(player.getLocation().getDirection());
                        player.teleport(landing);

                        // Add particle effect
                        // Play a big poof effect to notify others
                        player.getWorld().spawnParticle(
                                Particle.EXPLOSION_NORMAL,
                                player.getLocation(),
                                50, // number of particles
                                1.5, 0.2, 1.5, // wide spread on ground
                                0.1 // speed
                        );

                        // Optionally add a little dust/sand effect for extra flair
                        player.getWorld().spawnParticle(
                                Particle.BLOCK_DUST,
                                player.getLocation(),
                                80, // number of particles
                                1.5, 0.2, 1.5, // wide spread
                                0.1, // speed
                                Material.SAND.createBlockData() // looks like sand blast
                        );

                        // Apply effects on enemy
                        finalNearest.setVelocity(player.getLocation().getDirection().normalize().multiply(2).setY(1));
                        finalNearest.damage(10, player);

                        cancel();
                    } else if (player.isOnGround() && ticks > 10) { // Enemy got away from spartan jump
                        player.sendMessage("§c§l(!) §cYou missed!");
                        cancel();
                    } else if (ticks >= 100) {
                        // Reduce lag, likely if occurs if player's client is bugged/spoof or player is air
                        cancel();
                    }
                    ticks++;
                }
            }.runTaskTimer(Bukkit.getPluginManager().getPlugin("CustomArmorSets"), 0, 1L);

            abilityCooldowns.put(uuid, now);
            Plugin plugin = Bukkit.getPluginManager().getPlugin("CustomArmorSets");
            CooldownBarUtil.startCooldownBar(plugin, player, (int) (COOLDOWN / 1000));

        } else {
            player.sendMessage("§c§l(!) §cNo enemies nearby to leap toward!");
        }

    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        ArmorSet set = CustomArmorSetsCore.getArmorSet(player);
        if (!(set instanceof LastSpartanArmorSet)) return;

        // Check if the player is holding a sword
        Material itemInHand = player.getInventory().getItemInMainHand().getType();
        if (itemInHand.name().endsWith("_SWORD")) {
            double originalDamage = event.getDamage();
            event.setDamage(originalDamage * 1.25); // Increase damage by 25%
        }
    }

}
