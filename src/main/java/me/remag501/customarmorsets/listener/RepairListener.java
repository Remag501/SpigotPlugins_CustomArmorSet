package me.remag501.customarmorsets.listener;

import me.remag501.bgscore.api.event.EventService;
import me.remag501.bgscore.api.namespace.NamespaceService;
import me.remag501.bgscore.api.util.BGSColor;
import me.remag501.customarmorsets.service.ArmorService;
import me.remag501.customarmorsets.service.CosmeticService;
import me.remag501.customarmorsets.service.RepairKitService;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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

public class RepairListener {

    private final ArmorService armorService;
    private final CosmeticService cosmeticService;
    private final NamespaceService namespaceService;

    public RepairListener(ArmorService armorService, CosmeticService cosmeticService, RepairKitService repairKitService, NamespaceService namespaceService, EventService eventService) {
        this.armorService = armorService;
        this.cosmeticService = cosmeticService;
        this.namespaceService = namespaceService;

        eventService.subscribe(InventoryClickEvent.class)
                .filter(e -> e.getWhoClicked() instanceof Player) // Must be a player
                .filter(e -> repairKitService.isRepairKit(e.getCursor())) // Must be a kit
                .filter(e -> e.getCurrentItem() != null && e.getCurrentItem().getType() != Material.AIR) // Must click something
                .handler(this::onInventoryClick);
    }

    public void onInventoryClick(InventoryClickEvent event) {
//        if (!(event.getWhoClicked() instanceof Player player)) return;
//
        ItemStack cursor = event.getCursor();
//        if (!itemService.isRepairKit(cursor)) return;

        ItemStack clickedItem = event.getCurrentItem();
//        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

//        Material type = clickedItem.getType();
//        if (type.getMaxDurability() <= 0 || !(clickedItem.getItemMeta() instanceof Damageable))
//            return;

        // Get tier and corresponding repair amount
        ItemMeta meta = cursor.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        int tier = container.getOrDefault(namespaceService.getRepairKitTierKey(), PersistentDataType.INTEGER, 0);

        int repairAmount = switch (tier) {
            case 0 -> 10; // Weak
            case 1 -> 25; // Normal
            case 2 -> 100; // Strong
            default -> 0;
        };

        Player player = (Player) event.getWhoClicked();

        if (tryRepair(player, clickedItem, repairAmount)) {
            event.setCancelled(true);
        }
    }

    private boolean tryRepair(Player player, ItemStack item, int repairAmount) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (armorService.isCustomArmorPiece(item)) {
            return customRepair(player, item, repairAmount);
        }

        if (!(item.getItemMeta() instanceof Damageable damageable)) return false;
        if (damageable.getDamage() <= 0) {
            player.sendMessage(BGSColor.NEGATIVE + "This piece is already at full durability!");
            return true;
        }

        // Logic to repair gear
        damageable.setDamage(Math.max(0, damageable.getDamage() - repairAmount)); // Heal 10 durability
        item.setItemMeta(damageable);
        player.sendMessage(BGSColor.POSITIVE + "Durability restored by " + repairAmount + "!");

        // Reduce repair kit by one
        ItemStack cursor = player.getItemOnCursor();
        cursor.setAmount(cursor.getAmount()-1);
        player.setItemOnCursor(cursor);

        return true;
    }

    private boolean customRepair(Player player, ItemStack item, int repairAmount) {
        if (!armorService.isCustomArmorPiece(item)) {
            player.sendMessage(BGSColor.NEGATIVE + "This is not a custom armor piece.");
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        NamespacedKey durabilityKey = namespaceService.getDurabilityKey();
        NamespacedKey maxDurabilityKey = namespaceService.getMaxDurabilityKey();

        int currentDurability = container.getOrDefault(durabilityKey, PersistentDataType.INTEGER, 100);
        int maxDurability = container.getOrDefault(maxDurabilityKey, PersistentDataType.INTEGER, 100);

        if (currentDurability >= maxDurability) {
            player.sendMessage(BGSColor.NEGATIVE + "This armor piece is already fully repaired.");
            return false;
        }

        int newDurability = Math.min(maxDurability, currentDurability + repairAmount);
        container.set(durabilityKey, PersistentDataType.INTEGER, newDurability);

        // Update lore
        String durabilityLine = ChatColor.GRAY + "Durability: " + ChatColor.WHITE + newDurability + " / " + maxDurability;

        if (item.getType() == Material.PLAYER_HEAD) {
            cosmeticService.updateCosmeticHelmetLoreSafely(item, Collections.singletonList(durabilityLine));
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

        player.sendMessage(BGSColor.POSITIVE + "Repaired armor durability by " + repairAmount + "!");
        return true;
    }


}

