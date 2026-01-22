package me.remag501.customarmorsets.armor.impl;

import me.remag501.customarmorsets.armor.ArmorSet;
import me.remag501.customarmorsets.armor.ArmorSetType;
import me.remag501.customarmorsets.util.CooldownBarUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

import static me.remag501.customarmorsets.util.LookEntitiesUtil.getNearestEntityInSight;

public class DevoidArmorSet extends ArmorSet implements Listener {

    private static final Map<UUID, Long> abilityCooldowns = new HashMap<>();
    private static final long COOLDOWN = 10 * 1000;

    public DevoidArmorSet() {
        super(ArmorSetType.DEVOID);
    }

    @Override
    public void applyPassive(Player player) {
//        player.sendMessage("You equipped the Devoid set");
    }

    @Override
    public void removePassive(Player player) {
//        player.sendMessage("You removed the Devoid set");
    }

    private static class KineticData {
        long tickCreated;
        Vector lastVelocity;

        public KineticData(long tickCreated, Vector lastVelocity) {
            this.tickCreated = tickCreated;
            this.lastVelocity = lastVelocity;
        }
    }

    private final Map<UUID, KineticData> velocityMap = new HashMap<>();

    @Override
    public void triggerAbility(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (abilityCooldowns.containsKey(uuid) && now - abilityCooldowns.get(uuid) < COOLDOWN) {
            long timeLeft = (COOLDOWN - (now - abilityCooldowns.get(uuid))) / 1000;
            player.sendMessage("§c§l(!) §cAbility is on cooldown for " + timeLeft + " more seconds!");
            return;
        }

        boolean isSneaking = player.isSneaking();
        Plugin plugin = Bukkit.getPluginManager().getPlugin("CustomArmorSets");

        CooldownBarUtil.startCooldownBar(plugin, player, 1);

        new BukkitRunnable() {
            int ticks = 0;
            Set<LivingEntity> targets = new HashSet<>();

            @Override
            public void run() {
                if (ticks <= 6) {
                    LivingEntity target = getNearestEntityInSight(player, isSneaking ? 15 : 25);
                    if (target != null) {
                        targets.add(target);
                        // Track the target with the tick they were affected
                        velocityMap.put(target.getUniqueId(), new KineticData(ticks, target.getVelocity()));
                    }
                } else if (ticks >= 20) {
                    CooldownBarUtil.startCooldownBar(plugin, player, (int) (COOLDOWN / 1000));
                    abilityCooldowns.put(uuid, now);
                    cancel();
                }

                for (LivingEntity target : targets) {
                    if (target == null || !target.isValid()) continue;

                    Location playerLoc = player.getLocation();
                    Location targetLoc = target.getLocation();
                    double distance = playerLoc.distance(targetLoc);

                    Vector direction;
                    double strength;
                    if (isSneaking) {
                        // Pull
                        strength = Math.min(1.5, 0.1 * distance);
                        direction = playerLoc.toVector().subtract(targetLoc.toVector()).normalize().multiply(strength);
                    } else {
                        // Push
                        strength = Math.max(0.5, 2.5 - 0.1 * distance);
                        direction = targetLoc.toVector().subtract(playerLoc.toVector()).normalize().multiply(strength);
                    }

                    double yDiff = playerLoc.getY() - targetLoc.getY();
                    direction.setY(Math.max(0.05, 0.2 + yDiff * (isSneaking ? 0.15 : 0.1)));

                    target.setVelocity(direction);
                }

                ticks += 2;
            }
        }.runTaskTimer(plugin, 0, 2L);

        // Run kinetic damage checker every tick
        new BukkitRunnable() {
            @Override
            public void run() {
                Iterator<Map.Entry<UUID, KineticData>> iterator = velocityMap.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<UUID, KineticData> entry = iterator.next();
                    LivingEntity target = Bukkit.getEntity(entry.getKey()) instanceof LivingEntity le ? le : null;
                    if (target == null || !target.isValid()) {
                        iterator.remove();
                        continue;
                    }

                    Vector currentVelocity = target.getVelocity();
                    Vector previousVelocity = entry.getValue().lastVelocity;
                    double speedDelta = previousVelocity.length() - currentVelocity.length();

                    // Wait at least 3 ticks (6L with 2L intervals)
                    if (System.currentTimeMillis() - entry.getValue().tickCreated * 50L < 150L) continue;

                    if (speedDelta > 0.6 && currentVelocity.length() < 0.1) {
                        double damage = Math.min(10.0, speedDelta * 5.0);
                        target.damage(damage);
                        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation(), 10);
                        iterator.remove();
                    } else {
                        entry.getValue().lastVelocity = currentVelocity;
                    }
                }

                if (velocityMap.isEmpty()) {
                    cancel(); // Stop this task when no more tracked entities
                }
            }
        }.runTaskTimer(plugin, 0, 1L);

        player.sendMessage(isSneaking ? "§a§l(!) §aYou pulled enemies!" : "§a§l(!) §aYou pushed enemies!");
    }

}
