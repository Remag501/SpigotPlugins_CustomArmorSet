package me.remag501.customarmorsets.listeners;

import me.remag501.customarmorsets.core.DamageStats;
import me.remag501.customarmorsets.core.DamageStats.WeaponType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class DamageStatsListener implements Listener {

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

//        UUID uuid = player.getUniqueId();

        // Determine target
        boolean isPvP = event.getEntity() instanceof Player;
        boolean isPvE = event.getEntity() instanceof LivingEntity;

        // Get weapon type
        ItemStack weapon = player.getInventory().getItemInMainHand();
        WeaponType weaponType = getWeaponType(weapon.getType());

        if (weaponType == null) return;

        double baseDamage = event.getDamage();

        // Apply multipliers
        if (isPvP && DamageStats.hasPvPMultiplier(weaponType, player)) {
            double multiplier = DamageStats.getPvPMultiplier(weaponType, player);
            event.setDamage(baseDamage * multiplier);
        } else if (isPvE && DamageStats.hasPvEMultiplier(weaponType, player)) {
            double multiplier = DamageStats.getPvEMultiplier(weaponType, player);
            event.setDamage(baseDamage * multiplier);
        }

        // emulate old combat if enabled
        if (DamageStats.hasOldCombat(player)) {
//            emulateOldCombat(player);
            player.setCooldown(player.getInventory().getItemInMainHand().getType(), 0);
        }
    }

    private WeaponType getWeaponType(Material mat) {
        String name = mat.name();

        if (name.endsWith("_SWORD")) return WeaponType.SWORD;
        if (name.endsWith("_AXE")) return WeaponType.AXE;
        if (name.endsWith("_BOW")) return WeaponType.BOW;
        if (name.endsWith("_CROSSBOW")) return WeaponType.CROSSBOW;
        if (name.endsWith("_TRIDENT")) return WeaponType.TRIDENT;

        return null;
    }

    private void emulateOldCombat(Player player) {
        // Disable cooldown for old PvP combat feel
        // Bukkit resets attack speed per weapon, so simulate with attack speed manipulation or status
        // Optional: can use metadata or potion effects

        // Here we could apply a Weakness effect briefly to cancel bonus damage or use a flag
        // However, Bukkit doesn't allow modifying attack cooldown directly
        // This function is a stub for now
    }
}
