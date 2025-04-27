package me.remag501.customarmorsets.Commands;

import me.remag501.customarmorsets.Core.ArmorSetType;
import me.remag501.customarmorsets.Utils.ArmorUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
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

        if (args.length < 1 || !args[0].equalsIgnoreCase("give")) {
            sender.sendMessage(ChatColor.RED + "Usage: /customarmorsets give <player> <set> OR /customarmorsets give <set>");
            return true;
        }

        if (args.length == 2 && sender instanceof Player player) {
            // /customarmorsets give <set> (self)
            String setId = args[1];
            giveArmorSet(player, setId);
        }
        else if (args.length == 3) {
            // /customarmorsets give <player> <set> (give to another)
            String targetName = args[1];
            String setId = args[2];

            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player '" + targetName + "' not found.");
                return true;
            }

            giveArmorSet(target, setId);
            sender.sendMessage(ChatColor.GREEN + "Gave " + setId + " armor set to " + target.getName() + "!");
        }
        else {
            sender.sendMessage(ChatColor.RED + "Usage: /customarmorsets give <player> <set> OR /customarmorsets give <set>");
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

}
