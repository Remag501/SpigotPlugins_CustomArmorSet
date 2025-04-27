package me.remag501.customarmorsets.ArmorSets;

import me.remag501.customarmorsets.Core.ArmorSet;
import me.remag501.customarmorsets.Core.ArmorSetType;
import me.remag501.customarmorsets.Utils.CooldownBarUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VikingCaptainArmorSet extends ArmorSet implements Listener {

    private static final Map<UUID, Long> abilityCooldowns = new HashMap<>();
    private static final long COOLDOWN = 10 * 1000; // 10 seconds cooldown

    public VikingCaptainArmorSet() {
        super(ArmorSetType.VIKING_CAPTAIN);
    }

    @Override
    public void applyPassive(Player player) {
        player.sendMessage("✅ You equipped the Viking Captain set");
        // Passive: 20% axe damage buff, -20% sword damage (needs manual handling in combat events)
    }

    @Override
    public void removePassive(Player player) {
        player.sendMessage("❌ You removed the Viking Captain set");
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

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand.getType().name().endsWith("_AXE")) {
            Item thrownAxe = player.getWorld().dropItem(player.getEyeLocation(), mainHand.clone());
            thrownAxe.setVelocity(player.getLocation().getDirection().multiply(1.5));
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            player.sendMessage("§eYou threw your axe!");
        } else {
            player.sendMessage("§cYou must hold an axe to use this ability!");
        }

        Plugin plugin = Bukkit.getPluginManager().getPlugin("CustomArmorSets");
        CooldownBarUtil.startCooldownBar(plugin, player, (int)(COOLDOWN / 1000));
        abilityCooldowns.put(uuid, now);
    }
}
