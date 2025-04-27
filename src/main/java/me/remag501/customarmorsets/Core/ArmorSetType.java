package me.remag501.customarmorsets.Core;

import me.remag501.customarmorsets.ArmorSets.SnowmanArmorSet;
import me.remag501.customarmorsets.ArmorSets.InfernusArmorSet;
import org.bukkit.Color;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public enum ArmorSetType {
    SNOWMAN("snowman", "Snow Man", Color.WHITE, "http://textures.minecraft.net/texture/bf96f13c7fb55b00a172ded93c12419c912188700389ef366d43eb3c107aab71", SnowmanArmorSet::new,
            Arrays.asList("I like men", "Dik"), new int[]{1, 1, 1, 1}),
    INFERNUS("infernus", "Infernus", Color.ORANGE, "http://textures.minecraft.net/texture/4d4b8c12f1c24d08cbea761f127fd5d0cf5a5b9b8dbf4cf0b6d2c01d5b1b9c49", InfernusArmorSet::new,
            Arrays.asList("§6Ability: §ePress F to shoot a fire trail", "§6Passive: §eFire resistance, trail of fire"),
            new int[]{2, 5, 4, 2});

    // FLAME(..., FlameArmorSet::new);

    private final String id;
    private final String displayName;
    private final Color leatherColor;
    private final String headUrl;
    private final Supplier<ArmorSet> constructor;// now no plugin

    private final List<String> lore;

    private final int[] armorPoints;

    ArmorSetType(String id, String displayName, Color leatherColor, String headUrl,
                 Supplier<ArmorSet> constructor, List<String> lore, int[] armorPoints) {
        this.id = id;
        this.displayName = displayName;
        this.leatherColor = leatherColor;
        this.headUrl = headUrl;
        this.constructor = constructor;
        this.lore = lore; // or pass this in as usual
        this.armorPoints = armorPoints;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Color getLeatherColor() { return leatherColor; }
    public String getHeadUrl() { return headUrl; }

    public List<String> getLore() {
        return lore;
    }

    public int[] getArmorPoints() {
        return this.armorPoints;
    }

    public ArmorSet create() { return constructor.get(); }

    public static Optional<ArmorSetType> fromId(String id) {
        return Arrays.stream(values()).filter(s -> s.id.equalsIgnoreCase(id)).findFirst();
    }
}

