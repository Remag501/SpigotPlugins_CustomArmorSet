package me.remag501.customarmorsets.Commands;

import me.remag501.customarmorsets.Utils.ArmorUtil;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

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

        if (args.length < 2 || !args[1].equalsIgnoreCase("snowman")) {
            player.sendMessage(ChatColor.RED + "Unknown armor set. Try: snowman");
            return true;
        }

        ItemStack[] snowmanArmor = ArmorUtil.createLeatherArmorSet(
                plugin,
                "Snowman",
                Arrays.asList(
                        ChatColor.GRAY + "Stay frosty.",
                        ChatColor.AQUA + "Full set bonus: Speed I",
                        ChatColor.AQUA + "Offhand Ability: Throw snowballs!"
                ),
                Color.WHITE,
                "snowman"
        );

        for (ItemStack piece : snowmanArmor) {
            player.getInventory().addItem(piece);
        }

        player.sendMessage(ChatColor.GREEN + "You received the Snowman Armor Set!");
        return true;
    }
}
