package me.remag501.customarmorsets.armor.impl;

import me.remag501.customarmorsets.armor.ArmorSet;
import me.remag501.customarmorsets.armor.ArmorSetType;
import me.remag501.customarmorsets.manager.ArmorManager;
import me.remag501.customarmorsets.manager.CooldownBarManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VikingCaptainArmorSet extends ArmorSet implements Listener {

    private static final Map<UUID, Long> abilityCooldowns = new HashMap<>();
    private static final long COOLDOWN = 7 * 1000; // 7 seconds cooldown

    public VikingCaptainArmorSet() {
        super(ArmorSetType.VIKING_CAPTAIN);
    }

    @Override
    public void applyPassive(Player player) {
        player.sendMessage("✅ You equipped the Viking Captain set");
        // Passive: 20% axe damage buff, -20% sword damage (needs manual handling in combat events)
    }

    @Override
    public void removePassive(Player player) {
        player.sendMessage("❌ You removed the Viking Captain set");
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

        Plugin plugin = Bukkit.getPluginManager().getPlugin("CustomArmorSets");

        // Task to track the axe
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!thrownAxe.isValid() || !thrownAxe.getWorld().equals(player.getWorld())) {
                    returnAxe(player, thrownAxeStack);
                    cancel();
                    return;
                }

                // Check for nearby entities (2 block radius)
                for (Entity entity : thrownAxe.getNearbyEntities(1.2, 1.2, 1.2)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        ((LivingEntity) entity).damage(calculateAxeDamage(thrownAxeStack) * 1.25, player);
                        player.sendMessage("§cYou hit " + entity.getName() + " with your axe!");
                        thrownAxe.remove();
                        returnAxe(player, thrownAxeStack);
                        cancel();
                        return;
                    }
                }

                // If the axe hits the ground (very low Y velocity)
                if (thrownAxe.isOnGround() || ticks > 40) {
                    thrownAxe.remove();
                    returnAxe(player, thrownAxeStack);
                    cancel();
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // Start cooldown and cooldown bar
        CooldownBarManager.startCooldownBar(plugin, player, (int) (COOLDOWN / 1000));
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

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        ArmorSet set = ArmorManager.getArmorSet(player);
        if (!(set instanceof VikingCaptainArmorSet)) return;

        // Check if the player is holding a sword
        Material itemInHand = player.getInventory().getItemInMainHand().getType();
        if (itemInHand.name().endsWith("_SWORD")) {
            double originalDamage = event.getDamage();
            event.setDamage(originalDamage * 0.75); // Decrease sword damage
        } else if (itemInHand.name().endsWith("_AXE")) {
            double originalDamage = event.getDamage();
            event.setDamage(originalDamage * 1.25); // Increase axe damage
        }
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
