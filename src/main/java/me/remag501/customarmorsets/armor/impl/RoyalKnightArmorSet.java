package me.remag501.customarmorsets.armor.impl;

import me.remag501.bgscore.api.ability.AbilityDisplay;
import me.remag501.bgscore.api.ability.AbilityService;
import me.remag501.bgscore.api.combat.AttributeService;
import me.remag501.bgscore.api.util.BGSColor;
import me.remag501.customarmorsets.armor.ArmorSet;
import me.remag501.customarmorsets.armor.ArmorSetType;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RoyalKnightArmorSet extends ArmorSet {

    private static final long COOLDOWN = 7; // 7 seconds cooldown

    private final AbilityService abilityService;
    private final AttributeService attributeService;

    public RoyalKnightArmorSet(AbilityService abilityService, AttributeService attributeService) {
        super(ArmorSetType.ROYAL_KNIGHT);
        this.abilityService = abilityService;
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
        abilityService.reset(player.getUniqueId(), getType().getId());
    }

    @Override
    public void triggerAbility(Player player) {
        UUID uuid = player.getUniqueId();

        if (!abilityService.isReady(uuid, getType().getId())) {
            long timeLeft = (abilityService.getRemainingMillis(uuid, getType().getId())) / 1000;
            player.sendMessage(BGSColor.NEGATIVE + "Ability is on cooldown for " + timeLeft + " more seconds!");
            return;
        }

        // Heal 3 hearts (6 health)
        player.setHealth(Math.min(player.getAttribute(Attribute.MAX_HEALTH).getValue(), player.getHealth() + 6));
        player.sendMessage(BGSColor.POSITIVE + "You used Royal Knight's Healing!");

        abilityService.startCooldown(uuid, getType().getId(), Duration.ofSeconds(COOLDOWN), AbilityDisplay.XP_BAR);
    }

}
