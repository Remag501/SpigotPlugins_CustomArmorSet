package me.remag501.armor.listener;

import me.remag501.bgscore.api.util.BGSColor;
import me.remag501.armor.armor.ArmorSetType;
import me.remag501.armor.manager.ArmorManager;
import me.remag501.armor.service.ArmorService;
import me.remag501.armor.service.RepairKitService;
import me.remag501.armor.lib.armorequipevent.ArmorEquipEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class ArmorEquipListener implements Listener {

    private final ArmorManager armorManager;
    private final ArmorService armorService;
    private final RepairKitService repairKitService;


    public ArmorEquipListener(ArmorManager armorManager, ArmorService armorService, RepairKitService repairKitService) {
        this.armorManager = armorManager;
        this.armorService = armorService;
        this.repairKitService = repairKitService;
    }

    @EventHandler
    public void onEquip(ArmorEquipEvent event) {
        // Does not detect hot swapping armor

        // Handle logic for broken armor
        ItemStack newArmor = event.getNewArmorPiece();
        if (repairKitService.isBroken(newArmor)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(BGSColor.NEGATIVE + "This armor is broken and can't be equipped!");
            return;
        }

        // Other stuff
        Player player = event.getPlayer();

        ArmorSetType wasWearing = armorService.hasFullArmorSet(player, event.getOldArmorPiece(), event.getType());
        ArmorSetType isWearing = armorService.hasFullArmorSet(player, event.getNewArmorPiece(), event.getType());

        if (wasWearing != null && isWearing == null) {
            armorManager.unequipArmor(player);
        }

        ArmorSetType type = (wasWearing != null) ? wasWearing : isWearing;

        if (wasWearing == null && isWearing != null) {
            armorManager.equipArmor(player, type);
        }
    }

}


