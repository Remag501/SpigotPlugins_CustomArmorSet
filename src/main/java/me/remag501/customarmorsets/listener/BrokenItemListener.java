package me.remag501.customarmorsets.listener;

import me.remag501.bgscore.api.TaskHelper;
import me.remag501.customarmorsets.service.ItemService;
import org.bukkit.ChatColor;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;

public class BrokenItemListener {

    private final ItemService itemService;

    public BrokenItemListener(ItemService itemService, TaskHelper bgsApi) {
        this.itemService = itemService;

        // Subscribe to Damage
        //                    Player player = (Player) e.getDamager();
        //                    if (itemService.isBroken(player.getInventory().getItemInMainHand())) {
        //                        e.setCancelled(true);
        //                        player.sendMessage("§cThis weapon is broken and can't be used!");
        //                    }
        bgsApi.subscribe(EntityDamageByEntityEvent.class)
                .filter(e -> e.getDamager() instanceof Player)
                .filter(e -> itemService.isBroken(((Player) e.getDamager()).getInventory().getItemInMainHand()))
                .handler(this::onEntityDamage);

        // Subscribe to Interact
        //                    e.setCancelled(true);
        //                    e.getPlayer().sendMessage("§cThis item is broken and can't be used!");
        bgsApi.subscribe(PlayerInteractEvent.class)
                .filter(e -> e.getItem() != null) // Avoid air clicks
                .filter(e -> itemService.isBroken(e.getItem()))
                .handler(this::onInteract);

    }

    public void onInteract(PlayerInteractEvent event) {
//        if (itemService.isBroken(event.getItem())) {
//
//        }
        event.setCancelled(true);
        event.getPlayer().sendMessage("§cThis item is broken and can't be used!");
    }

    public void onEntityDamage(EntityDamageByEntityEvent event) {
//        Player player = (Player) event.getDamager();
//        if (itemService.isBroken(player.getInventory().getItemInMainHand())) {
//            event.setCancelled(true);
//            player.sendMessage("§cThis weapon is broken and can't be used!");
//        }

        event.setCancelled(true);
        event.getDamager().sendMessage("§cThis weapon is broken and can't be used!");
    }

//    @EventHandler
//    public void onEntityDamage(EntityDamageByEntityEvent event) {
//        if (!(event.getDamager() instanceof Player player)) return;
//
//        if (itemService.isBroken(player.getInventory().getItemInMainHand())) {
//            event.setCancelled(true);
//            player.sendMessage(ChatColor.RED + "This weapon is broken and can't be used!");
//        }
//    }
//
//    @EventHandler
//    public void onInteract(PlayerInteractEvent event) {
//        if (itemService.isBroken(event.getItem())) {
//            event.setCancelled(true);
//            event.getPlayer().sendMessage(ChatColor.RED + "This item is broken and can't be used!");
//        }
//    }

}

