package me.remag501.customarmorsets.listener;

import me.remag501.bgscore.api.event.EventService;
import me.remag501.customarmorsets.service.ItemService;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class BrokenItemListener {

    public BrokenItemListener(ItemService itemService, EventService eventService) {

        eventService.subscribe(EntityDamageByEntityEvent.class)
                .filter(e -> e.getDamager() instanceof Player)
                .filter(e -> itemService.isBroken(((Player) e.getDamager()).getInventory().getItemInMainHand()))
                .handler(this::onEntityDamage);

        eventService.subscribe(PlayerInteractEvent.class)
                .filter(e -> e.getItem() != null) // Avoid air clicks
                .filter(e -> itemService.isBroken(e.getItem()))
                .handler(this::onInteract);

    }

    public void onInteract(PlayerInteractEvent event) {
        event.setCancelled(true);
        event.getPlayer().sendMessage("§cThis item is broken and can't be used!");
    }

    public void onEntityDamage(EntityDamageByEntityEvent event) {
        event.setCancelled(true);
        event.getDamager().sendMessage("§cThis weapon is broken and can't be used!");

    }

}

