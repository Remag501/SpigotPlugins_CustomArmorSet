package me.remag501.customarmorsets.armor.impl;

import me.remag501.bgscore.api.TaskHelper;
import me.remag501.customarmorsets.armor.ArmorSet;
import me.remag501.customarmorsets.armor.ArmorSetType;
import me.remag501.customarmorsets.manager.CooldownBarManager;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InfernusArmorSet extends ArmorSet {

    private final TaskHelper api;
    private final CooldownBarManager cooldownBarManager;

    // OPTION A: Static memory for per-player cooldowns
    private final Map<UUID, Long> abilityCooldowns = new ConcurrentHashMap<>();
    private final long COOLDOWN = 10 * 1000;

    public InfernusArmorSet(TaskHelper api, CooldownBarManager cooldownBarManager) {
        super(ArmorSetType.INFERNUS);
        this.api = api;
        this.cooldownBarManager = cooldownBarManager;
    }

    @Override
    public void applyPassive(Player player) {
        UUID id = player.getUniqueId();
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, true, false));

        api.subscribe(PlayerMoveEvent.class)
                .owner(id)
                .namespace(type.getId())
                .filter(e -> e.getFrom().getBlockX() != e.getTo().getBlockX() || e.getFrom().getBlockZ() != e.getTo().getBlockZ())
                .handler(e -> {
                    Block feet = e.getTo().getBlock();
                    if (feet.getType() == Material.AIR && feet.getRelative(0, -1, 0).getType().isSolid()) {
                        feet.setType(Material.FIRE);
                        api.delay(60, () -> {
                            if (feet.getType() == Material.FIRE) feet.setType(Material.AIR);
                        });
                    }
                });
    }

    @Override
    public void removePassive(Player player) {
        UUID id = player.getUniqueId();
        player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);

        // Clean up the API side
        api.unregisterListener(id, type.getId());
        api.stopTask(id, type.getId());

        // OPTION A CLEANUP: Keep memory clean when they take the armor off
        abilityCooldowns.remove(id);
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

        // Set up the 2 second active ability phase
        int activeDurationSeconds = 2;

        // Show bar during active ability
        cooldownBarManager.startCooldownBar(player, activeDurationSeconds);

        api.subscribe(player.getUniqueId(), type.getId(), 0, 2, (ticks) -> {

            if (ticks >= activeDurationSeconds * 20) {
                // After ability ends, start cooldown bar
                int cooldownSeconds = (int) (COOLDOWN / 1000);
                abilityCooldowns.put(uuid, System.currentTimeMillis());
                cooldownBarManager.startCooldownBar(player, cooldownSeconds);
                return true;
            }

            // Emit a trail of flame particles
            for (double i = 0; i <= 5; i += 0.5) {
                Vector point = player.getLocation().getDirection().normalize().multiply(i);
                player.getWorld().spawnParticle(Particle.FLAME,
                        player.getLocation().add(point),
                        5, 0.2, 0.2, 0.2, 0);

                // Damage entities nearby
                for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
                    if (entity instanceof LivingEntity livingEntity && entity != player) {
                        if (entity.getLocation().distance(player.getLocation().add(point)) < 1.5) {
                            livingEntity.damage(4, player);
                            livingEntity.setFireTicks(160);
                        }
                    }
                }
            }

            return false;
        });
    }

}