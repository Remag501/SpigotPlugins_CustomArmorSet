package me.remag501.customarmorsets;

import me.remag501.customarmorsets.ArmorSets.InfernusArmorSet;
import me.remag501.customarmorsets.ArmorSets.LastSpartanArmorSet;
import me.remag501.customarmorsets.ArmorSets.SnowmanArmorSet;
import me.remag501.customarmorsets.ArmorSets.VikingCaptainArmorSet;
import me.remag501.customarmorsets.Commands.CustomArmorSetCommand;
import me.remag501.customarmorsets.Listeners.ArmorEquipListener;
import me.remag501.customarmorsets.Listeners.DisconnectReconnectListener;
import me.remag501.customarmorsets.Listeners.OffHandAbilityListener;
import me.remag501.customarmorsets.Listeners.WorldChangeListener;
import me.remag501.customarmorsets.lib.armorequipevent.ArmorListener;
import me.remag501.customarmorsets.lib.armorequipevent.DispenserArmorListener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class CustomArmorSets extends JavaPlugin {

    private static Plugin plugin;
    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("Custom Armor Sets have started up!");
        plugin = this;
        // Listeners fo equipping and using armor set
        getCommand("customarmorsets").setExecutor(new CustomArmorSetCommand(this));
        getServer().getPluginManager().registerEvents(new ArmorEquipListener(), this);
        getServer().getPluginManager().registerEvents(new OffHandAbilityListener(), this);
        // Listeners for world change and connection
        getServer().getPluginManager().registerEvents(new DisconnectReconnectListener(), this);
        getServer().getPluginManager().registerEvents(new WorldChangeListener(), this);
        // Register listeners for armor sets
        getServer().getPluginManager().registerEvents(new SnowmanArmorSet(), this);
        getServer().getPluginManager().registerEvents(new InfernusArmorSet(), this);
        getServer().getPluginManager().registerEvents(new LastSpartanArmorSet(), this);
        getServer().getPluginManager().registerEvents(new VikingCaptainArmorSet(), this);
        // Libraries
        getServer().getPluginManager().registerEvents(new ArmorListener(getConfig().getStringList("blocked")), this);
        getServer().getPluginManager().registerEvents(new DispenserArmorListener(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("Custom Armor Sets have shut down!");
    }

    public static Plugin getInstance() {
        return plugin;
    }
}
