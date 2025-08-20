package me.remag501.customarmorsets.listeners;

import me.remag501.customarmorsets.core.ArmorSetType;
import me.remag501.customarmorsets.core.CustomArmorSetsCore;
import me.remag501.customarmorsets.utils.HelmetCosmeticUtil;
import me.remag501.customarmorsets.utils.ArmorUtil;
import me.remag501.customarmorsets.utils.ItemUtil;
import me.remag501.customarmorsets.lib.armorequipevent.ArmorEquipEvent;
import org.bukkit.*;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class ArmorEquipListener implements Listener {

    @EventHandler
    public void onEquip(ArmorEquipEvent event) {

        // Does not detect hot swapping armor

        // Handle logic for broken armor
        ItemStack newArmor = event.getNewArmorPiece();
        if (ItemUtil.isBroken(newArmor)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "This armor is broken and can't be equipped!");
            return;
        }

        // Other stuff
        Player player = event.getPlayer();

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

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.hasItem()) {
            return;
        }
        ItemStack heldItem = event.getItem();
        if (heldItem == null)
            return;
        if (isArmor(heldItem.getType()) && CustomArmorSetsCore.getArmorSet(event.getPlayer()) != null) {
//            event.getPlayer().sendMessage(ChatColor.RED + "You can't hotswap armor!");
            event.setCancelled(true);
        }
    }

    private boolean isArmor(Material material) {
        String name = material.name();
        return name.endsWith("_HELMET") ||
                name.endsWith("_CHESTPLATE") ||
                name.endsWith("_LEGGINGS") ||
                name.endsWith("_BOOTS");
    }
}


