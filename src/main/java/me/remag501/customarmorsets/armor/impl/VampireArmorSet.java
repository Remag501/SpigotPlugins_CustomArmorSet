package me.remag501.customarmorsets.armor.impl;

import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import me.remag501.customarmorsets.armor.ArmorSet;
import me.remag501.customarmorsets.armor.ArmorSetType;
import me.remag501.customarmorsets.core.CustomArmorSetsCore;
import me.remag501.customarmorsets.util.AttributesUtil;
import me.remag501.customarmorsets.util.CooldownBarUtil;
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
import java.util.*;


public class VampireArmorSet extends ArmorSet implements Listener {

    private static final int RADIUS = 7;
    private static final int DURATION_TICKS = 60; // 3 seconds
    private static final int INTERVAL_TICKS = 5; // every 0.5s
    private static final int COOLDOWN_TICKS = 15 * 20; // 15 seconds
    private static final double LIFESTEAL_AMOUNT = 0.3;

    private static final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final Set<UUID> batForm = new HashSet<>();
    private final Map<UUID, List<Bat>> cosmeticBats = new HashMap<>();
    private final Map<UUID, BukkitRunnable> batTasks = new HashMap<>();

    public VampireArmorSet() {
        super(ArmorSetType.VAMPIRE);
    }

    @Override
    public void applyPassive(Player player) {
        AttributesUtil.applyHealth(player, 0.5);
    }

    @Override
    public void removePassive(Player player) {
        AttributesUtil.removeHealth(player);
        batForm.remove(player.getUniqueId());
    }

    @Override
    public void triggerAbility(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        // Bat form early exit toggle
        if (batForm.contains(uuid)) {
            cancelBatForm(player);
            return;
        }

        Plugin plugin = Bukkit.getPluginManager().getPlugin("CustomArmorSets");

        // Check for vampire orb in 5 block radius
        for (Entity entity : player.getNearbyEntities(RADIUS, RADIUS, RADIUS)) {
            if (entity instanceof ArmorStand stand) {
                NamespacedKey key = new NamespacedKey(plugin, "vampire_kill_mark");
                if (stand.getPersistentDataContainer().has(key, PersistentDataType.BYTE)
                        && stand.getLocation().distanceSquared(player.getLocation()) <= RADIUS * RADIUS) {
                    stand.remove(); // destroy the orb
                    player.sendMessage(ChatColor.DARK_RED + "You absorb the vampire orb!");

                    // Logic for morphing or healing
                    if (player.isSneaking()) {
                        // Bat form logic
                        enterBatForm(player);
                        spawnBatStorm(player);
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (batForm.contains(uuid)) cancelBatForm(player);
                        }, DURATION_TICKS);
                        return;

                    } else {
                        if (player.getHealth() / 10.0 >= 0.75)
                            player.setAbsorptionAmount(4.0 + player.getAbsorptionAmount());
                        else
                            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, DURATION_TICKS, 3));
                        return;
                    }

                }
            }
        }

        // Now we check cooldown before performing main ability
        if (cooldowns.containsKey(uuid) && now < cooldowns.get(uuid)) {
            long remaining = (cooldowns.get(uuid) - now) / 1000;
            player.sendMessage(ChatColor.RED + "Vampire ability on cooldown (" + remaining + "s left)");
            return;
        }

        CooldownBarUtil.startCooldownBar(plugin, player, DURATION_TICKS / 20);

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
                        target.damage(3, player);
                        player.setHealth(Math.min(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(), player.getHealth() + 1.0));
                        drawParticleLine(target.getEyeLocation(), player.getEyeLocation(), Particle.REDSTONE, Color.MAROON);
                    }
                }

                ticksRun += INTERVAL_TICKS;
            }
        }.runTaskTimer(plugin, 0L, INTERVAL_TICKS);

        // Store cooldown
        cooldowns.put(uuid, now + COOLDOWN_TICKS * 50);
        CooldownBarUtil.startCooldownBar(plugin, player, COOLDOWN_TICKS / 20);

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

        // Make them fly like a bat
        player.setAllowFlight(true);
        player.setFlying(true);
        player.teleport(player.getLocation().add(0, 2, 0));
        player.setInvulnerable(true);
        // Display player as bat
        MobDisguise disguise = new MobDisguise(DisguiseType.BAT);
        disguise.setReplaceSounds(true); // Optional: mob sounds instead of player
        DisguiseAPI.disguiseToAll(player, disguise);

        player.setFlySpeed((float) 0.2);

        bat.getPersistentDataContainer().set(new NamespacedKey(Bukkit.getPluginManager().getPlugin("CustomArmorSets"), "bat_form_owner"), PersistentDataType.STRING, player.getUniqueId().toString());
    }

    private void cancelBatForm(Player player) {

        UUID uuid = player.getUniqueId();
        batForm.remove(uuid);

        // Remove modifications from player
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setInvulnerable(false);
        player.setInvisible(false);
        player.setFlySpeed((float) 0.1);
        DisguiseAPI.undisguiseToAll(player);

        // Remove bat
        Bukkit.getWorlds().forEach(world ->
                world.getEntitiesByClass(Bat.class).forEach(bat -> {
                    String ownerId = bat.getPersistentDataContainer().get(new NamespacedKey(Bukkit.getPluginManager().getPlugin("CustomArmorSets"), "bat_form_owner"), PersistentDataType.STRING);
                    if (ownerId != null && ownerId.equals(uuid.toString())) {
                        bat.remove();
                    }
                })
        );

        cleanupBatForm(player);

        player.sendMessage(ChatColor.GRAY + "You return to your true form.");
    }

    private void spawnBatStorm(Player player) {
        World world = player.getWorld();
        UUID uuid = player.getUniqueId();

        // Spawn 4 bats
        List<Bat> bats = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Bat bat = (Bat) world.spawnEntity(player.getLocation().add(0, 0 + i*0.25, 0), EntityType.BAT);
            bat.setInvulnerable(true);
            bat.setSilent(false);
            bat.setAI(true);
            bat.setAware(true);
            bat.setCollidable(false);
            bats.add(bat);
        }
        cosmeticBats.put(uuid, bats);

        // Task to move bats and do AoE damage + warning circle
        BukkitRunnable task = new BukkitRunnable() {
            double t = 0;
            int tickCount = 0;

            @Override
            public void run() {
                if (!player.isOnline() || !batForm.contains(uuid)) {
                    cancel();
                    // Cleanup bats
                    List<Bat> toRemove = cosmeticBats.remove(uuid);
                    if (toRemove != null) toRemove.forEach(Entity::remove);
                    batTasks.remove(uuid);
                    return;
                }

                Location center = player.getLocation().add(0, 1.8, 0);

                // Move bats in circle
                int i = 0;
                for (Bat bat : bats) {
                    Location batLocation = bat.getLocation();
                    if (batLocation.distance(center) > 2)
                        bat.teleport(center.add(0, 1.5*Math.random()-1.5, 0));
                    i++;
                }

                // Spawn ambient dark particles around player continuously
                center.getWorld().spawnParticle(Particle.SMOKE_NORMAL, center, 3, 0.3, 0.1, 0.3, 0.01);
                tickCount++;

                if (tickCount % 5 == 0) {
                    double radius = 2.5;
                    for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
                        if (entity instanceof LivingEntity living && !entity.equals(player)) {
                            living.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 3)); // 2 sec Wither 4
                        }
                    }
                }

                t += 0.05;
            }
        };

        task.runTaskTimer(Bukkit.getPluginManager().getPlugin("CustomArmorSets"), 0L, 2L);
        batTasks.put(uuid, task);
    }

    private void cleanupBatForm(Player player) {
        UUID uuid = player.getUniqueId();

        // Remove cosmetic bats
        List<Bat> bats = cosmeticBats.remove(uuid);
        if (bats != null) {
            bats.forEach(Entity::remove);
        }

        // Cancel running tasks
        BukkitRunnable task = batTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }

        // Reset player states you set in enterBatForm if needed
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setInvulnerable(false);
        DisguiseAPI.undisguiseToAll(player);
    }

    @EventHandler
    public void playerDamageEvent(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        if (damager == victim) return;
        if (batForm.contains(damager.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        // Confirm player is wearing Vampire set
        if (!(CustomArmorSetsCore.getArmorSet(damager) instanceof VampireArmorSet)) return;

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
