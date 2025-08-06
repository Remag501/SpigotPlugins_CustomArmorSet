package me.remag501.customarmorsets.armorsets;

import com.github.retrooper.packetevents.protocol.particle.type.ParticleType;
import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.*;
import me.libraryaddict.disguise.disguisetypes.watchers.AreaEffectCloudWatcher;
import me.libraryaddict.disguise.disguisetypes.watchers.ArmorStandWatcher;
import me.libraryaddict.disguise.disguisetypes.watchers.FallingBlockWatcher;
import me.libraryaddict.disguise.disguisetypes.watchers.PlayerWatcher;
import me.remag501.customarmorsets.CustomArmorSets;
import me.remag501.customarmorsets.core.*;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class SnowmanArmorSet extends ArmorSet implements Listener {

//    private final HashMap<UUID, Long> cooldowns = new HashMap<>();
    private long cooldown;

    public SnowmanArmorSet() {
        super(ArmorSetType.SNOWMAN);
    }

    @Override
    public void applyPassive(Player player) {
        player.sendMessage("✅ You equipped the snowman set");
        DamageStats.setMobMultiplier(player.getUniqueId(),2, TargetCategory.ALL);
        DamageStats.setMobMultiplier(player.getUniqueId(),1, TargetCategory.UNDEAD);
//        DamageStats.setWeaponMultiplier(player.getUniqueId(),1.5f, DamageStats.WeaponType.OTHER);
    }

    @Override
    public void removePassive(Player player) {
        DamageStats.clearAll(player.getUniqueId());
        player.sendMessage("❌ You removed the snowman set");
    }

    public void triggerAbility(Player player) {


    }

    @EventHandler
    public void onEntityHit(EntityDamageByEntityEvent event) {
//        if (!(event.getDamager() instanceof Player player)) return;
//        if (!(event.getEntity() instanceof LivingEntity target)) return;
//        if (event.getEntity() instanceof ArmorStand) {
//            event.getEntity().playEffect(EntityEffect.TOTEM_RESURRECT);
//            CustomArmorSets.getInstance().getLogger().info("Somthing");
//            event.setCancelled(true);
//        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (true)
            return;

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
