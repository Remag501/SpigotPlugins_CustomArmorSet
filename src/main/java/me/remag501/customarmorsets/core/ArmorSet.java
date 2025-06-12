package me.remag501.customarmorsets.core;

import org.bukkit.entity.Player;

public abstract class ArmorSet {
    protected final ArmorSetType type;
    public ArmorSet(ArmorSetType type) {
        this.type = type;
    }
    public ArmorSetType getType() {
        return type;
    }
    public abstract void applyPassive(Player player);
    public abstract void removePassive(Player player);
    public abstract void triggerAbility(Player player);
}

