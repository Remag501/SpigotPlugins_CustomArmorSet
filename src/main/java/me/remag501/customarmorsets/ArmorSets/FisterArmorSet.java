package me.remag501.customarmorsets.ArmorSets;

import me.remag501.customarmorsets.Core.ArmorSet;
import me.remag501.customarmorsets.Core.ArmorSetType;
import me.remag501.customarmorsets.Utils.ArmorUtil;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.*;
//import org.bukkit.entity.;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.mcmonkey.sentinel.SentinelTrait;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FisterArmorSet extends ArmorSet implements Listener {

    private static Map<UUID, NPC> afterImagesOne = new HashMap<>();
    private static Map<UUID, NPC> afterImagesTwo = new HashMap<>();

    public FisterArmorSet() {
        super(ArmorSetType.FISTER);
    }

    public void applyPassive(Player player) {
        // Give player haste

        // Create npc after images
        NPC afterImageOne = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, player.getName());
        NPC afterImageTwo = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, player.getName());

        // Register Sentinel trait
//        afterImageOne.addTrait(SentinelTrait.class);
//        afterImageTwo.addTrait(SentinelTrait.class);

//        SentinelTrait sentinelOne = afterImageOne.getOrAddTrait(SentinelTrait.class);
//        SentinelTrait sentinelTwo = afterImageTwo.getOrAddTrait(SentinelTrait.class);
//
//        // Make them attack everything (monsters, animals, other players)
//        sentinelOne.addTarget("players");
//        sentinelOne.addTarget("monsters");
//        sentinelOne.addTarget("passivemobs");
////
////        // Avoid attacking their summoner
//        sentinelOne.addIgnore("player:" + player.getName());
////        sentinelTwo.addIgnore("player:" + player.getName());
////
////        // Optional behavior tuning
//        sentinelOne.invincible = true;
//        sentinelOne.speed = 0;
//        sentinelOne.accuracy = 0;
//        sentinelOne.attackRate = 2; // attacks every 1 second
////        sentinelOne.chaseRange = 20; // how far they chase targets
//        sentinelOne.range = 5;      // how far they can hit

        afterImagesOne.put(player.getUniqueId(), afterImageOne);
        afterImagesTwo.put(player.getUniqueId(), afterImageTwo);
    }

    @Override
    public void removePassive(Player player) {
        // TODO: Remove potion effects and any overshield
        NPC afterImageOne = afterImagesOne.remove(player.getUniqueId());
        NPC afterImageTwo = afterImagesTwo.remove(player.getUniqueId());
        afterImageOne.destroy();
        afterImageTwo.destroy();
    }

    @Override
    public void triggerAbility(Player player) {
        // TODO: Trigger burst punches (e.g., rapid melee attacks)
        // You might launch multiple punches via cooldowned task
    }

    @EventHandler
    public void playerAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (ArmorUtil.isFullArmorSet(player) != ArmorSetType.FISTER) return;

        if (player.getInventory().getItemInMainHand().getType() != Material.AIR) {
            event.setCancelled(true);
            return;
        }

        Entity target = event.getEntity();
        Location baseLoc = target.getLocation().add(0, 0.5, 0);

        NPC afterImageOne = afterImagesOne.get(player.getUniqueId());
        NPC afterImageTwo = afterImagesTwo.get(player.getUniqueId());

        Plugin plugin = Bukkit.getPluginManager().getPlugin("CustomArmorSets");

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick == 5 || tick == 10) {
                    Location offset = target.getLocation().add(randomOffset(player, target, tick == 5 ? 1 : -1));
                    NPC afterImage = tick == 5 ? afterImageOne : afterImageTwo;
                    spawnAfterImage(afterImage, offset, target);
                }
                if (tick == 15) {
                    afterImageOne.despawn();
                }
                if (tick == 20) {
                    afterImageTwo.despawn();
                    cancel();
                }

                tick += 5;
            }
        }.runTaskTimer(plugin, 0, 5);
    }

    private Vector randomOffset(Player player, Entity target, int side) {
        // Get direction from player to target on the XZ plane
        Vector toTarget = target.getLocation().toVector().subtract(player.getLocation().toVector());
        toTarget.setY(0).normalize();

        // Get the perpendicular vector (right direction)
        Vector right = new Vector(-toTarget.getZ(), 0, toTarget.getX()).normalize();

        // Scale left (-1) or right (+1) with some jitter
        Vector offset = right.multiply(0.8 * side); // ~0.8 blocks to each side
        offset.add(new Vector(
                (Math.random() - 0.5) * 0.2, // small XZ jitter
                0.4 + Math.random() * 0.2,    // vertical lift
                (Math.random() - 0.5) * 0.2
        ));

        return offset;
    }

    private void spawnAfterImage(NPC npc, Location location, Entity target) {
        npc.spawn(location);
        Entity npcEntity = npc.getEntity();
        npcEntity.setVelocity(new Vector(0, 0, 0)); // upward hop
        target.getWorld().spawnParticle(Particle.CRIT, npcEntity.getLocation(), 10);

        if (target instanceof LivingEntity victim) {
            victim.damage(3, npcEntity);
        }
    }


    @EventHandler
    public void playerInteract(PlayerInteractAtEntityEvent event) {
//        event.getPlayer().sendMessage("blocks when click on entity");

    }

    @EventHandler
    public void onSwing(PlayerAnimationEvent event) {
//        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;
//
//        Player player = event.getPlayer();
//        Entity target = getTargetedEntity(player, 4); // up to 4 blocks
//
//        if (target instanceof Boat) {
//            player.sendMessage("You swung at a boat!");
//        } else if (target instanceof Arrow) {
//            player.sendMessage("You swung at an arrow!");
//        }
    }

    private Entity getTargetedEntity(Player player, double maxDistance) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        World world = player.getWorld();

        // Get nearby entities within max range
        for (Entity entity : world.getNearbyEntities(eye, maxDistance, maxDistance, maxDistance)) {
            if (entity == player) continue;

            BoundingBox box = entity.getBoundingBox().expand(0.3); // expand slightly for hitbox margin
            for (double i = 0; i < maxDistance; i += 0.1) {
                Vector point = eye.toVector().add(direction.clone().multiply(i));
                if (box.contains(point)) {
                    return entity;
                }
            }
        }
        return null;
    }

}

