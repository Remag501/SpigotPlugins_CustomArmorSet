package me.remag501.customarmorsets.armorsets;

import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import me.libraryaddict.disguise.disguisetypes.PlayerDisguise;
import me.libraryaddict.disguise.disguisetypes.watchers.PlayerWatcher;
import me.remag501.customarmorsets.CustomArmorSets;
import me.remag501.customarmorsets.core.ArmorSet;
import me.remag501.customarmorsets.core.ArmorSetType;
import me.remag501.customarmorsets.core.CustomArmorSetsCore;
import org.bukkit.*;
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
    }


    @Override
    public void removePassive(Player player) {
        player.sendMessage("❌ You removed the snowman set");
    }

    public void triggerAbility(Player player) {
        Location location = player.getLocation();
        World world = location.getWorld();

        // 1. Spawn decoy (use ArmorStand disguised as player)
        ArmorStand decoy = (ArmorStand) world.spawnEntity(location, EntityType.ARMOR_STAND);
//        decoy.setVisible(false);
        decoy.setGravity(false);
        decoy.setMarker(false); // small hitbox
//        decoy.setInvulnerable(true);

        // Apply disguise (optional)
        PlayerDisguise disguise = new PlayerDisguise(player.getName());
        PlayerWatcher watcher = disguise.getWatcher();

        // Copy each armor slot
        watcher.setHelmet(player.getInventory().getHelmet());
        watcher.setChestplate(player.getInventory().getChestplate());
        watcher.setLeggings(player.getInventory().getLeggings());
        watcher.setBoots(player.getInventory().getBoots());

        // Optional: copy held item
        watcher.setItemInMainHand(player.getInventory().getItemInMainHand());
        watcher.setItemInOffHand(player.getInventory().getItemInOffHand());

        // Apply disguise
        DisguiseAPI.disguiseToAll(decoy, disguise);

        // 2. Run AI task
        BukkitRunnable task = new BukkitRunnable() {
            int lifetime = 200; // 10 seconds

            @Override
            public void run() {
                if (!decoy.isValid() || lifetime <= 0) {
                    decoy.remove();
                    cancel();
                    return;
                }

                for (Entity nearby : decoy.getNearbyEntities(20, 20, 20)) {
                    if (nearby instanceof Mob mob) {
                        if (mob.getTarget() == null || mob.getTarget().equals(player)) {
                            mob.setTarget(decoy); // force target only if free/targeting player
                        }
                    }
                }

                lifetime -= 20; // decrease lifetime
            }
        };
        task.runTaskTimer(CustomArmorSets.getInstance(), 0L, 20L); // check every second
    }


    @EventHandler
    public void onEntityHit(EntityDamageByEntityEvent event) {
//        if (!(event.getDamager() instanceof Player player)) return;
//        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (event.getEntity() instanceof ArmorStand) {
            event.getEntity().playEffect(EntityEffect.TOTEM_RESURRECT);
            CustomArmorSets.getInstance().getLogger().info("Somthing");
            event.setCancelled(true);
        }
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
