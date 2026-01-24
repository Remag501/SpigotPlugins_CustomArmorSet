package me.remag501.customarmorsets.armor.impl;

import me.remag501.bgscore.api.TaskHelper;
import me.remag501.customarmorsets.armor.ArmorSet;
import me.remag501.customarmorsets.armor.ArmorSetType;
import me.remag501.customarmorsets.manager.ArmorManager;
import me.remag501.customarmorsets.service.AttributesService;
import me.remag501.customarmorsets.manager.CooldownBarManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RoyalKnightArmorSet extends ArmorSet {

    private static final Map<UUID, Long> abilityCooldowns = new HashMap<>();
    private static final long COOLDOWN = 7 * 1000; // 7 seconds cooldown

    private final TaskHelper api;
    private final CooldownBarManager cooldownBarManager;
    private final AttributesService attributesService;

    public RoyalKnightArmorSet(TaskHelper api, CooldownBarManager cooldownBarManager, AttributesService attributesService) {
        super(ArmorSetType.ROYAL_KNIGHT);
        this.api = api;
        this.cooldownBarManager = cooldownBarManager;
        this.attributesService = attributesService;
    }

    @Override
    public void applyPassive(Player player) {
        attributesService.applyHealth(player, 1.25);
        attributesService.applyDamage(player, 0.85);

    }

    @Override
    public void removePassive(Player player) {
        attributesService.removeHealth(player);
        attributesService.removeDamage(player);

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

        // Heal 3 hearts (6 health)
        player.setHealth(Math.min(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(), player.getHealth() + 6));
        player.sendMessage("§a§l(!) §aYou used Royal Knight's Healing!");

        cooldownBarManager.startCooldownBar(player, (int)(COOLDOWN / 1000));
        abilityCooldowns.put(uuid, now);
    }

}
