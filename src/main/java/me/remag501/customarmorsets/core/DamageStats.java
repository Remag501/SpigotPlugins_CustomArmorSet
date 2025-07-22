package me.remag501.customarmorsets.core;

import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DamageStats {

    public enum WeaponType {
        SWORD, AXE, BOW, CROSSBOW, TRIDENT, MELEE_GENERIC, PROJECTILE_GENERIC
    }

    // PvE and PvP aware maps for each weapon type
    private static final Map<WeaponType, Map<UUID, Float>> pvpMultipliers = new EnumMap<>(WeaponType.class);
    private static final Map<WeaponType, Map<UUID, Float>> pveMultipliers = new EnumMap<>(WeaponType.class);
    private static final Map<UUID, Boolean> pveOld = new HashMap<>();

    static {
        for (WeaponType type : WeaponType.values()) {
            pvpMultipliers.put(type, new ConcurrentHashMap<>());
            pveMultipliers.put(type, new ConcurrentHashMap<>());
        }
    }

    // General setters
    public static void setPvPMultiplier(WeaponType type, Player player, float multiplier) {
        pvpMultipliers.get(type).put(player.getUniqueId(), multiplier);
    }

    public static void setPvEMultiplier(WeaponType type, Player player, float multiplier) {
        pveMultipliers.get(type).put(player.getUniqueId(), multiplier);
    }

    // Getters with default fallback of 1.0
    public static float getPvPMultiplier(WeaponType type, Player player) {
        return pvpMultipliers.get(type).getOrDefault(player.getUniqueId(), 1.0f);
    }

    public static float getPvEMultiplier(WeaponType type, Player player) {
        return pveMultipliers.get(type).getOrDefault(player.getUniqueId(), 1.0f);
    }

    // Clearers (e.g. when armor is unequipped)
    public static void clearPvPMultiplier(WeaponType type, Player player) {
        pvpMultipliers.get(type).remove(player.getUniqueId());
    }

    public static void clearPvEMultiplier(WeaponType type, Player player) {
        pveMultipliers.get(type).remove(player.getUniqueId());
    }

    // Full clear for player (on disconnect or armor set change)
    public static void clearAll(Player player) {
        UUID uuid = player.getUniqueId();
        for (WeaponType type : WeaponType.values()) {
            pvpMultipliers.get(type).remove(uuid);
            pveMultipliers.get(type).remove(uuid);
        }
        pveOld.remove(uuid);
    }

    // Check if player has multiplier
    public static boolean hasPvPMultiplier(WeaponType type, Player player) {
        return pvpMultipliers.get(type).containsKey(player.getUniqueId());
    }

    public static boolean hasPvEMultiplier(WeaponType type, Player player) {
        return pveMultipliers.get(type).containsKey(player.getUniqueId());
    }

    // Setup old combat
    public static void setOldCombat(Player player, boolean oldCombat) {
        pveOld.put(player.getUniqueId(), oldCombat);
    }

    public static boolean hasOldCombat(Player player) {
        return pveOld.getOrDefault(player.getUniqueId(), false);
    }
}
