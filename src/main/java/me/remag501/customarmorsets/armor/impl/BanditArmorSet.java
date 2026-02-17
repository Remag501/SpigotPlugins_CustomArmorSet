package me.remag501.customarmorsets.armor.impl;

import me.remag501.bgscore.api.ability.AbilityDisplay;
import me.remag501.bgscore.api.ability.AbilityService;
import me.remag501.bgscore.api.combat.AttributeService;
import me.remag501.bgscore.api.event.EventService;
import me.remag501.bgscore.api.task.TaskService;
import me.remag501.bgscore.api.util.BGSColor;
import me.remag501.customarmorsets.armor.ArmorSet;
import me.remag501.customarmorsets.armor.ArmorSetType;
import me.remag501.customarmorsets.manager.ArmorManager;
import me.remag501.customarmorsets.service.ArmorStateService;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class BanditArmorSet extends ArmorSet {

    // A random number generator for the item drop chance.
    private final Random random = new Random();

    // The maximum number of dodges a player can store.
    private final int MAX_DODGES = 3;

    // Regeneration time for doge
    private final int DODGE_COOLDOWN = 5;

    private final ArmorStateService armorStateService;
    private final AbilityService abilityService;
    private final AttributeService attributeService;
    private final EventService eventService;
    private final TaskService taskService;

    public BanditArmorSet(EventService eventService, TaskService taskService, ArmorStateService armorStateService,
                          AbilityService abilityService, AttributeService attributeService) {
        super(ArmorSetType.BANDIT);
        this.eventService = eventService;
        this.taskService = taskService;
        this.armorStateService = armorStateService;
        this.abilityService = abilityService;
        this.attributeService = attributeService;
    }

    @Override
    public void applyPassive(Player player) {

        UUID id = player.getUniqueId();
        eventService.subscribe(PlayerMoveEvent.class)
                .owner(id)
                .namespace(type.getId())
                .filter(e -> e.getPlayer().getUniqueId().equals(id))
                .handler(this::onPlayerMove);

        eventService.subscribe(EntityDamageByEntityEvent.class)
                .owner(id)
                .namespace(type.getId())
                .filter(e -> e.getDamager().getUniqueId().equals(id) && e.getEntity() instanceof Player)
                .handler(this::onEntityDamageByEntity);

        abilityService.setupStocks(id, getType().getId(), MAX_DODGES, Duration.ofSeconds(DODGE_COOLDOWN), AbilityDisplay.XP_BAR);
    }

    @Override
    public void removePassive(Player player) {
        // Cancel the regeneration task when the set is removed.
        UUID uuid = player.getUniqueId();
        eventService.unregisterListener(uuid, type.getId());
        taskService.stopTask(uuid, "bandit_task");
        attributeService.resetSource(player, type.getId());
        abilityService.reset(uuid, type.getId());
    }

    // --- Passive 1: Run Faster With Fists Out ---
    // This event handler is necessary to check if the player's hands are empty.
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!(armorStateService.isWearing(player.getUniqueId(), ArmorSetType.BANDIT))) {
            return;
        }

        // This is much cleaner now!
        if (player.getInventory().getItemInMainHand().getType() == Material.AIR) {
            attributeService.applySpeed(player, type.getId(), 0.25); // Apply a 25% speed increase
        } else {
            attributeService.resetSource(player, type.getId()); // Remove the speed increase
        }
    }

    // --- Passive 2: 10% Chance to Drop Non-Hotbar Item ---
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }
        if (!(armorStateService.isWearing(attacker.getUniqueId(), ArmorSetType.BANDIT))) {
            return;
        }
        if (!(event.getEntity() instanceof Player target)) {
            return;
        }

        if (random.nextInt(100) < 10) {
            PlayerInventory targetInv = target.getInventory();
            for (int i = 9; i < targetInv.getSize(); i++) {
                ItemStack item = targetInv.getItem(i);
                if (item != null && item.getType() != Material.AIR) {
                    target.getWorld().dropItemNaturally(target.getLocation(), item);
                    targetInv.setItem(i, new ItemStack(Material.AIR));
                    target.sendMessage("§c§l(!) §cYou feel your pockets getting lighter!");
                    attacker.sendMessage("§a§l(!) §aA piece of loot was dropped!");
                    break;
                }
            }
        }
    }

    @Override
    public void triggerAbility(Player player) {
        UUID uuid = player.getUniqueId();
        int currentDodges = abilityService.getAvailableStacks(player.getUniqueId(), getType().getId());

        if (abilityService.isReady(uuid, getType().getId())) {
            // Get the normalized direction
            Vector direction = player.getLocation().getDirection().normalize();

            // Multiply horizontal components (X and Z) by 1.5 for a strong dash
            double x = direction.getX() * 1.5;
            double z = direction.getZ() * 1.5;

            // Multiply the vertical component (Y) by a smaller factor (e.g., 0.5)
            // to prevent excessive upward flying
            double y = direction.getY() * 0.75;

            // Reconstruct the vector and apply it
            Vector velocity = new Vector(x, y, z);
            player.setVelocity(velocity);

            player.playSound(player.getLocation(), Sound.ENTITY_HORSE_JUMP, 1.0f, 2.0f);
            player.sendMessage(BGSColor.POSITIVE + "You dodged! Dodges left: " + currentDodges);

            abilityService.consumeStack(uuid, getType().getId());

        } else {
            player.sendMessage(BGSColor.NEGATIVE + "You have no dodges left!");
        }
    }
}
