package me.remag501.customarmorsets.Utils;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class ItemUtil {

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

