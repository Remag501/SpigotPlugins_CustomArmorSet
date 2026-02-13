package me.remag501.customarmorsets.armor.impl;

import me.remag501.bgscore.api.task.TaskService;
import me.remag501.customarmorsets.armor.ArmorSet;
import me.remag501.customarmorsets.armor.ArmorSetType;
import me.remag501.customarmorsets.armor.WeaponType;
import me.remag501.customarmorsets.manager.CooldownBarManager;
import me.remag501.customarmorsets.manager.DamageStatsManager;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VikingCaptainArmorSet extends ArmorSet implements Listener {

    private static final Map<UUID, Long> abilityCooldowns = new HashMap<>();
    private static final long COOLDOWN = 7 * 1000; // 7 seconds cooldown

    private final TaskService taskService;
    private final DamageStatsManager damageStatsManager;
    private final CooldownBarManager cooldownBarManager;

    public VikingCaptainArmorSet(TaskService taskService, DamageStatsManager damageStatsManager, CooldownBarManager cooldownBarManager) {
        super(ArmorSetType.VIKING_CAPTAIN);
        this.taskService = taskService;
        this.damageStatsManager = damageStatsManager;
        this.cooldownBarManager = cooldownBarManager;

    }

    @Override
    public void applyPassive(Player player) {
        damageStatsManager.setWeaponMultiplier(player.getUniqueId(), (float) 0.75, WeaponType.SWORD);
        damageStatsManager.setWeaponMultiplier(player.getUniqueId(), (float) 1.25, WeaponType.AXE);
    }

    @Override
    public void removePassive(Player player) {
        damageStatsManager.clearAll(player.getUniqueId());
    }

    @Override
    public void triggerAbility(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (abilityCooldowns.containsKey(uuid) && now - abilityCooldowns.get(uuid) < COOLDOWN) {
            long timeLeft = (COOLDOWN - (now - abilityCooldowns.get(uuid))) / 1000;
            player.sendMessage("§cAbility is on cooldown for " + timeLeft + " more seconds!");
            return;
        }

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (!mainHand.getType().name().endsWith("_AXE")) {
            player.sendMessage("§cYou must hold an axe to use this ability!");
            return;
        }

        // Clone the axe and remove it from player’s hand
        ItemStack thrownAxeStack = mainHand.clone();
        player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        player.sendMessage("§eYou threw your axe!");
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f); // Play warning sound effect

        Item thrownAxe = player.getWorld().dropItem(player.getEyeLocation(), thrownAxeStack);
        thrownAxe.setPickupDelay(Integer.MAX_VALUE); // Prevent anyone from picking it up
        thrownAxe.setVelocity(player.getLocation().getDirection().multiply(1.5));

        taskService.subscribe(player.getUniqueId(), 0, 1, (ticks) -> {
            if (!thrownAxe.isValid() || !thrownAxe.getWorld().equals(player.getWorld())) {
                returnAxe(player, thrownAxeStack);
                return true;
            }

            // Check for nearby entities (2 block radius)
            for (Entity entity : thrownAxe.getNearbyEntities(1.2, 1.2, 1.2)) {
                if (entity instanceof LivingEntity && entity != player) {
                    ((LivingEntity) entity).damage(calculateAxeDamage(thrownAxeStack) * 1.25, player);
                    player.sendMessage("§cYou hit " + entity.getName() + " with your axe!");
                    thrownAxe.remove();
                    returnAxe(player, thrownAxeStack);
                    return true;
                }
            }

            // If the axe hits the ground (very low Y velocity)
            if (thrownAxe.isOnGround() || ticks > 40) {
                thrownAxe.remove();
                returnAxe(player, thrownAxeStack);
                return true;
            }
            return false;
        });

        // Start cooldown and cooldown bar
        cooldownBarManager.startCooldownBar(player, (int) (COOLDOWN / 1000));
        abilityCooldowns.put(uuid, now);
    }

    // Returns the axe to the player
    private void returnAxe(Player player, ItemStack axe) {
        if (player.getInventory().getItemInMainHand().getType() == Material.AIR) {
            player.getInventory().setItemInMainHand(axe);
        } else {
            player.getInventory().addItem(axe);
        }
        player.sendMessage("§aYour axe has returned.");
    }

    /**
     * Calculates base melee damage for a given axe and Sharpness level.
     *
     * @param item The axe ItemStack.
     * @return Total base damage with Sharpness included.
     */
    public static double calculateAxeDamage(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return 1.0;

        double baseDamage = switch (item.getType()) {
            case WOODEN_AXE -> 7.0;
            case STONE_AXE -> 9.0;
            case IRON_AXE -> 9.0;
            case GOLDEN_AXE -> 7.0;
            case DIAMOND_AXE -> 9.0;
            case NETHERITE_AXE -> 10.0;
            default -> 1.0;
        };

        int sharpnessLevel = item.getEnchantmentLevel(Enchantment.DAMAGE_ALL);
        if (sharpnessLevel > 0) {
            // Sharpness formula: 1 + (level - 1) * 0.5
            baseDamage += 1.0 + 0.5 * (sharpnessLevel - 1);
        }

        return baseDamage;
    }
}
