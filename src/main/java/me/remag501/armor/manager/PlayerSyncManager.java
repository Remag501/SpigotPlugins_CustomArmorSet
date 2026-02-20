package me.remag501.armor.manager;

import me.remag501.bgscore.api.combat.AttributeService;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.*;

public class PlayerSyncManager {
    // Stores original potion effects
    private final Map<UUID, Collection<PotionEffect>> originalEffects = new HashMap<>();
    private final Map<UUID, Integer> playerOnFire = new HashMap<>();

    // Stores original inventory (ItemStack[])
    private final Map<UUID, ItemStack[]> originalInventory = new HashMap<>();

    // Stores original health (double)
    private final Map<UUID, Double> originalHealth = new HashMap<>();

    private final AttributeService attributeService;

    public PlayerSyncManager(AttributeService attributeService) {
        this.attributeService = attributeService;
    }


    public void syncPotionEffects(Player player, LivingEntity mob) {
        // Save player's current effects
        originalEffects.put(player.getUniqueId(), new ArrayList<>(player.getActivePotionEffects()));
        playerOnFire.put(player.getUniqueId(), player.getFireTicks());

        // Clear existing effects
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        // Apply mob's effects
        for (PotionEffect effect : mob.getActivePotionEffects()) {
            player.addPotionEffect(new PotionEffect(
                    effect.getType(),
                    effect.getDuration(),
                    effect.getAmplifier(),
                    effect.isAmbient(),
                    effect.hasParticles(),
                    effect.hasIcon()
            ));
        }
        player.setFireTicks(mob.getFireTicks()); // Apply mob's fire to player
    }


    public void syncInventory(Player player, LivingEntity mob) {
        // Save player's current inventory
        originalInventory.put(player.getUniqueId(), player.getInventory().getContents().clone());

        EntityEquipment mobEquip = mob.getEquipment();
        if (mobEquip != null) {
            player.getInventory().clear();

            // Set armor
            player.getInventory().setHelmet(mobEquip.getHelmet());
            player.getInventory().setChestplate(mobEquip.getChestplate());
            player.getInventory().setLeggings(mobEquip.getLeggings());
            player.getInventory().setBoots(mobEquip.getBoots());

            // Set hand items
            player.getInventory().setItemInMainHand(mobEquip.getItemInMainHand());
            player.getInventory().setItemInOffHand(mobEquip.getItemInOffHand());
        }

        player.updateInventory();
    }


    public void syncHealth(Player player, LivingEntity mob) {
        // Save current player health
        originalHealth.put(player.getUniqueId(), player.getHealth());

        // Scale max health first
        attributeService.applyMaxHealth(player, "sync_manager", mob.getMaxHealth() / 20.0);

        // Set current health proportionally
        double scaledHealth = (mob.getHealth() / mob.getMaxHealth()) *
                player.getAttribute(Attribute.MAX_HEALTH).getValue();

        player.setHealth(Math.min(scaledHealth, player.getAttribute(Attribute.MAX_HEALTH).getValue()));
    }


    public void restorePotionEffects(Player player) {
        Collection<PotionEffect> effects = originalEffects.remove(player.getUniqueId());
        if (effects != null) {
            // Clear mob effects first
            for (PotionEffect effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }

            // Reapply original effects
            for (PotionEffect effect : effects) {
                player.addPotionEffect(effect);
            }
            // Reapply fire ticks
            player.setFireTicks(playerOnFire.remove(player.getUniqueId()));
        }
    }

    public void restoreInventory(Player player) {
        ItemStack[] inventory = originalInventory.remove(player.getUniqueId());
        if (inventory != null) {
            player.getInventory().setContents(inventory);
            player.updateInventory();
        }
    }

    public void restoreHealth(Player player) {
        Double health = originalHealth.remove(player.getUniqueId());
        if (health != null) {
            attributeService.resetSource(player, "sync_manager");
            player.setHealth(Math.min(health, player.getAttribute(Attribute.MAX_HEALTH).getValue()));
        }
    }


}
