package me.remag501.customarmorsets.Core;

import me.remag501.customarmorsets.ArmorSets.*;

import org.bukkit.Color;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public enum ArmorSetType {
    SNOWMAN("snowman", "Snow Man", Color.WHITE,
            "http://textures.minecraft.net/texture/bf96f13c7fb55b00a172ded93c12419c912188700389ef366d43eb3c107aab71",
            SnowmanArmorSet::new,
            Arrays.asList("", ""),
            new int[]{1, 1, 1, 1},
            new int[]{55, 80, 70, 65},
            new int[]{0, 0, 0, 0}),
    INFERNUS("infernus", "Infernus", Color.ORANGE,
            "http://textures.minecraft.net/texture/9ceee19eb02fda233b468e5462f591fb1f1288dacb70cc37cd073c5ac6120cdb",
            InfernusArmorSet::new,
            Arrays.asList("§6Ability: §ePress F to shoot a fire trail", "§6Passive: §eFire resistance, trail of fire"),
            new int[]{1, 8, 6, 2},
            new int[]{55, 528, 495, 195},
            new int[]{0, 2, 2, 0}),
    ROYAL_KNIGHT("royal_knight", "Royal Knight", Color.GRAY,
            "http://textures.minecraft.net/texture/e2941b8b71abe79ce12775aee601fec9126dee730e2a57257a784231de6da848",
            RoyalKnightArmorSet::new,
            Arrays.asList("§6Ability: §ePress F to heal for 3 hearts", "§6Passive: §e+50% HP, -15% weapon damage"),
            new int[]{2, 7, 6, 2},
            new int[]{70, 495, 460, 210},
            new int[]{1, 2, 2, 0}),

    LAST_SPARTAN("last_spartan", "Last Spartan", Color.RED,
            "http://textures.minecraft.net/texture/f517fbca9751798d6200d1a71b7af0aab0e96eb5f41ba97294198688f5a7127b",
            LastSpartanArmorSet::new,
            Arrays.asList("§6Ability: §ePress F to leap forward and kick", "§6Passive: §e+25% sword damage, 70% HP"),
            new int[]{2, 6, 5, 2},
            new int[]{65, 460, 430, 200},
            new int[]{1, 2, 1, 0}),

    VIKING_CAPTAIN("viking_captain", "Viking Captain", Color.MAROON,
            "http://textures.minecraft.net/texture/9772642ffccfc9e11b350c874f2c84678fc08044b51e7a8e3a0919f8f788ed9a",
            VikingCaptainArmorSet::new,
            Arrays.asList("§6Ability: §ePress F to throw your axe", "§6Passive: §e+20% axe damage, -20% sword damage"),
            new int[]{2, 7, 6, 2},
            new int[]{70, 510, 480, 210},
            new int[]{1, 2, 2, 0}),

    BANDIT("bandit", "Bandit", Color.BLACK,
            "http://textures.minecraft.net/texture/73abc6192f1a559ed566e50fddf6a7b50c42cb0a15862091411487ace1d60ab8",
            BanditArmorSet::new,
            Arrays.asList("§6Ability: §ePress F to dodge attack", "§6Passive: §eIncreased sneak speed"),
            new int[]{1, 3, 2, 1},
            new int[]{45, 150, 130, 90},
            new int[]{0, 0, 0, 0}),

    DEVOID("devoid", "Devoid", Color.PURPLE,
            "http://textures.minecraft.net/texture/b8444224af2cd4a5d3909db772c7d0c95b1216c3ac019f4a0e7cdbb8f07f418f",
            DevoidArmorSet::new,
            Arrays.asList("§6Ability: §ePress F to teleport where you look", "§6Passive: §eShift+F pulls enemies, F pushes"),
            new int[]{2, 5, 4, 2},
            new int[]{60, 380, 350, 180},
            new int[]{1, 1, 1, 0}),

    WORLD_GUARDIAN("world_guardian", "World Guardian", Color.TEAL,
            "http://textures.minecraft.net/texture/4045651c083b9873386acc07b66c780d7dbc8319ddebab1d138ceaf09f497e27",
            WorldGuardianArmorSet::new,
            Arrays.asList("§6Ability: §ePress F to become invulnerable (3s)", "§6Passive: §e+150% HP, reduced speed"),
            new int[]{3, 8, 7, 3},
            new int[]{80, 600, 570, 250},
            new int[]{2, 3, 2, 1}),
    VAMPIRE("vampire", "Vampire", Color.MAROON,
            "http://textures.minecraft.net/texture/cbd36b1881aa4cd26ab403508437abe0e8d3728d0d8035e0280a4bfc7e53dc0f",
            VampireArmorSet::new,
            Arrays.asList("§6Ability: §eDrain HP or transform on kill", "§6Passive: §eHeal when attacking living entities"),
            new int[]{2, 5, 4, 2},
            new int[]{65, 310, 295, 225},
            new int[]{1, 2, 2, 1}),

    FISTER("fister", "Fister", Color.GRAY,
            "http://textures.minecraft.net/texture/fad8c8cf7ee1a2cb01a27f9a89b8fbd8b640fe4c5a1e34d2c6ff0fa02a21a7cf",
            FisterArmorSet::new,
            Arrays.asList("§6Ability: §eFist flurry", "§6Passive: §eInvulnerability, Overshield, Regen, No weapons"),
            new int[]{1, 4, 3, 1},
            new int[]{40, 210, 180, 150},
            new int[]{0, 1, 1, 0}),

    ARCHER("archer", "Archer", Color.LIME,
            "http://textures.minecraft.net/texture/9b09f4c9b2240c2c6fdfc0a6f1f0a7bc5de6a8e9d0e5cdde192774eef25f79d5",
            ArcherArmorSet::new,
            Arrays.asList("§6Ability: §eArrow slash and knockback", "§6Passive: §eSpeed, Half HP, 25% more bow damage"),
            new int[]{1, 3, 2, 1},
            new int[]{45, 160, 140, 120},
            new int[]{0, 0, 0, 0});

    private final String id;
    private final String displayName;
    private final Color leatherColor;
    private final String headUrl;
    private final Supplier<ArmorSet> constructor;// now no plugin
    private final List<String> lore;
    private final int[] armorPoints;
    private final int[] durability;
    private final int[] armorToughness;

    ArmorSetType(String id, String displayName, Color leatherColor, String headUrl,
                 Supplier<ArmorSet> constructor, List<String> lore, int[] armorPoints,
                 int[] durability, int[] armorToughness) {
        this.id = id;
        this.displayName = displayName;
        this.leatherColor = leatherColor;
        this.headUrl = headUrl;
        this.constructor = constructor;
        this.lore = lore; // or pass this in as usual
        this.armorPoints = armorPoints;
        this.durability = durability;
        this.armorToughness = armorToughness;
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

    public int[] getDurability() {
        return durability;
    }

    public int[] getArmorToughness() {
        return armorToughness;
    }

    public ArmorSet create() { return constructor.get(); }

    public static Optional<ArmorSetType> fromId(String id) {
        return Arrays.stream(values()).filter(s -> s.id.equalsIgnoreCase(id)).findFirst();
    }
}

