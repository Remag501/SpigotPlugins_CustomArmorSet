package me.remag501.customarmorsets.manager;

import me.remag501.bgscore.api.BGSApi;
import me.remag501.bgscore.api.ability.AbilityService;
import me.remag501.bgscore.api.combat.AttributeService;
import me.remag501.bgscore.api.combat.CombatStatsService;
import me.remag501.bgscore.api.event.EventService;
import me.remag501.bgscore.api.namespace.NamespaceService;
import me.remag501.bgscore.api.task.TaskService;
import me.remag501.customarmorsets.armor.ArmorSet;
import me.remag501.customarmorsets.armor.ArmorSetType;
import me.remag501.customarmorsets.armor.impl.*;
import me.remag501.customarmorsets.service.ArmorService;
import me.remag501.customarmorsets.service.CosmeticService;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ArmorManager {

    private final Map<ArmorSetType, ArmorSet> setRegistry = new HashMap<>();
    private final Map<UUID, ArmorSet> equippedArmor = new HashMap<>();
    private final Map<UUID, ArmorSetType> equippedHelmet = new HashMap<>();

    private final Plugin plugin;
    private final CosmeticService cosmeticService;
    private final AttributeService attributeService;

    private static final List<String> BANNED_WORLDS = List.of("spawn", "dungeonhub", "honeyclicker");
    private static final String BUNKER_PREFIX = "bunker";

    public ArmorManager(Plugin plugin,
                        TaskService taskService,
                        EventService eventService,
                        CosmeticService cosmeticService,
                        AttributeService attributeService,
                        AbilityService abilityService,
                        CombatStatsService combatStatsService,
                        ArmorService armorService,
                        PlayerSyncManager playerSyncManager,
                        NamespaceService namespaceService) {
        this.plugin = plugin;
        this.cosmeticService = cosmeticService;
        this.attributeService = attributeService;

        // --- THE WIRING HUB ---
        // Register all sets here. This is where we satisfy those new constructors.
        register(ArmorSetType.SNOWMAN, new SnowmanArmorSet());
        register(ArmorSetType.INFERNUS, new InfernusArmorSet(eventService, taskService, abilityService));
        register(ArmorSetType.DEVOID, new DevoidArmorSet(taskService, abilityService));
        register(ArmorSetType.LAST_SPARTAN, new LastSpartanArmorSet(eventService, taskService, abilityService, attributeService, combatStatsService));
        register(ArmorSetType.VIKING_CAPTAIN, new VikingCaptainArmorSet(taskService, combatStatsService, abilityService));
        register(ArmorSetType.ROYAL_KNIGHT, new RoyalKnightArmorSet(abilityService, attributeService));
        register(ArmorSetType.WORLD_GUARDIAN, new WorldGuardianArmorSet(eventService, taskService, abilityService, attributeService));
        register(ArmorSetType.VAMPIRE, new VampireArmorSet(eventService, taskService, this, abilityService, attributeService, namespaceService));
        register(ArmorSetType.FISTER, new FisterArmorSet(eventService, taskService, this, abilityService, attributeService));
        register(ArmorSetType.ARCHER, new ArcherArmorSet(eventService, abilityService, attributeService));
        register(ArmorSetType.NECROMANCER, new NecromancerArmorSet(eventService, taskService, this, combatStatsService, attributeService, playerSyncManager, namespaceService));
        register(ArmorSetType.ICEMAN, new IcemanArmorSet(eventService, taskService, this, abilityService, attributeService));
        register(ArmorSetType.GOLEM_BUSTER, new GolemBusterArmorSet(eventService, taskService, abilityService, attributeService, combatStatsService));
        register(ArmorSetType.BANDIT, new BanditArmorSet(eventService, taskService, this, abilityService, attributeService));
    }

    private void register(ArmorSetType type, ArmorSet instance) {
        setRegistry.put(type, instance);
        // Automatically register each set's @EventHandler methods
//        Bukkit.getPluginManager().registerEvents((Listener) instance, plugin);
    }

    public boolean equipArmor(Player player, ArmorSetType type) {
        // 1. Handle Helmet Cosmetic
        Bukkit.getScheduler().runTask(plugin, () -> {
            ItemStack helmet = player.getInventory().getHelmet();
            if (helmet != null) {
                player.getInventory().setHelmet(cosmeticService.makeCosmeticHelmet(helmet, type.getHeadUrl()));
            }
        });
        equippedHelmet.put(player.getUniqueId(), type);

        // 2. World Check
        String worldName = player.getWorld().getName().toLowerCase();
        if (BANNED_WORLDS.contains(worldName) || worldName.startsWith(BUNKER_PREFIX)) {
            return false;
        }

        // 3. Activate Passive
        ArmorSet set = setRegistry.get(type);
        if (set != null) {
            equippedArmor.put(player.getUniqueId(), set);
            set.applyPassive(player);
            return true;
        }
        return false;
    }

    public void unequipArmor(Player player) {
        ArmorSet set = equippedArmor.remove(player.getUniqueId());
        ArmorSetType type = equippedHelmet.remove(player.getUniqueId());

        if (set != null) {
            set.removePassive(player);
        }

        if (type != null) {
            ItemStack helmet = player.getInventory().getHelmet();
            if (helmet != null) {
                player.getInventory().setHelmet(cosmeticService.restoreOriginalHelmet(helmet, Color.fromRGB(type.getLeatherColor())));
            }
        }

//        ItemStack boots = player.getInventory().getBoots();
//        if (boots != null) {
//            attributesService.removeAllArmorAttributes(boots);
//        }
        // No need for boots now
    }

    public ArmorSet getArmorSet(Player player) {
        return equippedArmor.get(player.getUniqueId());
    }

    public Map<UUID, ArmorSet> getEquippedArmor() {
        return equippedArmor;
    }
}