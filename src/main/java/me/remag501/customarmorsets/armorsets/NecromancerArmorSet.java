package me.remag501.customarmorsets.armorsets;

import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.api.mobs.entities.SpawnReason;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.remag501.customarmorsets.CustomArmorSets;
import me.remag501.customarmorsets.core.ArmorSet;
import me.remag501.customarmorsets.core.ArmorSetType;
import me.remag501.customarmorsets.core.CustomArmorSetsCore;
import me.remag501.customarmorsets.listeners.MythicMobsYamlGenerator;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class NecromancerArmorSet extends ArmorSet implements Listener {

    private static final Map<UUID, Long> resurrectionCooldowns = new HashMap<>();
    private static final long RESURRECTION_COOLDOWN = 120 * 1000; // 2 minutes
    private static final Map<ArmorStand, MythicMob> killedMobs = new HashMap<>();
    private static final Map<UUID, List<MythicMob>> summonedMobs = new HashMap<>();

    private static final String RESURRECTED_MOB_PREFIX = MythicMobsYamlGenerator.getPrefix();

    public NecromancerArmorSet() {
        super(ArmorSetType.NECROMANCER);
    }

    @Override
    public void applyPassive(Player player) {
        player.sendMessage("✅ You equipped the Necromancer set");
    }

    @Override
    public void removePassive(Player player) {
        player.sendMessage("❌ You removed the Necromancer set");
    }

    @Override
    public void triggerAbility(Player player) {
        // Check PDC matches up
        Plugin plugin = CustomArmorSets.getInstance();
        // Check for vampire orb in 5 block radius
        final int RADIUS = 4;
        for (Entity entity : player.getNearbyEntities(RADIUS, RADIUS, RADIUS)) {
            if (entity instanceof ArmorStand stand) {
                NamespacedKey key = new NamespacedKey(plugin, "necromancer_" + player.getUniqueId());
                if (stand.getPersistentDataContainer().has(key, PersistentDataType.BYTE)
                        && stand.getLocation().distanceSquared(player.getLocation()) <= RADIUS * RADIUS) {
                    stand.remove(); // destroy the orb
                    player.sendMessage(ChatColor.GOLD + "Born Again!");
                    MythicMob resMob = killedMobs.get(entity);
                    player.sendMessage("Maybe? " + resMob.getDisplayName());
                    reviveMob(player, resMob);
                }
            }
        }

    }

    public ActiveMob reviveMob(Player player, MythicMob originalMythicMob) {
        if (!MythicBukkit.inst().isEnabled()) {
            player.sendMessage("§cError: MythicMobs is not enabled!");
            return null;
        }

        if (originalMythicMob == null) {
            player.sendMessage("§cError: Invalid original mob definition provided.");
            return null;
        }

        // --- 1. Determine the name of the resurrected mob type ---
        // This relies on the convention established by your YAML generator (e.g., "Resurrected_ZombieKing")
        String resurrectedMobInternalName = RESURRECTED_MOB_PREFIX + originalMythicMob.getInternalName();

        // --- 2. Attempt to get the MythicMob definition for the resurrected version ---
        Optional<MythicMob> optResurrectedMobType = MythicBukkit.inst().getMobManager().getMythicMob(resurrectedMobInternalName);

        if (optResurrectedMobType.isEmpty()) {
//            player.sendMessage("§cError: No resurrected version found for " + originalMythicMob.getDisplayName() + " (§e" + resurrectedMobInternalName + "§c).");
//            player.sendMessage("§cPlease ensure you have generated and moved the '" + resurrectedMobInternalName + ".yml' file.");
//            player.sendMessage("§cIf you just started the server, trigger a MythicMobs reload and move files first.");
            player.sendMessage("§cResurrection has failed!");
            return null;
        }

        MythicMob resurrectedMobType = optResurrectedMobType.get();
        Location spawnLocation = player.getLocation();
        UUID ownerUUID = player.getUniqueId();

        // --- 3. Prepare Spawn Data (including owner) ---
        HashMap<String, String> spawnData = new HashMap<>();
        spawnData.put("owner", ownerUUID.toString());
        // Faction, AITargetSelectors, and AIGoals are now defined within resurrectedMobType's YAML

        // --- 4. Spawn the Resurrected Mob ---
        ActiveMob activeMob = resurrectedMobType.spawn(
                BukkitAdapter.adapt(spawnLocation),
                1.0 // Mob level (adjust as needed, 1.0 is default)
                // Or whatever spawn reason is appropriate for your system
                // prespawnFunc (not typically needed here)
                // MythicSpawner (not typically needed here)
                // Pass owner data. MythicMobs will set the owner from this.
        );

        if (activeMob == null) {
            player.sendMessage("§cFailed to revive " + resurrectedMobType.getDisplayName() + "!");
            return null;
        }

        // --- 5. Explicitly set owner (redundant with spawnData but harmless and adds robustness) ---
        // Some older MythicMobs versions or specific setups might benefit from this explicit call.
        activeMob.setOwner(ownerUUID);

        // Set name of the mob
        String mobName = activeMob.getDisplayName();
        if (mobName == null)
            mobName = activeMob.getEntity().getName();
        activeMob.setDisplayName(player.getDisplayName() + "'s Resurrected " + ChatColor.BOLD + mobName);

        player.sendMessage("§aYou have successfully revived a loyal " + activeMob.getDisplayName() + "!");
        return activeMob;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
    }

    @EventHandler
    public void onPlayerKillMob(EntityDeathEvent event) {
        // Basic checks
        Player player = event.getEntity().getKiller();
        if (player == null || !(CustomArmorSetsCore.getArmorSet(player) instanceof NecromancerArmorSet)) return; // Not killed by a player or player not wearing the set
        Optional<ActiveMob> optActiveMob = MythicBukkit.inst().getMobManager().getActiveMob(event.getEntity().getUniqueId());
        if (optActiveMob.isEmpty()) return; // Not a mythic mob
        // Logic for mythic mob
        optActiveMob.ifPresent(activeMob -> {

            player.sendMessage("That was a Mythic Mob called " + activeMob.getDisplayName());
            spawnCosmeticHead(event.getEntity().getLocation(), activeMob, String.valueOf(player.getUniqueId()));

        });
    }

    public void spawnCosmeticHead(Location loc, ActiveMob activeMob, String uuid) {
        Location headLoc = loc.clone().add(0, 1.2, 0); // Adjust Y for head height
        ArmorStand stand = (ArmorStand) loc.getWorld().spawnEntity(headLoc, EntityType.ARMOR_STAND);

        stand.setVisible(false);
        stand.setSmall(true);
        stand.setGravity(false);
        stand.setMarker(true); // Prevents interaction and collision
        stand.setBasePlate(false);
        stand.setInvulnerable(true);
        stand.setSilent(true); // No armor stand sounds

        // Set the custom name to the MythicMob's display name
        stand.setCustomNameVisible(true);
        String mobName = activeMob.getDisplayName();
        if (mobName == null)
            mobName = activeMob.getEntity().getName();
        mobName = ChatColor.BOLD + mobName;
        stand.setCustomName(mobName);

        // Add texture to skull
        String textureUrl = "http://textures.minecraft.net/texture/15378267b72a33618c8c9d8ff4be2d452a26509a9964b080b19d7c308ec79605";
        stand.setHelmet(getCustomSkull(textureUrl));

        Plugin plugin = CustomArmorSets.getInstance();
        // Mark with PDC so it can be tracked/removed
        stand.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "necromancer_" + uuid), // Use your plugin's specific key
                PersistentDataType.BYTE,
                (byte) 1
        );

        // Link stand to mythic mob
        killedMobs.put(stand, activeMob.getType());

        // Remove the armor stand after 5 seconds (100 ticks)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!stand.isDead() && stand.isValid()) {
                stand.remove();
            }
        }, 100L);
    }

    public static ItemStack getCustomSkull(String textureUrl) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        if (meta != null) {
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
            PlayerTextures textures = profile.getTextures();

            try {
                URL url = new URL(textureUrl);
                textures.setSkin(url);
                profile.setTextures(textures);
                meta.setOwnerProfile(profile);
            } catch (MalformedURLException e) {
                Bukkit.getLogger().severe("Invalid texture URL: " + textureUrl);
                e.printStackTrace();
            }

            skull.setItemMeta(meta);
        }

        return skull;
    }

}