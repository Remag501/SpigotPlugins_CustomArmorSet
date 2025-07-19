package me.remag501.customarmorsets.armorsets;

import me.remag501.customarmorsets.core.ArmorSet;
import me.remag501.customarmorsets.core.ArmorSetType;
import me.remag501.customarmorsets.core.CustomArmorSetsCore;
import me.remag501.customarmorsets.utils.AttributesUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class IcemanArmorSet extends ArmorSet implements Listener {

    public IcemanArmorSet() {
        super(ArmorSetType.ICEMAN);
    }

    @Override
    public void applyPassive(Player player) {
        AttributesUtil.applySpeed(player, 1.3);
        player.sendMessage("✅ You equipped the Iceman set");
    }

    @Override
    public void removePassive(Player player) {
        AttributesUtil.removeSpeed(player);
        player.sendMessage("❌ You removed the Iceman set");
    }

    @Override
    public void triggerAbility(Player player) {
        Location center = player.getLocation();
        double radius = 4.5;

        for (Entity e : player.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof LivingEntity mob && !e.equals(player)) {
                mob.damage(2.0);
                mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 2));
            }
        }

        player.getWorld().spawnParticle(Particle.SNOWFLAKE, center, 100, 2, 1, 2);
        player.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.8f);
        player.sendMessage("§bIce ring activated!");
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(CustomArmorSetsCore.getArmorSet(player) instanceof IcemanArmorSet)) return;

        if (event.getEntity() instanceof LivingEntity target) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 1));
            player.sendMessage("§bYou froze your target!");
        }
    }
}

