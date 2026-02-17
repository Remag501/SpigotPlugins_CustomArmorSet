package me.remag501.customarmorsets;

import me.remag501.bgscore.api.BGSApi;
import me.remag501.bgscore.api.ability.AbilityService;
import me.remag501.bgscore.api.combat.AttributeService;
import me.remag501.bgscore.api.combat.CombatStatsService;
import me.remag501.bgscore.api.command.CommandService;
import me.remag501.bgscore.api.event.EventService;
import me.remag501.bgscore.api.namespace.NamespaceService;
import me.remag501.bgscore.api.task.TaskService;
import me.remag501.customarmorsets.armor.impl.*;
import me.remag501.customarmorsets.command.CustomArmorSetCommand;
import me.remag501.customarmorsets.manager.*;
import me.remag501.customarmorsets.listener.*;
import me.remag501.customarmorsets.lib.armorequipevent.ArmorListener;
import me.remag501.customarmorsets.lib.armorequipevent.DispenserArmorListener;
import me.remag501.customarmorsets.service.*;
import org.bukkit.Bukkit;
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

        // 1. Get the API sevices from core
        TaskService taskService = BGSApi.tasks();
        EventService eventService = BGSApi.events();
        CommandService commandService = BGSApi.commands();
        NamespaceService namespaceService = BGSApi.namespaces();
        AttributeService attributeService = BGSApi.attribute();
        CombatStatsService combatStatsService = BGSApi.combat();
        AbilityService abilityService = BGSApi.ability();

        // 2. Initialize services and managers

        // Setup services
        CosmeticService cosmeticService = new CosmeticService();
        ArmorService armorService = new ArmorService(namespaceService);
//        AttributesService attributesService = new AttributesService(this);
        ItemService itemService = new ItemService(namespaceService, armorService);

        // Setup managers
//        DamageStatsManager damageStatsManager = new DamageStatsManager();
//        DefenseStatsManager defenseStatsManager = new DefenseStatsManager();
//        CooldownBarManager cooldownBarManager = new CooldownBarManager(this);
        PlayerSyncManager playerSyncManager = new PlayerSyncManager(attributeService);
        armorManager = new ArmorManager(this, taskService, eventService, cosmeticService, attributeService, abilityService,
                combatStatsService, armorService, playerSyncManager, namespaceService);

        // 3. Register command to plugin
        CustomArmorSetCommand armorSetCommand = new CustomArmorSetCommand(itemService, namespaceService);
        getCommand("customarmorsets").setExecutor(armorSetCommand);
        commandService.registerSubcommand("armor", armorSetCommand);

        // 4. Register all listeners to plugin

        // Listeners fo equipping and using armor set
        getServer().getPluginManager().registerEvents(new ArmorEquipListener(armorManager, armorService, itemService), this);
        new ArmorInteractListener(armorManager, eventService);
        new OffHandAbilityListener(armorManager, eventService);
        new DurabilityListener(armorService, cosmeticService, namespaceService, eventService);

        // Listeners for world change and connection
        new DisconnectReconnectListener(armorManager, armorService, eventService);
        new WorldChangeListener(armorManager, armorService, eventService);

        // Listener for broken items
        new BrokenItemListener(itemService, eventService);
        new RepairListener(armorService, cosmeticService, itemService, namespaceService, eventService);

        // Listener for damage stats
//        new DamageListener(damageStatsManager, defenseStatsManager, eventService);

        // Mythic mobs
        getServer().getPluginManager().registerEvents(new MythicMobsListener(this), this);

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
