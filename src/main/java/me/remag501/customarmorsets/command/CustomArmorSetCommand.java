package me.remag501.customarmorsets.command;

import me.remag501.customarmorsets.armor.ArmorSetType;
import me.remag501.customarmorsets.service.ArmorService;
import me.remag501.customarmorsets.service.ItemService;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CustomArmorSetCommand implements CommandExecutor {

    private final JavaPlugin plugin;

    public CustomArmorSetCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length < 1) {
            sender.sendMessage("§b§lARMOR §8» §cUsage: /customarmorsets <give|givepiece|getrepairkit> ...");
            return true;
        }

        // Parse global -e flag if present
        Map<Enchantment, Integer> extraEnchants = parseEnchantmentFlag(args);

        // Clean args for logic (removes -e and trailing enchant data)
        String[] cleanArgs = Arrays.stream(args)
                .filter(arg -> !arg.equalsIgnoreCase("-e") && !isEnchantData(arg, extraEnchants))
                .toArray(String[]::new);

        if (cleanArgs[0].equalsIgnoreCase("give")) {
            if (cleanArgs.length == 2 && sender instanceof Player player) {
                giveArmorSet(player, cleanArgs[1], extraEnchants);
            } else if (cleanArgs.length == 3) {
                Player target = Bukkit.getPlayerExact(cleanArgs[1]);
                if (target == null) {
                    sender.sendMessage("§b§lARMOR §8» §cPlayer not found.");
                    return true;
                }
                giveArmorSet(target, cleanArgs[2], extraEnchants);
                sender.sendMessage("§b§lARMOR §8» §aGave " + cleanArgs[2] + " armor set to " + target.getName() + "!");
            } else {
                sender.sendMessage("§b§lARMOR §8» §cUsage: /customarmorsets give <player> <set> [-e ...]");
            }
        }

        else if (cleanArgs[0].equalsIgnoreCase("givepiece")) {
            if (cleanArgs.length != 4) {
                sender.sendMessage("§b§lARMOR §8» §cUsage: /customarmorsets givepiece <type> <player> <set> [-e ...]");
                return true;
            }

            String pieceType = cleanArgs[1].toUpperCase();
            String playerName = cleanArgs[2];
            String setId = cleanArgs[3];

            Player target = Bukkit.getPlayerExact(playerName);
            if (target == null) {
                sender.sendMessage("§b§lARMOR §8» §cPlayer '" + playerName + "' not found.");
                return true;
            }

            EquipmentSlot slot = switch (pieceType) {
                case "HELMET" -> EquipmentSlot.HEAD;
                case "CHESTPLATE" -> EquipmentSlot.CHEST;
                case "LEGGINGS" -> EquipmentSlot.LEGS;
                case "BOOTS" -> EquipmentSlot.FEET;
                default -> null;
            };

            if (slot == null) {
                sender.sendMessage("§b§lARMOR §8» §cInvalid piece type. Use HELMET, CHESTPLATE, LEGGINGS, or BOOTS.");
                return true;
            }

            giveArmorPiece(target, setId, slot, extraEnchants);
            sender.sendMessage("§b§lARMOR §8» §aGave " + pieceType + " of " + setId + " to " + target.getName() + ".");
        }

        else if (args.length == 1 && args[0].equalsIgnoreCase("getrepairkit") && sender instanceof Player player) {
            ItemStack repairKit = ItemService.createRepairKit(1, 0);
            player.getInventory().addItem(repairKit);
            player.sendMessage("§b§lARMOR §8» §7You received an Armor Repair Kit!");
            return true;
        }

        else if (args.length == 2 && args[0].equalsIgnoreCase("getrepairkit")) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage("§b§lARMOR §8» §cPlayer not found.");
                return true;
            }

            target.getInventory().addItem(ItemService.createRepairKit(1, 0));
            sender.sendMessage("§b§lARMOR §8» §aRepair kit given to " + target.getName() + "!");
            target.sendMessage("§b§lARMOR §8» §7You received an Armor Repair Kit!");
            return true;
        }

        else if (args.length == 3 && args[0].equalsIgnoreCase("getrepairkit") && sender instanceof Player player) {
            // Give to self
            try {
                int amount = Integer.parseInt(args[1]);
                int tier = Integer.parseInt(args[2]);

                if (amount <= 0 || tier < 0) {
                    sender.sendMessage("§b§lARMOR §8» §cAmount must be positive and tier must be non-negative.");
                    return true;
                }

                ItemStack repairKit = ItemService.createRepairKit(amount, tier);
                player.getInventory().addItem(repairKit);
                player.sendMessage("§b§lARMOR §8» §7You received " + amount + " Armor Repair Kit" + (amount > 1 ? "s" : "") + " (Tier " + tier + ")!");
            } catch (NumberFormatException e) {
                sender.sendMessage("§b§lARMOR §8» §cUsage: /customarmorsets getrepairkit <amount> <tier> [player]");
            }
            return true;
        }

        else if (args.length == 4 && args[0].equalsIgnoreCase("getrepairkit")) {
            // Give to another player
            try {
                int amount = Integer.parseInt(args[1]);
                int tier = Integer.parseInt(args[2]);

                if (amount <= 0 || tier < 0) {
                    sender.sendMessage("§b§lARMOR §8» §cAmount must be positive and tier must be non-negative.");
                    return true;
                }

                Player target = Bukkit.getPlayerExact(args[3]);
                if (target == null) {
                    sender.sendMessage("§b§lARMOR §8» §cPlayer not found.");
                    return true;
                }

                ItemStack repairKit = ItemService.createRepairKit(amount, tier);
                target.getInventory().addItem(repairKit);
                sender.sendMessage("§b§lARMOR §8» §aGave " + amount + " Armor Repair Kit" + (amount > 1 ? "s" : "") + " (Tier " + tier + ") to " + target.getName() + "!");
                target.sendMessage("§b§lARMOR §8» §7You received " + amount + " Armor Repair Kit" + (amount > 1 ? "s" : "") + " (Tier " + tier + ")!");
            } catch (NumberFormatException e) {
                sender.sendMessage("§b§lARMOR §8» §cUsage: /customarmorsets getrepairkit <amount> <tier> [player]");
            }
            return true;
        }

        else {
            sender.sendMessage("§b§lARMOR §8» §aUsage: /customarmorsets give <player> <set> OR /customarmorsets give <set> OR /customarmorsets givepiece <type> <player> <set> OR /customarmorsets getrepairkit <player>");
        }

        return true;
    }

    private void giveArmorSet(Player player, String setId, Map<Enchantment, Integer> enchants) {
        ArmorSetType.fromId(setId).ifPresent(type -> {
            ItemStack[] armor = ArmorService.createLeatherArmorSet(plugin, type.getDisplayName(), type.getLore(), type.getLeatherColor(), type.getRarity(), type.getCustomModelData(), type.getId(), type.getArmorPoints(), type.getDurability(), type.getArmorToughness());
            for (ItemStack item : armor) {
                if (item != null) {
                    enchants.forEach(item::addUnsafeEnchantment);
                    player.getInventory().addItem(item);
                }
            }
            player.sendMessage("§b§lARMOR §8» §7You received the " + type.getDisplayName() + " Armor Set!");
        });
    }

    /**
     * Parses the -e flag and returns a map of Enchantments and their Levels.
     * Expects format: -e protection 4 sharpness 5
     */
    private Map<Enchantment, Integer> parseEnchantmentFlag(String[] args) {
        Map<Enchantment, Integer> enchantMap = new HashMap<>();
        int flagIndex = -1;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-e")) {
                flagIndex = i;
                break;
            }
        }

        if (flagIndex != -1) {
            // Iterate through pairs after -e
            for (int i = flagIndex + 1; i < args.length; i += 2) {
                if (i + 1 >= args.length) break; // No level found for this enchant

                String enchantName = args[i].toLowerCase().replace(",", "");
                String levelStr = args[i+1].replace(",", "");

                Enchantment enchant = Enchantment.getByKey(NamespacedKey.minecraft(enchantName));
                if (enchant != null) {
                    try {
                        int level = Integer.parseInt(levelStr);
                        enchantMap.put(enchant, level);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return enchantMap;
    }

    private boolean isEnchantData(String arg, Map<Enchantment, Integer> parsed) {
        // Helper to filter out the values used by the flag during arg cleaning
        for (Enchantment e : parsed.keySet()) {
            if (arg.equalsIgnoreCase(e.getKey().getKey())) return true;
        }
        for (Integer i : parsed.values()) {
            if (arg.equalsIgnoreCase(String.valueOf(i))) return true;
        }
        return false;
    }

    private void giveArmorPiece(Player player, String setId, EquipmentSlot slot, Map<Enchantment, Integer> enchants) {
        ArmorSetType.fromId(setId).ifPresentOrElse(type -> {
            ItemStack[] armor = ArmorService.createLeatherArmorSet(plugin, type.getDisplayName(), type.getLore(), type.getLeatherColor(), type.getRarity(), type.getCustomModelData(), type.getId(), type.getArmorPoints(), type.getDurability(), type.getArmorToughness());

            ItemStack pieceToGive = switch (slot) {
                case HEAD -> armor[0];
                case CHEST -> armor[1];
                case LEGS -> armor[2];
                case FEET -> armor[3];
                default -> null;
            };

            if (pieceToGive != null) {
                enchants.forEach(pieceToGive::addUnsafeEnchantment);
                player.getInventory().addItem(pieceToGive);
                player.sendMessage("§b§lARMOR §8» §7You received the " + slot.name() + " of " + type.getDisplayName() + "!");
            }
        }, () -> {
            player.sendMessage("§b§lARMOR §8» §cUnknown armor set.");
        });
    }

}
