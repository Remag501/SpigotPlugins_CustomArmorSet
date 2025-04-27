package me.remag501.customarmorsets.Utils;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownBarUtil {

    private static final Map<UUID, Integer> originalLevels = new HashMap<>();
    private static final Map<UUID, Float> originalXp = new HashMap<>();
    private static final Map<UUID, BossBar> activeBossBars = new HashMap<>();

    /**
     * Starts a cooldown display using both XP bar and BossBar.
     *
     * @param plugin Your plugin instance
     * @param player The player to show cooldown to
     * @param seconds Cooldown time in seconds
//     * @param barColor The BossBar color
     */
    public static void startCooldownBar(Plugin plugin, Player player, int seconds) {
        if (seconds <= 0) return;

        UUID uuid = player.getUniqueId();

        // Save original XP and level if not already tracked
        if (!originalLevels.containsKey(uuid)) {
            originalLevels.put(uuid, player.getLevel());
            originalXp.put(uuid, player.getExp());
        }

        // Create a BossBar for extra visual feedback
//        BossBar bossBar = Bukkit.createBossBar("Cooldown...", barColor, BarStyle.SEGMENTED_10);
//        bossBar.addPlayer(player);
//        activeBossBars.put(uuid, bossBar);

        final int totalTicks = seconds * 20;
        final long startTime = System.currentTimeMillis();

        new BukkitRunnable() {
            int ticksLeft = totalTicks;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cleanup(player);
                    cancel();
                    return;
                }

                long elapsedMillis = System.currentTimeMillis() - startTime;
                double progress = 1.0 - (elapsedMillis / (seconds * 1000.0));
                progress = Math.max(0.0, Math.min(1.0, progress)); // Clamp 0.0 - 1.0

                // Update XP bar
                player.setExp((float) progress);

                // Update XP level to remaining seconds
                int secondsLeft = (int) Math.ceil((seconds * 1000.0 - elapsedMillis) / 1000.0);
                player.setLevel(Math.max(secondsLeft, 0));

                // Update BossBar
//                bossBar.setProgress(progress);

                // When done
                if (progress <= 0.0) {
                    flashXpBar(plugin, player);
                    cleanup(player);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 2L); // updates every 2 ticks (~0.1s)
    }

    private static void flashXpBar(Plugin plugin, Player player) {
        UUID uuid = player.getUniqueId();

        new BukkitRunnable() {
            int flashes = 0;
            boolean on = false;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                if (flashes >= 6) { // Flash 3 times (on + off = 2 ticks)
                    restorePlayerBar(player);
                    cancel();
                    return;
                }

                if (on) {
                    player.setExp(1.0f); // Full bar
                } else {
                    player.setExp(0.0f); // Empty bar
                }
                on = !on;
                flashes++;
            }
        }.runTaskTimer(plugin, 0L, 5L); // Flash every 5 ticks (~0.25s)
    }

    private static void cleanup(Player player) {
        UUID uuid = player.getUniqueId();

        // Remove BossBar
//        BossBar bar = activeBossBars.remove(uuid);
//        if (bar != null) {
//            bar.removeAll();
//        }
    }

    private static void restorePlayerBar(Player player) {
        UUID uuid = player.getUniqueId();

        if (originalLevels.containsKey(uuid)) {
            player.setLevel(originalLevels.remove(uuid));
        } else {
            player.setLevel(0);
        }

        if (originalXp.containsKey(uuid)) {
            player.setExp(originalXp.remove(uuid));
        } else {
            player.setExp(0);
        }
    }
}
