package me.remag501.customarmorsets.ArmorSets;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CustomArmorSetsCore {

    public static Map<UUID, ArmorSet> equippedArmor = new HashMap<>();
//    public static Map<UUID, List<ArmorSet>> armorTypes;

    public static void equipArmor(Player player, String setID) {
        ArmorSet set = new SnowmanArmorSet();
        if (equippedArmor.get(player.getUniqueId()) != null)
            return;
        equippedArmor.put(player.getUniqueId(), set); // use set id to figure out which class to use
        set.applyPassive(player);

    }

    public static void unequipArmor(Player player, String setID) {
        if (equippedArmor.get(player.getUniqueId()) == null)
            return;
        ArmorSet set = equippedArmor.get(player.getUniqueId());
        set.removePassive(player);
        equippedArmor.remove(player.getUniqueId()); // use set id to figure out which class to use
    }

}
