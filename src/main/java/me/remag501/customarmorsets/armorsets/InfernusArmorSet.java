package me.remag501.customarmorsets.armorsets;

import me.remag501.customarmorsets.core.ArmorSet;
import me.remag501.customarmorsets.core.ArmorSetType;
import me.remag501.customarmorsets.core.CustomArmorSetsCore;
import me.remag501.customarmorsets.utils.CooldownBarUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InfernusArmorSet extends ArmorSet implements Listener {


    private static final Map<UUID, Long> abilityCooldowns = new HashMap<>();
    private static final long COOLDOWN = 10 * 1000; // 10 seconds cooldown in milliseconds

    public InfernusArmorSet() {
        super(ArmorSetType.INFERNUS);
    }

    @Override
    public void applyPassive(Player player) {
        // Give player permanent fire resistance (infinite duration)
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, true, false));
//        player.sendMessage("✅ You equipped the Infernus set");
    }

    @Override
    public void removePassive(Player player) {
        // Remove fire resistance when unequipped
        player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
//        player.sendMessage("❌ You removed the Infernus set");
    }

    @Override
    public void triggerAbility(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (abilityCooldowns.containsKey(uuid) && now - abilityCooldowns.get(uuid) < COOLDOWN) {
            long timeLeft = (COOLDOWN - (now - abilityCooldowns.get(uuid))) / 1000;
            player.sendMessage("§c§l(!) §cAbility is on cooldown for " + timeLeft + " more seconds!");
            return;
        }

//        Vector direction = player.getLocation().getDirection().normalize();

        // Set up the 2 second active ability phase
        int activeDurationSeconds = 2;
        Plugin plugin = Bukkit.getPluginManager().getPlugin("CustomArmorSets");

        // Show bar during active ability
        CooldownBarUtil.startCooldownBar(plugin, player, activeDurationSeconds);
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= activeDurationSeconds * 20) { // 2 seconds (20 ticks at 0.1s per run)
                    cancel();

                    // After ability ends, start cooldown bar
                    int cooldownSeconds = (int) (COOLDOWN / 1000);
                    long now = System.currentTimeMillis();
                    abilityCooldowns.put(uuid, now);
                    CooldownBarUtil.startCooldownBar(plugin, player, cooldownSeconds);
                    return;
                }

                // Emit a trail of flame particles
                for (double i = 0; i <= 5; i += 0.5) {
//                    Vector point = direction.clone().multiply(i);
                    Vector point = player.getLocation().getDirection().normalize().multiply(i);
                    player.getWorld().spawnParticle(Particle.FLAME,
                            player.getLocation().add(point),
                            5, 0.2, 0.2, 0.2, 0);

                    // Damage entities nearby
                    for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
                        if (entity instanceof LivingEntity livingEntity && entity != player) {
                            if (entity.getLocation().distance(player.getLocation().add(point)) < 1.5) {
                                livingEntity.damage(4, player);
                                livingEntity.setFireTicks(160);
                            }
                        }
                    }
                }

                ticks++;
            }

        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("CustomArmorSets"), 0L, 2L); // Every 0.1 seconds
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        ArmorSet set = CustomArmorSetsCore.getArmorSet(player);
        if (!(set instanceof InfernusArmorSet)) return;

        // Only trigger if the player actually moved a block (not just rotated)
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Block blockBelow = player.getLocation().subtract(0, 1, 0).getBlock();
        Block blockAtFeet = player.getLocation().getBlock();

        // Only place fire if standing over solid block AND current block is air
        if (blockBelow.getType().isSolid() && blockAtFeet.getType() == Material.AIR) {
            blockAtFeet.setType(Material.FIRE);

            // Remove the fire after 3 seconds
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (blockAtFeet.getType() == Material.FIRE) {
                        blockAtFeet.setType(Material.AIR);
                    }
                }
            }.runTaskLater(Bukkit.getPluginManager().getPlugin("CustomArmorSets"), 60L);
        }
    }

}
