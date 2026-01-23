package me.remag501.customarmorsets.service;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;

public class ItemService {

    private final NamespacedKey REPAIR_KIT_KEY;
    private final ArmorService armorService;

//    private final Plugin plugin;

    public ItemService(Plugin plugin, ArmorService armorService) {
//        REPAIR_KIT_KEY = new NamespacedKey("customarmorsets", "is_repair_kit");
        REPAIR_KIT_KEY = new NamespacedKey(plugin, "is_repair_kit");
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
       container.set(REPAIR_KIT_KEY, PersistentDataType.BYTE, (byte) 1); // identifies as repair kit
       container.set(new NamespacedKey("customarmorsets", "repair_kit_tier"), PersistentDataType.INTEGER, tier); // stores tier

       kit.setItemMeta(meta);
       return kit;
   }

    public boolean isRepairKit(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.has(REPAIR_KIT_KEY, PersistentDataType.BYTE);
    }

    public boolean isBroken(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();

        // Custom durability system
        if (armorService.isCustomArmorPiece(item)) {
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

