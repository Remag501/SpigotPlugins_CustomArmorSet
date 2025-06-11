package me.remag501.customarmorsets.ArmorSets;

import me.remag501.customarmorsets.Core.ArmorSet;
import me.remag501.customarmorsets.Core.ArmorSetType;
import me.remag501.customarmorsets.Utils.ArmorUtil;
import me.remag501.customarmorsets.Utils.AttributesUtil;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
//import org.bukkit.entity.;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
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

    private static Map<UUID, Long> cooldowns = new HashMap<>();
    private static Map<UUID, Entity> dodging = new HashMap<>();

    private static final int COOLDOWN_TICKS = 2 * 20;

    public FisterArmorSet() {
        super(ArmorSetType.FISTER);
    }

    public void applyPassive(Player player) {
        // Give player attack speed
        AttributesUtil.applyAttackSpeed(player, 3.0);

        // Create npc after images
        NPC afterImageOne = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, player.getName());
        NPC afterImageTwo = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, player.getName());

        afterImagesOne.put(player.getUniqueId(), afterImageOne);
        afterImagesTwo.put(player.getUniqueId(), afterImageTwo);
    }

    @Override
    public void removePassive(Player player) {
        // Remove player attack speed
        AttributesUtil.removeAttackSpeed(player);

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
        if (event.getEntity() instanceof Player damaged && dodging.getOrDefault(damaged.getUniqueId(), null) == event.getDamager()) {
            event.setCancelled(true);
            // Subtitle feedback
            String message = ChatColor.GRAY + "You dodged the hit!";
            damaged.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
            // Sound effect
            damaged.getWorld().playSound(damaged.getLocation(), Sound.ENTITY_IRON_GOLEM_REPAIR, 1f, 1.2f);
            // Particle effect
            damaged.getWorld().spawnParticle(Particle.SWEEP_ATTACK, damaged.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0);
            return;
        }
        if (!(event.getDamager() instanceof Player player)) return;
        // Check if damaged entity is invulerable

        // Check if player is wearing armor and apply after image passive
        if (ArmorUtil.isFullArmorSet(player) != ArmorSetType.FISTER) return;

        if (player.getInventory().getItemInMainHand().getType() != Material.AIR) {
            event.setCancelled(true);
            return;
        }

        Entity target = event.getEntity();

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
        Vector offset = right.multiply(1.2 * side); // ~0.8 blocks to each side
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

        // Rotate the NPC to look at the target
        npc.faceLocation(target.getLocation());

        // Optional: deal damage
        if (target instanceof LivingEntity victim) {
            victim.damage(3, npcEntity);
        }
    }


    @EventHandler
    public void playerInteract(PlayerInteractAtEntityEvent event) {
        // Filter out off-hand interactions
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        if (ArmorUtil.isFullArmorSet(player) != ArmorSetType.FISTER) return;

        Entity clicked = event.getRightClicked();

        // Handle npc case
        for (NPC npc : afterImagesOne.values()) {
            if (npc.isSpawned() && npc.getEntity().getUniqueId().equals(clicked.getUniqueId())) {
                swapLocation(player, npc);
                return;
            }
        }

        for (NPC npc : afterImagesTwo.values()) {
            if (npc.isSpawned() && npc.getEntity().getUniqueId().equals(clicked.getUniqueId())) {
                swapLocation(player, npc);
                return;
            }
        }

        // Allow blocks, check if block is on cooldown
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (cooldowns.containsKey(uuid) && now < cooldowns.get(uuid)) {
            // Subtitle feedback
            long remaining = (cooldowns.get(uuid) - now) / 1000;
            String message = ChatColor.RED + "You have " + remaining + " seconds before you can block again.";
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
            return;
        }
        // Player is invulnerable for half a second
        dodging.put(uuid, clicked);
        new BukkitRunnable() {
            @Override
            public void run() {
                dodging.remove(uuid);
                cancel();
            }
        }.runTaskLater(Bukkit.getPluginManager().getPlugin("CustomArmorSets"), 10);
        cooldowns.put(uuid, now + COOLDOWN_TICKS * 50);

    }

    private void swapLocation(Player player, NPC npc) {
        Location oldLocation = player.getLocation();
        player.teleport(npc.getEntity().getLocation());
        npc.teleport(oldLocation, PlayerTeleportEvent.TeleportCause.UNKNOWN);
    }

    @EventHandler
    public void onSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;

        Player player = event.getPlayer();
        Entity target = getTargetedEntity(player, 4);

        if (target instanceof Arrow arrow) {
            arrow.remove();
            Location loc = arrow.getLocation();

            loc.getWorld().playSound(loc, Sound.ENTITY_ITEM_BREAK, 1.0f, 1.2f);

            loc.getWorld().spawnParticle(Particle.ITEM_CRACK, loc, 10, 0.1, 0.1, 0.1, new ItemStack(Material.ARROW));
            player.sendMessage("Arrow shattered!");
        }
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

