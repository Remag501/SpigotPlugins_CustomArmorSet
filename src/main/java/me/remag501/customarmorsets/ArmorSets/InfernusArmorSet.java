package me.remag501.customarmorsets.ArmorSets;

import me.remag501.customarmorsets.Core.ArmorSet;
import me.remag501.customarmorsets.Core.ArmorSetType;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class InfernusArmorSet extends ArmorSet {

    public InfernusArmorSet() {
        super(ArmorSetType.INFERNUS);
    }

    @Override
    public void applyPassive(Player player) {
        // Give player permanent fire resistance (infinite duration)
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, true, false));

        // Optional: You can also spawn particles as a passive effect
        // This is often handled by a scheduled task outside of the ArmorSet class
    }

    @Override
    public void removePassive(Player player) {
        // Remove fire resistance when unequipped
        player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
    }

    @Override
    public void triggerAbility(Player player) {
        // Create a fire trail when player presses F (Swap hand key)

        Vector direction = player.getLocation().getDirection().normalize().multiply(0.5);

        for (int i = 0; i < 10; i++) { // Fire trail for 10 blocks
            player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(direction.clone().multiply(i)), 10, 0.2, 0.2, 0.2, 0.01);

            player.getWorld().spawnParticle(Particle.SMOKE_NORMAL, player.getLocation().add(direction.clone().multiply(i)), 5, 0.2, 0.2, 0.2, 0.01);
        }

        // Optional: Play a fiery sound
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);

        // Damage nearby enemies along the fire trail? (bonus feature)
        // Can add later if you want.
    }
}
