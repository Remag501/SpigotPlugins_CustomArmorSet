package me.remag501.customarmorsets.service;

import me.remag501.bgscore.api.namespace.NamespaceService;
import me.remag501.customarmorsets.armor.ArmorSetType;
import me.remag501.customarmorsets.lib.armorequipevent.ArmorType;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class ArmorService {

    private final NamespaceService namespaceService;

    public ArmorService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    public ArmorSetType hasFullArmorSet(Player player, ItemStack armorPiece, ArmorType armorType) {
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

    public ArmorSetType isFullArmorSet(Player player) {
        ItemStack[] armor = player.getInventory().getArmorContents();

        if (armor.length != 4) return null;

        // First, get the id from the first armor piece (helmet)
        String id = getArmorSetId(armor[3]);
        if (id == null) return null;

        // Now check if ALL armor pieces match the same id
        for (ItemStack piece : armor) {
            if (!id.equals(getArmorSetId(piece))) {
                return null;
            }
        }

        // Now lookup the ArmorSetType from the id
        return ArmorSetType.fromId(id).orElse(null);
    }

    private String getArmorSetId(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        NamespacedKey key = namespaceService.getArmorSetKey();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(key, PersistentDataType.STRING) ? container.get(key, PersistentDataType.STRING) : null;
    }

    public boolean isCustomArmorPiece(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        NamespacedKey armorSetKey = namespaceService.getArmorSetKey();

        return container.has(armorSetKey, PersistentDataType.STRING);
    }

}
