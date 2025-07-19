package me.remag501.customarmorsets.core;

import me.remag501.customarmorsets.armorsets.*;

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
    VAMPIRE("vampire", "Vampire", Color.PURPLE,
            "http://textures.minecraft.net/texture/560ee6543960fe2538e1d412b55edcec2dc0f34637fa2fb6d34e60660aecf95b",
            VampireArmorSet::new,
            Arrays.asList("§6Ability: §eDrain HP or transform on kill", "§6Passive: §eHeal when attacking living entities"),
            new int[]{2, 5, 4, 2},
            new int[]{65, 310, 295, 225},
            new int[]{1, 2, 2, 1}),

    FISTER("fister", "Fister", Color.YELLOW,
            "http://textures.minecraft.net/texture/74c76d07417fb8cd3dcaed4ded98537131658c996ae2e5536bddd9aef752804c",
            FisterArmorSet::new,
            Arrays.asList("§6Ability: §eFist flurry", "§6Passive: §eInvulnerability, Overshield, Regen, No weapons"),
            new int[]{1, 4, 3, 1},
            new int[]{40, 210, 180, 150},
            new int[]{0, 1, 1, 0}),

    ARCHER("archer", "Archer", Color.LIME,
            "http://textures.minecraft.net/texture/368d5d2eb8d5d01ecb88325e6b56d87d0b9be41721f128f89ba5f130aae89fa",
            ArcherArmorSet::new,
            Arrays.asList("§6Ability: §eArrow slash and knockback", "§6Passive: §eSpeed, Half HP, 25% more bow damage"),
            new int[]{1, 3, 2, 1},
            new int[]{45, 160, 140, 120},
            new int[]{0, 0, 0, 0}),
    NECROMANCER("necromancer", "Necromancer", Color.PURPLE,
            "http://textures.minecraft.net/texture/30d5d1d1c0a7de4fef5b6cf32c27e9f378279b11c372ce4cd0112a742556fc",
            NecromancerArmorSet::new,
            Arrays.asList(
                    "§6Ability: §ePress F to revive mythic mobs, Ctrl+F to control them",
                    "§6Passive: §eResurrect yourself every two minutes"
            ),
            new int[]{1, 3, 3, 1},
            new int[]{50, 190, 190, 130},
            new int[]{1, 1, 0, 0}),

    ICEMAN("iceman", "Iceman", Color.AQUA,
            "http://textures.minecraft.net/texture/4d7c1651a7853e9bb96126b57ecae3f926c9ff29c0f9fbb5ff5e9f7740ebc7",
            IcemanArmorSet::new,
            Arrays.asList(
                    "§6Ability: §ePress F to summon ice ring around you dealing DoT to mobs",
                    "§6Passive: §eFreeze mobs you hit, Faster movement"
            ),
            new int[]{1, 3, 2, 1},
            new int[]{45, 170, 150, 120},
            new int[]{0, 0, 0, 1}),

    GOLEM_BUSTER("golem_buster", "Golem Buster", Color.ORANGE,
            "http://textures.minecraft.net/texture/b69cfce1b1b7ac08a176bc16dfd9fc3d3707398f72319b45e8970bc64a510e",
            GolemBusterArmorSet::new,
            Arrays.asList(
                    "§6Ability: §eF to use battery gun, Ctrl+F to transform into a golem (F to stun)",
                    "§6Passive: §eReduced damage to mobs and projectiles, Gain battery on kills"
            ),
            new int[]{2, 4, 3, 1},
            new int[]{55, 200, 180, 130},
            new int[]{1, 1, 0, 0});

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

