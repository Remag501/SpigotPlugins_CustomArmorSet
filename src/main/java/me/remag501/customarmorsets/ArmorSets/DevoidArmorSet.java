package me.remag501.customarmorsets.ArmorSets;

import me.remag501.customarmorsets.Core.ArmorSet;
import me.remag501.customarmorsets.Core.ArmorSetType;
import me.remag501.customarmorsets.Utils.CooldownBarUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DevoidArmorSet extends ArmorSet implements Listener {

    private static final Map<UUID, Long> abilityCooldowns = new HashMap<>();
    private static final long COOLDOWN = 15 * 1000;

    public DevoidArmorSet() {
        super(ArmorSetType.DEVOID);
    }

    @Override
    public void applyPassive(Player player) {
        player.sendMessage("✅ You equipped the Devoid set");
    }

    @Override
    public void removePassive(Player player) {
        player.sendMessage("❌ You removed the Devoid set");
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

        boolean isSneaking = player.isSneaking();
        for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
            if (entity instanceof Player target && !target.equals(player)) {
                Vector vector = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
                if (isSneaking) {
                    target.setVelocity(vector.multiply(-1.5)); // Pull
                } else {
                    target.setVelocity(vector.multiply(1.5)); // Push
                }
            }
        }

        Plugin plugin = Bukkit.getPluginManager().getPlugin("CustomArmorSets");
        CooldownBarUtil.startCooldownBar(plugin, player, (int)(COOLDOWN / 1000));
        abilityCooldowns.put(uuid, now);

        player.sendMessage(isSneaking ? "§bYou pulled enemies!" : "§bYou pushed enemies!");
    }
}
