package me.remag501.customarmorsets.utils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;

public class ItemUtil {

    private static final NamespacedKey REPAIR_KIT_KEY = new NamespacedKey("customarmorsets", "is_repair_kit");

   // int amount, int strength
    public static ItemStack createRepairKit() {
        ItemStack kit = new ItemStack(Material.SHULKER_SHELL);
        ItemMeta meta = kit.getItemMeta();

        meta.setDisplayName(ChatColor.GOLD + "Repair Kit");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Drag and drop on a damaged piece",
                ChatColor.GRAY + "to restore durability."
        ));

        // Tag it as a repair kit
        meta.getPersistentDataContainer().set(REPAIR_KIT_KEY, PersistentDataType.BYTE, (byte) 1);
        kit.setItemMeta(meta);

        return kit;
    }

    public static boolean isRepairKit(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.has(REPAIR_KIT_KEY, PersistentDataType.BYTE);
    }

    public static boolean isBroken(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();

        // Custom durability system
        if (ArmorUtil.isCustomArmorPiece(item)) {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            NamespacedKey durabilityKey = new NamespacedKey("customarmorsets", "internal_durability");

            int currentDurability = container.getOrDefault(durabilityKey, PersistentDataType.INTEGER, 100);
            return currentDurability <= 0;
        }

        // Vanilla durability (Damageable)
        if (meta instanceof Damageable damageable && item.getType().getMaxDurability() > 0) {
            int maxDurability = item.getType().getMaxDurability();
            int currentDamage = damageable.getDamage();
            return currentDamage >= maxDurability;
        }

        return false;
    }
}

