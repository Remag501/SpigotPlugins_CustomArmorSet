package me.remag501.customarmorsets.listener;

import me.remag501.bgscore.api.event.EventService;
import me.remag501.customarmorsets.manager.DamageStatsManager;
import me.remag501.customarmorsets.manager.DefenseStatsManager;
import me.remag501.customarmorsets.armor.TargetCategory;
import me.remag501.customarmorsets.armor.WeaponType;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class DamageListener {

    private final DamageStatsManager damageStatsManager;
    private final DefenseStatsManager defenseStatsManager;

    public DamageListener(DamageStatsManager damageStatsManager, DefenseStatsManager defenseStatsManager, EventService eventService) {
        this.damageStatsManager = damageStatsManager;
        this.defenseStatsManager = defenseStatsManager;

        // Subscribe to the Damage Event via Core
        eventService.subscribe(EntityDamageByEntityEvent.class)
                .filter(e -> e.getEntity() instanceof LivingEntity) // Must involve a LivingEntity victim
                .handler(this::onEntityDamageByEntity);
    }

    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Must involve a LivingEntity on at least one side
        LivingEntity victim = (LivingEntity) event.getEntity();

        UUID attackerId = null;
        UUID victimId = null;

        // -------------------------
        // 1. Attacker multipliers (Damage Boost)
        // -------------------------
        float outgoingMult = 1.0f;
        if (event.getDamager() instanceof Player attacker) {
            attackerId = attacker.getUniqueId();

            // Determine weapon type for attacker
            WeaponType weaponType = getWeaponType(attacker.getInventory().getItemInMainHand());

            // Determine target category (who they're attacking)
            TargetCategory targetCategory = getTargetCategory(victim);

            // Combine weapon + mob multipliers
            outgoingMult = damageStatsManager.getWeaponMultiplier(attackerId, weaponType)
                    * damageStatsManager.getMobMultiplier(attackerId, targetCategory);
        }

        // -------------------------
        // 2. Victim multipliers (Damage Reduction)
        // -------------------------
        float incomingMult = 1.0f;
        if (victim instanceof Player victimPlayer) {
            victimId = victimPlayer.getUniqueId();

            // Determine weapon type for damage source
            WeaponType weaponType = WeaponType.ALL;
            if (event.getDamager() instanceof LivingEntity livingDamager) {
                weaponType = getWeaponType(livingDamager.getEquipment() != null
                        ? livingDamager.getEquipment().getItemInMainHand()
                        : null);
            }

            // Determine category of attacker
            TargetCategory attackerCategory = getTargetCategory(event.getDamager());

            // Combine reductions
            incomingMult = defenseStatsManager.getSourceReduction(victimId, attackerCategory)
                    * defenseStatsManager.getWeaponReduction(victimId, weaponType);
        }

        // -------------------------
        // 3. Apply final damage
        // -------------------------
        double newDamage = event.getDamage() * outgoingMult * incomingMult;
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
