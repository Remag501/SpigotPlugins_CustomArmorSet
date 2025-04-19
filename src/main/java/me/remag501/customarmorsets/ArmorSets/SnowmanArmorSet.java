package me.remag501.customarmorsets.ArmorSets;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SnowmanArmorSet extends ArmorSet {
    @Override
    public String getId() { return "snowman"; }

    @Override
    public void applyPassive(Player player) {
        player.getInventory().setHelmet(new ItemStack(Material.SNOW_BLOCK));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, true, false));
    }

    @Override
    public void removePassive(Player player) {
        player.removePotionEffect(PotionEffectType.SPEED);
        // Restore helmet logic handled elsewhere (e.g., store original)
    }

    @Override
    public void triggerAbility(Player player) {
        Snowball snowball = player.launchProjectile(Snowball.class);
        snowball.setShooter(player);
        snowball.setVelocity(player.getLocation().getDirection().multiply(1.5));
    }
}
