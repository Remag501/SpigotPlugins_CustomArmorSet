package me.remag501.armor.util;

import me.remag501.bgscore.api.namespace.NamespaceService;
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

import java.util.ArrayList;
import java.util.List;

public class ArmorUtil {

    public static ItemStack createLeatherArmorPiece(NamespaceService namespaceService, Material material, String displayName, List<String> lore,
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
//        AttributeModifier modifier = new AttributeModifier(UUID.randomUUID(), "generic.armor", armorPoints, AttributeModifier.Operation.ADD_NUMBER, slot);
//        meta.addAttributeModifier(Attribute.ARMOR, modifier);
//        modifier = new AttributeModifier(UUID.randomUUID(), "generic.armor.toughness", armorToughness, AttributeModifier.Operation.ADD_NUMBER, slot);
//        meta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, modifier);
////        meta.setUnbreakable(true); // Make default unbreakable so we use internal durability
        // 1. Define the unique keys for these modifiers (best practice)
        NamespacedKey armorKey = namespaceService.key("armor_modifier");
        NamespacedKey toughnessKey = namespaceService.key("toughness_modifier");

        // 2. Create the modifiers using the new API
        // AttributeModifier(Key, Amount, Operation, SlotGroup)
        AttributeModifier armorMod = new AttributeModifier(
                armorKey,
                (double) armorPoints,
                AttributeModifier.Operation.ADD_NUMBER,
                slot.getGroup() // Converts EquipmentSlot to EquipmentSlotGroup (HEAD, CHEST, etc.)
        );

        AttributeModifier toughnessMod = new AttributeModifier(
                toughnessKey,
                (double) armorToughness,
                AttributeModifier.Operation.ADD_NUMBER,
                slot.getGroup()
        );

        // 3. Apply them to the meta
        meta.addAttributeModifier(Attribute.ARMOR, armorMod);
        meta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, toughnessMod);



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
        NamespacedKey durabilityKey = namespaceService.getDurabilityKey();
        NamespacedKey maxDurabilityKey = namespaceService.getMaxDurabilityKey();
        NamespacedKey key = namespaceService.getArmorSetKey();
        container.set(key, PersistentDataType.STRING, armorSetId);
        container.set(durabilityKey, PersistentDataType.INTEGER, durability);
        container.set(maxDurabilityKey, PersistentDataType.INTEGER, durability);
        item.setItemMeta(meta);

        // Now adjust durability with datapack, add custom NBT (requires NMS or a helper lib like PersistentDataContainer)
//        key = new NamespacedKey(plugin, "cDamageMax");
        key = namespaceService.key("cDamageMax");
        ItemMeta armorMeta = item.getItemMeta();
        if (armorMeta != null) {
            armorMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, durability); // Simulating diamond durability
            item.setItemMeta(armorMeta);
        }

        return item;
    }

    public static ItemStack[] createLeatherArmorSet(NamespaceService namespaceService, String displayName, List<String> lore, int color, int rarity, int customModelData,
                                                    String armorSetId, int[] armorPoints, int[] durability, int[] armorToughness) {
        return new ItemStack[]{
                createLeatherArmorPiece(namespaceService, Material.LEATHER_HELMET, displayName + " Helmet", lore, color, rarity, customModelData, armorSetId, armorPoints[0], durability[0], armorToughness[0]),
                createLeatherArmorPiece(namespaceService, Material.LEATHER_CHESTPLATE, displayName + " Chestplate", lore, color, rarity, customModelData, armorSetId, armorPoints[1], durability[1], armorToughness[1]),
                createLeatherArmorPiece(namespaceService, Material.LEATHER_LEGGINGS, displayName + " Leggings", lore, color, rarity, customModelData, armorSetId, armorPoints[2], durability[2], armorToughness[2]),
                createLeatherArmorPiece(namespaceService, Material.LEATHER_BOOTS, displayName + " Boots", lore, color, rarity, customModelData, armorSetId, armorPoints[3], durability[3], armorToughness[3])
        };
    }


}
