//package me.remag501.customarmorsets.service;
//
//import org.bukkit.NamespacedKey;
//import org.bukkit.plugin.Plugin;
//
//import java.util.UUID;
//
//public class NamespaceService {
//
//    // Define all keys here
//    private final String namespace = "customarmorsets";
//
//    public final NamespacedKey armorSet;
//    public final NamespacedKey repairKit;
//    public final NamespacedKey repairKitTier;
//    public final NamespacedKey internalDurability;
//    public final NamespacedKey maxDurability;
//    public final NamespacedKey vampireKillMark;
//    public final NamespacedKey batFormOwner;
//
//    public NamespaceService(Plugin plugin) {
//        // Use the plugin instance to create the keys
//        // If you need legacy support, you can hardcode the string here instead
//
//        this.armorSet = new NamespacedKey(namespace, "armor_set");
//        this.repairKit = new NamespacedKey(namespace, "is_repair_kit");
//        this.repairKitTier = new NamespacedKey(namespace, "repair_kit_tier");
//        this.internalDurability = new NamespacedKey(namespace, "internal_durability");
//        this.maxDurability = new NamespacedKey(namespace, "internal_max_durability");
//        this.vampireKillMark = new NamespacedKey(namespace, "vampire_kill_mark");
//        this.batFormOwner = new NamespacedKey(namespace, "bat_form_owner");
//
//    }
//
//    public NamespacedKey getNecromancerNamespace(String uuid) {
//        return new NamespacedKey(namespace, "necromancer_" + uuid);
//    }
//
//}