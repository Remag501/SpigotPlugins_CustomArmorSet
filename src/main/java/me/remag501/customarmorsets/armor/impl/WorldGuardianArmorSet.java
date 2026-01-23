package me.remag501.customarmorsets.armor.impl;

import me.remag501.customarmorsets.armor.ArmorSet;
import me.remag501.customarmorsets.armor.ArmorSetType;
import me.remag501.customarmorsets.manager.ArmorManager;
import me.remag501.customarmorsets.service.AttributesService;
import me.remag501.customarmorsets.manager.CooldownBarManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WorldGuardianArmorSet extends ArmorSet implements Listener {

    private static final Map<UUID, Long> abilityCooldowns = new HashMap<>();
    private static final long COOLDOWN = 25 * 1000;

    private boolean isInvulnerable = false;

    private final Plugin plugin;
    private final ArmorManager armorManager;
    private final CooldownBarManager cooldownBarManager;
    private final AttributesService attributesService;

    public WorldGuardianArmorSet(Plugin plugin, ArmorManager armorManager, CooldownBarManager cooldownBarManager, AttributesService attributesService) {
        super(ArmorSetType.WORLD_GUARDIAN);
        this.plugin = plugin;
        this.armorManager = armorManager;
        this.cooldownBarManager = cooldownBarManager;
        this.attributesService = attributesService;
    }

    @Override
    public void applyPassive(Player player) {
        attributesService.applyHealth(player, 1.5);
        attributesService.applySpeed(player, 0.8);
        player.sendMessage("You equipped the World Guardian set");

    }

    @Override
    public void removePassive(Player player) {
        attributesService.removeHealth(player);
        attributesService.removeSpeed(player);
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
        cooldownBarManager.startCooldownBar(player, 3);
        new BukkitRunnable() {
            @Override
            public void run() {
                isInvulnerable = false;
                cooldownBarManager.startCooldownBar(player, (int)(COOLDOWN / 1000));
                abilityCooldowns.put(uuid, now);
            }
        }.runTaskLater(plugin, 60L);

        player.sendMessage("§aYou are invulnerable for 3 seconds!");
    }

    @EventHandler
    public void onEntityDamageEvent(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ArmorSet set = armorManager.getArmorSet(player);
        if (!(set instanceof WorldGuardianArmorSet armorSet)) return;

        if (armorSet.isInvulnerable == true) {
            event.setCancelled(true);
        }


    }
}
