package me.remag501.customarmorsets.service;

import me.remag501.bgscore.api.namespace.NamespaceService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;

public class RepairKitService {

    private final NamespaceService namespaceService;
    private final ArmorService armorService;

    public RepairKitService(NamespaceService namespaceService, ArmorService armorService) {
        this.namespaceService = namespaceService;
        this.armorService = armorService;
    }

   // int amount, int strength
   public ItemStack createRepairKit(int amount, int tier) {
       ItemStack kit = new ItemStack(Material.SHULKER_SHELL, amount);
       ItemMeta meta = kit.getItemMeta();

       String name;
       switch (tier) {
           case 0 -> name = ChatColor.YELLOW + "Weak Repair Kit";
           case 2 -> name = ChatColor.RED + "Strong Repair Kit";
           default -> name = ChatColor.GOLD + "Repair Kit"; // Tier 1 or fallback
       }

       meta.setDisplayName(name);
       meta.setLore(Arrays.asList(
               ChatColor.GRAY + "Drag and drop on a damaged piece",
               ChatColor.GRAY + "to restore durability.",
               ChatColor.DARK_GRAY + "Tier: " + tier
       ));

       PersistentDataContainer container = meta.getPersistentDataContainer();
       container.set(namespaceService.getRepairKitKey(), PersistentDataType.BYTE, (byte) 1); // identifies as repair kit
       container.set(namespaceService.getRepairKitTierKey(), PersistentDataType.INTEGER, tier); // stores tier

       kit.setItemMeta(meta);
       return kit;
   }

    public boolean isRepairKit(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.has(namespaceService.getRarityKey(), PersistentDataType.BYTE);
    }

    public boolean isBroken(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();

        // Custom durability system
        if (armorService.isCustomArmorPiece(item)) {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            NamespacedKey durabilityKey = namespaceService.getDurabilityKey();

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

