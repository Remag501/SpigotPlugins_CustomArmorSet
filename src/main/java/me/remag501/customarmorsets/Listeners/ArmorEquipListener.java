package me.remag501.customarmorsets.Listeners;

import me.remag501.customarmorsets.Core.ArmorSetType;
import me.remag501.customarmorsets.Core.CustomArmorSetsCore;
import me.remag501.customarmorsets.Utils.HelmetCosmeticUtil;
import me.remag501.customarmorsets.Utils.ArmorUtil;
import me.remag501.customarmorsets.lib.armorequipevent.ArmorEquipEvent;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class ArmorEquipListener implements Listener {

    public static boolean isFullArmorSet(ItemStack[] armor, String tag) {
        for (ItemStack piece : armor) {
            if (piece == null) return false;
            ItemMeta meta = piece.getItemMeta();
            NamespacedKey key = new NamespacedKey(Bukkit.getPluginManager().getPlugin("CustomArmorSets"), "armor_set");
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
        Player player = event.getPlayer();

//        for (ArmorSetType type : ArmorSetType.values()) {
            ArmorSetType wasWearing = ArmorUtil.hasFullArmorSet(player, event.getOldArmorPiece(), event.getType());
            ArmorSetType isWearing = ArmorUtil.hasFullArmorSet(player, event.getNewArmorPiece(), event.getType());

            if (wasWearing != null && isWearing == null) {
                CustomArmorSetsCore.unequipArmor(player);
            }

            ArmorSetType type = (wasWearing != null) ? wasWearing : isWearing;

            if (wasWearing == null && isWearing != null) {
                CustomArmorSetsCore.equipArmor(player, type);
            }
//        }
    }



    @EventHandler
    public void helmetBlockRemoval(InventoryClickEvent event) {

        HumanEntity player = event.getWhoClicked();

        if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
            if (player.getInventory().getHelmet() == null)
                return; // Player does not have helmet equipped
            if (!player.getInventory().getHelmet().getType().name().startsWith("LEATHER_")) {
                player.getInventory().setHelmet(HelmetCosmeticUtil.restoreOriginalHelmet(player.getInventory().getHelmet(), Color.WHITE));
            }
        }
    }

}
