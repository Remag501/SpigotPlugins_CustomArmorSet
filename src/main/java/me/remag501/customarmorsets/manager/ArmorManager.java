package me.remag501.customarmorsets.manager;

import me.remag501.customarmorsets.armor.ArmorRegistry;
import me.remag501.customarmorsets.armor.ArmorSet;
import me.remag501.customarmorsets.armor.ArmorSetType;
import me.remag501.customarmorsets.service.ArmorStateService;
import me.remag501.customarmorsets.service.CosmeticService;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class ArmorManager {

    private final ArmorRegistry registry;
    private final ArmorStateService stateService;
    private final Plugin plugin;
    private final CosmeticService cosmeticService;

    private static final List<String> BANNED_WORLDS = List.of("spawn", "dungeonhub", "honeyclicker");
    private static final String BUNKER_PREFIX = "bunker";

    public ArmorManager(Plugin plugin,
                        ArmorRegistry registry,
                        ArmorStateService stateService,
                        CosmeticService cosmeticService) {
        this.plugin = plugin;
        this.registry = registry;
        this.stateService = stateService;
        this.cosmeticService = cosmeticService;
    }

    public boolean equipArmor(Player player, ArmorSetType type) {
        // 1. Cosmetic Update
        Bukkit.getScheduler().runTask(plugin, () -> {
            ItemStack helmet = player.getInventory().getHelmet();
            if (helmet != null) {
                player.getInventory().setHelmet(cosmeticService.makeCosmeticHelmet(helmet, type.getHeadUrl()));
            }
        });

        // 2. World Check
        String worldName = player.getWorld().getName().toLowerCase();
        if (BANNED_WORLDS.contains(worldName) || worldName.startsWith(BUNKER_PREFIX)) {
            // Note: We update helmet type for cosmetic reasons but don't activate passive
            stateService.setState(player.getUniqueId(), null, type);
            return false;
        }

        // 3. Activate Passive
        ArmorSet set = registry.get(type);
        if (set != null) {
            stateService.setState(player.getUniqueId(), set, type);
            set.applyPassive(player);
            return true;
        }
        return false;
    }

    public void unequipArmor(Player player) {
        ArmorSet set = stateService.getActiveSet(player.getUniqueId());
        ArmorSetType type = stateService.getActiveType(player.getUniqueId());

        if (set != null) {
            set.removePassive(player);
        }

        if (type != null) {
            ItemStack helmet = player.getInventory().getHelmet();
            if (helmet != null) {
                player.getInventory().setHelmet(cosmeticService.restoreOriginalHelmet(helmet, Color.fromRGB(type.getLeatherColor())));
            }
        }

        stateService.setState(player.getUniqueId(), null, null);
    }
}