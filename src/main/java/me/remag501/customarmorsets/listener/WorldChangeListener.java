package me.remag501.customarmorsets.listener;

import me.remag501.customarmorsets.armor.ArmorSetType;
import me.remag501.customarmorsets.manager.ArmorManager;
import me.remag501.customarmorsets.service.ArmorService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public class WorldChangeListener implements Listener {

    private final ArmorManager armorManager;
    private final ArmorService armorService;

    public WorldChangeListener(ArmorManager armorManager, ArmorService armorService) {
        this.armorManager = armorManager;
        this.armorService = armorService;
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();

        // Then immediately re-equip in the new world
        ArmorSetType currentSet = armorService.isFullArmorSet(player);
        if (currentSet != null) {
            if (!armorManager.equipArmor(player, currentSet)) // Player is in wrong world
                armorManager.unequipArmor(player);
        }
    }
}
