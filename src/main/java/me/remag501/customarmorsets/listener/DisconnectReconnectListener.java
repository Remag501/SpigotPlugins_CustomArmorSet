package me.remag501.customarmorsets.listener;

import me.remag501.bgscore.api.event.EventService;
import me.remag501.customarmorsets.armor.ArmorSetType;
import me.remag501.customarmorsets.manager.ArmorManager;
import me.remag501.customarmorsets.service.ArmorService;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class DisconnectReconnectListener {

    public DisconnectReconnectListener(ArmorManager armorManager, ArmorService armorService, EventService eventService) {

        // 1. Handle Join - Re-equip if wearing a set
        eventService.subscribe(PlayerJoinEvent.class)
                .handler(e -> {
                    ArmorSetType set = armorService.isFullArmorSet(e.getPlayer());
                    if (set != null) {
                        armorManager.equipArmor(e.getPlayer(), set);
                    }
                });

        // 2. Handle Quit - Unequip to clean up passives/attributes
        eventService.subscribe(PlayerQuitEvent.class)
                .handler(e -> {
                    // We check if the manager actually has them tracked to avoid unnecessary logic
                    if (armorManager.getArmorSet(e.getPlayer()) != null) {
                        armorManager.unequipArmor(e.getPlayer());
                    }
                });
    }
}