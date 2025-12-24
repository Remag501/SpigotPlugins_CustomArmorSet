package me.remag501.customarmorsets.armorsets;

import me.remag501.customarmorsets.core.ArmorSet;
import me.remag501.customarmorsets.core.ArmorSetType;
import me.remag501.customarmorsets.core.CustomArmorSetsCore;
import me.remag501.customarmorsets.utils.AttributesUtil;
import me.remag501.customarmorsets.utils.CooldownBarUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RoyalKnightArmorSet extends ArmorSet implements Listener {

    private static final Map<UUID, Long> abilityCooldowns = new HashMap<>();
    private static final long COOLDOWN = 7 * 1000; // 7 seconds cooldown

    public RoyalKnightArmorSet() {
        super(ArmorSetType.ROYAL_KNIGHT);
    }

    @Override
    public void applyPassive(Player player) {
        // 125% max hp
        AttributesUtil.applyHealth(player, 1.25);
//        player.sendMessage("You equipped the Royal Knight set");
    }

    @Override
    public void removePassive(Player player) {
        AttributesUtil.removeHealth(player);
//        player.sendMessage("You removed the Royal Knight set");
    }

    @Override
    public void triggerAbility(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (abilityCooldowns.containsKey(uuid) && now - abilityCooldowns.get(uuid) < COOLDOWN) {
            long timeLeft = (COOLDOWN - (now - abilityCooldowns.get(uuid))) / 1000;
            player.sendMessage("§c§l(!) §cAbility is on cooldown for " + timeLeft + " more seconds!");
            return;
        }

        // Heal 3 hearts (6 health)
        player.setHealth(Math.min(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(), player.getHealth() + 6));
        player.sendMessage("§a§l(!) §aYou used Royal Knight's Healing!");

        Plugin plugin = Bukkit.getPluginManager().getPlugin("CustomArmorSets");
        CooldownBarUtil.startCooldownBar(plugin, player, (int)(COOLDOWN / 1000));
        abilityCooldowns.put(uuid, now);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Player player = null;

        // Case 1: Direct melee damage by player
        if (damager instanceof Player p) {
            player = p;

            Material weapon = player.getInventory().getItemInMainHand().getType();
            String name = weapon.name();
            // Reduce damage only for sword or axe
            if (!(name.endsWith("_SWORD") || name.endsWith("_AXE") || name.endsWith("TRIDENT"))) {
                return;
            }
        }

        // Case 2: Projectile damage (arrow, trident) shot by player
        else if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player p) {
            player = p;

            if (!(damager instanceof Arrow || damager instanceof Trident)) {
                return; // Only reduce for arrow/trident, not snowball/egg/etc.
            }
        }

        if (player == null) return;

        ArmorSet set = CustomArmorSetsCore.getArmorSet(player);
        if (!(set instanceof RoyalKnightArmorSet)) return;

        double originalDamage = event.getDamage();
        event.setDamage(originalDamage * 0.85); // Reduce outgoing damage by 15%
    }

}
