package me.remag501.customarmorsets.listeners;

import me.remag501.customarmorsets.core.ArmorSetType;
import me.remag501.customarmorsets.core.CustomArmorSetsCore;
import me.remag501.customarmorsets.utils.ArmorUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class DisconnectReconnectListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ArmorSetType set = ArmorUtil.isFullArmorSet(player);

        if (set != null) {
            CustomArmorSetsCore.equipArmor(player, set);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        ArmorSetType set = ArmorUtil.isFullArmorSet(player);

        if (set != null) {
            CustomArmorSetsCore.unequipArmor(player);
        }
    }
}
