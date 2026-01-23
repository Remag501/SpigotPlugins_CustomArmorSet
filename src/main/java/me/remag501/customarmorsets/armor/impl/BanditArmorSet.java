package me.remag501.customarmorsets.armor.impl;

import me.remag501.customarmorsets.CustomArmorSets;
import me.remag501.customarmorsets.armor.ArmorSet;
import me.remag501.customarmorsets.armor.ArmorSetType;
import me.remag501.customarmorsets.manager.ArmorManager;
import me.remag501.customarmorsets.util.AttributesUtil;
import me.remag501.customarmorsets.util.CooldownBarUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class BanditArmorSet extends ArmorSet implements Listener {

    // A map to store the number of available dodges for each player.
    private final Map<UUID, Integer> playerDodges = new HashMap<>();

    // A map to store the regeneration task for each player.
    private final Map<UUID, BukkitTask> regenTasks = new HashMap<>();

    // A random number generator for the item drop chance.
    private final Random random = new Random();

    // The maximum number of dodges a player can store.
    private final int MAX_DODGES = 3;

    // Regeneration time for doge
    private final int DODGE_COOLDOWN = 5;

    public BanditArmorSet() {
        super(ArmorSetType.BANDIT);
    }

    @Override
    public void applyPassive(Player player) {
//        player.sendMessage("✅ You equipped the Bandit set!");
    }

    @Override
    public void removePassive(Player player) {
//        player.sendMessage("❌ You removed the Bandit set.");
        // The speed attribute is handled conditionally in the PlayerMoveEvent handler.
        // It will be removed automatically once the player's hands are no longer empty.

        // Cancel the regeneration task when the set is removed.
        BukkitTask task = regenTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }

        CooldownBarUtil.restorePlayerBar(player);
    }

    // --- Passive 1: Run Faster With Fists Out ---
    // This event handler is necessary to check if the player's hands are empty.
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!(ArmorManager.getArmorSet(player) instanceof BanditArmorSet)) {
            return;
        }

        // This is much cleaner now!
        if (player.getInventory().getItemInMainHand().getType() == Material.AIR) {
            AttributesUtil.applySpeed(player, 1.25); // Apply a 25% speed increase
        } else {
            AttributesUtil.removeSpeed(player); // Remove the speed increase
        }
    }

    // --- Passive 2: 10% Chance to Drop Non-Hotbar Item ---
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }
        if (!(ArmorManager.getArmorSet(attacker) instanceof BanditArmorSet)) {
            return;
        }
        if (!(event.getEntity() instanceof Player target)) {
            return;
        }

        if (random.nextInt(100) < 10) {
            PlayerInventory targetInv = target.getInventory();
            for (int i = 9; i < targetInv.getSize(); i++) {
                ItemStack item = targetInv.getItem(i);
                if (item != null && item.getType() != Material.AIR) {
                    target.getWorld().dropItemNaturally(target.getLocation(), item);
                    targetInv.setItem(i, new ItemStack(Material.AIR));
                    target.sendMessage("§c§l(!) §cYou feel your pockets getting lighter!");
                    attacker.sendMessage("§a§l(!) §aA piece of loot was dropped!");
                    break;
                }
            }
        }
    }

    @Override
    public void triggerAbility(Player player) {
        int currentDodges = playerDodges.getOrDefault(player.getUniqueId(), MAX_DODGES);

        if (currentDodges > 0) {
            currentDodges--;
            playerDodges.put(player.getUniqueId(), currentDodges);

            // Check if player is out of dodges for ui
            CooldownBarUtil.setLevel(player, currentDodges);
            CooldownBarUtil.startMiniCooldownBar(CustomArmorSets.getInstance(), player, DODGE_COOLDOWN);

            // Get the normalized direction
            Vector direction = player.getLocation().getDirection().normalize();

            // Multiply horizontal components (X and Z) by 1.5 for a strong dash
            double x = direction.getX() * 1.5;
            double z = direction.getZ() * 1.5;

            // Multiply the vertical component (Y) by a smaller factor (e.g., 0.5)
            // to prevent excessive upward flying
            double y = direction.getY() * 0.75;

            // Reconstruct the vector and apply it
            Vector velocity = new Vector(x, y, z);
            player.setVelocity(velocity);

            player.playSound(player.getLocation(), Sound.ENTITY_HORSE_JUMP, 1.0f, 2.0f);
            player.sendMessage("§a§l(!) §aYou dodged! Dodges left: " + currentDodges);

            // Start the regen task if it's not already running.
            if (!regenTasks.containsKey(player.getUniqueId()) || regenTasks.get(player.getUniqueId()).isCancelled()) {
                startDodgeRegenTask(player);
            }
        } else {
            player.sendMessage("§c§l(!) §cYou have no dodges left!");
        }
    }

    private void startDodgeRegenTask(Player player) {
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                int currentDodges = playerDodges.getOrDefault(player.getUniqueId(), 0);
                if (currentDodges < MAX_DODGES) {
                    currentDodges++;
                    playerDodges.put(player.getUniqueId(), currentDodges);
                    player.sendMessage("§a§l(!) §aYou regenerated a dodge! Dodges left: " + currentDodges);
                    CooldownBarUtil.setLevel(player, currentDodges);
                }

                // Cancel the task once the player has max dodges again.
                if (currentDodges >= MAX_DODGES) {
                    this.cancel();
                    regenTasks.remove(player.getUniqueId());
                }
            }
        }.runTaskTimer(CustomArmorSets.getInstance(), 20 * DODGE_COOLDOWN, 20 * DODGE_COOLDOWN);

        regenTasks.put(player.getUniqueId(), task);
    }
}
