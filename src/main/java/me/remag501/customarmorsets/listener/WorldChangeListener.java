package me.remag501.customarmorsets.listener;

import me.remag501.bgscore.api.TaskHelper;
import me.remag501.customarmorsets.armor.ArmorSetType;
import me.remag501.customarmorsets.manager.ArmorManager;
import me.remag501.customarmorsets.service.ArmorService;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public class WorldChangeListener {

    public WorldChangeListener(ArmorManager armorManager, ArmorService armorService, TaskHelper bgsApi) {

        bgsApi.subscribe(PlayerChangedWorldEvent.class)
                .handler(e -> {
                    Player player = e.getPlayer();

                    // Check if they are wearing a set in the new world
                    ArmorSetType currentSet = armorService.isFullArmorSet(player);

                    if (currentSet != null) {
                        // The equipArmor method likely returns false if the world is blacklisted
                        boolean success = armorManager.equipArmor(player, currentSet);

                        if (!success) {
                            // If they can't wear it here, ensure all passives/tasks are purged
                            armorManager.unequipArmor(player);
                        }
                    } else {
                        // If they aren't wearing a full set anymore after the change, clean up
                        armorManager.unequipArmor(player);
                    }
                });
    }
}