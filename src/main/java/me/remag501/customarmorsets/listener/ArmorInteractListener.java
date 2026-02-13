package me.remag501.customarmorsets.listener;

import me.remag501.bgscore.api.event.EventService;
import me.remag501.customarmorsets.manager.ArmorManager;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerInteractEvent;

public class ArmorInteractListener {

    public ArmorInteractListener(ArmorManager armorManager, EventService eventService) {

        eventService.subscribe(PlayerInteractEvent.class)
                // 1. Filter: Must be a right-click action with an item
                .filter(e -> e.getAction().name().contains("RIGHT"))
                .filter(PlayerInteractEvent::hasItem)
                // 2. Filter: Is it armor?
                .filter(e -> isArmor(e.getItem().getType()))
                // 3. Filter: Are they already wearing a custom set?
                .filter(e -> armorManager.getArmorSet(e.getPlayer()) != null)
                .handler(e -> {
                    // Cancel the hotswap
                    e.setCancelled(true);
                    // Optional: e.getPlayer().sendMessage("§cYou cannot hotswap while wearing custom armor!");
                });
    }

    private boolean isArmor(Material material) {
        String name = material.name();
        return name.endsWith("_HELMET") ||
                name.endsWith("_CHESTPLATE") ||
                name.endsWith("_LEGGINGS") ||
                name.endsWith("_BOOTS");
    }
}