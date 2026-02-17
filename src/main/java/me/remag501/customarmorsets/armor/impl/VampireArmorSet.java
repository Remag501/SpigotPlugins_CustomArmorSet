package me.remag501.customarmorsets.armor.impl;

import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import me.remag501.bgscore.api.ability.AbilityDisplay;
import me.remag501.bgscore.api.ability.AbilityService;
import me.remag501.bgscore.api.combat.AttributeService;
import me.remag501.bgscore.api.event.EventService;
import me.remag501.bgscore.api.namespace.NamespaceService;
import me.remag501.bgscore.api.task.TaskService;
import me.remag501.bgscore.api.util.BGSColor;
import me.remag501.customarmorsets.armor.ArmorSet;
import me.remag501.customarmorsets.armor.ArmorSetType;
import me.remag501.customarmorsets.manager.ArmorManager;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.util.Vector;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;


public class VampireArmorSet extends ArmorSet {

    private static final int RADIUS = 7;
    private static final int DURATION_TICKS = 60; // 3 seconds
    private static final int INTERVAL_TICKS = 5; // every 0.5s
    private static final int COOLDOWN_TICKS = 15 * 20; // 15 seconds
    private static final double LIFESTEAL_AMOUNT = 0.3;

    private static final Set<UUID> batForm = new HashSet<>();
    private final Map<UUID, List<Bat>> cosmeticBats = new HashMap<>();
    private final List<UUID> batTasks = new ArrayList<>();

    private final EventService eventService;
    private final TaskService taskService;
    private final ArmorManager armorManager;
    private final AbilityService abilityService;
    private final AttributeService attributeService;
    private final NamespaceService namespaceService;

    public VampireArmorSet(EventService eventService, TaskService taskService, ArmorManager armorManager, AbilityService abilityService,
                           AttributeService attributeService, NamespaceService namespaceService) {
        super(ArmorSetType.VAMPIRE);
        this.eventService = eventService;
        this.taskService = taskService;
        this.armorManager = armorManager;
        this.abilityService = abilityService;
        this.attributeService = attributeService;
        this.namespaceService = namespaceService;
    }

    @Override
    public void applyPassive(Player player) {
        attributeService.applyMaxHealth(player, type.getId(), 0.5);

        // Register listener(s)
        UUID id = player.getUniqueId();
        eventService.subscribe(EntityDamageByEntityEvent.class)
                .owner(id)
                .namespace(type.getId())
                .filter(e -> e.getDamager().getUniqueId().equals(id))
                .handler(this::playerDamageEvent);
    }

    @Override
    public void removePassive(Player player) {
        attributeService.resetSource(player, type.getId());
        batForm.remove(player.getUniqueId());

        eventService.unregisterListener(player.getUniqueId(), type.getId());
        taskService.stopTask(player.getUniqueId(), "vampire_bats");
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


        // Check for vampire orb in 5 block radius
        for (Entity entity : player.getNearbyEntities(RADIUS, RADIUS, RADIUS)) {
            if (entity instanceof ArmorStand stand) {
                NamespacedKey key = namespaceService.getVampireKillMarkKey();
                if (stand.getPersistentDataContainer().has(key, PersistentDataType.BYTE)
                        && stand.getLocation().distanceSquared(player.getLocation()) <= RADIUS * RADIUS) {
                    stand.remove(); // destroy the orb
                    player.sendMessage(BGSColor.POSITIVE + "You absorb the vampire orb!");

                    // Logic for morphing or healing
                    if (player.isSneaking()) {

                        // Bat form logic
                        enterBatForm(player);
                        spawnBatStorm(player);
                        taskService.delay(DURATION_TICKS, () -> {
                            if (batForm.contains(uuid)) cancelBatForm(player);
                        });
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
        if (abilityService.isReady(uuid, getType().getId())) {
            long remaining = (abilityService.getRemainingMillis(uuid, getType().getId())) / 1000;
            player.sendMessage(BGSColor.NEGATIVE + "Vampire ability on cooldown (" + remaining + "s left)");
            return;
        }

        // Start cooldown
        abilityService.start(uuid, getType().getId(), Duration.ofSeconds(COOLDOWN_TICKS / 20), Duration.ofSeconds(DURATION_TICKS / 20), AbilityDisplay.XP_BAR);


        // Default: drain enemies and heal
        List<LivingEntity> targets = player.getNearbyEntities(RADIUS, RADIUS, RADIUS).stream()
                .filter(e -> e instanceof LivingEntity && e != player)
                .map(e -> (LivingEntity) e)
                .toList();

        taskService.subscribe(player.getUniqueId(), type.getId(), 0, INTERVAL_TICKS, (ticksRun) -> {
            if (ticksRun >= DURATION_TICKS || player.isDead()) {
                return true;
            }

            for (LivingEntity target : targets) {
                if (!target.isDead() && target.getLocation().distanceSquared(player.getLocation()) <= RADIUS * RADIUS) {
                    target.damage(3, player);
                    player.setHealth(Math.min(player.getAttribute(Attribute.MAX_HEALTH).getValue(), player.getHealth() + 1.0));
                    drawParticleLine(target.getEyeLocation(), player.getEyeLocation(), Particle.BLOCK, Color.MAROON);
                }
            }
            return false;
        });


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
        bat.getPersistentDataContainer().set(namespaceService.getBatFormOwnerKey(), PersistentDataType.STRING, player.getUniqueId().toString());

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
                    String ownerId = bat.getPersistentDataContainer().get(namespaceService.getBatFormOwnerKey(), PersistentDataType.STRING);
                    if (ownerId != null && ownerId.equals(uuid.toString())) {
                        bat.remove();
                    }
                })
        );

        cleanupBatForm(player);

        player.sendMessage(BGSColor.POSITIVE + "You return to your human form.");
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
        AtomicReference<Double> t = new AtomicReference<>((double) 0);
        taskService.subscribe(player.getUniqueId(), "vampire_bats", 0, 2, (tickCount) -> {
            if (!player.isOnline() || !batForm.contains(uuid)) {
                // Cleanup bats
                List<Bat> toRemove = cosmeticBats.remove(uuid);
                if (toRemove != null) toRemove.forEach(Entity::remove);
                batTasks.remove(uuid);
                return true;
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
            center.getWorld().spawnParticle(Particle.SMOKE, center, 3, 0.3, 0.1, 0.3, 0.01);
            tickCount++;

            if (tickCount % 5 == 0) {
                double radius = 2.5;
                for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
                    if (entity instanceof LivingEntity living && !entity.equals(player)) {
                        living.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 3)); // 2 sec Wither 4
                    }
                }
            }

            t.updateAndGet(v -> ((double) (v + 0.05)));
            return false;
        });

        batTasks.add(uuid);
    }

    private void cleanupBatForm(Player player) {
        UUID uuid = player.getUniqueId();

        // Remove cosmetic bats
        List<Bat> bats = cosmeticBats.remove(uuid);
        if (bats != null) {
            bats.forEach(Entity::remove);
        }

        // Cancel running tasks
        if (batTasks.contains(uuid)) {
            taskService.stopTask(uuid, "vampire_bats");
        }

        // Reset player states you set in enterBatForm if needed
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setInvulnerable(false);
        DisguiseAPI.undisguiseToAll(player);
    }

    @EventHandler
    public void playerDamageEvent(EntityDamageByEntityEvent event) {
        // Filter ensures damager is our Player and matches the 'id'
        Player damager = (Player) event.getDamager();

        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        if (damager == victim) return;
        if (batForm.contains(damager.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        // Confirm player is wearing Vampire set
        if (!(armorManager.getArmorSet(damager) instanceof VampireArmorSet)) return;

        // Heal the player by a portion of the damage dealt
        double newHealth = Math.min(
                damager.getAttribute(Attribute.MAX_HEALTH).getValue(),
                damager.getHealth() + LIFESTEAL_AMOUNT
        );
        damager.setHealth(newHealth);

        // If this hit is lethal, spawn vampire orb
        double finalHealth = victim.getHealth() - event.getFinalDamage();
        if (finalHealth > 0) return;

        // Delay spawning until after entity death is processed
        taskService.delay(1, () -> {
            spawnCosmeticHead(victim.getLocation(), damager);
        });

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

        // Mark with PDC so it can be tracked/removed
        stand.getPersistentDataContainer().set(
                namespaceService.getVampireKillMarkKey(),
                PersistentDataType.BYTE,
                (byte) 1
        );

        // Optional: remove it later
        taskService.delay(100, () -> {
            if (!stand.isDead() && stand.isValid()) stand.remove();
        });
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
