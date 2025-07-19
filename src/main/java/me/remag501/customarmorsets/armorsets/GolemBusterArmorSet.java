package me.remag501.customarmorsets.armorsets;

import me.remag501.customarmorsets.core.ArmorSet;
import me.remag501.customarmorsets.core.ArmorSetType;
import me.remag501.customarmorsets.core.CustomArmorSetsCore;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

public class GolemBusterArmorSet extends ArmorSet implements Listener {

    public GolemBusterArmorSet() {
        super(ArmorSetType.GOLEM_BUSTER);
    }

    @Override
    public void applyPassive(Player player) {
        player.sendMessage("✅ You equipped the Golem Buster set");
    }

    @Override
    public void removePassive(Player player) {
        player.sendMessage("❌ You removed the Golem Buster set");
    }

    @Override
    public void triggerAbility(Player player) {
        // F to fire battery gun (beam or particles w/ damage)
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1f, 1f);
        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5);
        player.sendMessage("§eBattery gun discharged!");
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(CustomArmorSetsCore.getArmorSet(player) instanceof GolemBusterArmorSet)) return;

        if (event.getEntity() instanceof Monster) {
            event.setDamage(event.getDamage() * 0.8); // Reduced damage from mobs
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
//        if (!(event.getEntity().getKiller())) return;
        Player player = event.getEntity().getKiller();
        if (player == null) return;
        if (!(CustomArmorSetsCore.getArmorSet(player) instanceof GolemBusterArmorSet)) return;

        // Add battery energy (placeholder)
        player.sendMessage("§6Battery +1 from kill!");
    }
}
