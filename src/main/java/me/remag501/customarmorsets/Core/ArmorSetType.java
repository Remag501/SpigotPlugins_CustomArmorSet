package me.remag501.customarmorsets.Core;

import me.remag501.customarmorsets.ArmorSets.SnowmanArmorSet;
import org.bukkit.Color;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public enum ArmorSetType {
    SNOWMAN("snowman", "Snow Man", Color.WHITE, Material.SNOW_BLOCK, SnowmanArmorSet::new);
    // FLAME(..., FlameArmorSet::new);

    private final String id;
    private final String displayName;
    private final Color leatherColor;
    private final Material headMaterial;
    private final Supplier<ArmorSet> constructor;// now no plugin
    private final List<String> lore;

    ArmorSetType(String id, String displayName, Color leatherColor, Material headMaterial,
                 Supplier<ArmorSet> constructor) {
        this.id = id;
        this.displayName = displayName;
        this.leatherColor = leatherColor;
        this.headMaterial = headMaterial;
        this.constructor = constructor;
        this.lore = List.of(); // or pass this in as usual
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Color getLeatherColor() { return leatherColor; }
    public Material getHeadMaterial() { return headMaterial; }
    public ArmorSet create() { return constructor.get(); }

    public static Optional<ArmorSetType> fromId(String id) {
        return Arrays.stream(values()).filter(s -> s.id.equalsIgnoreCase(id)).findFirst();
    }
}

