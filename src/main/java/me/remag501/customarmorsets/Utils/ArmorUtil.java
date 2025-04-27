package me.remag501.customarmorsets.Utils;

import me.remag501.customarmorsets.Core.ArmorSetType;
import me.remag501.customarmorsets.CustomArmorSets;
import me.remag501.customarmorsets.lib.armorequipevent.ArmorType;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;

public class ArmorUtil {

    public static ItemStack createLeatherArmorPiece(JavaPlugin plugin, Material material, String displayName, List<String> lore,
                                                    Color color, String armorSetId, int armorPoints, int durability, int armorToughness) {
        if (!material.name().startsWith("LEATHER_")) {
            throw new IllegalArgumentException("Material must be a leather armor piece!");
        }

        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        if (meta == null) return item;

        // Get piece type (Helmet, Chestplate, Leggings, Boots
        EquipmentSlot slot;
        switch (material.name()) {
            case "LEATHER_HELMET":
                slot = EquipmentSlot.HEAD;
                break;
            case "LEATHER_CHESTPLATE":
                slot = EquipmentSlot.CHEST;
                break;
            case "LEATHER_LEGGINGS":
                slot = EquipmentSlot.LEGS;
                break;
            case "LEATHER_BOOTS":
                slot = EquipmentSlot.FEET;
                break;
            default:
                slot = null;
        }

        // Clear existing meta (Dyed & +x Armor)
//        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(ItemFlag.HIDE_DYE);

        // Adjust set armor points, durability, and knockback
        AttributeModifier modifier = new AttributeModifier(UUID.randomUUID(), "generic.armor", armorPoints, AttributeModifier.Operation.ADD_NUMBER, slot);
        meta.addAttributeModifier(Attribute.GENERIC_ARMOR, modifier);
        modifier = new AttributeModifier(UUID.randomUUID(), "generic.armor.toughness", armorToughness, AttributeModifier.Operation.ADD_NUMBER, slot);
        meta.addAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS, modifier);

        // Currently only in armor passives, but might be added for armor piece stats
        // Add armor attributes (hardcoded temporarily)
//        AttributeModifier modifier = new AttributeModifier(UUID.randomUUID(), "generic.maxHealth", 100.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND);
//        meta.addAttributeModifier(Attribute.GENERIC_MAX_HEALTH, modifier);

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

        // Now adjust durability with datapack, add custom NBT (requires NMS or a helper lib like PersistentDataContainer)
        key = new NamespacedKey(plugin, "cDamageMax");
        ItemMeta armorMeta = item.getItemMeta();
        if (armorMeta != null) {
            armorMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, durability); // Simulating diamond durability
            item.setItemMeta(armorMeta);
        }

        return item;
    }

    public static ItemStack[] createLeatherArmorSet(JavaPlugin plugin, String displayName, List<String> lore, Color color,
                                                    String armorSetId, int[] armorPoints, int[] durability, int[] armorToughness) {
        return new ItemStack[]{
                createLeatherArmorPiece(plugin, Material.LEATHER_HELMET, displayName + " Helmet", lore, color, armorSetId, armorPoints[0], durability[0], armorToughness[0]),
                createLeatherArmorPiece(plugin, Material.LEATHER_CHESTPLATE, displayName + " Chestplate", lore, color, armorSetId, armorPoints[1], durability[1], armorToughness[1]),
                createLeatherArmorPiece(plugin, Material.LEATHER_LEGGINGS, displayName + " Leggings", lore, color, armorSetId, armorPoints[2], durability[2], armorToughness[2]),
                createLeatherArmorPiece(plugin, Material.LEATHER_BOOTS, displayName + " Boots", lore, color, armorSetId, armorPoints[3], durability[3], armorToughness[3])
        };
    }

    public static ArmorSetType hasFullArmorSet(Player player, ItemStack armorPiece, ArmorType armorType) {
        // Get the ID from the armor piece in question (e.g., chestplate)
        String setId = getArmorSetId(armorPiece);
        if (setId == null) return null;

        // Simulate current armor content
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

    public static ArmorSetType isFullArmorSet(Player player) {
        ItemStack[] armor = player.getInventory().getArmorContents();

        if (armor.length != 4) return null;

        // First, get the id from the first armor piece (helmet)
        String id = ArmorUtil.getArmorSetId(armor[3]);
        if (id == null) return null;

        // Now check if ALL armor pieces match the same id
        for (ItemStack piece : armor) {
            if (!id.equals(ArmorUtil.getArmorSetId(piece))) {
                return null;
            }
        }

        // Now lookup the ArmorSetType from the id
        return ArmorSetType.fromId(id).orElse(null);
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
