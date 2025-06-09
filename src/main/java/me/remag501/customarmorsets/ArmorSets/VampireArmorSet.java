package me.remag501.customarmorsets.ArmorSets;

import me.remag501.customarmorsets.Core.ArmorSet;
import me.remag501.customarmorsets.Core.ArmorSetType;
import me.remag501.customarmorsets.Utils.ArmorUtil;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class VampireArmorSet extends ArmorSet implements Listener {

    private static final int RADIUS = 5;
    private static final int DURATION_TICKS = 60; // 3 seconds
    private static final int INTERVAL_TICKS = 10; // every 0.5s
    private static final int COOLDOWN_TICKS = 15 * 20; // 15 seconds
    private static final double LIFESTEAL_AMOUNT = 0.3;

    private static final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final Set<UUID> batForm = new HashSet<>();

    public VampireArmorSet() {
        super(ArmorSetType.VAMPIRE);
    }

    @Override
    public void applyPassive(Player player) {
        // TODO: Give passive life-drain effect (e.g., deal damage heals you slightly)
    }

    @Override
    public void removePassive(Player player) {
        // TODO: Remove any potion effects or modifiers given in applyPassive
    }

    @Override
    public void triggerAbility(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (cooldowns.containsKey(uuid) && now < cooldowns.get(uuid)) {
            long remaining = (cooldowns.get(uuid) - now) / 1000;
            player.sendMessage(ChatColor.RED + "Vampire ability on cooldown (" + remaining + "s left)");
            return;
        }

        // Bat form early exit toggle
        if (batForm.contains(uuid)) {
            cancelBatForm(player);
            return;
        }

        // Store cooldown
        cooldowns.put(uuid, now + COOLDOWN_TICKS * 50L);

        Plugin plugin = Bukkit.getPluginManager().getPlugin("CustomArmorSets");

        if (player.isSneaking()) {
            // Bat form logic
            enterBatForm(player);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (batForm.contains(uuid)) cancelBatForm(player);
            }, DURATION_TICKS);

            return;
        }

        // Check for vampire orb in 5 block radius
        boolean orbNearby = player.getNearbyEntities(RADIUS, RADIUS, RADIUS).stream()
                .filter(e -> e instanceof ArmorStand stand && stand.getPersistentDataContainer().has(new NamespacedKey(plugin, "vampire_kill_mark"), PersistentDataType.BYTE))
                .anyMatch(e -> e.getLocation().distanceSquared(player.getLocation()) <= RADIUS * RADIUS);

        if (orbNearby) {
            player.sendMessage(ChatColor.DARK_RED + "You absorb the vampire orb!");
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, DURATION_TICKS, 1));
            return;
        }

        // Default: drain enemies and heal
        List<LivingEntity> targets = player.getNearbyEntities(RADIUS, RADIUS, RADIUS).stream()
                .filter(e -> e instanceof LivingEntity && e != player)
                .map(e -> (LivingEntity) e)
                .toList();

        new BukkitRunnable() {
            int ticksRun = 0;

            @Override
            public void run() {
                if (ticksRun >= DURATION_TICKS || player.isDead()) {
                    cancel();
                    return;
                }

                for (LivingEntity target : targets) {
                    if (!target.isDead() && target.getLocation().distanceSquared(player.getLocation()) <= RADIUS * RADIUS) {
                        target.damage(1.0, player);
                        player.setHealth(Math.min(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(), player.getHealth() + 1.0));
                        drawParticleLine(target.getEyeLocation(), player.getEyeLocation(), Particle.REDSTONE, Color.MAROON);
                    }
                }

                ticksRun += INTERVAL_TICKS;
            }
        }.runTaskTimer(plugin, 0L, INTERVAL_TICKS);
    }

    private void drawParticleLine(Location from, Location to, Particle particle, Color color) {
        double distance = from.distance(to);
        Vector direction = to.toVector().subtract(from.toVector()).normalize().multiply(0.3);
        Location current = from.clone();

        for (double i = 0; i < distance; i += 0.3) {
            from.getWorld().spawnParticle(particle, current, 0, new Particle.DustOptions(color, 1.5f));
            current.add(direction);
        }
    }

    private void enterBatForm(Player player) {
        World world = player.getWorld();
        Location loc = player.getLocation();

        Bat bat = (Bat) world.spawnEntity(loc, EntityType.BAT);
        bat.setInvisible(true);
        bat.setInvulnerable(true);
        bat.setSilent(true);
        bat.setAI(false);
        bat.addPassenger(player);

        batForm.add(player.getUniqueId());
        player.setGameMode(GameMode.SPECTATOR);
        bat.getPersistentDataContainer().set(new NamespacedKey(Bukkit.getPluginManager().getPlugin("CustomArmorSets"), "bat_form_owner"), PersistentDataType.STRING, player.getUniqueId().toString());
    }

    private void cancelBatForm(Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        UUID uuid = player.getUniqueId();
        batForm.remove(uuid);

        // Remove bat
        Bukkit.getWorlds().forEach(world ->
                world.getEntitiesByClass(Bat.class).forEach(bat -> {
                    String ownerId = bat.getPersistentDataContainer().get(new NamespacedKey(Bukkit.getPluginManager().getPlugin("CustomArmorSets"), "bat_form_owner"), PersistentDataType.STRING);
                    if (ownerId != null && ownerId.equals(uuid.toString())) {
                        bat.remove();
                    }
                })
        );

        player.sendMessage(ChatColor.GRAY + "You return to your true form.");
    }

    @EventHandler
    public void playerDamageEvent(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        if (damager == victim) return;

        // Confirm player is wearing Vampire set
        ArmorSetType set = ArmorUtil.isFullArmorSet(damager);
        if (set != ArmorSetType.VAMPIRE) return;

        // Heal the player by a portion of the damage dealt
        double newHealth = Math.min(
                damager.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(),
                damager.getHealth() + LIFESTEAL_AMOUNT
        );
        damager.setHealth(newHealth);

        // If this hit is lethal, spawn vampire orb
        double finalHealth = victim.getHealth() - event.getFinalDamage();
        if (finalHealth > 0) return;

        Plugin plugin = Bukkit.getPluginManager().getPlugin("CustomArmorSets");
        // Delay spawning until after entity death is processed
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            spawnCosmeticHead(victim.getLocation(), damager);
        }, 1L);
    }

    private void spawnCosmeticHead(Location loc, Player source) {
        Location headLoc = loc.clone().add(0, 1.2, 0);
        ArmorStand stand = (ArmorStand) loc.getWorld().spawnEntity(headLoc, EntityType.ARMOR_STAND);

        stand.setVisible(false);
        stand.setSmall(true);
        stand.setGravity(false);
        stand.setMarker(true);
        stand.setBasePlate(false);
        stand.setInvulnerable(true);
        stand.setSilent(true);
        stand.setHelmet(getCustomSkull("http://textures.minecraft.net/texture/cb47759e963f10257ac363e15f5685c54897e300c2b8d05df0bf35e4b4c3ac82")); // Cosmetic skull

        Plugin plugin = Bukkit.getPluginManager().getPlugin("CustomArmorSets");
        // Mark with PDC so it can be tracked/removed
        stand.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "vampire_kill_mark"),
                PersistentDataType.BYTE,
                (byte) 1
        );

        // Optional: remove it later
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!stand.isDead() && stand.isValid()) stand.remove();
        }, 100L); // 5 seconds later
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
