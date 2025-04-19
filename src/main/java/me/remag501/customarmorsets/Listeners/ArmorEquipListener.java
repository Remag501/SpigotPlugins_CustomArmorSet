package me.remag501.customarmorsets.Listeners;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;

public class ArmorEquipListener implements Listener {

    public boolean isWearingFullSet(Player player, String setId) {
        PlayerInventory inv = player.getInventory();
        for (ItemStack item : new ItemStack[] {
                inv.getHelmet(), inv.getChestplate(), inv.getLeggings(), inv.getBoots()
        }) {
            if (item == null || !item.hasItemMeta()) return false;
            NamespacedKey key = new NamespacedKey(Bukkit.getPluginManager().getPlugin("CustomArmorSets"), "armor_family");
            if (!item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING)) return false;
            if (!item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING).equals(setId)) return false;
        }
        return true;
    }

    @EventHandler
    public void inventoryClickEvent(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (isWearingFullSet(player, "snowman"))
            player.sendMessage("You equipped snowman");
    }

    @EventHandler
    public void InventoryDragEvent(InventoryDragEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (isWearingFullSet(player, "snowman"))
            player.sendMessage("You equipped snowman");
    }

    @EventHandler
    public void PlayerItemHeldEvent(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (isWearingFullSet(player, "snowman"))
            player.sendMessage("You equipped snowman");
    }

}
