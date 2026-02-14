package me.remag501.customarmorsets.listener;

import me.remag501.customarmorsets.armor.ArmorSetType;
import me.remag501.customarmorsets.manager.ArmorManager;
import me.remag501.customarmorsets.service.ArmorService;
import me.remag501.customarmorsets.service.ItemService;
import me.remag501.customarmorsets.lib.armorequipevent.ArmorEquipEvent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class ArmorEquipListener implements Listener {

    private final ArmorManager armorManager;
    private final ArmorService armorService;
    private final ItemService itemService;


    public ArmorEquipListener(ArmorManager armorManager, ArmorService armorService, ItemService itemService) {
        this.armorManager = armorManager;
        this.armorService = armorService;
        this.itemService = itemService;
    }

    @EventHandler
    public void onEquip(ArmorEquipEvent event) {
        // Does not detect hot swapping armor

        // Handle logic for broken armor
        ItemStack newArmor = event.getNewArmorPiece();
        if (itemService.isBroken(newArmor)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "This armor is broken and can't be equipped!");
            return;
        }

        // Other stuff
        Player player = event.getPlayer();

        ArmorSetType wasWearing = armorService.hasFullArmorSet(player, event.getOldArmorPiece(), event.getType());
        ArmorSetType isWearing = armorService.hasFullArmorSet(player, event.getNewArmorPiece(), event.getType());

        if (wasWearing != null && isWearing == null) {
            armorManager.unequipArmor(player);
        }

        player.sendMessage(" " + (wasWearing == null) + " " + (isWearing == null));

        ArmorSetType type = (wasWearing != null) ? wasWearing : isWearing;

        if (wasWearing == null && isWearing != null) {
            armorManager.equipArmor(player, type);
        }
    }

}


