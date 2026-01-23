package me.remag501.customarmorsets.manager;

import me.remag501.customarmorsets.armor.TargetCategory;
import me.remag501.customarmorsets.armor.WeaponType;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DefenseStatsManager {

    // Reductions: UUID -> Category -> Multiplier (0.0 - 1.0)
    private final Map<UUID, Map<WeaponType, Float>> weaponReductions = new HashMap<>();
    private final Map<UUID, Map<TargetCategory, Float>> sourceReductions = new HashMap<>();

    // ---------- Weapon-based Reduction ----------
    public void setWeaponReduction(UUID player, float multiplier, WeaponType... types) {
        weaponReductions
                .computeIfAbsent(player, k -> new EnumMap<>(WeaponType.class));
        for (WeaponType type : types) {
            weaponReductions.get(player).put(type, multiplier);
        }
    }

    public float getWeaponReduction(UUID player, WeaponType type) {
        Map<WeaponType, Float> map = weaponReductions.get(player);
        if (map == null) return 1.0f;
        return map.getOrDefault(type, map.getOrDefault(WeaponType.ALL, 1.0f));
    }

    public void clearWeaponReduction(UUID player, WeaponType... types) {
        Map<WeaponType, Float> map = weaponReductions.get(player);
        if (map != null) {
            if (types.length == 0) map.clear();
            else for (WeaponType type : types) map.remove(type);
        }
    }

    public boolean hasWeaponReduction(UUID player, WeaponType type) {
        Map<WeaponType, Float> map = weaponReductions.get(player);
        return map != null && (map.containsKey(type) || map.containsKey(WeaponType.ALL));
    }

    // ---------- Source-based Reduction ----------
    public void setSourceReduction(UUID player, float multiplier, TargetCategory... categories) {
        sourceReductions
                .computeIfAbsent(player, k -> new EnumMap<>(TargetCategory.class));
        for (TargetCategory category : categories) {
            sourceReductions.get(player).put(category, multiplier);
        }
    }

    public float getSourceReduction(UUID player, TargetCategory category) {
        Map<TargetCategory, Float> map = sourceReductions.get(player);
        if (map == null) return 1.0f;

        Float mult = map.get(category);
        if (mult == null && category != TargetCategory.PLAYERS) {
            mult = map.get(TargetCategory.NON_PLAYER);
        }
        return mult != null ? mult : map.getOrDefault(TargetCategory.ALL, 1.0f);
    }

    public void clearSourceReduction(UUID player, TargetCategory... categories) {
        Map<TargetCategory, Float> map = sourceReductions.get(player);
        if (map != null) {
            if (categories.length == 0) map.clear();
            else for (TargetCategory category : categories) map.remove(category);
        }
    }

    public boolean hasSourceReduction(UUID player, TargetCategory category) {
        Map<TargetCategory, Float> map = sourceReductions.get(player);
        return map != null && (map.containsKey(category) || map.containsKey(TargetCategory.ALL));
    }

    // ---------- Clear All ----------
    public void clearAll(UUID player) {
        weaponReductions.remove(player);
        sourceReductions.remove(player);
    }
}
