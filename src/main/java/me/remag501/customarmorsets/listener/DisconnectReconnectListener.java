package me.remag501.customarmorsets.listener;

import me.remag501.customarmorsets.armor.ArmorSetType;
import me.remag501.customarmorsets.manager.ArmorManager;
import me.remag501.customarmorsets.service.ArmorService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class DisconnectReconnectListener implements Listener {

    private final ArmorManager armorManager;
    private final ArmorService armorService;


    public DisconnectReconnectListener(ArmorManager armorManager, ArmorService armorService) {
        this.armorManager = armorManager;
        this.armorService = armorService;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ArmorSetType set = armorService.isFullArmorSet(player);

        if (set != null) {
            armorManager.equipArmor(player, set);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        ArmorSetType set = armorService.isFullArmorSet(player);

        if (set != null) {
            armorManager.unequipArmor(player);
        }
    }
}
