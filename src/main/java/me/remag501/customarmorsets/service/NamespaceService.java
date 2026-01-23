package me.remag501.customarmorsets.service;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public class NamespaceService {

    // Define all keys here
    public final NamespacedKey armorSet;
    public final NamespacedKey repairKit;
    public final NamespacedKey repairKitTier;
    public final NamespacedKey internalDurability;
    public final NamespacedKey maxDurability;

    public NamespaceService(Plugin plugin) {
        // Use the plugin instance to create the keys
        // If you need legacy support, you can hardcode the string here instead
        String namespace = "customarmorsets";

        this.armorSet = new NamespacedKey(namespace, "armor_set");
        this.repairKit = new NamespacedKey(namespace, "is_repair_kit");
        this.repairKitTier = new NamespacedKey(namespace, "repair_kit_tier");
        this.internalDurability = new NamespacedKey(namespace, "internal_durability");
        this.maxDurability = new NamespacedKey(namespace, "internal_max_durability");
    }

}