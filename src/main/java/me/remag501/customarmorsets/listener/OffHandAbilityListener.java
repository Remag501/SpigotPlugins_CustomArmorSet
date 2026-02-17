package me.remag501.customarmorsets.listener;

import me.remag501.bgscore.api.event.EventService;
import me.remag501.customarmorsets.manager.ArmorManager;
import me.remag501.customarmorsets.service.ArmorStateService;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class OffHandAbilityListener {

    public OffHandAbilityListener(ArmorStateService armorStateService, EventService eventService) {

        eventService.subscribe(PlayerSwapHandItemsEvent.class)
                // 1. Only care if they are actually wearing a custom set
                .filter(e -> armorStateService.getActiveSet(e.getPlayer().getUniqueId()) != null)
                .handler(e -> {
                    Player player = e.getPlayer();

                    // 2. Stop the actual item swap
                    e.setCancelled(true);

                    // 3. Fire the custom ability
                    armorStateService.getActiveSet(e.getPlayer().getUniqueId()).triggerAbility(player);
                });
    }
}
