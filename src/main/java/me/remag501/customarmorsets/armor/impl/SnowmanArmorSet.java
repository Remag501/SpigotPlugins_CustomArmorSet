package me.remag501.customarmorsets.armor.impl;


import me.remag501.customarmorsets.armor.ArmorSet;
import me.remag501.customarmorsets.armor.ArmorSetType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.util.Vector;

public class SnowmanArmorSet extends ArmorSet implements Listener {


    public SnowmanArmorSet() {
        super(ArmorSetType.SNOWMAN);
    }

    @Override
    public void applyPassive(Player player) {

    }

    @Override
    public void removePassive(Player player) {

    }

    @Override
    public void triggerAbility(Player player) {
        if (player.isOnGround()) {
            player.sendMessage("On ground");
            player.setVelocity(player.getLocation().getDirection().normalize().add(new Vector(0,0.5,0)).multiply(1.5));
        } else if (player.isGliding()) {
            player.sendMessage("you are fly man " + player.getVelocity());

            // 1. Get the direction the player is looking
            Vector lookDir = player.getLocation().getDirection();

        // 2. Get their current momentum
            Vector currentVel = player.getVelocity();
            double currentSpeed = currentVel.length();

        // 3. Define our scaling variables
        // A 'base' speed so they don't get stuck if they are at 0 velocity
            double baseSpeed = 0.6;
        // How much we reward current speed (1.1 = 10% acceleration)
            double accelerationFactor = 2;
            // Fast down, slow up
            double yGravityBonus = lookDir.getY() < 0 ? 1.2 : 0.9;

        // A cap to prevent them from hitting "infinite" speed and crashing the server
            double maxSpeed = 100;

        // 4. Calculate the new velocity
        // We take the direction they look, and multiply it by a value
        // that is proportional to how fast they are already going.
            double newSpeed = Math.max(baseSpeed, currentSpeed * accelerationFactor * yGravityBonus);
            newSpeed = Math.min(newSpeed, maxSpeed); // Apply the speed cap

            Vector newVelocity = lookDir.multiply(newSpeed);

            // 5. Apply the velocity
            player.setVelocity(newVelocity);

            // Optional: Play a "whoosh" sound that gets higher pitched as they go faster
//            float pitch = (float) (0.5 + (newSpeed / maxSpeed));
//            player.playSound(player.getLocation(), Sound.ITEM_ELYTRA_FLYING, 0.5f, pitch);

        }
        else {
            player.sendMessage("Not on ground");
            player.setGliding(true);
        }
    }

    @EventHandler
    public void entityToggleEvent(EntityToggleGlideEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player player)) return;

        // Prevent manual toggling off while in mid-air (Force them to land or use ability)
        if (!event.isGliding() && !player.isOnGround()) {
            // If they try to stop gliding mid-air, check if we want to allow it or trigger shockwave
            // For this implementation, we allow the toggle but trigger the shockwave logic
            event.setCancelled(true);
            return;
        }
        triggerShockwave(player);
    }

    @EventHandler
    public void checkSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!player.isSneaking() && player.isGliding()) {
            player.sendMessage("hovering");
            player.setGliding(false);
            player.setGravity(false);
            player.setVelocity(new Vector(0, 0, 0));
        } else if(player.isSneaking() && !player.hasGravity()) {
            player.sendMessage("no hovering");
            player.setGliding(true);
            player.setGravity(true);
        } else if (!player.isSneaking()) {
            player.sendMessage("no sneak");
        }
    }

    /**
     * Triggers a shockwave based on the player's current velocity.
     * Scales damage and knockback based on how fast they were moving.
     */
    private void triggerShockwave(Player player) {
        Vector velocity = player.getVelocity();
        // Calculate speed (magnitude). Flying fast = higher speed.
        double speed = velocity.length();
        player.sendMessage(speed + "");

        // Threshold: Only trigger if they are moving at a reasonable pace
        if (speed < 0.5) return;

        // Scaling factors (Adjust these to balance your game)
        double range = 3.0 + (speed * 5.0); // Base 3 blocks + speed scaling
        double damage = speed * 15.0;       // Scale damage directly with speed
        double knockback = 0.5 + speed;      // Upward and outward force

        // Visuals: Explosion and Smoke
        player.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, player.getLocation(), 1);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 30, 0.5, 0.2, 0.5, 0.1);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);

        // Affect nearby entities
        for (Entity target : player.getNearbyEntities(range, range / 2, range)) {
            if (target instanceof LivingEntity living && target != player) {
                // Calculate direction from player to target for knockback
                Vector pushDir = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
                pushDir.setY(0.5); // Add a slight vertical "pop"
                pushDir.multiply(knockback);

                living.damage(damage, player);
                living.setVelocity(pushDir);

                // Visual effect on the target
                living.getWorld().spawnParticle(Particle.CRIT, living.getLocation(), 10);
            }
        }
    }

    @EventHandler
    public void onLand(EntityDamageEvent event) {
        // Optional: Trigger shockwave on impact even if they didn't "toggle" glide (e.g., hitting a wall)
        if (event.getEntity() instanceof Player player && player.isGliding()) {
            if (event.getCause() == EntityDamageEvent.DamageCause.FLY_INTO_WALL ||
                    event.getCause() == EntityDamageEvent.DamageCause.FALL) {

                triggerShockwave(player);
                // Optional: Reduce or cancel landing damage if they perform a successful shockwave
                event.setDamage(event.getDamage() * 0.5);
            }
        }
    }

}
