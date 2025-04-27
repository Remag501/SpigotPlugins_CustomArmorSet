package me.remag501.customarmorsets.ArmorSets;

import me.remag501.customarmorsets.Core.ArmorSet;
import me.remag501.customarmorsets.Core.ArmorSetType;
import me.remag501.customarmorsets.Utils.CooldownBarUtil;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LastSpartanArmorSet extends ArmorSet {

    private static final Map<UUID, Long> abilityCooldowns = new HashMap<>();
    private static final long COOLDOWN = 12 * 1000;

    public LastSpartanArmorSet() {
        super(ArmorSetType.LAST_SPARTAN);
    }

    @Override
    public void applyPassive(Player player) {
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(14); // 70% of 20
        player.sendMessage("✅ You equipped the Last Spartan set");
    }

    @Override
    public void removePassive(Player player) {
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20);
        player.sendMessage("❌ You removed the Last Spartan set");
    }

    @Override
    public void triggerAbility(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (abilityCooldowns.containsKey(uuid) && now - abilityCooldowns.get(uuid) < COOLDOWN) {
            long timeLeft = (COOLDOWN - (now - abilityCooldowns.get(uuid))) / 1000;
            player.sendMessage("§cAbility is on cooldown for " + timeLeft + " more seconds!");
            return;
        }

        Entity nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Entity entity : player.getNearbyEntities(10, 10, 10)) {
            if (entity instanceof LivingEntity && entity != player) {
                double dist = player.getLocation().distanceSquared(entity.getLocation());
                if (dist < nearestDistance) {
                    nearest = entity;
                    nearestDistance = dist;
                }
            }
        }

        if (nearest != null) {
            Vector leap = nearest.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(1.5).setY(0.8);
            player.setVelocity(leap);

            nearest.setVelocity(new Vector(0, 1.5, 0));
        } else {
            player.sendMessage("§cNo enemies nearby to leap toward!");
        }

        abilityCooldowns.put(uuid, now);
        Plugin plugin = Bukkit.getPluginManager().getPlugin("CustomArmorSets");
        CooldownBarUtil.startCooldownBar(plugin, player, (int) (COOLDOWN / 1000));
    }
}
