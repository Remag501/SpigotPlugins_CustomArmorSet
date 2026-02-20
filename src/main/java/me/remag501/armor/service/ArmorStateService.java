package me.remag501.armor.service;

import me.remag501.armor.armor.ArmorSet;
import me.remag501.armor.armor.ArmorSetType;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ArmorStateService {
    private final Map<UUID, ArmorSet> equippedArmor = new HashMap<>();
    private final Map<UUID, ArmorSetType> equippedHelmetTypes = new HashMap<>();

    public void setState(UUID uuid, ArmorSet set, ArmorSetType type) {
        if (set == null) {
            equippedArmor.remove(uuid);
            equippedHelmetTypes.remove(uuid);
        } else {
            equippedArmor.put(uuid, set);
            equippedHelmetTypes.put(uuid, type);
        }
    }

    public ArmorSet getActiveSet(UUID uuid) {
        return equippedArmor.get(uuid);
    }

    public ArmorSetType getActiveType(UUID uuid) {
        return equippedHelmetTypes.get(uuid);
    }

    public boolean isWearing(UUID uuid, ArmorSetType type) {
        return equippedHelmetTypes.get(uuid) == type;
    }

    public Map<UUID, ArmorSet> getEquippedArmorMap() {
        return equippedArmor;
    }
}