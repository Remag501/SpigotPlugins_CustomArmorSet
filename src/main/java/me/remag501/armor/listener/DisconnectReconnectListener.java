package me.remag501.armor.listener;

import me.remag501.bgscore.api.event.EventService;
import me.remag501.armor.armor.ArmorSetType;
import me.remag501.armor.manager.ArmorManager;
import me.remag501.armor.service.ArmorService;
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
                    if (armorService.isFullArmorSet(e.getPlayer()) != null) {
                        armorManager.unequipArmor(e.getPlayer());
                    }
                });
    }
}