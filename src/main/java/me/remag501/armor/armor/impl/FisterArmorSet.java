package me.remag501.armor.armor.impl;

import me.remag501.bgscore.api.ability.AbilityDisplay;
import me.remag501.bgscore.api.ability.AbilityService;
import me.remag501.bgscore.api.combat.AttributeService;
import me.remag501.bgscore.api.event.EventService;
import me.remag501.bgscore.api.task.TaskService;
import me.remag501.bgscore.api.util.BGSColor;
import me.remag501.armor.armor.ArmorSet;
import me.remag501.armor.armor.ArmorSetType;
import me.remag501.armor.service.ArmorStateService;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class FisterArmorSet extends ArmorSet {

    private static Map<UUID, NPC> afterImagesOne = new HashMap<>();
    private static Map<UUID, NPC> afterImagesTwo = new HashMap<>();

    private static Map<UUID, Long> dodgeCooldowns = new HashMap<>();
//    private static Map<UUID, Long> abilityCooldowns = new HashMap<>();
    private static Map<UUID, Entity> dodging = new HashMap<>();
//
//    private static final List<UUID> meditating = new ArrayList<UUID>();

    private static final int DODGING_COOLDOWN_TICKS = 2 * 20;
    private static final int ABILITY_COOLDOWN = 15;
    private static final int MEDIATION_TIME = 3;

    private final AbilityService abilityService;
    private final ArmorStateService armorStateService;
    private final AttributeService attributeService;
    private final EventService eventService;
    private final TaskService taskService;

    public FisterArmorSet(EventService eventService, TaskService taskService, ArmorStateService armorStateService,
                          AbilityService abilityService, AttributeService attributeService) {
        super(ArmorSetType.FISTER);
        this.eventService = eventService;
        this.taskService = taskService;
        this.armorStateService = armorStateService;
        this.abilityService = abilityService;
        this.attributeService = attributeService;
    }

    public void applyPassive(Player player) {
        attributeService.applyAttackSpeed(player, type.getId(), 3.0);

        // Create npc after images
        NPC afterImageOne = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, player.getName());
        NPC afterImageTwo = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, player.getName());

        afterImagesOne.put(player.getUniqueId(), afterImageOne);
        afterImagesTwo.put(player.getUniqueId(), afterImageTwo);

        // Register listener(s)
        UUID id = player.getUniqueId();
        // 1. Flight Listener
        eventService.subscribe(PlayerToggleFlightEvent.class)
                .owner(id)
                .namespace(type.getId())
                .filter(e -> e.getPlayer().getUniqueId().equals(id))
                .handler(this::detectFlight);

        // 2. Damage Listener
        eventService.subscribe(EntityDamageByEntityEvent.class)
                .owner(id)
                .namespace(type.getId())
                .filter(e -> e.getDamager() instanceof Player || e.getDamager() instanceof Projectile || e.getEntity() instanceof Player)
                .handler(this::playerAttack);

        // 3. Move Listener
        eventService.subscribe(PlayerMoveEvent.class)
                .owner(id)
                .namespace(type.getId())
                .filter(e -> e.getPlayer().getUniqueId().equals(id))
                .filter(e -> abilityService.isActive(id, getType().getId()))
                .handler(this::onMoveWhileMeditating);

        // 4. Interact Listener
        eventService.subscribe(PlayerInteractAtEntityEvent.class)
                .owner(id)
                .namespace(type.getId())
                .filter(e -> e.getPlayer().getUniqueId().equals(id) && e.getHand() == EquipmentSlot.HAND)
                .handler(this::playerInteract);

        // 5. Animation (Swing) Listener
        eventService.subscribe(PlayerAnimationEvent.class)
                .owner(id)
                .namespace(type.getId())
                .filter(e -> e.getPlayer().getUniqueId().equals(id) && e.getAnimationType() == PlayerAnimationType.ARM_SWING)
                .handler(this::onSwing);
    }

    @Override
    public void removePassive(Player player) {
        // Remove player attack speed
        attributeService.resetSource(player, type.getId());

        afterImagesOne.remove(player.getUniqueId()).destroy();
        afterImagesTwo.remove(player.getUniqueId()).destroy();;

        eventService.unregisterListener(player.getUniqueId(), type.getId());
        taskService.stopTask(player.getUniqueId(), "fister_meditate");
        taskService.stopTask(player.getUniqueId(), "fister_task");
        abilityService.reset(player.getUniqueId(), getType().getId());
    }

    @Override
    public void triggerAbility(Player player) {
        UUID uuid = player.getUniqueId();

        // Cancel meditative state if already active
        if (abilityService.isActive(uuid, getType().getId())) {
            Vector launch = player.getLocation().getDirection().multiply(1.2).setY(0.4);
            player.setVelocity(launch);
            endMeditation(player);
            return;
        }

        // Check if player is on cooldown
        if (!abilityService.isReady(uuid, getType().getId())) {
            long remaining = (abilityService.getRemainingMillis(uuid, getType().getId())) / 1000;
            player.sendMessage(BGSColor.NEGATIVE + "Your meditate ability is on cooldown for another " + remaining + " seconds.");
            return;
        }

        // Start cooldown
        abilityService.start(uuid, getType().getId(), Duration.ofSeconds(MEDIATION_TIME),  Duration.ofSeconds(ABILITY_COOLDOWN), AbilityDisplay.XP_BAR);

        // Begin meditative state
        Location floatLocation = player.getLocation().clone().add(0, 3, 0);
        player.teleport(floatLocation);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setFlySpeed(0);
        player.setInvulnerable(true);
        attributeService.applyMaxHealth(player, type.getId(), 0.5);

        // Ring particles + aura
        World world = player.getWorld();
        AtomicReference<Double> angle = new AtomicReference<>((double) 0);

        taskService.subscribe(uuid, "fister_meditate", 0, 4, (ticks) -> {
            if (!abilityService.isActive(uuid, getType().getId())) {
                return true;
            }
            angle.updateAndGet(v -> (double) (v + Math.PI / 8));
            for (double i = 0; i < 2 * Math.PI; i += Math.PI / 8) {
                double x = Math.cos(i + angle.get()) * 2;
                double z = Math.sin(i + angle.get()) * 2;
                world.spawnParticle(Particle.END_ROD, floatLocation.clone().add(x, -1, z), 0);
            }
            world.spawnParticle(Particle.ENCHANT, floatLocation, 5, 0.5, 0.5, 0.5, 0);
            // Constantly add regen
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20, 3));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 20, 3));
            return false;
        });

        // Sound effect
        world.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1f);

        // Pushback + debuffs on nearby entities
        for (Entity nearby : player.getNearbyEntities(5, 5, 5)) {
            if (nearby instanceof LivingEntity le && !le.equals(player)) {
                Vector knockback = le.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(1.5);
                knockback.setY(0.5);
                le.setVelocity(knockback);

                le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 1));
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2));
                le.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 60, 0));
            }
        }

        taskService.delay(55, () -> {
            if (abilityService.isActive(uuid, getType().getId()))
                endMeditation(player);
        });
    }

    private void endMeditation(Player player) {
        UUID uuid = player.getUniqueId();

        player.setInvulnerable(false);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setFlySpeed((float)0.1);

        // Apply potion effects
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 1)); // 3 seconds
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 100, 0));

        // Play exit sound and particles
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1f, 1.2f);
        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 10);

        abilityService.stopChanneling(uuid, getType().getId());

        // Start timer to remove hp
        taskService.delay(100, () -> {
            attributeService.removeModifier(player, Attribute.MAX_HEALTH,type.getId());
        });
    }

    @EventHandler
    public void detectFlight(PlayerToggleFlightEvent event) {
        // We already know it's the right player because of .owner(id)
        if (abilityService.isActive(event.getPlayer().getUniqueId(), getType().getId())) {
            endMeditation(event.getPlayer());
        }
    }

    @EventHandler
    public void onMoveWhileMeditating(PlayerMoveEvent event) {
        if (!event.getFrom().toVector().equals(event.getTo().toVector())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void playerAttack(EntityDamageByEntityEvent event) {
        // Check if damaged entity is invulerable
        if (event.getEntity() instanceof Player damaged && dodging.getOrDefault(damaged.getUniqueId(), null) == event.getDamager()) {
            event.setCancelled(true);
            // Subtitle feedback
            String message = "§a§l(!) §aYou dodged the hit!";
            damaged.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
            // Sound effect
            damaged.getWorld().playSound(damaged.getLocation(), Sound.ENTITY_IRON_GOLEM_REPAIR, 1f, 1.2f);
            // Particle effect
            damaged.getWorld().spawnParticle(Particle.SWEEP_ATTACK, damaged.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0);
            return;
        }

        // Prevent ranged attacks
        Entity damager = event.getDamager();
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player p) {

            if (armorStateService.isWearing(damager.getUniqueId(), ArmorSetType.FISTER)) {
                event.setCancelled(true);
                return; // Only reduce for arrow/trident, not snowball/egg/etc., actually it reduces for all in this case
            }
        }

        // Check if player is wearing armor and apply after image passive
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(armorStateService.isWearing(damager.getUniqueId(), ArmorSetType.FISTER))) return;

        if (player.getInventory().getItemInMainHand().getType() != Material.AIR) {
            event.setCancelled(true);
            return;
        }

        Entity target = event.getEntity();

        NPC afterImageOne = afterImagesOne.get(player.getUniqueId());
        NPC afterImageTwo = afterImagesTwo.get(player.getUniqueId());

        taskService.subscribe(player.getUniqueId(), "fister_task", 0, 5, (tick) -> {

            if (tick == 5 || tick == 10) {
                Location offset = target.getLocation().add(randomOffset(player, target, tick == 5 ? 1 : -1));
                NPC afterImage = tick == 5 ? afterImageOne : afterImageTwo;
                spawnAfterImage(afterImage, offset, target);
                giveNPCArmor(player, afterImage);
            }
            if (tick == 15) {
                afterImageOne.despawn();
            }
            if (tick == 20) {
                afterImageTwo.despawn();
                return true;
            }

            return false;
        });

    }

    private void giveNPCArmor(Player player, NPC npc) {
        if (npc == null || !npc.isSpawned()) return;

        if (npc.getEntity() instanceof Player npcPlayer) {
            PlayerInventory playerInv = player.getInventory();
            PlayerInventory npcInv = npcPlayer.getInventory();

            npcInv.setHelmet(playerInv.getHelmet());
            npcInv.setChestplate(playerInv.getChestplate());
            npcInv.setLeggings(playerInv.getLeggings());
            npcInv.setBoots(playerInv.getBoots());

            npcPlayer.updateInventory(); // optional but helps visuals
            npc.getEntity().teleport(npc.getEntity().getLocation()); // force client-side sync
        }
    }

    private Vector randomOffset(Player player, Entity target, int side) {
        // Get direction from player to target on the XZ plane
        Vector toTarget = target.getLocation().toVector().subtract(player.getLocation().toVector());
        toTarget.setY(0).normalize();

        // Get the perpendicular vector (right direction)
        Vector right = new Vector(-toTarget.getZ(), 0, toTarget.getX()).normalize();

        // Scale left (-1) or right (+1) with some jitter
        Vector offset = right.multiply(1.2 * side); // ~0.8 blocks to each side
        offset.add(new Vector(
                (Math.random() - 0.5) * 0.2, // small XZ jitter
                0.4 + Math.random() * 0.2,    // vertical lift
                (Math.random() - 0.5) * 0.2
        ));

        return offset;
    }

    private void spawnAfterImage(NPC npc, Location location, Entity target) {
        npc.spawn(location);
        Entity npcEntity = npc.getEntity();
        npcEntity.setVelocity(new Vector(0, 0, 0)); // upward hop
        target.getWorld().spawnParticle(Particle.CRIT, npcEntity.getLocation(), 10);

        // Rotate the NPC to look at the target
        npc.faceLocation(target.getLocation());

        // Optional: deal damage
        if (target instanceof LivingEntity victim) {
            victim.damage(3, npcEntity);
        }
    }

    @EventHandler
    public void playerInteract(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        Entity clicked = event.getRightClicked();

        // Handle npc case
        for (NPC npc : afterImagesOne.values()) {
            if (npc.isSpawned() && npc.getEntity().getUniqueId().equals(clicked.getUniqueId())) {
                swapLocation(player, npc);
                return;
            }
        }

        for (NPC npc : afterImagesTwo.values()) {
            if (npc.isSpawned() && npc.getEntity().getUniqueId().equals(clicked.getUniqueId())) {
                swapLocation(player, npc);
                return;
            }
        }

        // Allow blocks, check if block is on cooldown
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (dodgeCooldowns.containsKey(uuid) && now < dodgeCooldowns.get(uuid)) {
            // Subtitle feedback
            long remaining = (dodgeCooldowns.get(uuid) - now) / 1000;
            String message = "§c§l(!) §cYou have " + remaining + " §cseconds before you can block again.";
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
            return;
        }

        dodging.put(uuid, clicked);
        // Player is invulnerable for half a second
        taskService.delay(10, () -> {
            dodging.remove(uuid);
        });

        dodgeCooldowns.put(uuid, now + DODGING_COOLDOWN_TICKS * 50);

    }

    private void swapLocation(Player player, NPC npc) {
        Location oldLocation = player.getLocation();
        player.teleport(npc.getEntity().getLocation());
        npc.teleport(oldLocation, PlayerTeleportEvent.TeleportCause.UNKNOWN);
    }

    @EventHandler
    public void onSwing(PlayerAnimationEvent event) {
        Player player = event.getPlayer();
        Entity target = getTargetedEntity(player, 4);

        if (target instanceof Arrow arrow) {
            arrow.remove();
            Location loc = arrow.getLocation();

            loc.getWorld().playSound(loc, Sound.ENTITY_ITEM_BREAK, 1.0f, 1.2f);

            loc.getWorld().spawnParticle(Particle.ITEM, loc, 10, 0.1, 0.1, 0.1, new ItemStack(Material.ARROW));
            player.sendMessage(BGSColor.POSITIVE + "Arrow shattered!");
        }
    }


    private Entity getTargetedEntity(Player player, double maxDistance) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        World world = player.getWorld();

        // Get nearby entities within max range
        for (Entity entity : world.getNearbyEntities(eye, maxDistance, maxDistance, maxDistance)) {
            if (entity == player) continue;

            BoundingBox box = entity.getBoundingBox().expand(0.3); // expand slightly for hitbox margin
            for (double i = 0; i < maxDistance; i += 0.1) {
                Vector point = eye.toVector().add(direction.clone().multiply(i));
                if (box.contains(point)) {
                    return entity;
                }
            }
        }
        return null;
    }

}

