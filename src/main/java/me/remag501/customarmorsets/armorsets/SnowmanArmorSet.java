package me.remag501.customarmorsets.armorsets;

import me.remag501.customarmorsets.core.ArmorSet;
import me.remag501.customarmorsets.core.ArmorSetType;
import me.remag501.customarmorsets.core.CustomArmorSetsCore;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SnowmanArmorSet extends ArmorSet implements Listener {

//    private final HashMap<UUID, Long> cooldowns = new HashMap<>();
    private long cooldown;

    public SnowmanArmorSet() {
        super(ArmorSetType.SNOWMAN);
    }

    @Override
    public void applyPassive(Player player) {
        player.sendMessage("✅ You equipped the snowman set");
    }


    @Override
    public void removePassive(Player player) {
        player.sendMessage("❌ You removed the snowman set");
    }

    @Override
    public void triggerAbility(Player player) {
        long now = System.currentTimeMillis();

        if (now - cooldown < 5000) {
            player.sendMessage("⏳ Ability on cooldown!");
            return;
        }

        Snowball snowball = player.launchProjectile(Snowball.class);
        snowball.setShooter(player);
        snowball.setVelocity(player.getLocation().getDirection().multiply(1.5));
        player.sendMessage("❄️ Snowball launched!");
        cooldown = now;
    }

    @EventHandler
    public void onEntityHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        ArmorSet set = CustomArmorSetsCore.getArmorSet(player);
        if (!(set instanceof SnowmanArmorSet)) return;

        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 1)); // 3 seconds of Slowness II
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        ArmorSet set = CustomArmorSetsCore.getArmorSet(player);
        if (!(set instanceof SnowmanArmorSet)) return;

        Location from = event.getFrom();
        Location to = event.getTo();

        // Ignore head turns
        if (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ()) return;

        // Leave a snowy trail!
        Location blockBelow = player.getLocation().clone().subtract(0, 1, 0);
        if (blockBelow.getBlock().getType().isAir()) return;

        // Only on solid ground
        if (!player.isOnGround()) return;

        // Make trail block (snow layer or frosted ice)
        Material oldType = blockBelow.getBlock().getType();
        blockBelow.getBlock().setType(Material.SNOW_BLOCK);

        // Remove after 3 seconds
        Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("CustomArmorSets"), () -> {
            if (blockBelow.getBlock().getType() == Material.SNOW_BLOCK) {
                blockBelow.getBlock().setType(oldType);
            }
        }, 60L); // 60 ticks = 3 seconds

        // Speed boost while on snow
        if (blockBelow.getBlock().getType() == Material.SNOW_BLOCK) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 0, true, false));
            player.spawnParticle(Particle.SNOW_SHOVEL, player.getLocation(), 8, 0.2, 0.2, 0.2, 0.01);
        }
    }

}
