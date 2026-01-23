package me.remag501.customarmorsets.manager;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class CooldownBarManager {

    private final Map<UUID, Integer> originalLevels = new HashMap<>();
    private final Map<UUID, Float> originalXp = new HashMap<>();
    private final Map<UUID, Boolean> inUse = new HashMap<>();
    private final Map<UUID, BukkitTask> cooldownTasks = new HashMap<>();
    private final Map<UUID, Double> currentCooldownDurations = new HashMap<>();
    private final Map<UUID, Double> currentCooldownRemaining = new HashMap<>();
    private final Map<UUID, Queue<Double>> cooldownQueues = new HashMap<>();

    private final Plugin plugin;

    public CooldownBarManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void startCooldownBar(Player player, int seconds) {
        if (seconds <= 0) return;

        UUID uuid = player.getUniqueId();

        // Save original XP and level if not already tracked
        if (!originalLevels.containsKey(uuid)) {
            originalLevels.put(uuid, player.getLevel());
            originalXp.put(uuid, player.getExp());
        }

        final int totalTicks = seconds * 20;
        final long startTime = System.currentTimeMillis();

        inUse.put(player.getUniqueId(), true);

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

                // When done
                if (progress <= 0.0) {
                    inUse.put(player.getUniqueId(), false);
                    flashXpBar(plugin, player);
                    cleanup(player);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 2L); // updates every 2 ticks (~0.1s)
    }

    public void startMiniCooldownBar(Player player, double seconds) {
        if (seconds <= 0) return;

        UUID uuid = player.getUniqueId();

        // Save original XP and level if not already being tracked.
        if (!originalXp.containsKey(uuid)) {
            originalXp.put(uuid, player.getExp());
            originalLevels.put(uuid, player.getLevel());
        }

        // Get or create the cooldown queue for the player.
        Queue<Double> queue = cooldownQueues.computeIfAbsent(uuid, k -> new LinkedList<>());
        queue.add(seconds);

        // If a cooldown task is already running, we're done. The existing task will handle the new queue item.
        if (cooldownTasks.containsKey(uuid)) {
            return;
        }

        // Start a new BukkitRunnable to manage the XP bar display.
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                // Check if the player is still online. If not, clean up and cancel.
                if (!player.isOnline()) {
                    cleanup(player);
                    cooldownTasks.remove(uuid).cancel();
                    return;
                }

                // Get the current cooldown from the queue.
                Double currentCooldown = queue.peek();

                // If the queue is empty, the cooldown is finished.
                if (currentCooldown == null) {
                    player.setExp(0);
                    cleanup(player);
                    cooldownTasks.remove(uuid).cancel();
                    return;
                }

                // If this is the start of a new cooldown, set the duration.
                if (!currentCooldownDurations.containsKey(uuid)) {
                    currentCooldownDurations.put(uuid, currentCooldown);
                    currentCooldownRemaining.put(uuid, currentCooldown);
                }

                // Decrement the remaining time by 0.1 seconds (2 ticks).
                double remaining = currentCooldownRemaining.get(uuid) - 0.1;
                currentCooldownRemaining.put(uuid, remaining);

                // Calculate XP bar progress based on the current cooldown.
                double duration = currentCooldownDurations.get(uuid);
                double progress = remaining / duration;

                player.setExp((float) Math.min(1.0, 1 - Math.min(1.0, progress)));

                // If the current cooldown is finished, poll the queue to move to the next one.
                if (remaining <= 0.0) {
                    queue.poll();
                    currentCooldownDurations.remove(uuid);
                    currentCooldownRemaining.remove(uuid);
                }
            }
        }.runTaskTimer(plugin, 0L, 2L); // updates every 2 ticks (~0.1s)

        // Store the task so we can check if it's already running.
        cooldownTasks.put(uuid, task);
    }

    private void flashXpBar(Player player) {
        UUID uuid = player.getUniqueId();
        if (inUse.get(uuid))
            return;

        new BukkitRunnable() {
            int flashes = 0;
            boolean on = false;

            @Override
            public void run() {
                if (!player.isOnline() || inUse.get(uuid)) {
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

    private void cleanup(Player player) {
        UUID uuid = player.getUniqueId();
    }

    public void restorePlayerBar(Player player) {
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

    public void setLevel(Player player, int level) {
        UUID uuid = player.getUniqueId();
        if (!originalXp.containsKey(uuid)) {
            originalXp.put(uuid, player.getExp());
            originalLevels.put(uuid, player.getLevel());
        }
        player.setLevel(level);
        player.setExp(0);
    }
}
