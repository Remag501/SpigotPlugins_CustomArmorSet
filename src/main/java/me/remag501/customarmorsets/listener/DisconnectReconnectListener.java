package me.remag501.customarmorsets.listener;

import me.remag501.bgscore.api.TaskHelper;
import me.remag501.customarmorsets.armor.ArmorSetType;
import me.remag501.customarmorsets.manager.ArmorManager;
import me.remag501.customarmorsets.service.ArmorService;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class DisconnectReconnectListener {

    public DisconnectReconnectListener(ArmorManager armorManager, ArmorService armorService, TaskHelper bgsApi) {

        // 1. Handle Join - Re-equip if wearing a set
        bgsApi.subscribe(PlayerJoinEvent.class)
                .handler(e -> {
                    ArmorSetType set = armorService.isFullArmorSet(e.getPlayer());
                    if (set != null) {
                        armorManager.equipArmor(e.getPlayer(), set);
                    }
                });

        // 2. Handle Quit - Unequip to clean up passives/attributes
        bgsApi.subscribe(PlayerQuitEvent.class)
                .handler(e -> {
                    // We check if the manager actually has them tracked to avoid unnecessary logic
                    if (armorManager.getArmorSet(e.getPlayer()) != null) {
                        armorManager.unequipArmor(e.getPlayer());
                    }
                });
    }
}