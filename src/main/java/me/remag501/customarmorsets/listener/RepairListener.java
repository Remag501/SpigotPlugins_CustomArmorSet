package me.remag501.customarmorsets.listener;

import me.remag501.customarmorsets.util.ArmorUtil;
import me.remag501.customarmorsets.util.HelmetCosmeticUtil;
import me.remag501.customarmorsets.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RepairListener implements Listener {

    // Clicking in inventory while holding stick
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack cursor = event.getCursor();
        if (!ItemUtil.isRepairKit(cursor)) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        Material type = clickedItem.getType();
        if (type.getMaxDurability() <= 0 || !(clickedItem.getItemMeta() instanceof Damageable))
            return;

        // Get tier and corresponding repair amount
        ItemMeta meta = cursor.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        int tier = container.getOrDefault(new NamespacedKey("customarmorsets", "repair_kit_tier"), PersistentDataType.INTEGER, 0);

        int repairAmount = switch (tier) {
            case 0 -> 10; // Weak
            case 1 -> 25; // Normal
            case 2 -> 100; // Strong
            default -> 0;
        };

        if (tryRepair(player, clickedItem, repairAmount)) {
            event.setCancelled(true);
        }
    }

    private boolean tryRepair(Player player, ItemStack item, int repairAmount) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (ArmorUtil.isCustomArmorPiece(item)) {
            return customRepair(player, item, repairAmount);
        }

        if (!(item.getItemMeta() instanceof Damageable damageable)) return false;
        if (damageable.getDamage() <= 0) {
            player.sendMessage(ChatColor.GRAY + "This piece is already at full durability!");
            return true;
        }

        // Logic to repair gear
        damageable.setDamage(Math.max(0, damageable.getDamage() - repairAmount)); // Heal 10 durability
        item.setItemMeta(damageable);
        player.sendMessage(ChatColor.GREEN + "Durability restored by " + repairAmount + "!");

        // Reduce repair kit by one
        ItemStack cursor = player.getItemOnCursor();
        cursor.setAmount(cursor.getAmount()-1);
        player.setItemOnCursor(cursor);

        return true;
    }

    private boolean customRepair(Player player, ItemStack item, int repairAmount) {
        if (!ArmorUtil.isCustomArmorPiece(item)) {
            player.sendMessage(ChatColor.RED + "This is not a custom armor piece.");
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        NamespacedKey durabilityKey = new NamespacedKey("customarmorsets", "internal_durability");
        NamespacedKey maxDurabilityKey = new NamespacedKey("customarmorsets", "internal_max_durability");

        int currentDurability = container.getOrDefault(durabilityKey, PersistentDataType.INTEGER, 100);
        int maxDurability = container.getOrDefault(maxDurabilityKey, PersistentDataType.INTEGER, 100);

        if (currentDurability >= maxDurability) {
            player.sendMessage(ChatColor.YELLOW + "This armor piece is already fully repaired.");
            return false;
        }

        int newDurability = Math.min(maxDurability, currentDurability + repairAmount);
        container.set(durabilityKey, PersistentDataType.INTEGER, newDurability);

        // Update lore
        String durabilityLine = ChatColor.GRAY + "Durability: " + ChatColor.WHITE + newDurability + " / " + maxDurability;

        if (item.getType() == Material.PLAYER_HEAD) {
            HelmetCosmeticUtil.updateCosmeticHelmetLoreSafely(item, Collections.singletonList(durabilityLine));
        } else {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.removeIf(line -> ChatColor.stripColor(line).contains("Durability"));
            lore.add(durabilityLine);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        item.setItemMeta(meta); // Save updated durability

        // Consume 1 repair kit from cursor
        ItemStack cursor = player.getItemOnCursor();
        if (cursor != null && cursor.getAmount() > 1) {
            cursor.setAmount(cursor.getAmount() - 1);
            player.setItemOnCursor(cursor);
        } else {
            player.setItemOnCursor(null);
        }

        player.sendMessage(ChatColor.GREEN + "Repaired armor durability by " + repairAmount + "!");
        return true;
    }


}

