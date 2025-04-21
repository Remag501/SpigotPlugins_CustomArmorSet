package me.remag501.customarmorsets.Commands;

import me.remag501.customarmorsets.Core.ArmorSetType;
import me.remag501.customarmorsets.Utils.ArmorUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CustomArmorSetCommand implements CommandExecutor {

    private final JavaPlugin plugin;

    public CustomArmorSetCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (args.length < 1 || !args[0].equalsIgnoreCase("give")) {
            player.sendMessage(ChatColor.RED + "Usage: /customarmorsets give snowman");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /customarmorsets give <set>");
            return true;
        }

        ArmorSetType.fromId(args[1]).ifPresentOrElse(type -> {
            ItemStack[] armor = ArmorUtil.createLeatherArmorSet(
                    plugin,
                    type.getDisplayName(), // use display name instead of capitalized ID
                    List.of(
                            ChatColor.GRAY + "Unique bonus coming soon!",
                            "Full set bonus: TBD"
                    ),
                    type.getLeatherColor(),
                    type.getId()
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

        return true;
    }
}
