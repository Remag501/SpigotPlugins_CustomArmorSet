package me.remag501.customarmorsets;

import me.remag501.customarmorsets.armor.impl.*;
import me.remag501.customarmorsets.command.CustomArmorSetCommand;
import me.remag501.customarmorsets.manager.*;
import me.remag501.customarmorsets.listener.*;
import me.remag501.customarmorsets.lib.armorequipevent.ArmorListener;
import me.remag501.customarmorsets.lib.armorequipevent.DispenserArmorListener;
import me.remag501.customarmorsets.service.*;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public final class CustomArmorSets extends JavaPlugin {

    private static volatile boolean isServerShuttingDown = false;

    private ArmorManager armorManager;

    // Getter for the shutdown status
    public static boolean isServerShuttingDown() {
        return isServerShuttingDown;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("Custom Armor Sets have started up!");

        // 1. Create managers and services

        // Setup services
        CosmeticService cosmeticService = new CosmeticService();
        NamespaceService namespaceService = new NamespaceService(this);
        ArmorService armorService = new ArmorService(namespaceService);
        AttributesService attributesService = new AttributesService(this);
        ItemService itemService = new ItemService(namespaceService, armorService);

        // Setup managers
        DamageStatsManager damageStatsManager = new DamageStatsManager();
        DefenseStatsManager defenseStatsManager = new DefenseStatsManager();
        CooldownBarManager cooldownBarManager = new CooldownBarManager(this);
        PlayerSyncManager playerSyncManager = new PlayerSyncManager(attributesService);
        armorManager = new ArmorManager(this, cosmeticService, attributesService, cooldownBarManager, damageStatsManager, defenseStatsManager, armorService, playerSyncManager);

        // 2. Register command to plugin
        getCommand("customarmorsets").setExecutor(new CustomArmorSetCommand(this, itemService));

        // 3. Register all listeners to plugin

        // Listeners fo equipping and using armor set
        getServer().getPluginManager().registerEvents(new ArmorEquipListener(armorManager, armorService, itemService), this);
        getServer().getPluginManager().registerEvents(new OffHandAbilityListener(armorManager), this);
        getServer().getPluginManager().registerEvents(new DurabilityListener(armorService, cosmeticService), this);
        // Listeners for world change and connection
        getServer().getPluginManager().registerEvents(new DisconnectReconnectListener(armorManager, armorService), this);
        getServer().getPluginManager().registerEvents(new WorldChangeListener(armorManager, armorService), this);
        // Listener for broken items
        getServer().getPluginManager().registerEvents(new BrokenItemListener(itemService), this);
        getServer().getPluginManager().registerEvents(new RepairListener(armorService, cosmeticService, itemService), this);
        // Listener for damage stats
        getServer().getPluginManager().registerEvents(new DamageListener(damageStatsManager, defenseStatsManager), this);
        // Mythic mobs
        getServer().getPluginManager().registerEvents(new MythicMobsListener(this), this);

        // Register listeners for armor sets
        getServer().getPluginManager().registerEvents(new SnowmanArmorSet(), this);
        getServer().getPluginManager().registerEvents(new InfernusArmorSet(this, armorManager, cooldownBarManager), this);
        getServer().getPluginManager().registerEvents(new LastSpartanArmorSet(this, armorManager, cooldownBarManager, attributesService), this);
        getServer().getPluginManager().registerEvents(new VikingCaptainArmorSet(damageStatsManager, cooldownBarManager), this);
        getServer().getPluginManager().registerEvents(new RoyalKnightArmorSet(armorManager, cooldownBarManager, attributesService), this);
        getServer().getPluginManager().registerEvents(new WorldGuardianArmorSet(this, armorManager, cooldownBarManager, attributesService), this);
        getServer().getPluginManager().registerEvents(new VampireArmorSet(this, armorManager, cooldownBarManager, attributesService), this);
        getServer().getPluginManager().registerEvents(new FisterArmorSet(this, armorManager, cooldownBarManager, attributesService, armorService), this);
        getServer().getPluginManager().registerEvents(new ArcherArmorSet(armorManager, cooldownBarManager, attributesService), this);
        getServer().getPluginManager().registerEvents(new NecromancerArmorSet(this, armorManager, damageStatsManager, attributesService, playerSyncManager), this);
        getServer().getPluginManager().registerEvents(new IcemanArmorSet(this, armorManager, cooldownBarManager, attributesService), this);
        getServer().getPluginManager().registerEvents(new GolemBusterArmorSet(this, armorManager, cooldownBarManager, attributesService, damageStatsManager, defenseStatsManager), this);
        getServer().getPluginManager().registerEvents(new BanditArmorSet(this, armorManager, cooldownBarManager, attributesService), this);

        // Libraries
        getServer().getPluginManager().registerEvents(new ArmorListener(getConfig().getStringList("blocked")), this);
        getServer().getPluginManager().registerEvents(new DispenserArmorListener(), this);

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        isServerShuttingDown = true;
        // Disable any kits that a player has equipped
        for (UUID uuid: armorManager.getEquippedArmor().keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null)
                armorManager.unequipArmor(player); // Won't work since events can't get registered
        }
        getLogger().info("Custom Armor Sets have shut down!");
    }

}
