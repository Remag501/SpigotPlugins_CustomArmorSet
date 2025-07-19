package me.remag501.customarmorsets.armorsets;

import me.remag501.customarmorsets.core.ArmorSet;
import me.remag501.customarmorsets.core.ArmorSetType;
import me.remag501.customarmorsets.core.CustomArmorSetsCore;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NecromancerArmorSet extends ArmorSet implements Listener {

    private static final Map<UUID, Long> resurrectionCooldowns = new HashMap<>();
    private static final long RESURRECTION_COOLDOWN = 120 * 1000; // 2 minutes

    public NecromancerArmorSet() {
        super(ArmorSetType.NECROMANCER);
    }

    @Override
    public void applyPassive(Player player) {
        player.sendMessage("✅ You equipped the Necromancer set");
    }

    @Override
    public void removePassive(Player player) {
        player.sendMessage("❌ You removed the Necromancer set");
    }

    @Override
    public void triggerAbility(Player player) {
        // Placeholder for reviving mythic mobs nearby
        player.sendMessage("§dYou summon the dead to fight again!");
        player.getWorld().spawnParticle(Particle.SOUL, player.getLocation(), 20, 1, 1, 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!(CustomArmorSetsCore.getArmorSet(player) instanceof NecromancerArmorSet)) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (resurrectionCooldowns.containsKey(uuid) && now - resurrectionCooldowns.get(uuid) < RESURRECTION_COOLDOWN) return;

        resurrectionCooldowns.put(uuid, now);

        // Cancel death
//        event.s(true); // Not going to work anyways
        event.getEntity().setHealth(10);
        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation(), 30, 1, 1, 1);
        player.sendMessage("§dYou resurrected through necrotic power!");
    }
}