package me.remag501.customarmorsets.core;

import me.remag501.customarmorsets.CustomArmorSets;
import me.remag501.customarmorsets.utils.HelmetCosmeticUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CustomArmorSetsCore {

    public static Map<UUID, ArmorSet> equippedArmor = new HashMap<>();
    public static Map<UUID, ArmorSetType> equippedHelmet = new HashMap<>();

    private static List<String> allowedWorlds = List.of("kuroko", "icecaverns", "sahara", "test");

    public static boolean equipArmor(Player player, ArmorSetType type) {
        // Equip player head
        Bukkit.getScheduler().runTask(CustomArmorSets.getInstance(), () -> {
            ItemStack helmet = player.getInventory().getHelmet();
            if (helmet != null) {
                player.getInventory().setHelmet(HelmetCosmeticUtil.makeCosmeticHelmet(helmet, type.getHeadUrl()));
            }
        });
        equippedHelmet.put(player.getUniqueId(), type);

        // Check if player is pvp world
        World world = player.getWorld();
        if (!allowedWorlds.contains(world.getName())) {
            return false;
        }

        // Create armor set instance, map it to player, and activate passive
        ArmorSet set = type.create();
        equippedArmor.put(player.getUniqueId(), set);
        set.applyPassive(player);
        return true;
    }

    public static void unequipArmor(Player player) {
        // Get set and helmet instances
        ArmorSet set = equippedArmor.remove(player.getUniqueId());
        ArmorSetType type = equippedHelmet.get(player.getUniqueId());
        // First check player has a set
        if (set != null) {
            // Remove armor set instance from player map and deactivate passive
            set.removePassive(player);
        }

        // Unequip player head
        if (type != null) {
            ItemStack helmet = player.getInventory().getHelmet();
            if (helmet != null) {
                player.getInventory().setHelmet(HelmetCosmeticUtil.restoreOriginalHelmet(helmet, type.getLeatherColor()));
            }
            equippedHelmet.remove(player.getUniqueId());
        }

    }

    public static ArmorSet getArmorSet(Player player) {
        UUID uuid = player.getUniqueId();
        return equippedArmor.get(uuid);
    }

}
