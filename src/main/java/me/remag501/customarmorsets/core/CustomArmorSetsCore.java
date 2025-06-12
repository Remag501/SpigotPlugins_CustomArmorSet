package me.remag501.customarmorsets.core;

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

    private static List<String> allowedWorlds = List.of("kuroko", "icecaverns", "sahara", "test");

    public static boolean equipArmor(Player player, ArmorSetType type) {
        // Check if player is pvp world
        World world = player.getWorld();
        if (!allowedWorlds.contains(world.getName())) {
            return false;
        }

        // Equip player head
        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("CustomArmorSets"), () -> {
            ItemStack helmet = player.getInventory().getHelmet();
            if (helmet != null) {
                player.getInventory().setHelmet(HelmetCosmeticUtil.makeCosmeticHelmet(helmet, type.getHeadUrl()));
            }
        });

        // Create armor set instance, map it to player, and activate passive
        ArmorSet set = type.create();
        equippedArmor.put(player.getUniqueId(), set);
        set.applyPassive(player);
        return true;
    }

    public static void unequipArmor(Player player) {

        ArmorSet set = equippedArmor.remove(player.getUniqueId());
        if (set != null) {

            // Unequip player head
            ArmorSetType type = set.getType();
            ItemStack helmet = player.getInventory().getHelmet();
            if (helmet != null) {
                player.getInventory().setHelmet(HelmetCosmeticUtil.restoreOriginalHelmet(helmet, type.getLeatherColor()));
            }

            // Remove armor set instance from player map and deactivate passive
            set.removePassive(player);

        }

    }

    public static ArmorSet getArmorSet(Player player) {
        UUID uuid = player.getUniqueId();
        return equippedArmor.get(uuid);
    }

}
