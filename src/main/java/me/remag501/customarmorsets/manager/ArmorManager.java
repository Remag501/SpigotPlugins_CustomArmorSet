package me.remag501.customarmorsets.manager;

import io.lumine.mythic.bukkit.utils.lib.jooq.impl.QOM;
import me.remag501.customarmorsets.armor.ArmorSet;
import me.remag501.customarmorsets.armor.ArmorSetType;
import me.remag501.customarmorsets.armor.impl.*;
import me.remag501.customarmorsets.service.ArmorService;
import me.remag501.customarmorsets.service.AttributesService;
import me.remag501.customarmorsets.service.CosmeticService;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
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
    private final AttributesService attributesService;

    private static final List<String> BANNED_WORLDS = List.of("spawn", "dungeonhub", "honeyclicker");
    private static final String BUNKER_PREFIX = "bunker";

    public ArmorManager(Plugin plugin,
                        CosmeticService cosmeticService,
                        AttributesService attributesService,
                        CooldownBarManager cooldownManager,
                        DamageStatsManager damageStatsManager,
                        DefenseStatsManager defenseStatsManager,
                        ArmorService armorService,
                        PlayerSyncManager playerSyncManager) {
        this.plugin = plugin;
        this.cosmeticService = cosmeticService;
        this.attributesService = attributesService;

        // --- THE WIRING HUB ---
        // Register all sets here. This is where we satisfy those new constructors.
        register(ArmorSetType.SNOWMAN, new SnowmanArmorSet());
        register(ArmorSetType.INFERNUS, new InfernusArmorSet(plugin, this, cooldownManager));
        register(ArmorSetType.LAST_SPARTAN, new LastSpartanArmorSet(plugin, this, cooldownManager, attributesService));
        register(ArmorSetType.VIKING_CAPTAIN, new VikingCaptainArmorSet(damageStatsManager, cooldownManager));
        register(ArmorSetType.ROYAL_KNIGHT, new RoyalKnightArmorSet(this, cooldownManager, attributesService));
        register(ArmorSetType.WORLD_GUARDIAN, new WorldGuardianArmorSet(plugin, this, cooldownManager, attributesService));
        register(ArmorSetType.VAMPIRE, new VampireArmorSet(plugin, this, cooldownManager, attributesService));
        register(ArmorSetType.FISTER, new FisterArmorSet(plugin, this, cooldownManager, attributesService, armorService));
        register(ArmorSetType.ARCHER, new ArcherArmorSet(this, cooldownManager, attributesService));
        register(ArmorSetType.NECROMANCER, new NecromancerArmorSet(plugin, this, damageStatsManager, attributesService, playerSyncManager));
        register(ArmorSetType.ICEMAN, new IcemanArmorSet(plugin, this, cooldownManager, attributesService));
        register(ArmorSetType.GOLEM_BUSTER, new GolemBusterArmorSet(plugin, this, cooldownManager, attributesService, damageStatsManager, defenseStatsManager));
        register(ArmorSetType.BANDIT, new BanditArmorSet(plugin, this, cooldownManager, attributesService));
    }

    private void register(ArmorSetType type, ArmorSet instance) {
        setRegistry.put(type, instance);
        // Automatically register each set's @EventHandler methods
        Bukkit.getPluginManager().registerEvents((Listener) instance, plugin);
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

        ItemStack boots = player.getInventory().getBoots();
        if (boots != null) {
            attributesService.removeAllArmorAttributes(boots);
        }
    }

    public ArmorSet getArmorSet(Player player) {
        return equippedArmor.get(player.getUniqueId());
    }

    public Map<UUID, ArmorSet> getEquippedArmor() {
        return equippedArmor;
    }
}