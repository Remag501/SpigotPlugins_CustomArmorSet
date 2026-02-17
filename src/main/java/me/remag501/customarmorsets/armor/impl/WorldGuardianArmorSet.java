package me.remag501.customarmorsets.armor.impl;

import me.remag501.bgscore.api.ability.AbilityDisplay;
import me.remag501.bgscore.api.ability.AbilityService;
import me.remag501.bgscore.api.combat.AttributeService;
import me.remag501.bgscore.api.event.EventService;
import me.remag501.bgscore.api.task.TaskService;
import me.remag501.bgscore.api.util.BGSColor;
import me.remag501.customarmorsets.armor.ArmorSet;
import me.remag501.customarmorsets.armor.ArmorSetType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WorldGuardianArmorSet extends ArmorSet {

//    private static final Map<UUID, Boolean> isInvulnerable = new HashMap<>();
    private static final long COOLDOWN = 25 * 1000;

    private final EventService eventService;
    private final TaskService taskService;
    private final AbilityService abilityService;
    private final AttributeService attributeService;

    public WorldGuardianArmorSet(EventService eventService, TaskService taskService, AbilityService abilityService, AttributeService attributeService) {
        super(ArmorSetType.WORLD_GUARDIAN);
        this.eventService = eventService;
        this.taskService = taskService;
        this.abilityService = abilityService;
        this.attributeService = attributeService;
    }

    @Override
    public void applyPassive(Player player) {
        attributeService.applyMaxHealth(player, type.getId(), 1.5);
        attributeService.applySpeed(player, type.getId(), 0.8);

        // Register listener(s)
        UUID id = player.getUniqueId();
        eventService.subscribe(EntityDamageEvent.class)
                .owner(id)
                .namespace(type.getId())
                .filter(e -> e.getEntity() instanceof Player p && p.getUniqueId().equals(id))
//                .filter(e -> isInvulnerable.get(id) == true)
                .filter(e -> abilityService.isActive(id, getType().getId()))
                .handler(e -> e.setCancelled(true));
    }

    @Override
    public void removePassive(Player player) {
        attributeService.resetSource(player, type.getId());
        eventService.unregisterListener(player.getUniqueId(), type.getId());
        abilityService.reset(player.getUniqueId(), getType().getId());
    }

    @Override
    public void triggerAbility(Player player) {
        UUID uuid = player.getUniqueId();

        if (abilityService.isReady(uuid, getType().getId())) {
            long timeLeft = (abilityService.getRemainingMillis(uuid, getType().getId())) / 1000;
            player.sendMessage(BGSColor.NEGATIVE + "Ability is on cooldown for " + timeLeft + " more seconds!");
            return;
        }

        abilityService.start(uuid, getType().getId(), Duration.ofSeconds(COOLDOWN), Duration.ofSeconds(3), AbilityDisplay.XP_BAR);

//        isInvulnerable.put(uuid, true);
//
//        cooldownBarManager.startCooldownBar(player, 3);
//
//        taskService.delay(60, () -> {
//            isInvulnerable.put(uuid, false);
//            cooldownBarManager.startCooldownBar(player, (int)(COOLDOWN / 1000));
//            abilityCooldowns.put(uuid, now);
//        });

        player.sendMessage(BGSColor.POSITIVE + "You are invulnerable for 3 seconds!");
    }

}
