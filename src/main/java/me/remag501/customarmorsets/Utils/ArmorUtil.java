package me.remag501.customarmorsets.Utils;

import me.remag501.customarmorsets.Core.ArmorSetType;
import me.remag501.customarmorsets.CustomArmorSets;
import me.remag501.customarmorsets.lib.armorequipevent.ArmorType;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

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

    public static ArmorSetType hasFullArmorSet(Player player, ItemStack armorPiece, ArmorType armorType) {
        // Get the ID from the armor piece in question (e.g., chestplate)
        String setId = getArmorSetId(armorPiece);
        if (setId == null) return null;

        // Simulate current armor contents
        ItemStack[] armor = player.getInventory().getArmorContents().clone();

        // Replace the slot with the passed `armorPiece` (simulate this equip)
        switch (armorType) {
            case HELMET -> armor[3] = armorPiece;
            case CHESTPLATE -> armor[2] = armorPiece;
            case LEGGINGS -> armor[1] = armorPiece;
            case BOOTS -> armor[0] = armorPiece;
        }

        // Check if all armor pieces exist and have matching set ID
        for (ItemStack piece : armor) {
            String otherId = getArmorSetId(piece);
            if (otherId == null || !otherId.equalsIgnoreCase(setId)) {
                return null;
            }
        }

        // Return matching ArmorSetType
        return ArmorSetType.fromId(setId).orElse(null);
    }

    private static String getArmorSetId(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        NamespacedKey key = new NamespacedKey(CustomArmorSets.getInstance(), "armor_set");
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(key, PersistentDataType.STRING) ? container.get(key, PersistentDataType.STRING) : null;
    }



}
