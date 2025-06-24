package me.remag501.customarmorsets.commands;

import me.remag501.customarmorsets.core.ArmorSetType;
import me.remag501.customarmorsets.utils.ArmorUtil;
import me.remag501.customarmorsets.utils.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.stream.Collectors;

public class CustomArmorSetCommand implements CommandExecutor {

    private final JavaPlugin plugin;

    public CustomArmorSetCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "sender.sendMessage(ChatColor.RED + \"Usage: /customarmorsets give <player> <set> OR /customarmorsets give <set> OR /customarmorsets givepiece <type> <player> <set> OR /customarmorsets getrepairkit <player>");
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            if (args.length == 2 && sender instanceof Player player) {
                // /customarmorsets give <set> (self)
                String setId = args[1];
                giveArmorSet(player, setId);
            } else if (args.length == 3) {
                // /customarmorsets give <player> <set>
                String targetName = args[1];
                String setId = args[2];

                Player target = Bukkit.getPlayerExact(targetName);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player '" + targetName + "' not found.");
                    return true;
                }

                giveArmorSet(target, setId);
                sender.sendMessage(ChatColor.GREEN + "Gave " + setId + " armor set to " + target.getName() + "!");
            } else {
                sender.sendMessage(ChatColor.RED + "Usage: /customarmorsets give <player> <set> OR /customarmorsets give <set>");
            }
        }

        else if (args[0].equalsIgnoreCase("givepiece")) {
            if (args.length != 4) {
                sender.sendMessage(ChatColor.RED + "Usage: /customarmorsets givepiece <type> <player> <set>");
                return true;
            }

            String pieceType = args[1].toUpperCase();
            String playerName = args[2];
            String setId = args[3];

            Player target = Bukkit.getPlayerExact(playerName);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player '" + playerName + "' not found.");
                return true;
            }

            switch (pieceType) {
                case "HELMET" -> giveArmorPiece(target, setId, EquipmentSlot.HEAD);
                case "CHESTPLATE" -> giveArmorPiece(target, setId, EquipmentSlot.CHEST);
                case "LEGGINGS" -> giveArmorPiece(target, setId, EquipmentSlot.LEGS);
                case "BOOTS" -> giveArmorPiece(target, setId, EquipmentSlot.FEET);
                default -> {
                    sender.sendMessage(ChatColor.RED + "Invalid piece type. Use HELMET, CHESTPLATE, LEGGINGS, or BOOTS.");
                    return true;
                }
            }

            sender.sendMessage(ChatColor.GREEN + "Gave " + pieceType + " of " + setId + " to " + target.getName() + ".");
        }

        else if (args.length == 1 && args[0].equalsIgnoreCase("getrepairkit") && sender instanceof Player player) {
            ItemStack repairKit = ItemUtil.createRepairKit(1, 0);
            player.getInventory().addItem(repairKit);
            player.sendMessage(ChatColor.GREEN + "You received an Armor Repair Kit!");
            return true;
        }

        else if (args.length == 2 && args[0].equalsIgnoreCase("getrepairkit")) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }

            target.getInventory().addItem(ItemUtil.createRepairKit(1, 0));
            sender.sendMessage(ChatColor.GREEN + "Repair kit given to " + target.getName() + "!");
            target.sendMessage(ChatColor.GREEN + "You received an Armor Repair Kit!");
            return true;
        }

        else if (args.length == 3 && args[0].equalsIgnoreCase("getrepairkit") && sender instanceof Player player) {
            // Give to self
            try {
                int amount = Integer.parseInt(args[1]);
                int tier = Integer.parseInt(args[2]);

                if (amount <= 0 || tier < 0) {
                    sender.sendMessage(ChatColor.RED + "Amount must be positive and tier must be non-negative.");
                    return true;
                }

                ItemStack repairKit = ItemUtil.createRepairKit(amount, tier);
                player.getInventory().addItem(repairKit);
                player.sendMessage(ChatColor.GREEN + "You received " + amount + " Armor Repair Kit" + (amount > 1 ? "s" : "") + " (Tier " + tier + ")!");
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Usage: /customarmorsets getrepairkit <amount> <tier> [player]");
            }
            return true;
        }

        else if (args.length == 4 && args[0].equalsIgnoreCase("getrepairkit")) {
            // Give to another player
            try {
                int amount = Integer.parseInt(args[1]);
                int tier = Integer.parseInt(args[2]);

                if (amount <= 0 || tier < 0) {
                    sender.sendMessage(ChatColor.RED + "Amount must be positive and tier must be non-negative.");
                    return true;
                }

                Player target = Bukkit.getPlayerExact(args[3]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }

                ItemStack repairKit = ItemUtil.createRepairKit(amount, tier);
                target.getInventory().addItem(repairKit);
                sender.sendMessage(ChatColor.GREEN + "Gave " + amount + " Armor Repair Kit" + (amount > 1 ? "s" : "") + " (Tier " + tier + ") to " + target.getName() + "!");
                target.sendMessage(ChatColor.GREEN + "You received " + amount + " Armor Repair Kit" + (amount > 1 ? "s" : "") + " (Tier " + tier + ")!");
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Usage: /customarmorsets getrepairkit <amount> <tier> [player]");
            }
            return true;
        }

        else {
            sender.sendMessage(ChatColor.RED + "Usage: /customarmorsets give <player> <set> OR /customarmorsets give <set> OR /customarmorsets givepiece <type> <player> <set> OR /customarmorsets getrepairkit <player>");
        }

        return true;
    }

    private void giveArmorSet(Player player, String setId) {
        ArmorSetType.fromId(setId).ifPresentOrElse(type -> {
            ItemStack[] armor = ArmorUtil.createLeatherArmorSet(
                    plugin,
                    type.getDisplayName(),
                    type.getLore(),
                    type.getLeatherColor(),
                    type.getId(),
                    type.getArmorPoints(),
                    type.getDurability(),
                    type.getArmorToughness()
            );

            for (ItemStack item : armor) {
                player.getInventory().addItem(item);
            }

            player.sendMessage(ChatColor.GREEN + "You received the " + type.getDisplayName() + ChatColor.GREEN + " Armor Set!");
        }, () -> {
            player.sendMessage(ChatColor.RED + "Unknown armor set. Try: " +
                    Arrays.stream(ArmorSetType.values())
                            .map(ArmorSetType::getId)
                            .collect(Collectors.joining(", "))
            );
        });
    }

    private void giveArmorPiece(Player player, String setId, EquipmentSlot slot) {
        ArmorSetType.fromId(setId).ifPresentOrElse(type -> {
            ItemStack[] armor = ArmorUtil.createLeatherArmorSet(
                    plugin,
                    type.getDisplayName(),
                    type.getLore(),
                    type.getLeatherColor(),
                    type.getId(),
                    type.getArmorPoints(),
                    type.getDurability(),
                    type.getArmorToughness()
            );

            ItemStack pieceToGive = switch (slot) {
                case HEAD -> armor[0];       // Helmet
                case CHEST -> armor[1];      // Chestplate
                case LEGS -> armor[2];       // Leggings
                case FEET -> armor[3];       // Boots
                default -> null;
            };

            if (pieceToGive != null) {
                player.getInventory().addItem(pieceToGive);
                player.sendMessage(ChatColor.GREEN + "You received the " + slot.name() + " of " + type.getDisplayName() + ChatColor.GREEN + "!");
            } else {
                player.sendMessage(ChatColor.RED + "Invalid armor slot.");
            }

        }, () -> {
            player.sendMessage(ChatColor.RED + "Unknown armor set. Try: " +
                    Arrays.stream(ArmorSetType.values())
                            .map(ArmorSetType::getId)
                            .collect(Collectors.joining(", "))
            );
        });
    }

}
