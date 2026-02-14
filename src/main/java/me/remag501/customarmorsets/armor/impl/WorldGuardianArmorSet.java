package me.remag501.customarmorsets.armor.impl;

import me.remag501.bgscore.api.event.EventService;
import me.remag501.bgscore.api.task.TaskService;
import me.remag501.bgscore.api.util.BGSColor;
import me.remag501.customarmorsets.armor.ArmorSet;
import me.remag501.customarmorsets.armor.ArmorSetType;
import me.remag501.customarmorsets.manager.ArmorManager;
import me.remag501.customarmorsets.service.AttributesService;
import me.remag501.customarmorsets.manager.CooldownBarManager;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WorldGuardianArmorSet extends ArmorSet {

    private static final Map<UUID, Long> abilityCooldowns = new HashMap<>();
    private static final Map<UUID, Boolean> isInvulnerable = new HashMap<>();
    private static final long COOLDOWN = 25 * 1000;

    private final EventService eventService;
    private final TaskService taskService;
    private final CooldownBarManager cooldownBarManager;
    private final AttributesService attributesService;

    public WorldGuardianArmorSet(EventService eventService, TaskService taskService, CooldownBarManager cooldownBarManager, AttributesService attributesService) {
        super(ArmorSetType.WORLD_GUARDIAN);
        this.eventService = eventService;
        this.taskService = taskService;
        this.cooldownBarManager = cooldownBarManager;
        this.attributesService = attributesService;
    }

    @Override
    public void applyPassive(Player player) {
        attributesService.applyHealth(player, 1.5);
        attributesService.applySpeed(player, 0.8);

        // Register listener(s)
        UUID id = player.getUniqueId();
        eventService.subscribe(EntityDamageEvent.class)
                .owner(id)
                .namespace(type.getId())
                .filter(e -> e.getEntity() instanceof Player p && p.getUniqueId().equals(id))
                .filter(e -> isInvulnerable.get(id) == true)
                .handler(e -> e.setCancelled(true));
    }

    @Override
    public void removePassive(Player player) {
        attributesService.removeHealth(player);
        attributesService.removeSpeed(player);

        eventService.unregisterListener(player.getUniqueId(), type.getId());
    }

    @Override
    public void triggerAbility(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (abilityCooldowns.containsKey(uuid) && now - abilityCooldowns.get(uuid) < COOLDOWN) {
            long timeLeft = (COOLDOWN - (now - abilityCooldowns.get(uuid))) / 1000;
            player.sendMessage(BGSColor.NEGATIVE + "Ability is on cooldown for " + timeLeft + " more seconds!");
            return;
        }

        isInvulnerable.put(uuid, true);

        cooldownBarManager.startCooldownBar(player, 3);

        taskService.delay(60, () -> {
            isInvulnerable.put(uuid, false);
            cooldownBarManager.startCooldownBar(player, (int)(COOLDOWN / 1000));
            abilityCooldowns.put(uuid, now);
        });

        player.sendMessage(BGSColor.POSITIVE + "You are invulnerable for 3 seconds!");
    }

}
