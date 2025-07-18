package me.remag501.customarmorsets.listeners;

import me.remag501.customarmorsets.core.ArmorSet;
import me.remag501.customarmorsets.core.ArmorSetType;
import me.remag501.customarmorsets.core.CustomArmorSetsCore;
import me.remag501.customarmorsets.utils.ArmorUtil;
import me.remag501.customarmorsets.utils.HelmetCosmeticUtil;
import me.remag501.customarmorsets.lib.armorequipevent.ArmorEquipEvent;
import me.remag501.customarmorsets.lib.armorequipevent.ArmorType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static me.remag501.customarmorsets.utils.HelmetCosmeticUtil.sendCosmeticHelmet;

public class DurabilityListener implements Listener {
    @EventHandler
    public void onPlayerDamage(PlayerItemDamageEvent event) {
        Player player = event.getPlayer();
        ItemStack damagedItem = event.getItem();

        if (!damagedItem.hasItemMeta()) return;
        if (!ArmorUtil.isCustomArmorPiece(damagedItem)) {
            // Check if regular piece has a durability of 0 and unequip it
            Damageable meta = (Damageable) event.getItem().getItemMeta();
            int maxDurability = event.getItem().getType().getMaxDurability();
            int damageTaken = meta.getDamage();
            int remainingDurability = maxDurability - damageTaken;
            // Check remaining durability is 1, break, cancel event and set it to 0
            if (remainingDurability <= 1) {
                meta.setDamage(maxDurability);
                event.getItem().setItemMeta(meta);
                unequipAndBreakArmorPiece(event.getPlayer(), event.getItem());
                event.setCancelled(true);
            }
            return;
        }

        ItemMeta meta = damagedItem.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        NamespacedKey durabilityKey = new NamespacedKey("customarmorsets", "internal_durability");
        NamespacedKey maxDurabilityKey = new NamespacedKey("customarmorsets", "internal_max_durability");

        int currentDurability = container.getOrDefault(durabilityKey, PersistentDataType.INTEGER, 100);
        int maxDurability = container.getOrDefault(maxDurabilityKey, PersistentDataType.INTEGER, 100);

        int unbreakingLevel = damagedItem.getEnchantmentLevel(Enchantment.DURABILITY);
        boolean shouldTakeDamage = shouldTakeDurabilityLoss(unbreakingLevel);

        int newDurability = shouldTakeDamage ? Math.max(0, currentDurability - 1) : currentDurability;

        container.set(durabilityKey, PersistentDataType.INTEGER, newDurability);

        // Prepare lore update
        String durabilityLine = ChatColor.GRAY + "Durability: " + ChatColor.WHITE + newDurability + " / " + maxDurability;
        List<String> appendedLore = Collections.singletonList(durabilityLine);

        event.setCancelled(true);

        damagedItem.setItemMeta(meta); // Save internal durability

        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.removeIf(line -> ChatColor.stripColor(line).contains("Durability"));
        lore.addAll(appendedLore);
        meta.setLore(lore);
        damagedItem.setItemMeta(meta);

        // Break item if needed
        if (newDurability == 0)
            unequipAndBreakArmorPiece(player, damagedItem);

        // REMOVE? Update the player helmet packet
//        if (event.getItem().getType().name().endsWith("_HELMET")) {
//            HelmetCosmeticUtil.updateCosmeticHelmetLoreSafely(player);
//        }

        // Mirror head durability update manually only when leggings are damaged
//        if (damagedItem.getType().name().endsWith("_LEGGINGS") && ArmorUtil.isFullArmorSet(player) != null) {
//            ItemStack helmet = player.getInventory().getHelmet();
//
//            if (helmet != null && helmet.getType() == Material.PLAYER_HEAD && helmet.hasItemMeta()) {
//                ItemMeta headMeta = helmet.getItemMeta();
//                PersistentDataContainer headContainer = headMeta.getPersistentDataContainer();
//
//                int headDurability = headContainer.getOrDefault(durabilityKey, PersistentDataType.INTEGER, 100);
//                int headMaxDurability = headContainer.getOrDefault(maxDurabilityKey, PersistentDataType.INTEGER, 100);
//
//                unbreakingLevel = helmet.getEnchantmentLevel(Enchantment.DURABILITY);
//                shouldTakeDamage = shouldTakeDurabilityLoss(unbreakingLevel);
//
//                int newHeadDurability = shouldTakeDamage ? Math.max(0, headDurability - 1) : headDurability;
//
//                headContainer.set(durabilityKey, PersistentDataType.INTEGER, newHeadDurability);
//
//                String headDurabilityLine = ChatColor.GRAY + "Durability: " + ChatColor.WHITE + newHeadDurability + " / " + headMaxDurability;
//                helmet.setItemMeta(headMeta); // Save updated container first
//                HelmetCosmeticUtil.updateCosmeticHelmetLoreSafely(helmet, Collections.singletonList(headDurabilityLine));
//
//                // Break head if needed
//                if (newHeadDurability == 0)
//                    unequipAndBreakArmorPiece(player, helmet);
//                player.sendMessage("reached");
//            }
//        }
    }

    private boolean shouldTakeDurabilityLoss(int unbreakingLevel) {
        if (unbreakingLevel <= 0) return true;
        double chance = 1.0 / (unbreakingLevel + 1);
        return Math.random() < chance;
    }

    // Unequips and breaks the armor piece if it is equipped
    private void unequipAndBreakArmorPiece(Player player, ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) return;

        PlayerInventory inventory = player.getInventory();
        Material type = itemStack.getType();
        ItemStack storage = null;
        ArmorType armorType = null;

        if (type.name().endsWith("_HELMET") || type == Material.PLAYER_HEAD) {
            storage = inventory.getHelmet();
            // Handle cosmetic case
            ArmorSetType armorSetType = ArmorUtil.isFullArmorSet(player);
            if (armorSetType != null) // Revert cosmetic
                HelmetCosmeticUtil.restoreOriginalHelmet(storage, armorSetType.getLeatherColor());
            // Handle normally
            inventory.setHelmet(null);
            armorType = ArmorType.HELMET;
        } else if (type.name().endsWith("_CHESTPLATE")) {
            storage = inventory.getChestplate();
            inventory.setChestplate(null);
            armorType = ArmorType.CHESTPLATE;
        } else if (type.name().endsWith("_LEGGINGS")) {
            storage = inventory.getLeggings();
            inventory.setLeggings(null);
            armorType = ArmorType.LEGGINGS;
        } else if (type.name().endsWith("_BOOTS")) {
            storage = inventory.getBoots();
            inventory.setBoots(null);
            armorType = ArmorType.BOOTS;
        }

        if (storage != null) {
            ArmorEquipEvent armorEquipEvent = new ArmorEquipEvent(player, ArmorEquipEvent.EquipMethod.BROKE, armorType, storage, null);
            Bukkit.getServer().getPluginManager().callEvent(armorEquipEvent);
            inventory.addItem(storage);
        }


        if (itemStack.getItemMeta().hasDisplayName())
            player.sendMessage(ChatColor.RED + "Your " + itemStack.getItemMeta().getDisplayName() + " broke!");
        else
            player.sendMessage(ChatColor.RED + "Your " + formatMaterialName(type) + " broke!");
    }

    private String formatMaterialName(Material material) {
        return material.name().toLowerCase().replace("_", " ");
    }

    @EventHandler
    public void onPlayerAnimation(PlayerAnimationEvent event) {
//        Player player = event.getPlayer();
////        String url = cachedCosmeticHeads.get(player.getUniqueId());
//        ArmorSet set = CustomArmorSetsCore.getArmorSet(player);
//        if (set == null)
//            return;
//        String url = set.getType().getHeadUrl();
//        if (url != null) {
//            for (Player viewer : Bukkit.getOnlinePlayers()) {
//                sendCosmeticHelmet(player, viewer, url);
//            }
//        }
    }

}
