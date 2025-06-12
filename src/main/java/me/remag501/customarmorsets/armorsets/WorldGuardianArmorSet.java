package me.remag501.customarmorsets.armorsets;

import me.remag501.customarmorsets.core.ArmorSet;
import me.remag501.customarmorsets.core.ArmorSetType;
import me.remag501.customarmorsets.core.CustomArmorSetsCore;
import me.remag501.customarmorsets.utils.AttributesUtil;
import me.remag501.customarmorsets.utils.CooldownBarUtil;
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

    private double prevSpeed;
    private boolean isInvulnerable = false;

    public WorldGuardianArmorSet() {
        super(ArmorSetType.WORLD_GUARDIAN);
    }

    @Override
    public void applyPassive(Player player) {
        AttributesUtil.applyHealth(player, 1.5);
        AttributesUtil.applySpeed(player, 0.8);
        player.sendMessage("You equipped the World Guardian set");

    }

    @Override
    public void removePassive(Player player) {
        AttributesUtil.removeHealth(player);
        AttributesUtil.removeSpeed(player);
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
            event.setCancelled(true);
        }


    }
}
