package me.remag501.customarmorsets.ArmorSets;

import me.remag501.customarmorsets.Core.ArmorSet;
import me.remag501.customarmorsets.Core.ArmorSetType;
import org.bukkit.entity.Player;

public class ArcherArmorSet extends ArmorSet {

    public ArcherArmorSet() {
        super(ArmorSetType.ARCHER);
    }

    @Override
    public void applyPassive(Player player) {
        // TODO: Set player speed boost, halve max HP (or set to 10 HP)
        // TODO: Restrict to bows only (e.g., strip other weapons)
        // TODO: Boost bow draw speed and damage
    }

    @Override
    public void removePassive(Player player) {
        // TODO: Revert movement speed and HP modifier
    }

    @Override
    public void triggerAbility(Player player) {
        // TODO: On arrow hit, slash or knock enemies back with area effect
    }
}

