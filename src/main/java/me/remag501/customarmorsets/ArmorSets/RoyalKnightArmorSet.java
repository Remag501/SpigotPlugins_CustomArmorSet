package me.remag501.customarmorsets.ArmorSets;

import me.remag501.customarmorsets.Core.ArmorSet;
import me.remag501.customarmorsets.Core.ArmorSetType;
import me.remag501.customarmorsets.Utils.CooldownBarUtil;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RoyalKnightArmorSet extends ArmorSet implements Listener {

    private static final Map<UUID, Long> abilityCooldowns = new HashMap<>();
    private static final long COOLDOWN = 15 * 1000; // 15 seconds cooldown

    public RoyalKnightArmorSet() {
        super(ArmorSetType.ROYAL_KNIGHT);
    }

    @Override
    public void applyPassive(Player player) {
        // 125% max hp
        AttributeInstance health = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(health.getBaseValue() * 1.25);
        }
        // -15% weapon damage (we could add Weakness effect)
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, Integer.MAX_VALUE, 0, true, false));
        player.sendMessage("✅ You equipped the Royal Knight set");
    }

    @Override
    public void removePassive(Player player) {
        AttributeInstance health = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(health.getDefaultValue());
        }
        player.removePotionEffect(PotionEffectType.WEAKNESS);
        player.sendMessage("❌ You removed the Royal Knight set");
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

        // Heal 3 hearts (6 health)
        player.setHealth(Math.min(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(), player.getHealth() + 6));
        player.sendMessage("§aYou used Royal Knight's Healing!");

        Plugin plugin = Bukkit.getPluginManager().getPlugin("CustomArmorSets");
        CooldownBarUtil.startCooldownBar(plugin, player, (int)(COOLDOWN / 1000));
        abilityCooldowns.put(uuid, now);
    }
}
