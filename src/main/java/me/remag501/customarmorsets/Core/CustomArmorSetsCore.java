package me.remag501.customarmorsets.Core;

import me.remag501.customarmorsets.Core.ArmorSet;
import me.remag501.customarmorsets.Core.ArmorSetType;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CustomArmorSetsCore {

    public static Map<UUID, ArmorSet> equippedArmor = new HashMap<>();
//    public static Map<UUID, List<ArmorSet>> armorTypes;

    public static void equipArmor(Player player, ArmorSetType type) {
        ArmorSet set = type.create();
        equippedArmor.put(player.getUniqueId(), set);
        set.applyPassive(player);
    }

    public static void unequipArmor(Player player) {
        ArmorSet set = equippedArmor.remove(player.getUniqueId());
        if (set != null)
            set.removePassive(player);
    }

    public static ArmorSet getArmorSet(Player player) {
        UUID uuid = player.getUniqueId();
        return equippedArmor.get(uuid);
    }

}
