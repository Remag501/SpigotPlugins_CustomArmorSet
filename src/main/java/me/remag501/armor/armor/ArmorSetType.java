package me.remag501.armor.armor;

import org.bukkit.Color;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum ArmorSetType {
    SNOWMAN("snowman", "Snow Man", Color.WHITE.asRGB(), 3, 0,
            "http://textures.minecraft.net/texture/bf96f13c7fb55b00a172ded93c12419c912188700389ef366d43eb3c107aab71",
//            SnowmanArmorSet::new,
            Arrays.asList("", ""),
            new int[]{1, 1, 1, 1}, // Armor points
            new int[]{55, 80, 70, 65}, // Durability
            new int[]{0, 0, 0, 0}), // Toughness
    INFERNUS("infernus", "§6Infernus", 4, 7, 1008, null,
//            InfernusArmorSet::new,
            Arrays.asList("§2Flamethrower §a§lPRESS F",
                    "§8• §7Shoot a rapid burst of flames.",
                    "",
                    "§e§lPASSIVE",
                    "§8• §a🔥 Resistance, §aFire Trail"),
            new int[]{2, 8, 5, 3},
            new int[]{165, 528, 225, 429},
            new int[]{0, 2, 0, 2}),
    ROYAL_KNIGHT("royal_knight", "§bRoyal Knight", 3, Color.GRAY.asRGB(), 0,
            "http://textures.minecraft.net/texture/e2941b8b71abe79ce12775aee601fec9126dee730e2a57257a784231de6da848",
//            RoyalKnightArmorSet::new,
            Arrays.asList("§2Royal Regen §a§lPRESS F",
                    "§8• §7Channel the healing powers of the Royal Knight.",
                    "",
                    "§e§lPASSIVE",
                    "§8• §a+50% ❤ §c-15% ❁ DMG"),
            new int[]{2, 6, 5, 2},
            new int[]{165, 240, 225, 195},
            new int[]{0, 0, 0, 0}),

    LAST_SPARTAN("last_spartan", "§cLast Spartan", 3, Color.RED.asRGB(),0,
            "http://textures.minecraft.net/texture/f517fbca9751798d6200d1a71b7af0aab0e96eb5f41ba97294198688f5a7127b",
//            LastSpartanArmorSet::new,
            Arrays.asList("§2Spartan Kick §a§lPRESS F",
                    "§8• §7Leap forward and kick with the strength of Sparta.",
                    "",
                    "§e§lPASSIVE",
                    "§8• §a+25% ⚔ DMG §c-15% ❤"),
            new int[]{2, 6, 2, 1},
            new int[]{165, 240, 75, 65},
            new int[]{0, 0, 0, 0}),

    VIKING_CAPTAIN("viking_captain", "§aViking Captain", 3, Color.MAROON.asRGB(),0,
            "http://textures.minecraft.net/texture/9772642ffccfc9e11b350c874f2c84678fc08044b51e7a8e3a0919f8f788ed9a",
//            VikingCaptainArmorSet::new,
            Arrays.asList("§2Axe Throw §a§lPRESS F",
                    "§8• §7Throw your axe with pure might.",
                    "",
                    "§e§lPASSIVE",
                    "§8• §a+20% 🪓 DMG §c-20% ⚔ DMG"),
            new int[]{2, 6, 4, 1},
            new int[]{165, 240, 225, 65},
            new int[]{0, 0, 0, 0}),

    BANDIT("bandit", "§cBandit", 3, 6,1007, null,
//            BanditArmorSet::new,
            Arrays.asList("§2Quick Escape §a§lPRESS F",
                    "§8• §7Swiftly dash away from enemies.",
                    "",
                    "§e§lPASSIVE",
                    "§8• §a+25% ☁"),
            new int[]{2, 6, 2, 1},
            new int[]{165, 240, 75, 195},
            new int[]{0, 0, 0, 0}),

    DEVOID("devoid", "§dDevoid", 3, 5,1006, null,
//            DevoidArmorSet::new,
            Arrays.asList("§2Void Tether §a§lSHIFT F",
                    "§8• §7Pull in enemies with the power of the unknown.",
                    "",
                    "§2Void Repulse §a§lPRESS F",
                    "§8• §7Push away enemies with the force of the unknown."),
            new int[]{2, 8, 5, 1},
            new int[]{165, 528, 225, 195},
            new int[]{0, 2, 0, 0}),

    WORLD_GUARDIAN("world_guardian", "§bWorld Guardian", 4, 4,1005, null,
//            WorldGuardianArmorSet::new,
            Arrays.asList("§2Guardian's Protection §a§lPRESS F",
                    "§8• §7Absorb protection from the guardian and become invulnerable.",
                    "",
                    "§e§lPASSIVE",
                    "§8• §a+50% ❤ §c-50% ☁"),
            new int[]{3, 8, 7, 3},
            new int[]{363, 528, 495, 429},
            new int[]{2, 2, 2, 2}),

    VAMPIRE("vampire", "§4Vampire", 5, Color.fromRGB(3,1,7).asRGB(),2017, null,
//            VampireArmorSet::new,
            Arrays.asList("§2Blood Drain §a§lPRESS F",
                    "§8• §7Drain HP or gain regen/overshield on kill.",
                    "",
                    "§2Biohazard §a§lSHIFT F",
                    "§8• §7Morph into a swarm of bugs with a wither cloud.",
                    "",
                    "§eLifesteal",
                    "§8• §7Get HP back when dealing damage",
                    "",
                    "§e§lPASSIVE",
                    "§8• §c-50% ❤"),
            new int[]{3, 8, 7, 3},
            new int[]{390, 550, 505, 435},
            new int[]{2, 3, 2, 2}),

    FISTER("fister", "§eFister", 5, Color.fromRGB(2,1,7).asRGB(),1010, null,
//            FisterArmorSet::new,
            Arrays.asList("§2Pull Out Game §a§lPRESS F",
                    "§8• §7Meditate and ditch quickly.",
                    "",
                    "§eFast Fingers",
                    "§8• §7Break arrows shot at you.",
                    "",
                    "§eQuicky",
                    "§8• §7After images quickly appear for you to swap between.",
                    "",
                    "§eUnpenetrable",
                    "§8• §7Right clicking an enemy allows you to block time.",
                    "",
                    "§e§lPASSIVE",
                    "§8• §a\uD83D\uDDE1, only fists"),
            new int[]{3, 6, 5, 2},
            new int[]{363, 528, 495, 429},
            new int[]{1, 1, 1, 1}),

    ARCHER("archer", "§aArcher", 5, Color.fromRGB(4, 2, 7).asRGB(),2013, null,
//            ArcherArmorSet::new,
            Arrays.asList("§2Bowstep §a§lPRESS F",
                    "§8• §7Arrow slash & knockback, gain jump boost for a few seconds.",
                    "",
                    "§e§lPASSIVE",
                    "§8• §a+50% ☁, §c-50% ❤, §a+25% 🏹 DMG"),
            new int[]{3, 6, 6, 2},
            new int[]{370, 535, 495, 429},
            new int[]{1, 2, 2, 1}),

    NECROMANCER("necromancer", "§5Necromancer", 5, Color.fromRGB(2,0,7).asRGB(),1009, null,
//    NecromancerArmorSet::new,
            Arrays.asList("§2Reanimation §a§lPRESS F",
                    "§8• §7Bring back mobs you've killed to your side.",
                    "",
                    "§2Psychic Link §a§lSHIFT F",
                    "§8• §7Control the mobs you have brought back.",
                    "",
                    "§eUnkillable",
                    "§8• §7Resurrect yourself every 120 sec by taking over a summon.",
                    "",
                    "§e§lPASSIVE",
                    "§8• §a+25% ♱ DMG"),
            new int[]{2, 8, 6, 3},
            new int[]{375, 537, 500, 429},
            new int[]{1, 3, 2, 1}),


    ICEMAN("iceman", "§bIceman", 5, Color.fromRGB(6,1,4).asRGB(),2030, null,
//    IcemanArmorSet::new,
            Arrays.asList("§2Frostbite §a§lPRESS F",
                    "§8• §7Consume freeze charge to shoot an ice cloud freezing opponents.",
                    "",
                    "§2Snow Globe §a§lShift F",
                    "§8• §7Trap enemies in an ice dome healing you and damaging/freezing them.",
                    "",
                    "§eCold Feet",
                    "§8• §7Consume freeze charge to create an ice bridge when running.",
                    "",
                    "§eThaw",
                    "§8• §7Hit a frozen opponent with fire to deal extra damage.",
                    "",
                    "§eFreeze Mark",
                    "§8• §7Hitting an opponent applies a freeze charge.",
                    "",
                    "§e§lPASSIVE",
                    "§8• §a+25% ☁"),
            new int[]{3, 8, 6, 3},
            new int[]{385, 540, 500, 429},
            new int[]{2, 3, 2, 1}),

    GOLEM_BUSTER("golem_buster", "§fGolem Buster", 5, Color.fromRGB(4,1,7).asRGB(),2012, null,
//    GolemBusterArmorSet::new,
            Arrays.asList("§2Battery Gun §a§lPRESS F",
                    "§8• §7Shoot a strong electric pulse that damages enemies or stuns as a golem.",
                    "",
                    "§2Transformation §a§lPRESS CTRL F",
                    "§8• §7Embody the spirit of the golem and transform.",
                    "",
                    "§e§lPASSIVE",
                    "§8• §a\uD83D\uDDE1, +50% ☠ DMG, +25% ⛨ PVE DMG"),
            new int[]{3, 8, 7, 2},
            new int[]{415, 575, 525, 445},
            new int[]{2, 3, 2, 2});

    private final String id;
    private final String displayName;
    private final int rarity;
    private final int leatherColor;
    private final int customModelData;
    private final String headUrl;
//    private final Supplier<ArmorSet> constructor;// now no plugin
    private final List<String> lore;
    private final int[] armorPoints;
    private final int[] durability;
    private final int[] armorToughness;

    ArmorSetType(String id, String displayName, int rarity, int leatherColor, int customModelData, String headUrl,
                 List<String> lore, int[] armorPoints, int[] durability, int[] armorToughness) {
        this.id = id;
        this.displayName = displayName;
        this.rarity = rarity;
        this.leatherColor = leatherColor;
        this.customModelData = customModelData;
        this.headUrl = headUrl;
//        this.constructor = constructor;
        this.lore = lore; // or pass this in as usual
        this.armorPoints = armorPoints;
        this.durability = durability;
        this.armorToughness = armorToughness;
    }


    public int getRarity() {
        return rarity;
    }
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public int getLeatherColor() { return leatherColor; }
    public String getHeadUrl() { return headUrl; }
    public int getCustomModelData() {return customModelData; }
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

//    public ArmorSet create() { return constructor.get(); }

    public static Optional<ArmorSetType> fromId(String id) {
        return Arrays.stream(values()).filter(s -> s.id.equalsIgnoreCase(id)).findFirst();
    }
}

