package me.remag501.customarmorsets.utils;

import org.bukkit.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HelmetCosmeticUtil {
    private static final NamespacedKey ORIGINAL_TYPE_KEY = new NamespacedKey(Bukkit.getPluginManager().getPlugin("CustomArmorSets"), "original_type");

    public static ItemStack makeCosmeticHelmet(ItemStack original, String texture) {

        original.setType(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) original.getItemMeta();
        // Set the custom texture
        PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID().toString());
        PlayerTextures playerTexture = profile.getTextures();
        try {
            URL url = new URL(texture);
            playerTexture.setSkin(url);
            profile.setTextures(playerTexture);
            skullMeta.setOwnerProfile(profile);
        } catch (MalformedURLException e) {
            Bukkit.getLogger().severe("Invalid skin URL: " + texture);
            e.printStackTrace();
        }
        original.setItemMeta(skullMeta);
        return original;

    }

    /**
     * Updates the lore of any armor piece by appending new lines, safely preserving cosmetic helmet textures.
     * This function assumes that PDC handling and durability logic have already been processed outside.
     *
     * @param armorPiece The ItemStack to modify.
     * @param newLoreLines The new lore lines to append.
     */
    public static void updateCosmeticHelmetLoreSafely(ItemStack armorPiece, List<String> newLoreLines) {
        if (armorPiece == null || !armorPiece.hasItemMeta()) return;

        ItemMeta meta = armorPiece.getItemMeta();
        if (meta == null) return;

        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        // Remove old durability line(s)
        lore.removeIf(line -> ChatColor.stripColor(line).contains("Durability"));

        // Append new lore
        lore.addAll(newLoreLines);
        meta.setLore(lore);

//        // Special handling for textured heads
//        if (armorPiece.getType() == Material.PLAYER_HEAD && meta instanceof SkullMeta skullMeta) {
//            PlayerProfile profile = skullMeta.getOwnerProfile();
//            if (profile != null) {
//                skullMeta.setOwnerProfile(profile); // Restore profile after mutation
//            }
//            armorPiece.setItemMeta(skullMeta);
//        } else {
//            armorPiece.setItemMeta(meta);
//        }
        armorPiece.setItemMeta(meta);
    }

    public static ItemStack restoreOriginalHelmet(ItemStack cosmetic, Color color) {

        cosmetic.setType(Material.LEATHER_HELMET);
        LeatherArmorMeta meta = (LeatherArmorMeta) cosmetic.getItemMeta();
        meta.setColor(color);
        cosmetic.setItemMeta(meta);
        return cosmetic;

    }

}

