package me.remag501.customarmorsets.Listeners;

import me.remag501.customarmorsets.ArmorSets.CustomArmorSetsCore;
import me.remag501.customarmorsets.lib.armorequipevent.ArmorEquipEvent;
import me.remag501.customarmorsets.lib.armorequipevent.ArmorType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class ArmorEquipListener implements Listener {

    public static boolean isFullArmorSet(ItemStack[] armor, String tag) {
        for (ItemStack piece : armor) {
            if (piece == null) return false;
            ItemMeta meta = piece.getItemMeta();
            NamespacedKey key = new NamespacedKey(Bukkit.getPluginManager().getPlugin("CustomArmorSets"), "armor_family");
            if (meta == null || !meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                return false;
            }

            String set = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
            if (!tag.equals(set)) return false;
        }
        return true;
    }

//    public boolean isWearingFullSet(Player player, String setId) {
//        PlayerInventory inv = player.getInventory();
//        for (ItemStack item : new ItemStack[] {
//                inv.getHelmet(), inv.getChestplate(), inv.getLeggings(), inv.getBoots()
//        }) {
//            if (item == null || !item.hasItemMeta()) return false;
//            NamespacedKey key = new NamespacedKey(Bukkit.getPluginManager().getPlugin("CustomArmorSets"), "armor_family");
//            if (!item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING)) return false;
//            if (!item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING).equals(setId)) return false;
//        }
//        return true;
//    }

//    @EventHandler
//    public void inventoryClickEvent(InventoryClickEvent event) {
//        Player player = (Player) event.getWhoClicked();
//        if (isWearingFullSet(player, "snowman"))
//            player.sendMessage("You equipped snowman");
//    }
//
//    @EventHandler
//    public void InventoryDragEvent(InventoryDragEvent event) {
//        Player player = (Player) event.getWhoClicked();
//        if (isWearingFullSet(player, "snowman"))
//            player.sendMessage("You equipped snowman");
//    }
//
//    @EventHandler
//    public void PlayerItemHeldEvent(PlayerItemHeldEvent event) {
//        Player player = event.getPlayer();
//        if (isWearingFullSet(player, "snowman"))
//            player.sendMessage("You equipped snowman");
//    }

    @EventHandler
    public void onEquip(ArmorEquipEvent event) {
        if (event.getMethod() == ArmorEquipEvent.EquipMethod.HOTBAR_SWAP) return;

        Player player = event.getPlayer();

        // Simulate OLD armor contents
        ItemStack[] oldArmor = player.getInventory().getArmorContents().clone();
        switch (event.getType()) {
            case HELMET -> oldArmor[3] = event.getOldArmorPiece();
            case CHESTPLATE -> oldArmor[2] = event.getOldArmorPiece();
            case LEGGINGS -> oldArmor[1] = event.getOldArmorPiece();
            case BOOTS -> oldArmor[0] = event.getOldArmorPiece();
        }

        // Simulate NEW armor contents
        ItemStack[] newArmor = player.getInventory().getArmorContents().clone();
        switch (event.getType()) {
            case HELMET -> newArmor[3] = event.getNewArmorPiece();
            case CHESTPLATE -> newArmor[2] = event.getNewArmorPiece();
            case LEGGINGS -> newArmor[1] = event.getNewArmorPiece();
            case BOOTS -> newArmor[0] = event.getNewArmorPiece();
        }

        boolean wasWearingSet = isFullArmorSet(oldArmor, "snowman");
        boolean nowWearingSet = isFullArmorSet(newArmor, "snowman");

        if (wasWearingSet && !nowWearingSet) {
            CustomArmorSetsCore.unequipArmor(player, "snowman");
        }

        // Optional: if you want to react to putting it on too
        if (!wasWearingSet && nowWearingSet) {
            CustomArmorSetsCore.equipArmor(player, "snowman");
        }
    }


}
