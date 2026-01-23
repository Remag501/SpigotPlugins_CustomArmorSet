package me.remag501.customarmorsets.listener;

import io.lumine.mythic.bukkit.events.MythicReloadedEvent;
import me.remag501.customarmorsets.CustomArmorSets;
import org.bukkit.entity.EntityType; // IMPORTANT: Add this import
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MythicMobsListener implements Listener {

    private final Plugin plugin;
    private final Yaml yaml;

    private static boolean isCurrentlyGenerating = false;

    private static final String RESURRECTED_PREFIX = "Resurrected_";

    private static final String GENERATED_MOBS_SUBDIR = "generated_mobs";
    private static final String GENERATED_SKILLS_SUBDIR = "generated_skills";

    public static String getPrefix() {
        return RESURRECTED_PREFIX;
    }

    // Regex for common hostile targeters that need to be made safe
    private static final Pattern PLAYER_TARGETER_PATTERN = Pattern.compile(
            "(@PlayersInRadius\\{[^}]*\\})|" +
                    "(@NearestPlayer(?:\\{[^}]*\\})?)|" +
                    "(@PlayerByName\\{[^}]*\\})|" +
                    "(@ThreatTablePlayers(?:\\{[^}]*\\})?)"
    );

    // Regex for generic entity targeters that should have 'ignore=players,PLAYER_ALLIES' added
    private static final Pattern GENERIC_ENTITY_TARGETER_PATTERN = Pattern.compile(
            "(@Target(?:\\{[^}]*\\})?)|" +
                    "(@LivingEntitiesInRadius\\{[^}]*\\})|" +
                    "(@EntitiesInRadius\\{[^}]*\\})"
    );

    public MythicMobsListener(Plugin plugin) {
        this.plugin = plugin;
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK); // Ensure block style for readability
        options.setPrettyFlow(true);
        options.setIndent(2);
        this.yaml = new Yaml(options);
    }

    @EventHandler
    public void onMythicMobsReload(MythicReloadedEvent event) {
        if (isCurrentlyGenerating) {
            return;
        }

        plugin.getLogger().info("MythicMobs has reloaded. Checking for mobs to generate friendly versions...");
        isCurrentlyGenerating = true;

        try {
            Path mythicMobsFolder = Paths.get(plugin.getDataFolder().getParentFile().getAbsolutePath(), "MythicMobs");
            Path originalMobsDir = mythicMobsFolder.resolve("Mobs");

            Path generatedMobsDir = Paths.get(plugin.getDataFolder().getAbsolutePath(), GENERATED_MOBS_SUBDIR);
            Path generatedSkillsDir = Paths.get(plugin.getDataFolder().getAbsolutePath(), GENERATED_SKILLS_SUBDIR);

            Files.createDirectories(generatedMobsDir);
            Files.createDirectories(generatedSkillsDir);

            Set<String> processedSkillFiles = new HashSet<>();

            if (!Files.exists(originalMobsDir)) {
                plugin.getLogger().warning("MythicMobs Mobs directory not found: " + originalMobsDir.toAbsolutePath() + ". Skipping generation.");
                return;
            }

            try (Stream<Path> paths = Files.walk(originalMobsDir)) {
                List<Path> mobYamlFiles = paths
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().toLowerCase().endsWith(".yml"))
                        .collect(Collectors.toList());

                for (Path mobFile : mobYamlFiles) {
                    processMobFile(mobFile, generatedMobsDir, generatedSkillsDir, processedSkillFiles);
                }
            }

            plugin.getLogger().info("-----------------------------------------------------");
            plugin.getLogger().info("Friendly MythicMobs YAMLs generated!");
            plugin.getLogger().info("Please move the generated files from:");
            plugin.getLogger().info("  " + generatedMobsDir.toAbsolutePath().toString());
            plugin.getLogger().info("  " + generatedSkillsDir.toAbsolutePath().toString());
            plugin.getLogger().info("TO their respective folders in the MythicMobs plugin directory:");
            plugin.getLogger().info("  plugins/MythicMobs/Mobs/");
            plugin.getLogger().info("  plugins/MythicMobs/Skills/");
            plugin.getLogger().info("After moving, run '/mm reload' to apply changes!");
            plugin.getLogger().info("-----------------------------------------------------");

        } catch (IOException e) {
            plugin.getLogger().severe("Error during MythicMobs YAML generation: " + e.getMessage());
            e.printStackTrace();
        } finally {
            isCurrentlyGenerating = false;
        }
    }

    private void processMobFile(Path mobFilePath, Path generatedMobsDir, Path generatedSkillsDir, Set<String> processedSkillFiles) throws IOException {
        Map<String, Object> fileContent;
        try (InputStream is = Files.newInputStream(mobFilePath)) {
            fileContent = yaml.load(is);
        }

        if (fileContent == null || fileContent.isEmpty()) {
            plugin.getLogger().warning("Mob YAML file is empty or malformed: " + mobFilePath.toAbsolutePath() + ". Skipping.");
            return;
        }

        for (Map.Entry<String, Object> entry : fileContent.entrySet()) {
            String originalMobName = entry.getKey();
            Map<String, Object> mobConfig = (Map<String, Object>) entry.getValue();

            if (originalMobName.startsWith(RESURRECTED_PREFIX) || originalMobName.startsWith("MMOCore_")) {
                plugin.getLogger().fine("Skipping processing of already-resurrected or ignored mob: " + originalMobName);
                continue;
            }

            plugin.getLogger().info("Processing mob definition: " + originalMobName + " from file: " + mobFilePath.getFileName());
            generateFriendlyMobDefinition(originalMobName, mobConfig, generatedMobsDir, generatedSkillsDir, processedSkillFiles);
        }
    }

    /**
     * Generates a friendly YAML file for a single mob definition.
     * This version intelligently handles the 'Type' field, inferring it from the name
     * if it's missing but matches a vanilla entity type.
     *
     * @param originalMobName   The original internal name of the mob (e.g., "SkeletalKnight").
     * @param mobConfig         The YAML configuration map for this specific mob.
     * @param generatedMobsDir  The directory to save generated mob YAMLs.
     * @param generatedSkillsDir The directory to save generated skill YAMLs.
     * @param processedSkillFiles A set to track skill files that have already been processed.
     * @throws IOException If there's an error writing files.
     */
    private void generateFriendlyMobDefinition(String originalMobName, Map<String, Object> mobConfig, Path generatedMobsDir, Path generatedSkillsDir, Set<String> processedSkillFiles) throws IOException {
        Map<String, Object> clonedMobConfig = new LinkedHashMap<>(mobConfig);

        // --- Crucial: Intelligently handle the 'Type' property ---
        Object originalType = mobConfig.get("Type");

        if (originalType == null) {
            // 'Type' is missing in the original YAML. Try to infer it from the mob's name.
            try {
                // Check if the original mob's name is a valid Bukkit EntityType (case-insensitive conversion to upper)
                EntityType inferredType = EntityType.valueOf(originalMobName.toUpperCase(Locale.ROOT));
                // If it's a valid entity type, use it.
                clonedMobConfig.put("Type", inferredType.name());
                plugin.getLogger().info("Inferred 'Type': " + inferredType.name() + " for mob: " + originalMobName);
            } catch (IllegalArgumentException e) {
                // If the name is not a valid Bukkit EntityType, it's a completely custom mob without 'Type'
                // Fallback to a default or log a more severe warning.
                plugin.getLogger().warning("Original mob '" + originalMobName + "' is missing 'Type' and its name is not a recognized vanilla entity type. Defaulting 'Type' to ZOMBIE.");
                clonedMobConfig.put("Type", "ZOMBIE"); // Fallback for truly custom mobs without a Type field
            }
        } else {
            // 'Type' was present in the original YAML, so just copy it directly.
            clonedMobConfig.put("Type", originalType);
        }

        // --- Ensure Health and Damage are present (copy from original or provide fallback) ---
        if (mobConfig.get("Health") != null) {
            clonedMobConfig.put("Health", mobConfig.get("Health"));
        } else {
            plugin.getLogger().warning("Original mob '" + originalMobName + "' is missing 'Health'. Defaulting to 20.");
            clonedMobConfig.put("Health", 20);
        }
        if (mobConfig.get("Damage") != null) {
            clonedMobConfig.put("Damage", mobConfig.get("Damage"));
        } else {
            plugin.getLogger().warning("Original mob '" + originalMobName + "' is missing 'Damage'. Defaulting to 5.");
            clonedMobConfig.put("Damage", 5);
        }


        // --- Now apply your specific modifications for friendly mobs ---
        clonedMobConfig.put("Faction", "PLAYER_ALLIES");

        List<String> aiTargetSelectors = new ArrayList<>();
        aiTargetSelectors.add("0 clear");
        aiTargetSelectors.add("1 owner_attackable_target");
        aiTargetSelectors.add("2 hurt_by_target");
        aiTargetSelectors.add("3 other_mobs");
        aiTargetSelectors.add("4 monsters");
        aiTargetSelectors.add("5 nearest_attackable_target");
        clonedMobConfig.put("AITargetSelectors", aiTargetSelectors);

        List<String> aiGoals = new ArrayList<>();
        aiGoals.add("0 owner_follow");
        aiGoals.add("1 attack");
        aiGoals.add("2 random_stroll");
        aiGoals.add("3 random_lookaround");
        clonedMobConfig.put("AIGoals", aiGoals);

        Map<String, Object> options = (Map<String, Object>) clonedMobConfig.getOrDefault("Options", new LinkedHashMap<>());
        options.put("PreventRandomDespawn", true);
        options.put("PreventOtherDrops", true);
        options.put("PreventLeashing", true);
        options.put("PreventBlockBreaking", true);
        options.put("PreventMobKillDrops", true);
        clonedMobConfig.put("Options", options);

        // --- Recursively process skills ---
        processSkillSection(clonedMobConfig, generatedSkillsDir, processedSkillFiles);

        // --- Save the new resurrected mob YAML to plugin's generated_mobs directory ---
        String newMobKey = RESURRECTED_PREFIX + originalMobName;
        Map<String, Object> newMobData = new LinkedHashMap<>();
        newMobData.put(newMobKey, clonedMobConfig);

        Path newMobPath = generatedMobsDir.resolve(newMobKey + ".yml");
        try (FileWriter writer = new FileWriter(newMobPath.toFile())) {
            yaml.dump(newMobData, writer);
            plugin.getLogger().info("Generated friendly mob YAML for: " + originalMobName + " to: " + newMobPath.toAbsolutePath());
        }
    }

    // (The rest of your class methods like processSkillSection and modifySkillLineTargeter remain the same)

    private void processSkillSection(Map<String, Object> configSection, Path generatedSkillsDir, Set<String> processedSkillFiles) throws IOException {
        List<Object> skills = (List<Object>) configSection.get("Skills");
        if (skills == null) {
            return;
        }

        List<Object> modifiedSkills = new ArrayList<>();
        for (Object skillEntry : skills) {
            if (skillEntry instanceof String) {
                String skillLine = (String) skillEntry;
                modifiedSkills.add(modifySkillLineTargeter(skillLine));
            } else if (skillEntry instanceof Map) {
                Map<String, Object> skillMap = (Map<String, Object>) skillEntry;

                if (skillMap.containsKey("skill")) {
                    Map<String, Object> skillRef = (Map<String, Object>) skillMap.get("skill");
                    String referencedSkillName = (String) skillRef.get("s");

                    if (referencedSkillName != null && !referencedSkillName.isEmpty()) {
                        if (processedSkillFiles.contains(referencedSkillName)) {
                            modifiedSkills.add(skillEntry);
                            continue;
                        }
                        processedSkillFiles.add(referencedSkillName);

                        String newReferencedSkillName = RESURRECTED_PREFIX + referencedSkillName;
                        Path originalSkillPath = Paths.get(plugin.getDataFolder().getParentFile().getAbsolutePath(), "MythicMobs", "Skills", referencedSkillName + ".yml");

                        if (!Files.exists(originalSkillPath)) {
                            plugin.getLogger().warning("Referenced skill YAML not found: " + originalSkillPath.toAbsolutePath() + ". Keeping original reference.");
                            modifiedSkills.add(skillEntry);
                            continue;
                        }

                        Map<String, Object> originalSkillData;
                        try (InputStream is = Files.newInputStream(originalSkillPath)) {
                            originalSkillData = yaml.load(is);
                        }

                        String skillFileKey = originalSkillData.keySet().iterator().next();
                        Map<String, Object> internalSkillConfig = (Map<String, Object>) originalSkillData.get(skillFileKey);

                        if (internalSkillConfig != null) {
                            processSkillSection(internalSkillConfig, generatedSkillsDir, processedSkillFiles);
                        }

                        Map<String, Object> newSkillData = new LinkedHashMap<>();
                        newSkillData.put(newReferencedSkillName, internalSkillConfig);
                        Path newSkillPath = generatedSkillsDir.resolve(newReferencedSkillName + ".yml");
                        try (FileWriter writer = new FileWriter(newSkillPath.toFile())) {
                            yaml.dump(newSkillData, writer);
                            plugin.getLogger().info("Generated friendly skill YAML for: " + referencedSkillName + " to: " + newSkillPath.toAbsolutePath());
                        }

                        skillRef.put("s", newReferencedSkillName);
                        modifiedSkills.add(skillMap);
                    } else {
                        modifiedSkills.add(skillEntry);
                    }
                } else {
                    Map<String, Object> currentSkillMap = new LinkedHashMap<>();
                    for (Map.Entry<String, Object> entry : skillMap.entrySet()) {
                        if (entry.getValue() instanceof String) {
                            currentSkillMap.put(entry.getKey(), modifySkillLineTargeter((String) entry.getValue()));
                        } else if (entry.getValue() instanceof List) {
                            currentSkillMap.put(entry.getKey(), entry.getValue());
                        } else {
                            currentSkillMap.put(entry.getKey(), entry.getValue());
                        }
                    }
                    modifiedSkills.add(currentSkillMap);
                }
            }
        }
        configSection.put("Skills", modifiedSkills);
    }

    private String modifySkillLineTargeter(String skillLine) {
        String modifiedLine = skillLine;

        Matcher playerTargeterMatcher = PLAYER_TARGETER_PATTERN.matcher(modifiedLine);
        modifiedLine = playerTargeterMatcher.replaceAll(match -> {
            return Matcher.quoteReplacement("@NearestAttackableTarget");
        });

        Matcher genericTargeterMatcher = GENERIC_ENTITY_TARGETER_PATTERN.matcher(modifiedLine);
        StringBuffer sb = new StringBuffer();
        while (genericTargeterMatcher.find()) {
            String fullMatch = genericTargeterMatcher.group(0);
            String targeterName = genericTargeterMatcher.group(1);

            String existingParams = "";
            int paramStart = fullMatch.indexOf("{");
            if (paramStart != -1) {
                existingParams = fullMatch.substring(paramStart + 1, fullMatch.length() - 1);
            }

            if (existingParams.contains("ignore=")) {
                genericTargeterMatcher.appendReplacement(sb, Matcher.quoteReplacement(fullMatch));
            } else {
                String newParamsContent = "ignore=players,PLAYER_ALLIES";
                if (!existingParams.isEmpty()) {
                    newParamsContent = existingParams + ";" + newParamsContent;
                }
                String replacement = targeterName + "{" + newParamsContent + "}";
                genericTargeterMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
        }
        genericTargeterMatcher.appendTail(sb);
        modifiedLine = sb.toString();

        return modifiedLine;
    }
}