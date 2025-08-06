package me.remag501.customarmorsets.core;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DamageStats {

    // ----- ENUMS -----
//    public enum WeaponType {
//        ALL,
//        SWORD,
//        AXE,
//        BOW,
//        CROSSBOW,
//        TRIDENT,
//        OTHER
//    }

//    public enum TargetCategory {
//        ALL,          // Applies to everything
//        PLAYERS,      // PvP
//        NON_PLAYER,   // Any non-player entity
//        UNDEAD,
//        ARTHROPOD,
//        ILLAGER,
//        BOSS,
//        GENERIC
//    }

    // ----- STORAGE -----
    private static final Map<UUID, Map<WeaponType, Float>> weaponMultipliers = new HashMap<>();
    private static final Map<UUID, Map<TargetCategory, Float>> mobMultipliers = new HashMap<>();

    // -----------------------
    // WEAPON MULTIPLIER LOGIC
    // -----------------------

    public static void setWeaponMultiplier(UUID player, float multiplier, WeaponType... types) {
        weaponMultipliers.putIfAbsent(player, new EnumMap<>(WeaponType.class));
        Map<WeaponType, Float> map = weaponMultipliers.get(player);
        for (WeaponType type : types) {
            map.put(type, multiplier);
        }
    }

    public static float getWeaponMultiplier(UUID player, WeaponType type) {
        Map<WeaponType, Float> map = weaponMultipliers.get(player);
        if (map == null) return 1.0f;

        // Exact type or fallback to ALL
        return map.getOrDefault(type, map.getOrDefault(WeaponType.ALL, 1.0f));
    }

    public static boolean hasWeaponMultiplier(UUID player, WeaponType type) {
        Map<WeaponType, Float> map = weaponMultipliers.get(player);
        return map != null && (map.containsKey(type) || map.containsKey(WeaponType.ALL));
    }

    public static void clearWeaponMultiplier(UUID player, WeaponType... types) {
        Map<WeaponType, Float> map = weaponMultipliers.get(player);
        if (map == null) return;
        for (WeaponType type : types) {
            map.remove(type);
        }
        if (map.isEmpty()) {
            weaponMultipliers.remove(player);
        }
    }

    // -----------------------
    // MOB (TARGET) MULTIPLIER LOGIC
    // -----------------------

    public static void setMobMultiplier(UUID player, float multiplier, TargetCategory... categories) {
        mobMultipliers.putIfAbsent(player, new EnumMap<>(TargetCategory.class));
        Map<TargetCategory, Float> map = mobMultipliers.get(player);
        for (TargetCategory category : categories) {
            map.put(category, multiplier);
        }
    }

    public static float getMobMultiplier(UUID player, TargetCategory category) {
        Map<TargetCategory, Float> map = mobMultipliers.get(player);
        if (map == null) return 1.0f;

        // Try exact category first
        Float multiplier = map.get(category);

        // If no exact match and NOT a player category, fallback to NON_PLAYER
        if (multiplier == null && category != TargetCategory.PLAYERS) {
            multiplier = map.get(TargetCategory.NON_PLAYER);
        }

        // Fallback to ALL or default 1.0
        if (multiplier == null) {
            multiplier = map.getOrDefault(TargetCategory.ALL, 1.0f);
        }

        return multiplier;
    }

    public static boolean hasMobMultiplier(UUID player, TargetCategory category) {
        Map<TargetCategory, Float> map = mobMultipliers.get(player);
        return map != null && (map.containsKey(category) || map.containsKey(TargetCategory.ALL));
    }

    public static void clearMobMultiplier(UUID player, TargetCategory... categories) {
        Map<TargetCategory, Float> map = mobMultipliers.get(player);
        if (map == null) return;
        for (TargetCategory category : categories) {
            map.remove(category);
        }
        if (map.isEmpty()) {
            mobMultipliers.remove(player);
        }
    }

    // -----------------------
    // UTILITY
    // -----------------------

    public static void clearAll(UUID player) {
        weaponMultipliers.remove(player);
        mobMultipliers.remove(player);
    }

    public static Map<WeaponType, Float> getWeaponMultipliers(UUID player) {
        return weaponMultipliers.getOrDefault(player, new EnumMap<>(WeaponType.class));
    }

    public static Map<TargetCategory, Float> getMobMultipliers(UUID player) {
        return mobMultipliers.getOrDefault(player, new EnumMap<>(TargetCategory.class));
    }
}
