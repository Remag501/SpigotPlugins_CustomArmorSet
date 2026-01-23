package me.remag501.customarmorsets.listener;


import me.remag501.bgscore.api.TaskHelper;
import me.remag501.customarmorsets.armor.ArmorSet;
import me.remag501.customarmorsets.manager.ArmorManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class OffHandAbilityListener {

    public OffHandAbilityListener(ArmorManager armorManager, TaskHelper bgsApi) {

        bgsApi.subscribe(PlayerSwapHandItemsEvent.class)
                // 1. Only care if they are actually wearing a custom set
                .filter(e -> armorManager.getArmorSet(e.getPlayer()) != null)
                .handler(e -> {
                    Player player = e.getPlayer();

                    // 2. Stop the actual item swap
                    e.setCancelled(true);

                    // 3. Fire the custom ability
                    armorManager.getArmorSet(player).triggerAbility(player);
                });
    }
}
