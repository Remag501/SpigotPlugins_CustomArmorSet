package me.remag501.customarmorsets.ArmorSets;

import me.remag501.customarmorsets.Core.ArmorSet;
import me.remag501.customarmorsets.Core.ArmorSetType;
import org.bukkit.entity.Player;

public class FisterArmorSet extends ArmorSet {

    public FisterArmorSet() {
        super(ArmorSetType.FISTER);
    }

    @Override
    public void applyPassive(Player player) {
        // TODO: Give passive resistance/invulnerability effect
        // TODO: Add overshield up to 150% health (e.g., absorption hearts)
        // TODO: Apply Haste, regeneration
        // TODO: Prevent player from using weapons
    }

    @Override
    public void removePassive(Player player) {
        // TODO: Remove potion effects and any overshield
    }

    @Override
    public void triggerAbility(Player player) {
        // TODO: Trigger burst punches (e.g., rapid melee attacks)
        // You might launch multiple punches via cooldowned task
    }
}

