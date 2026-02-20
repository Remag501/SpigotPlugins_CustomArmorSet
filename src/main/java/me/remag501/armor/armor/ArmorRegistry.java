package me.remag501.armor.armor;

import me.remag501.bgscore.api.ability.AbilityService;
import me.remag501.bgscore.api.combat.AttributeService;
import me.remag501.bgscore.api.combat.CombatStatsService;
import me.remag501.bgscore.api.event.EventService;
import me.remag501.bgscore.api.namespace.NamespaceService;
import me.remag501.bgscore.api.task.TaskService;
import me.remag501.armor.armor.impl.*;
import me.remag501.armor.manager.PlayerSyncManager;
import me.remag501.armor.service.ArmorStateService;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ArmorRegistry {

    private final Map<ArmorSetType, ArmorSet> setRegistry = new HashMap<>();

    /**
     * The Constructor acts as the "Assembly Line".
     * We pass in every service needed by any armor set.
     */
    public ArmorRegistry(
            TaskService taskService,
            EventService eventService,
            AttributeService attributeService,
            AbilityService abilityService,
            CombatStatsService combatStatsService,
            PlayerSyncManager playerSyncManager,
            NamespaceService namespaceService,
            ArmorStateService stateService // Replaces 'this' from ArmorManager
    ) {
        // --- THE WIRING HUB ---
        register(ArmorSetType.SNOWMAN, new SnowmanArmorSet());

        register(ArmorSetType.INFERNUS, new InfernusArmorSet(eventService, taskService, abilityService));

        register(ArmorSetType.DEVOID, new DevoidArmorSet(taskService, abilityService));

        register(ArmorSetType.LAST_SPARTAN, new LastSpartanArmorSet(eventService, taskService, abilityService, attributeService, combatStatsService));

        register(ArmorSetType.VIKING_CAPTAIN, new VikingCaptainArmorSet(taskService, combatStatsService, abilityService));

        register(ArmorSetType.ROYAL_KNIGHT, new RoyalKnightArmorSet(abilityService, attributeService));

        register(ArmorSetType.WORLD_GUARDIAN, new WorldGuardianArmorSet(eventService, abilityService, attributeService));

        register(ArmorSetType.VAMPIRE, new VampireArmorSet(eventService, taskService, stateService, abilityService, attributeService, namespaceService));

        register(ArmorSetType.FISTER, new FisterArmorSet(eventService, taskService, stateService, abilityService, attributeService));

        register(ArmorSetType.ARCHER, new ArcherArmorSet(eventService, abilityService, attributeService));

        register(ArmorSetType.NECROMANCER, new NecromancerArmorSet(eventService, taskService, stateService, combatStatsService, attributeService, playerSyncManager, namespaceService));

        register(ArmorSetType.ICEMAN, new IcemanArmorSet(eventService, taskService, stateService, abilityService, attributeService));

        register(ArmorSetType.GOLEM_BUSTER, new GolemBusterArmorSet(eventService, taskService, abilityService, attributeService, combatStatsService));

        register(ArmorSetType.BANDIT, new BanditArmorSet(eventService, taskService, stateService, abilityService, attributeService));
    }

    private void register(ArmorSetType type, ArmorSet instance) {
        setRegistry.put(type, instance);
    }

    public ArmorSet get(ArmorSetType type) {
        return setRegistry.get(type);
    }

    public Collection<ArmorSet> getAllSets() {
        return setRegistry.values();
    }
}