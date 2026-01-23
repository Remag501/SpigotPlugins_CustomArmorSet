package me.remag501.customarmorsets.manager;

import me.remag501.customarmorsets.CustomArmorSets;
import me.remag501.customarmorsets.armor.ArmorSet;
import me.remag501.customarmorsets.armor.ArmorSetType;
import me.remag501.customarmorsets.util.AttributesUtil;
import me.remag501.customarmorsets.util.HelmetCosmeticUtil;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ArmorManager {

    private Map<UUID, ArmorSet> equippedArmor = new HashMap<>();
    private Map<UUID, ArmorSetType> equippedHelmet = new HashMap<>();

//    private static List<String> allowedWorlds = List.of("kuroko", "icycaverns", "sahara", "Calino", "Musicland", "Thundra", "test");
      private static final List<String> bannedWorlds = List.of("spawn", "dungeonhub", "honeyclicker");
      private static final String bunkerPrefix = "bunker";

    public boolean equipArmor(Player player, ArmorSetType type) {
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
        String worldName = world.getName().toLowerCase();

        if (bannedWorlds.contains(worldName) || worldName.startsWith(bunkerPrefix)) {
            return false;
        }

        // Create armor set instance, map it to player, and activate passive
        ArmorSet set = type.create();
        equippedArmor.put(player.getUniqueId(), set);
        set.applyPassive(player);
        return true;
    }

    public void unequipArmor(Player player) {
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
                player.getInventory().setHelmet(HelmetCosmeticUtil.restoreOriginalHelmet(helmet, Color.fromRGB(type.getLeatherColor())));
            }
            equippedHelmet.remove(player.getUniqueId());
        }

        // Remove attributes from boots (if still there)
        ItemStack boots = player.getInventory().getBoots();
        if (boots != null) {
            AttributesUtil.removeAllArmorAttributes(boots);
        }

    }

    public ArmorSet getArmorSet(Player player) {
        UUID uuid = player.getUniqueId();
        return equippedArmor.get(uuid);
    }

}
