package me.remag501.customarmorsets.listener;

import me.remag501.customarmorsets.service.ItemService;
import org.bukkit.ChatColor;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;

public class BrokenItemListener implements Listener {

    private final ItemService itemService;

    public BrokenItemListener(ItemService itemService) {
        this.itemService = itemService;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
//        if (ItemUtil.isBroken(event.getPlayer().getInventory().getItemInMainHand())) {
//            event.setCancelled(true);
//            event.getPlayer().sendMessage(ChatColor.RED + "This tool is broken and can't be used!");
//        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        if (itemService.isBroken(player.getInventory().getItemInMainHand())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "This weapon is broken and can't be used!");
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (itemService.isBroken(event.getItem())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "This item is broken and can't be used!");
        }
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
//        Player player = event.getPlayer();
//        ItemStack item = player.getInventory().getItem(event.getNewSlot());
//
//        if (ItemUtil.isBroken(item)) {
//            event.setCancelled(true);
//            player.sendMessage(ChatColor.RED + "This item is broken and cannot be equipped!");
//        }
    }

}

