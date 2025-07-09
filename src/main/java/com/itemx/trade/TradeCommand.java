package com.itemx.trade;

import com.itemx.ItemX;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class TradeCommand implements CommandExecutor, TabCompleter {
    
    private final ItemX plugin;
    
    public TradeCommand(ItemX plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.getPrefix().append(
                plugin.getColorUtil().parseColor("<red>Usage: /trade <gui-name> [player]")
            ));
            return true;
        }
        
        // Treat first argument as GUI name
        return handleOpenGUI(sender, args);
    }
    
    private boolean handleOpenGUI(CommandSender sender, String[] args) {
        String guiName = args[0];
        Player target;
        
        if (args.length >= 2) {
            if (!sender.hasPermission("itemx.trade.admin")) {
                sender.sendMessage(plugin.getPrefix().append(plugin.getMessage("no-permission")));
                return true;
            }
            
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(plugin.getPrefix().append(plugin.getMessage("player-not-found", "%player%", args[1])));
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getPrefix().append(
                    plugin.getColorUtil().parseColor("<red>Console must specify a player name.")
                ));
                return true;
            }
            target = (Player) sender;
        }
        
        if (!sender.hasPermission("itemx.trade.use")) {
            sender.sendMessage(plugin.getPrefix().append(plugin.getMessage("no-permission")));
            return true;
        }
        
        plugin.getTradeManager().openTradeGUI(target, guiName);
        return true;
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Suggest GUI names
            completions.addAll(plugin.getTradeManager().getTradeGUINames());
        } else if (args.length == 2 && plugin.getTradeManager().hasTradeGUI(args[0])) {
            // After GUI name, suggest player names (admin only)
            if (sender.hasPermission("itemx.trade.admin")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            }
        }
        
        return completions;
    }
}