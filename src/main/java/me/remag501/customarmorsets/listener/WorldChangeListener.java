package me.remag501.customarmorsets.listener;

import me.remag501.customarmorsets.armor.ArmorSetType;
import me.remag501.customarmorsets.manager.ArmorManager;
import me.remag501.customarmorsets.util.ArmorUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public class WorldChangeListener implements Listener {

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();

        // Then immediately re-equip in the new world
        ArmorSetType currentSet = ArmorUtil.isFullArmorSet(player);
        if (currentSet != null) {
            if (!ArmorManager.equipArmor(player, currentSet)) // Player is in wrong world
                ArmorManager.unequipArmor(player);
        }
    }
}
