package me.remag501.customarmorsets.Utils;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URL;
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

    public static ItemStack restoreOriginalHelmet(ItemStack cosmetic, Color color) {

        cosmetic.setType(Material.LEATHER_HELMET);
        LeatherArmorMeta meta = (LeatherArmorMeta) cosmetic.getItemMeta();
        meta.setColor(color);
        cosmetic.setItemMeta(meta);
        return cosmetic;

    }

}

