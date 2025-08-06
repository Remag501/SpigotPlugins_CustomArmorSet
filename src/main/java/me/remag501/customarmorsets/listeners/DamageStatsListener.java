package me.remag501.customarmorsets.listeners;

import me.remag501.customarmorsets.core.DamageStats;
import me.remag501.customarmorsets.core.TargetCategory;
import me.remag501.customarmorsets.core.WeaponType;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class DamageStatsListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // --- 1. Validate damager ---
        if (!(event.getDamager() instanceof Player player)) return;

        UUID playerId = player.getUniqueId();

        // --- 2. Determine weapon type ---
        WeaponType weaponType = getWeaponType(player.getInventory().getItemInMainHand());

        // --- 3. Determine target category ---
        TargetCategory targetCategory = getTargetCategory(event.getEntity());

        // --- 4. Get multipliers ---
        float weaponMult = DamageStats.getWeaponMultiplier(playerId, weaponType);
        float mobMult = DamageStats.getMobMultiplier(playerId, targetCategory);

        // --- 5. Apply final damage ---
        double newDamage = event.getDamage() * weaponMult * mobMult;
        event.setDamage(newDamage);
    }

    // ----------------------------
    // HELPER: Determine Weapon Type
    // ----------------------------
    private WeaponType getWeaponType(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return WeaponType.OTHER;

        Material mat = item.getType();

        if (mat.name().endsWith("_SWORD")) return WeaponType.SWORD;
        if (mat.name().endsWith("_AXE")) return WeaponType.AXE;
        if (mat == Material.BOW) return WeaponType.BOW;
        if (mat == Material.CROSSBOW) return WeaponType.CROSSBOW;
        if (mat == Material.TRIDENT) return WeaponType.TRIDENT;

        return WeaponType.OTHER;
    }

    // ----------------------------
    // HELPER: Determine Target Category
    // ----------------------------
    private TargetCategory getTargetCategory(Entity target) {
        if (target instanceof Player) {
            return TargetCategory.PLAYERS;
        }

        // Non-player mobs get specific category, but can also benefit from NON_PLAYER fallback
        if (target instanceof Zombie || target instanceof Skeleton || target instanceof Wither) {
            return TargetCategory.UNDEAD;
        }
        if (target instanceof Spider || target instanceof CaveSpider || target instanceof Silverfish) {
            return TargetCategory.ARTHROPOD;
        }
        if (target instanceof Vindicator || target instanceof Evoker || target instanceof Pillager || target instanceof Illusioner) {
            return TargetCategory.ILLAGER;
        }
        if (target instanceof EnderDragon || target instanceof Wither) {
            return TargetCategory.BOSS;
        }

        return TargetCategory.GENERIC; // Default for other mobs
    }

}
