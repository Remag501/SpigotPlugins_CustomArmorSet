package me.remag501.customarmorsets;

import org.bukkit.plugin.java.JavaPlugin;

public final class CustomArmorSets extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("Custom Armor Sets have started up!");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("Custom Armor Sets have shut down!");
    }
}
