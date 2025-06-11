package me.remag501.customarmorsets.Utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Consumer;

import java.util.UUID;

public class AttributesUtil {

    private static final UUID HEALTH_MODIFIER_UUID = UUID.fromString("12345678-1234-1234-1234-1234567890ab");
    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("abcdefab-cdef-1234-5678-abcdefabcdef");

    public static void getBootsOnDelay(Player player, Consumer<ItemStack> callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                ItemStack boots = player.getInventory().getBoots();
                if (boots != null && boots.getType() != Material.AIR) {
                    callback.accept(boots);
                    cancel();
                }
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("CustomArmorSets"), 1L, 1L);
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

}
