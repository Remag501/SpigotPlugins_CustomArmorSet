package me.remag501.customarmorsets.armor.impl;

import me.remag501.bgscore.api.combat.AttributeService;
import me.remag501.bgscore.api.util.BGSColor;
import me.remag501.customarmorsets.armor.ArmorSet;
import me.remag501.customarmorsets.armor.ArmorSetType;
import me.remag501.customarmorsets.manager.CooldownBarManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RoyalKnightArmorSet extends ArmorSet {

    private static final Map<UUID, Long> abilityCooldowns = new HashMap<>();
    private static final long COOLDOWN = 7 * 1000; // 7 seconds cooldown

    private final CooldownBarManager cooldownBarManager;
    private final AttributeService attributeService;

    public RoyalKnightArmorSet(CooldownBarManager cooldownBarManager, AttributeService attributeService) {
        super(ArmorSetType.ROYAL_KNIGHT);
        this.cooldownBarManager = cooldownBarManager;
        this.attributeService = attributeService;
    }

    @Override
    public void applyPassive(Player player) {
        attributeService.applyMaxHealth(player, type.getId(), 1.25);
        attributeService.applyDamage(player, type.getId(), 0.85);

    }

    @Override
    public void removePassive(Player player) {
        attributeService.resetSource(player, type.getId());

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

        // Heal 3 hearts (6 health)
        player.setHealth(Math.min(player.getAttribute(Attribute.MAX_HEALTH).getValue(), player.getHealth() + 6));
        player.sendMessage(BGSColor.POSITIVE + "You used Royal Knight's Healing!");

        cooldownBarManager.startCooldownBar(player, (int)(COOLDOWN / 1000));
        abilityCooldowns.put(uuid, now);
    }

}
