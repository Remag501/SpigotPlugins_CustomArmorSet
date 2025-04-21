package me.remag501.customarmorsets.Utils;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class HelmetCosmeticUtil {
    private static final NamespacedKey ORIGINAL_TYPE_KEY = new NamespacedKey(Bukkit.getPluginManager().getPlugin("CustomArmorSets"), "original_type");

    public static ItemStack makeCosmeticHelmet(ItemStack original, Material cosmeticMaterial) {
//        if (original == null || original.getType() == Material.AIR) return null;
//
//        ItemStack copy = original.clone();
//        ItemMeta meta = copy.getItemMeta();
//        if (meta == null) return null;
//
//        // Store original material
//        meta.getPersistentDataContainer().set(ORIGINAL_TYPE_KEY, PersistentDataType.STRING, original.getType().name());
//        copy.setItemMeta(meta);
//        copy.setType(cosmeticMaterial);
//        return copy;
        original.setType(cosmeticMaterial);
        return original;
    }

    public static ItemStack restoreOriginalHelmet(ItemStack cosmetic, Color color) {
//        if (cosmetic == null || cosmetic.getType() == Material.AIR) return null;
//
//        ItemMeta meta = cosmetic.getItemMeta();
//        if (meta == null) return null;
//
//        PersistentDataContainer container = meta.getPersistentDataContainer();
//        if (!container.has(ORIGINAL_TYPE_KEY, PersistentDataType.STRING)) return cosmetic;
//
//        String originalTypeName = container.get(ORIGINAL_TYPE_KEY, PersistentDataType.STRING);
//        Material originalMaterial = Material.getMaterial(originalTypeName);
//        if (originalMaterial == null) return cosmetic;
//
//        // Restore the type
//        cosmetic.setType(originalMaterial);
//        return cosmetic;

        cosmetic.setType(Material.LEATHER_HELMET);
        LeatherArmorMeta meta = (LeatherArmorMeta) cosmetic.getItemMeta();
        meta.setColor(color);
        cosmetic.setItemMeta(meta);
        return cosmetic;
    }
}

