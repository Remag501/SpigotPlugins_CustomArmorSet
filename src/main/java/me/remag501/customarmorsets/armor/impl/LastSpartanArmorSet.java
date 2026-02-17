package me.remag501.customarmorsets.armor.impl;

import me.remag501.bgscore.api.ability.AbilityDisplay;
import me.remag501.bgscore.api.ability.AbilityService;
import me.remag501.bgscore.api.combat.AttributeService;
import me.remag501.bgscore.api.combat.CombatStatsService;
import me.remag501.bgscore.api.combat.WeaponType;
import me.remag501.bgscore.api.event.EventService;
import me.remag501.bgscore.api.task.TaskService;
import me.remag501.bgscore.api.util.BGSColor;
import me.remag501.customarmorsets.armor.ArmorSet;
import me.remag501.customarmorsets.armor.ArmorSetType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.*;

import static me.remag501.customarmorsets.util.LookEntitiesUtil.getNearestEntityInSight;

public class LastSpartanArmorSet extends ArmorSet {

//    private static final Map<UUID, Long> abilityCooldowns = new HashMap<>();
    private static final long COOLDOWN = 3;

    private final EventService eventService;
    private final TaskService taskService;
    private final AbilityService abilityService;
    private final AttributeService attributeService;
    private final CombatStatsService combatStatsService;

    public LastSpartanArmorSet(EventService eventService, TaskService taskService, AbilityService abilityService,
                               AttributeService attributeService, CombatStatsService combatStatsService) {
        super(ArmorSetType.LAST_SPARTAN);
        this.eventService = eventService;
        this.taskService = taskService;
        this.abilityService = abilityService;
        this.attributeService = attributeService;
        this.combatStatsService = combatStatsService;
    }

    @Override
    public void applyPassive(Player player) {
        attributeService.applyMaxHealth(player, type.getId(), -0.3);
        combatStatsService.setWeaponDamageMod(player.getUniqueId(), type.getId(), 1.25F, WeaponType.SWORD);
    }

    @Override
    public void removePassive(Player player) {
        attributeService.resetSource(player, type.getId());
        combatStatsService.removeAllMods(player.getUniqueId(), type.getId());

        eventService.unregisterListener(player.getUniqueId(), type.getId());
        taskService.stopTask(player.getUniqueId(), type.getId());
    }

    @Override
    public void triggerAbility(Player player) {
        UUID uuid = player.getUniqueId();

        if (abilityService.isReady(uuid, getType().getId())) {
            long timeLeft = abilityService.getRemainingMillis(uuid, getType().getId()) / 1000;
            player.sendMessage(BGSColor.NEGATIVE + "Ability is on cooldown for " + timeLeft + " more seconds!");
            return;
        }

        LivingEntity nearest = getNearestEntityInSight(player, 10);

        if (nearest != null) {
            Vector leap = nearest.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(1.25).setY(0.5);
            player.setVelocity(leap);

            // Play firework sparks at the player's feet when they leap
            player.getWorld().spawnParticle(
                    Particle.FIREWORK,
                    player.getLocation().add(0, 0.1, 0), // slightly above ground
                    30, // number of particles
                    0.2, 0.1, 0.2, // spread in x, y, z
                    0.05 // speed
            );

            // Tp player to entity if they are close to them
            LivingEntity finalNearest = nearest;
            taskService.subscribe(player.getUniqueId(), type.getId(), 0, 1, (ticks) -> {
                double distance = Math.sqrt(Math.pow(player.getLocation().getX() - finalNearest.getLocation().getX(), 2) + Math.pow(player.getLocation().getZ() - finalNearest.getLocation().getZ(), 2));
                if (distance < 1) {
                    // Activate spartan sequence here
                    Location landing = finalNearest.getLocation();
                    landing.setDirection(player.getLocation().getDirection());
                    player.teleport(landing);

                    // Add particle effect
                    // Play a big poof effect to notify others
                    player.getWorld().spawnParticle(
                            Particle.EXPLOSION,
                            player.getLocation(),
                            50, // number of particles
                            1.5, 0.2, 1.5, // wide spread on ground
                            0.1 // speed
                    );

                    // Optionally add a little dust/sand effect for extra flair
                    player.getWorld().spawnParticle(
                            Particle.BLOCK,
                            player.getLocation(),
                            80, // number of particles
                            1.5, 0.2, 1.5, // wide spread
                            0.1, // speed
                            Material.SAND.createBlockData() // looks like sand blast
                    );

                    // Apply effects on enemy
                    finalNearest.setVelocity(player.getLocation().getDirection().normalize().multiply(2).setY(1));
                    finalNearest.damage(10, player);

                    return true;
                } else if (player.isOnGround() && ticks > 10) { // Enemy got away from spartan jump
                    player.sendMessage(BGSColor.NEGATIVE + "You missed!");
                    return true;
                } else if (ticks >= 100) {
                    // Reduce lag, likely if occurs if player's client is bugged/spoof or player is air
                    return true;
                }

                return false;

            });

            // Add cooldown and visualize
            abilityService.startCooldown(uuid, getType().getId(), Duration.ofSeconds(COOLDOWN), AbilityDisplay.XP_BAR);

        } else {
            player.sendMessage(BGSColor.NEGATIVE + "No enemies nearby to leap toward!");
        }

    }

}
