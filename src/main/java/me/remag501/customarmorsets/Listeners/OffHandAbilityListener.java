package me.remag501.customarmorsets.Listeners;


import me.remag501.customarmorsets.Core.ArmorSet;
import me.remag501.customarmorsets.Core.CustomArmorSetsCore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class OffHandAbilityListener implements Listener {

    @EventHandler
    public void onOffhandUse(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        // Check if player has an armor setf
        ArmorSet set = CustomArmorSetsCore.getArmorSet(player);
        if (set == null) return;

        // Cancel the default action
        event.setCancelled(true); // May want to always cancel

        // Trigger the offhand ability
        set.triggerAbility(player);
    }


}
