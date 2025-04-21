package me.remag501.customarmorsets.Core;

import me.remag501.customarmorsets.ArmorSets.SnowmanArmorSet;
import org.bukkit.Color;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public enum ArmorSetType {
    SNOWMAN("snowman", "Snow Man", Color.WHITE, "http://textures.minecraft.net/texture/bf96f13c7fb55b00a172ded93c12419c912188700389ef366d43eb3c107aab71", SnowmanArmorSet::new);
    // FLAME(..., FlameArmorSet::new);

    private final String id;
    private final String displayName;
    private final Color leatherColor;
    private final String headUrl;
    private final Supplier<ArmorSet> constructor;// now no plugin
    private final List<String> lore;

    ArmorSetType(String id, String displayName, Color leatherColor, String headUrl,
                 Supplier<ArmorSet> constructor) {
        this.id = id;
        this.displayName = displayName;
        this.leatherColor = leatherColor;
        this.headUrl = headUrl;
        this.constructor = constructor;
        this.lore = List.of(); // or pass this in as usual
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Color getLeatherColor() { return leatherColor; }
    public String getHeadUrl() { return headUrl; }
    public ArmorSet create() { return constructor.get(); }

    public static Optional<ArmorSetType> fromId(String id) {
        return Arrays.stream(values()).filter(s -> s.id.equalsIgnoreCase(id)).findFirst();
    }
}

