package me.remag501.customarmorsets.ArmorSets;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.UUID;

public class SnowmanArmorSet extends ArmorSet implements Listener {

//    private final HashMap<UUID, Long> cooldowns = new HashMap<>();
    private long cooldown;

    @Override
    public String getId() {
        return "snowman";
    }

    @Override
    public void applyPassive(Player player) {
        player.sendMessage("✅ You equipped the snowman set");
    }

    @Override
    public void removePassive(Player player) {
        player.sendMessage("❌ You removed the snowman set");
        // Cleanup handled externally
        EntityDamageByEntityEvent.getHandlerList().unregister(this);
    }

    @Override
    public void triggerAbility(Player player) {
        UUID uuid = player.getUniqueId();
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

        // Check if on ground and not jumping or flying
        if (player.isOnGround() && !player.isFlying()) {
            Vector dir = player.getLocation().getDirection();
            Vector slippery = new Vector(dir.getX() * 0.15, 0, dir.getZ() * 0.15);
            player.setVelocity(player.getVelocity().add(slippery));
        }
    }
}
