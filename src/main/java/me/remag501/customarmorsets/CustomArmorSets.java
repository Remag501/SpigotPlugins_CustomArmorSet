package me.remag501.customarmorsets;

import me.remag501.customarmorsets.armorsets.*;
import me.remag501.customarmorsets.commands.CustomArmorSetCommand;
import me.remag501.customarmorsets.core.CustomArmorSetsCore;
import me.remag501.customarmorsets.listeners.*;
import me.remag501.customarmorsets.lib.armorequipevent.ArmorListener;
import me.remag501.customarmorsets.lib.armorequipevent.DispenserArmorListener;
//import me.remag501.customarmorsets.utils.PacketDebugger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public final class CustomArmorSets extends JavaPlugin {

    private static Plugin plugin;
    private CosmeticHelmetInterceptor helmetInterceptor;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("Custom Armor Sets have started up!");
        plugin = this;
        // Listeners fo equipping and using armor set
        getCommand("customarmorsets").setExecutor(new CustomArmorSetCommand(this));
        getServer().getPluginManager().registerEvents(new ArmorEquipListener(), this);
        getServer().getPluginManager().registerEvents(new OffHandAbilityListener(), this);
        getServer().getPluginManager().registerEvents(new DurabilityListener(), this);
        // Listeners for world change and connection
        getServer().getPluginManager().registerEvents(new DisconnectReconnectListener(), this);
        getServer().getPluginManager().registerEvents(new WorldChangeListener(), this);
        // Listener for broken items
        getServer().getPluginManager().registerEvents(new BrokenItemListener(), this);
        getServer().getPluginManager().registerEvents(new RepairListener(), this);
        // Register listeners for armor sets
        getServer().getPluginManager().registerEvents(new SnowmanArmorSet(), this);
        getServer().getPluginManager().registerEvents(new InfernusArmorSet(), this);
        getServer().getPluginManager().registerEvents(new LastSpartanArmorSet(), this);
        getServer().getPluginManager().registerEvents(new VikingCaptainArmorSet(), this);
        getServer().getPluginManager().registerEvents(new RoyalKnightArmorSet(), this);
        getServer().getPluginManager().registerEvents(new WorldGuardianArmorSet(), this);
        getServer().getPluginManager().registerEvents(new VampireArmorSet(), this);
        getServer().getPluginManager().registerEvents(new FisterArmorSet(), this);
        getServer().getPluginManager().registerEvents(new ArcherArmorSet(), this);
        // Libraries
        getServer().getPluginManager().registerEvents(new ArmorListener(getConfig().getStringList("blocked")), this);
        getServer().getPluginManager().registerEvents(new DispenserArmorListener(), this);
        // Init helmet interceptor
//        PacketDebugger.registerAllPacketDebuggers(this);
        helmetInterceptor = new CosmeticHelmetInterceptor();
        helmetInterceptor.init(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("Custom Armor Sets have shut down!");
        // Disable any kits that a player has equipped
        for (UUID uuid: CustomArmorSetsCore.equippedArmor.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null)
                CustomArmorSetsCore.unequipArmor(player);
        }
    }

    public static Plugin getInstance() {
        return plugin;
    }

    public CosmeticHelmetInterceptor getHelmetInterceptor() {
        return helmetInterceptor;
    }

}
