package me.remag501.customarmorsets.ArmorSets;

import me.remag501.customarmorsets.Core.ArmorSet;
import me.remag501.customarmorsets.Core.ArmorSetType;
import org.bukkit.entity.Player;

public class VampireArmorSet extends ArmorSet {

    public VampireArmorSet() {
        super(ArmorSetType.VAMPIRE);
    }

    @Override
    public void applyPassive(Player player) {
        // TODO: Give passive life-drain effect (e.g., deal damage heals you slightly)
    }

    @Override
    public void removePassive(Player player) {
        // TODO: Remove any potion effects or modifiers given in applyPassive
    }

    @Override
    public void triggerAbility(Player player) {
        // TODO: If enemy is alive: drain HP
        // TODO: On enemy death: heal player or turn into bat temporarily
    }
}
