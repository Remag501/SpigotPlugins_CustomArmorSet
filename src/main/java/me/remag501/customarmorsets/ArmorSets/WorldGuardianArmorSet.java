package me.remag501.customarmorsets.ArmorSets;

import me.remag501.customarmorsets.Core.ArmorSet;
import me.remag501.customarmorsets.Core.ArmorSetType;
import me.remag501.customarmorsets.Core.CustomArmorSetsCore;
import me.remag501.customarmorsets.Utils.CooldownBarUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WorldGuardianArmorSet extends ArmorSet implements Listener {

    private static final Map<UUID, Long> abilityCooldowns = new HashMap<>();
    private static final long COOLDOWN = 25 * 1000;

    private double prevSpeed;
    private boolean isInvulnerable = false;

    public WorldGuardianArmorSet() {
        super(ArmorSetType.WORLD_GUARDIAN);
    }

    @Override
    public void applyPassive(Player player) {
        AttributeInstance health = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(health.getBaseValue() * 1.5);
        }
        AttributeInstance speed = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speed != null) {
            prevSpeed = speed.getValue();
            speed.setBaseValue(speed.getValue() * 0.85);
        }
        player.sendMessage("You equipped the World Guardian set");
    }

    @Override
    public void removePassive(Player player) {
        AttributeInstance health = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(health.getDefaultValue());
        }
        AttributeInstance speed = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(prevSpeed);
        }
        player.sendMessage("You removed the World Guardian set");
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

        isInvulnerable = true;

        Plugin plugin = Bukkit.getPluginManager().getPlugin("CustomArmorSets");
        CooldownBarUtil.startCooldownBar(plugin, player, 3);
        new BukkitRunnable() {
            @Override
            public void run() {
                isInvulnerable = false;
                CooldownBarUtil.startCooldownBar(plugin, player, (int)(COOLDOWN / 1000));
                abilityCooldowns.put(uuid, now);
            }
        }.runTaskLater(plugin, 60L);

        player.sendMessage("§aYou are invulnerable for 3 seconds!");
    }

    @EventHandler
    public void onEntityDamageEvent(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ArmorSet set = CustomArmorSetsCore.getArmorSet(player);
        if (!(set instanceof WorldGuardianArmorSet armorSet)) return;

        if (armorSet.isInvulnerable == true) {
            player.sendMessage("reached");
            event.setCancelled(true);
        }


    }
}
