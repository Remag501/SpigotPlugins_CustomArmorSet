package me.remag501.customarmorsets.Utils;

import org.bukkit.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;

public class ArmorUtil {

    public static ItemStack createLeatherArmorPiece(JavaPlugin plugin, Material material, String displayName, List<String> lore, Color color, String armorSetId) {
        if (!material.name().startsWith("LEATHER_")) {
            throw new IllegalArgumentException("Material must be a leather armor piece!");
        }

        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        if (meta == null) return item;

        // Set display name and lore
        meta.setDisplayName(ChatColor.RESET + displayName);
        meta.setLore(lore);

        // Set dye color
        meta.setColor(color);

        // Tag with armor family ID
        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, "armor_set");
        container.set(key, PersistentDataType.STRING, armorSetId);

        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack[] createLeatherArmorSet(JavaPlugin plugin, String displayName, List<String> lore, Color color, String armorSetId) {
        return new ItemStack[]{
                createLeatherArmorPiece(plugin, Material.LEATHER_HELMET, displayName + " Helmet", lore, color, armorSetId),
                createLeatherArmorPiece(plugin, Material.LEATHER_CHESTPLATE, displayName + " Chestplate", lore, color, armorSetId),
                createLeatherArmorPiece(plugin, Material.LEATHER_LEGGINGS, displayName + " Leggings", lore, color, armorSetId),
                createLeatherArmorPiece(plugin, Material.LEATHER_BOOTS, displayName + " Boots", lore, color, armorSetId)
        };
    }
}
