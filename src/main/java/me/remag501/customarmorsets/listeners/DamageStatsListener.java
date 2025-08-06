package me.remag501.customarmorsets.listeners;

import me.remag501.customarmorsets.core.DamageStats;
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
        DamageStats.WeaponType weaponType = getWeaponType(player.getInventory().getItemInMainHand());

        // --- 3. Determine target category ---
        DamageStats.TargetCategory targetCategory = getTargetCategory(event.getEntity());

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
    private DamageStats.WeaponType getWeaponType(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return DamageStats.WeaponType.OTHER;

        Material mat = item.getType();

        if (mat.name().endsWith("_SWORD")) return DamageStats.WeaponType.SWORD;
        if (mat.name().endsWith("_AXE")) return DamageStats.WeaponType.AXE;
        if (mat == Material.BOW) return DamageStats.WeaponType.BOW;
        if (mat == Material.CROSSBOW) return DamageStats.WeaponType.CROSSBOW;
        if (mat == Material.TRIDENT) return DamageStats.WeaponType.TRIDENT;

        return DamageStats.WeaponType.OTHER;
    }

    // ----------------------------
    // HELPER: Determine Target Category
    // ----------------------------
    private DamageStats.TargetCategory getTargetCategory(Entity target) {
        if (target instanceof Player) {
            return DamageStats.TargetCategory.PLAYERS;
        }
        if (target instanceof Monster) {
            if (target instanceof Zombie || target instanceof Skeleton || target instanceof Wither) {
                return DamageStats.TargetCategory.UNDEAD;
            }
            if (target instanceof Spider || target instanceof CaveSpider || target instanceof Silverfish) {
                return DamageStats.TargetCategory.ARTHROPOD;
            }
            if (target instanceof Vindicator || target instanceof Evoker || target instanceof Pillager || target instanceof Illusioner) {
                return DamageStats.TargetCategory.ILLAGER;
            }
            if (target instanceof EnderDragon || target instanceof Wither) {
                return DamageStats.TargetCategory.BOSS;
            }
            return DamageStats.TargetCategory.GENERIC; // Generic hostile mob
        }

        // Passive mobs or misc
        return DamageStats.TargetCategory.GENERIC;
    }
}
