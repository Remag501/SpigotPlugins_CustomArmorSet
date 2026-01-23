package me.remag501.customarmorsets.util;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ArmorUtil {

    public static ItemStack createLeatherArmorPiece(JavaPlugin plugin, Material material, String displayName, List<String> lore,
                                                    int color, int rarity, int customModelData, String armorSetId, int armorPoints, int durability, int armorToughness) {
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

        // Adjust set armor points, durability, and toughness
        AttributeModifier modifier = new AttributeModifier(UUID.randomUUID(), "generic.armor", armorPoints, AttributeModifier.Operation.ADD_NUMBER, slot);
        meta.addAttributeModifier(Attribute.GENERIC_ARMOR, modifier);
        modifier = new AttributeModifier(UUID.randomUUID(), "generic.armor.toughness", armorToughness, AttributeModifier.Operation.ADD_NUMBER, slot);
        meta.addAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS, modifier);
//        meta.setUnbreakable(true); // Make default unbreakable so we use internal durability
        lore = new ArrayList<>(lore); // Make lore mutable
        lore.add("§7Durability: " + durability + "/" + durability);

        // Get rarity and add to display name
        String rarityStr = "";
        switch (rarity) {
            case 0:
                rarityStr = "§c✪";
                break;
            case 1:
                rarityStr = "§c✪✪";
                break;
            case 2:
                rarityStr = "§e✪✪✪";
                break;
            case 3:
                rarityStr = "§a✪✪✪✪";
                break;
            case 4:
                rarityStr = "§d✪✪✪✪✪";
                break;
            case 5:
                rarityStr = "§d✪✪✪✪✪§a✪";
                break;

            default:
                break;
        }

        // Set display name and lore
        meta.setDisplayName(ChatColor.RESET + displayName + " " + rarityStr);
        meta.setLore(lore);

        // Set dye color and cmd
        meta.setColor(Color.fromRGB(color));
        meta.setCustomModelData(customModelData);

        // Tag with armor family ID
        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey durabilityKey = new NamespacedKey(plugin, "internal_durability");
        NamespacedKey maxDurabilityKey = new NamespacedKey(plugin, "internal_max_durability");
        NamespacedKey key = new NamespacedKey(plugin, "armor_set");
        container.set(key, PersistentDataType.STRING, armorSetId);
        container.set(durabilityKey, PersistentDataType.INTEGER, durability);
        container.set(maxDurabilityKey, PersistentDataType.INTEGER, durability);

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

    public static ItemStack[] createLeatherArmorSet(JavaPlugin plugin, String displayName, List<String> lore, int color, int rarity, int customModelData,
                                                    String armorSetId, int[] armorPoints, int[] durability, int[] armorToughness) {
        return new ItemStack[]{
                createLeatherArmorPiece(plugin, Material.LEATHER_HELMET, displayName + " Helmet", lore, color, rarity, customModelData, armorSetId, armorPoints[0], durability[0], armorToughness[0]),
                createLeatherArmorPiece(plugin, Material.LEATHER_CHESTPLATE, displayName + " Chestplate", lore, color, rarity, customModelData, armorSetId, armorPoints[1], durability[1], armorToughness[1]),
                createLeatherArmorPiece(plugin, Material.LEATHER_LEGGINGS, displayName + " Leggings", lore, color, rarity, customModelData, armorSetId, armorPoints[2], durability[2], armorToughness[2]),
                createLeatherArmorPiece(plugin, Material.LEATHER_BOOTS, displayName + " Boots", lore, color, rarity, customModelData, armorSetId, armorPoints[3], durability[3], armorToughness[3])
        };
    }


}
