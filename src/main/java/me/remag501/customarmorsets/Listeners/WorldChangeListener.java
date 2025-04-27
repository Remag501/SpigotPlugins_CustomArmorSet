package me.remag501.customarmorsets.Listeners;

import me.remag501.customarmorsets.Core.ArmorSetType;
import me.remag501.customarmorsets.Core.CustomArmorSetsCore;
import me.remag501.customarmorsets.Utils.ArmorUtil;
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
            if (!CustomArmorSetsCore.equipArmor(player, currentSet)) // Player is in wrong world
                CustomArmorSetsCore.unequipArmor(player);
        }
    }
}
