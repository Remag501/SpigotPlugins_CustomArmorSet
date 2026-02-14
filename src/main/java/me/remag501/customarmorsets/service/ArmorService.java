package me.remag501.customarmorsets.service;

import me.remag501.bgscore.api.namespace.NamespaceService;
import me.remag501.customarmorsets.armor.ArmorSetType;
import me.remag501.customarmorsets.lib.armorequipevent.ArmorType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

public class ArmorService {

    private final NamespaceService namespaceService;

    public ArmorService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    /**
     * Checks if equipping a specific piece completes a set.
     */
    public ArmorSetType hasFullArmorSet(Player player, ItemStack armorPiece, ArmorType armorType) {
        String setId = getArmorSetId(armorPiece);
        if (setId == null) return null;

        ItemStack[] armor = player.getInventory().getArmorContents();

        // Simulate the change without modifying the actual inventory
        // Array Order: 0:Boots, 1:Legs, 2:Chest, 3:Helmet
        switch (armorType) {
            case HELMET     -> armor[3] = armorPiece;
            case CHESTPLATE -> armor[2] = armorPiece;
            case LEGGINGS   -> armor[1] = armorPiece;
            case BOOTS      -> armor[0] = armorPiece;
        }

        for (ItemStack piece : armor) {
            String otherId = getArmorSetId(piece);
            // Must have all 4 pieces of the SAME set ID
            if (otherId == null || !otherId.equalsIgnoreCase(setId)) {
                return null;
            }
        }

        return ArmorSetType.fromId(setId).orElse(null);
    }

    /**
     * Checks what set the player is currently wearing.
     */
    public ArmorSetType isFullArmorSet(Player player) {
        ItemStack[] armor = player.getInventory().getArmorContents();

        // Find the first non-null piece to get a baseline ID
        String targetId = null;
        for (ItemStack piece : armor) {
            targetId = getArmorSetId(piece);
            if (targetId != null) break;
        }

        if (targetId == null) return null;

        // Verify all slots match that ID
        for (ItemStack piece : armor) {
            if (!targetId.equalsIgnoreCase(getArmorSetId(piece))) {
                return null;
            }
        }

        return ArmorSetType.fromId(targetId).orElse(null);
    }

    public String getArmorSetId(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;

        // This returns PersistentDataContainerView, which is perfect for reading
        var container = item.getPersistentDataContainer();
        NamespacedKey key = namespaceService.getArmorSetKey();

        // .get() works exactly the same on a View
        return container.get(key, PersistentDataType.STRING);
    }

    public boolean isCustomArmorPiece(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        return item.getPersistentDataContainer().has(namespaceService.getArmorSetKey(), PersistentDataType.STRING);
    }
}