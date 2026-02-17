package me.remag501.customarmorsets.armor.impl;

import me.remag501.bgscore.api.ability.AbilityDisplay;
import me.remag501.bgscore.api.ability.AbilityService;
import me.remag501.bgscore.api.event.EventService;
import me.remag501.bgscore.api.task.TaskService;
import me.remag501.bgscore.api.util.BGSColor;
import me.remag501.customarmorsets.armor.ArmorSet;
import me.remag501.customarmorsets.armor.ArmorSetType;
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

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InfernusArmorSet extends ArmorSet {

    private final EventService eventService;
    private final TaskService taskService;
    private final AbilityService abilityService;

    private final long COOLDOWN = 10;
    private final long CHANNELING_TIME = 2;

    public InfernusArmorSet(EventService eventService, TaskService taskService, AbilityService abilityService) {
        super(ArmorSetType.INFERNUS);
        this.eventService = eventService;
        this.taskService = taskService;
        this.abilityService = abilityService;
    }

    @Override
    public void applyPassive(Player player) {
        UUID id = player.getUniqueId();
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, true, false));

        eventService.subscribe(PlayerMoveEvent.class)
                .owner(id)
                .namespace(type.getId())
                .filter(e -> e.getFrom().getBlockX() != e.getTo().getBlockX() || e.getFrom().getBlockZ() != e.getTo().getBlockZ())
                .handler(e -> {
                    Block feet = e.getTo().getBlock();
                    if (feet.getType() == Material.AIR && feet.getRelative(0, -1, 0).getType().isSolid()) {
                        feet.setType(Material.FIRE);
                        taskService.delay(60, () -> {
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
        eventService.unregisterListener(id, type.getId());
        taskService.stopTask(id, type.getId());
        abilityService.reset(id, getType().getId());
    }

    @Override
    public void triggerAbility(Player player) {
        UUID uuid = player.getUniqueId();

        if (abilityService.isActive(uuid, getType().getId())) {
            player.sendMessage(BGSColor.NEGATIVE + "Flame thrower already in use!");
            return;
        }

        if (!abilityService.isReady(uuid, getType().getId())) {
            long timeLeft = abilityService.getRemainingMillis(uuid, getType().getId()) / 1000;
            player.sendMessage(BGSColor.NEGATIVE + "Ability is on cooldown for " + timeLeft + " more seconds!");
            return;
        }

        abilityService.start(uuid, getType().getId(), Duration.ofSeconds(CHANNELING_TIME), Duration.ofSeconds(COOLDOWN), AbilityDisplay.XP_BAR);

        taskService.subscribe(player.getUniqueId(), type.getId(), 0, 2, (ticks) -> {

            if (ticks >= CHANNELING_TIME * 20) {
                // After ability ends, start cooldown bar
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