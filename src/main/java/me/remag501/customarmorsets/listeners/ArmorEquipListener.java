package me.remag501.customarmorsets.listeners;

import me.remag501.customarmorsets.core.ArmorSetType;
import me.remag501.customarmorsets.core.CustomArmorSetsCore;
import me.remag501.customarmorsets.utils.HelmetCosmeticUtil;
import me.remag501.customarmorsets.utils.ArmorUtil;
import me.remag501.customarmorsets.utils.ItemUtil;
import me.remag501.customarmorsets.lib.armorequipevent.ArmorEquipEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

    @EventHandler
    public void onEquip(ArmorEquipEvent event) {

        // Handle logic for broken armor
        ItemStack newArmor = event.getNewArmorPiece();
        if (ItemUtil.isBroken(newArmor)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "This armor is broken and can't be equipped!");
            return;
        }

        // Other stuff
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
    }

}
