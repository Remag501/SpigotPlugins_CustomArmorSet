package me.remag501.armor.armor.impl;


import me.remag501.armor.armor.ArmorSet;
import me.remag501.armor.armor.ArmorSetType;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public class SnowmanArmorSet extends ArmorSet implements Listener {


    public SnowmanArmorSet() {
        super(ArmorSetType.SNOWMAN);
    }

    @Override
    public void applyPassive(Player player) {

    }

    @Override
    public void removePassive(Player player) {

    }

    @Override
    public void triggerAbility(Player player) {

    }

}
