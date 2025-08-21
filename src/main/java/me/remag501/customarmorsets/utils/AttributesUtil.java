package me.remag501.customarmorsets.utils;

import me.remag501.customarmorsets.CustomArmorSets;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Consumer;

import java.util.UUID;

public class AttributesUtil {

    public static void getBootsOnDelay(Player player, Consumer<ItemStack> callback) {
        boolean shouldBeSynchronous = CustomArmorSets.isServerShuttingDown();
        if (shouldBeSynchronous) {
            ItemStack boots = player.getInventory().getBoots();
            if (boots != null && boots.getType() != Material.AIR) {
                callback.accept(boots);
            }
        } else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    ItemStack boots = player.getInventory().getBoots();
                    if (boots != null && boots.getType() != Material.AIR) {
                        callback.accept(boots);
                        cancel();
                    }
                }
            }.runTaskTimer(CustomArmorSets.getInstance(), 1L, 1L);
        }
    }


    public static void applyHealth(Player player, double mult) {
        getBootsOnDelay(player, boots -> {
            if (boots == null || !boots.hasItemMeta()) return;

            ItemMeta meta = boots.getItemMeta();
            if (meta == null) return;

            // Remove existing health modifiers to prevent stacking
            meta.removeAttributeModifier(Attribute.GENERIC_MAX_HEALTH);

            double baseHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(); // Default base health
            double bonusHealth = baseHealth * (mult - 1); // e.g., mult = 1.5 → +10 health

            AttributeModifier modifier = new AttributeModifier(
                    UUID.randomUUID(),
                    "custom_health_bonus",
                    bonusHealth,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlot.FEET
            );

            meta.addAttributeModifier(Attribute.GENERIC_MAX_HEALTH, modifier);
            boots.setItemMeta(meta);
        });

    }

    public static void removeHealth(Player player) {
        getBootsOnDelay(player, boots -> {
            if (boots == null || !boots.hasItemMeta()) return;

            ItemMeta meta = boots.getItemMeta();
            if (meta == null) return;

            // Remove all health modifiers from the helmet
            meta.removeAttributeModifier(Attribute.GENERIC_MAX_HEALTH);
            boots.setItemMeta(meta);
        });
    }

    public static void applyDamage(Player player, double mult) {
        getBootsOnDelay(player, boots -> {
            if (boots == null || !boots.hasItemMeta()) return;

            ItemMeta meta = boots.getItemMeta();
            if (meta == null) return;

            // Remove existing attack damage modifiers to prevent stacking
            meta.removeAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE);

            double baseDamage = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue();
            double bonusDamage = baseDamage * (mult - 1); // e.g., mult = 1.5 → +50% damage

            AttributeModifier modifier = new AttributeModifier(
                    UUID.randomUUID(),
                    "custom_damage_bonus",
                    bonusDamage,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlot.FEET
            );

            meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, modifier);
            boots.setItemMeta(meta);
        });
    }

    public static void removeDamage(Player player) {
        getBootsOnDelay(player, boots -> {
            if (boots == null || !boots.hasItemMeta()) return;

            ItemMeta meta = boots.getItemMeta();
            if (meta == null) return;

            // Remove all attack damage modifiers
            meta.removeAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE);
            boots.setItemMeta(meta);
        });
    }

    public static void applySpeed(Player player, double mult) {
        getBootsOnDelay(player, boots -> {
            if (boots == null || !boots.hasItemMeta()) return;

            ItemMeta meta = boots.getItemMeta();
            if (meta == null) return;

            // Remove existing speed modifiers to prevent stacking
            meta.removeAttributeModifier(Attribute.GENERIC_MOVEMENT_SPEED);

            double baseSpeed = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue();
            double bonusSpeed = baseSpeed * (mult - 1); // e.g., mult = 1.2 → +0.04 speed

            AttributeModifier modifier = new AttributeModifier(
                    UUID.randomUUID(),
                    "custom_speed_bonus",
                    bonusSpeed,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlot.FEET
            );

            meta.addAttributeModifier(Attribute.GENERIC_MOVEMENT_SPEED, modifier);
            boots.setItemMeta(meta);
        });
    }

    public static void removeSpeed(Player player) {
        getBootsOnDelay(player, boots -> {
            if (boots == null || !boots.hasItemMeta()) return;

            ItemMeta meta = boots.getItemMeta();
            if (meta == null) return;

            meta.removeAttributeModifier(Attribute.GENERIC_MOVEMENT_SPEED);
            boots.setItemMeta(meta);
        });
    }

    public static void applyAttackSpeed(Player player, double mult) {
        getBootsOnDelay(player, boots -> {
            if (boots == null || !boots.hasItemMeta()) return;

            ItemMeta meta = boots.getItemMeta();
            if (meta == null) return;

            // Remove existing attack speed modifiers to prevent stacking
            meta.removeAttributeModifier(Attribute.GENERIC_ATTACK_SPEED);

            double baseAttackSpeed = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED).getValue(); // Usually 4.0
            double bonusSpeed = baseAttackSpeed * (mult - 1); // e.g., mult = 1.5 → +2 attack speed

            AttributeModifier modifier = new AttributeModifier(
                    UUID.randomUUID(),
                    "custom_attack_speed_bonus",
                    bonusSpeed,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlot.FEET
            );

            meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, modifier);
            boots.setItemMeta(meta);
        });
    }

    public static void removeAttackSpeed(Player player) {
        getBootsOnDelay(player, boots -> {
            if (boots == null || !boots.hasItemMeta()) return;

            ItemMeta meta = boots.getItemMeta();
            if (meta == null) return;

            // Remove all attack speed modifiers from the boots
            meta.removeAttributeModifier(Attribute.GENERIC_ATTACK_SPEED);
            boots.setItemMeta(meta);
        });
    }

    public static void applyHealthDirect(Player player, double multiplier) {
        AttributeInstance attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr == null) return;

        // Store base value to restore later
        double base = 20.0; // Default vanilla base
        double newValue = base * multiplier;

        attr.setBaseValue(newValue);

        // Clamp current health to new max
        if (player.getHealth() > newValue) {
            player.setHealth(newValue);
        }
    }

    public static void applySpeedDirect(Player player, double multiplier) {
        AttributeInstance attr = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (attr == null) return;

        double base = 0.1; // Default vanilla base speed
        attr.setBaseValue(base * multiplier);
    }

    public static void applyDamageDirect(Player player, double multiplier) {
        AttributeInstance attr = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (attr == null) return;

        double base = 1.0; // Default vanilla base damage
        attr.setBaseValue(base * multiplier);
    }

    public static void restoreDefaults(Player player) {
        // Reset to vanilla values
        AttributeInstance healthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttr != null) healthAttr.setBaseValue(20.0);

        AttributeInstance speedAttr = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speedAttr != null) speedAttr.setBaseValue(0.1);

        AttributeInstance damageAttr = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (damageAttr != null) damageAttr.setBaseValue(1.0);

        }

    public static void removeAllArmorAttributes(ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        meta.removeAttributeModifier(Attribute.GENERIC_MAX_HEALTH);
        meta.removeAttributeModifier(Attribute.GENERIC_MOVEMENT_SPEED);
        meta.removeAttributeModifier(Attribute.GENERIC_ATTACK_SPEED);
        meta.removeAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE);
        meta.removeAttributeModifier(Attribute.GENERIC_ARMOR);
        itemStack.setItemMeta(meta);
    }
}
