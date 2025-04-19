package me.remag501.customarmorsets;

import me.remag501.customarmorsets.Commands.CustomArmorSetCommand;
import me.remag501.customarmorsets.Listeners.ArmorEquipListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class CustomArmorSets extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("Custom Armor Sets have started up!");
        getCommand("customarmorsets").setExecutor(new CustomArmorSetCommand(this));
        getServer().getPluginManager().registerEvents(new ArmorEquipListener(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("Custom Armor Sets have shut down!");
    }
}
