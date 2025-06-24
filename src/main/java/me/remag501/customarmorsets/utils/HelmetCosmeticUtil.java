package me.remag501.customarmorsets.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.PlayerDisguise;
import me.libraryaddict.disguise.disguisetypes.watchers.PlayerWatcher;
import me.remag501.customarmorsets.CustomArmorSets;
import me.remag501.customarmorsets.core.ArmorSet;
import me.remag501.customarmorsets.core.ArmorSetType;
import me.remag501.customarmorsets.core.CustomArmorSetsCore;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HelmetCosmeticUtil {
    private static final NamespacedKey ORIGINAL_TYPE_KEY = new NamespacedKey(Bukkit.getPluginManager().getPlugin("CustomArmorSets"), "original_type");

    public static ItemStack makeCosmeticHelmet(ItemStack original, String texture) {

//        original.setType(Material.PLAYER_HEAD);
//        List<String> lore = original.getItemMeta().getLore();

//         lore = original.getItemMeta().getLore();

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);

        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();

        // Set the custom texture
        PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID().toString());
        PlayerTextures playerTexture = profile.getTextures();

        try {
            URL url = new URL(texture);
            playerTexture.setSkin(url);
            profile.setTextures(playerTexture);
            skullMeta.setOwnerProfile(profile);
            skullMeta.setLore(original.getItemMeta().getLore());
            skullMeta.setDisplayName(original.getItemMeta().getDisplayName());
        } catch (MalformedURLException e) {
            Bukkit.getLogger().severe("Invalid skin URL: " + texture);
            e.printStackTrace();
        }

        head.setItemMeta(skullMeta);
        return head;

    }


    public static void applyCosmeticHelmet(Player player, String url) {

        ItemStack cosmeticHelmet = createFakePlayerHead(url);
        PlayerDisguise disguise = new PlayerDisguise(player.getName());
        
        // Apply fake armor
        PlayerWatcher watcher = disguise.getWatcher();
        watcher.setHelmet(cosmeticHelmet);

        // Disguise
        disguise.setViewSelfDisguise(true);
        disguise.setNotifyBar(null); // Disable action bar "Currently disguised as..."
        DisguiseAPI.disguiseToAll(player, disguise);

        // Try reapplying after a small delay (forces visual refresh)
        Bukkit.getScheduler().runTaskLater(CustomArmorSets.getInstance(), () -> {
            DisguiseAPI.undisguiseToAll(player);
            DisguiseAPI.disguiseToAll(player, disguise);
        }, 2L);
    }

    public static void removeCosmetic(Player player) {
        DisguiseAPI.undisguiseToAll(player);
    }


    public static void sendCosmeticHelmet(Player target, Player viewer, String textureUrl) {
//        ItemStack fakeHelmet = createCustomHead(textureUrl);
        ItemStack fakeHelmet = makeCosmeticHelmet(target.getInventory().getHelmet(), textureUrl);

        // Send helmet visually to the viewer (only changes it client-side)
//        List<Pair<EnumWrappers.ItemSlot, ItemStack>> equipment = List.of(
//                new Pair<>(EnumWrappers.ItemSlot.HEAD, fakeHelmet)
//        );
//
//        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_EQUIPMENT);
//        packet.getIntegers().write(0, target.getEntityId()); // ID of the entity wearing the helmet
//        packet.getSlotStackPairLists().write(0, equipment);
//
//        ProtocolLibrary.getProtocolManager().sendServerPacket(viewer, packet);

    }

    public static ItemStack createFakePlayerHead(String textureUrl) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();

        PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
        PlayerTextures textures = profile.getTextures();

        try {
            textures.setSkin(new URL(textureUrl));
            profile.setTextures(textures);
            skullMeta.setOwnerProfile(profile);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        skull.setItemMeta(skullMeta);
        return skull;
    }

    public static ItemStack createCustomHead(String texture) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();

        PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
        PlayerTextures textures = profile.getTextures();

        try {
            textures.setSkin(new URL(texture));
            profile.setTextures(textures);
            skullMeta.setOwnerProfile(profile);
        } catch (MalformedURLException e) {
            Bukkit.getLogger().severe("Invalid head URL: " + texture);
            e.printStackTrace();
        }

        head.setItemMeta(skullMeta);
        return head;
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

//        ItemMeta meta = armorPiece.getItemMeta();
//        if (meta == null) return;
//
//        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
//        // Remove old durability line(s)
//        lore.removeIf(line -> ChatColor.stripColor(line).contains("Durability"));
//
//        // Append new lore
//        lore.addAll(newLoreLines);
//        meta.setLore(lore);

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
//        armorPiece.setItemMeta(meta);
    }

    public static void updateCosmeticHelmetLoreSafely(Player player) {
        if (true) return;
        ArmorSet armorSet = CustomArmorSetsCore.getArmorSet(player);
        if (armorSet == null) return;

        ArmorSetType type = armorSet.getType();
        String textureUrl = type.getHeadUrl();
        if (textureUrl == null || textureUrl.isEmpty()) return;

        // Re-send packet to all viewers (including self, if needed)
        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("CustomArmorSets"), () -> {
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (viewer.canSee(player)) {
                    sendCosmeticHelmet(player, viewer, textureUrl);
                }
            }
        });

    }

    public static ItemStack restoreOriginalHelmet(ItemStack cosmetic, Color color) {

        cosmetic.setType(Material.LEATHER_HELMET);
        LeatherArmorMeta meta = (LeatherArmorMeta) cosmetic.getItemMeta();
        meta.setColor(color);
        cosmetic.setItemMeta(meta);
        return cosmetic;

    }

}

