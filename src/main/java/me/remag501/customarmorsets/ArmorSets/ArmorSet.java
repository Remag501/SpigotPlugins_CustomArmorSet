package me.remag501.customarmorsets.ArmorSets;

import org.bukkit.entity.Player;

public abstract class ArmorSet {
    public abstract String getId();
    public abstract void applyPassive(Player player);
    public abstract void removePassive(Player player);
    public abstract void triggerAbility(Player player);
}

