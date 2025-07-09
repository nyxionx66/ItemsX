package com.itemx.command;

import com.itemx.ItemX;
import com.itemx.item.ItemDefinition;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ItemXCommand implements CommandExecutor, TabCompleter {
    
    private final ItemX plugin;
    
    public ItemXCommand(ItemX plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.getPrefix().append(plugin.getMessage("invalid-usage")));
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "give":
                return handleGive(sender, args);
            case "get":
                return handleGet(sender, args);
            case "reload":
                return handleReload(sender);
            default:
                sender.sendMessage(plugin.getPrefix().append(plugin.getMessage("invalid-usage")));
                return true;
        }
    }
    
    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("itemx.give")) {
            sender.sendMessage(plugin.getPrefix().append(plugin.getMessage("no-permission")));
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(plugin.getPrefix().append(Component.text("Usage: /itemx give <item-id> [player]")));
            return true;
        }
        
        String itemId = args[1];
        Player target;
        
        if (args.length >= 3) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(plugin.getPrefix().append(plugin.getMessage("player-not-found", "%player%", args[2])));
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getPrefix().append(Component.text("Console must specify a player name.")));
                return true;
            }
            target = (Player) sender;
        }
        
        ItemDefinition definition = plugin.getItemManager().getItemDefinition(itemId);
        if (definition == null) {
            sender.sendMessage(plugin.getPrefix().append(plugin.getMessage("item-not-found", "%item%", itemId)));
            return true;
        }
        
        ItemStack item = plugin.getItemManager().createItem(definition);
        target.getInventory().addItem(item);
        
        Component message = plugin.getColorUtil().parseColor(
            plugin.getConfig().getString("give-message", "<green>Gave <yellow>%item%</yellow> to <blue>%player%</blue>")
                .replace("%item%", itemId)
                .replace("%player%", target.getName())
        );
        
        sender.sendMessage(plugin.getPrefix().append(message));
        return true;
    }
    
    private boolean handleGet(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getPrefix().append(Component.text("Only players can use the get command.")));
            return true;
        }
        
        if (!sender.hasPermission("itemx.get")) {
            sender.sendMessage(plugin.getPrefix().append(plugin.getMessage("no-permission")));
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(plugin.getPrefix().append(Component.text("Usage: /itemx get <item-id> or /itemx get category:<category-name>")));
            return true;
        }
        
        Player player = (Player) sender;
        String identifier = args[1];
        
        // Check if it's a category request
        if (identifier.startsWith("category:")) {
            String categoryName = identifier.substring(9); // Remove "category:" prefix
            return handleGetCategory(player, categoryName);
        } else {
            // Single item request
            return handleGetSingle(player, identifier);
        }
    }
    
    private boolean handleGetSingle(Player player, String itemId) {
        ItemDefinition definition = plugin.getItemManager().getItemDefinition(itemId);
        if (definition == null) {
            player.sendMessage(plugin.getPrefix().append(plugin.getMessage("item-not-found", "%item%", itemId)));
            return true;
        }
        
        ItemStack item = plugin.getItemManager().createItem(definition);
        player.getInventory().addItem(item);
        
        Component message = plugin.getColorUtil().parseColor(
            plugin.getConfig().getString("get-message", "<green>You received <yellow>%item%</yellow>")
                .replace("%item%", itemId)
        );
        
        player.sendMessage(plugin.getPrefix().append(message));
        return true;
    }
    
    private boolean handleGetCategory(Player player, String categoryName) {
        Set<ItemDefinition> categoryItems = plugin.getItemManager().getItemsByCategory(categoryName);
        
        if (categoryItems.isEmpty()) {
            player.sendMessage(plugin.getPrefix().append(Component.text("No items found in category: " + categoryName)));
            return true;
        }
        
        int itemsGiven = 0;
        for (ItemDefinition definition : categoryItems) {
            ItemStack item = plugin.getItemManager().createItem(definition);
            player.getInventory().addItem(item);
            itemsGiven++;
        }
        
        Component message = plugin.getColorUtil().parseColor(
            plugin.getConfig().getString("get-category-message", "<green>You received <yellow>%count%</yellow> items from category <aqua>%category%</aqua>")
                .replace("%count%", String.valueOf(itemsGiven))
                .replace("%category%", categoryName)
        );
        
        player.sendMessage(plugin.getPrefix().append(message));
        return true;
    }
    

    

    
    private boolean handleTradeCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("itemx.trade.admin")) {
            sender.sendMessage(plugin.getPrefix().append(plugin.getMessage("no-permission")));
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage(plugin.getPrefix().append(
                plugin.getColorUtil().parseColor("<red>Usage: /itemx trade create <gui-name> [title]")
            ));
            return true;
        }
        
        String guiName = args[2];
        String title = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : guiName;
        
        if (plugin.getTradeManager().hasTradeGUI(guiName)) {
            sender.sendMessage(plugin.getPrefix().append(
                plugin.getColorUtil().parseColor("<red>Trade GUI '" + guiName + "' already exists.")
            ));
            return true;
        }
        
        plugin.getTradeManager().createTradeGUI(guiName, title);
        sender.sendMessage(plugin.getPrefix().append(
            plugin.getColorUtil().parseColor("<green>Created trade GUI '" + guiName + "'")
        ));
        
        return true;
    }
    
    private boolean handleTradeDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("itemx.trade.admin")) {
            sender.sendMessage(plugin.getPrefix().append(plugin.getMessage("no-permission")));
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage(plugin.getPrefix().append(
                plugin.getColorUtil().parseColor("<red>Usage: /itemx trade delete <gui-name>")
            ));
            return true;
        }
        
        String guiName = args[2];
        
        if (!plugin.getTradeManager().hasTradeGUI(guiName)) {
            sender.sendMessage(plugin.getPrefix().append(
                plugin.getColorUtil().parseColor("<red>Trade GUI '" + guiName + "' not found.")
            ));
            return true;
        }
        
        plugin.getTradeManager().deleteTradeGUI(guiName);
        sender.sendMessage(plugin.getPrefix().append(
            plugin.getColorUtil().parseColor("<green>Deleted trade GUI '" + guiName + "'")
        ));
        
        return true;
    }
    
    private boolean handleTradeRename(CommandSender sender, String[] args) {
        if (!sender.hasPermission("itemx.trade.admin")) {
            sender.sendMessage(plugin.getPrefix().append(plugin.getMessage("no-permission")));
            return true;
        }
        
        if (args.length < 4) {
            sender.sendMessage(plugin.getPrefix().append(
                plugin.getColorUtil().parseColor("<red>Usage: /itemx trade rename <gui-name> <new-name>")
            ));
            return true;
        }
        
        String oldName = args[2];
        String newName = args[3];
        
        if (!plugin.getTradeManager().hasTradeGUI(oldName)) {
            sender.sendMessage(plugin.getPrefix().append(
                plugin.getColorUtil().parseColor("<red>Trade GUI '" + oldName + "' not found.")
            ));
            return true;
        }
        
        if (plugin.getTradeManager().hasTradeGUI(newName)) {
            sender.sendMessage(plugin.getPrefix().append(
                plugin.getColorUtil().parseColor("<red>Trade GUI '" + newName + "' already exists.")
            ));
            return true;
        }
        
        plugin.getTradeManager().renameTradeGUI(oldName, newName);
        sender.sendMessage(plugin.getPrefix().append(
            plugin.getColorUtil().parseColor("<green>Renamed trade GUI from '" + oldName + "' to '" + newName + "'")
        ));
        
        return true;
    }
    
    private boolean handleTradeTitle(CommandSender sender, String[] args) {
        if (!sender.hasPermission("itemx.trade.admin")) {
            sender.sendMessage(plugin.getPrefix().append(plugin.getMessage("no-permission")));
            return true;
        }
        
        if (args.length < 4) {
            sender.sendMessage(plugin.getPrefix().append(
                plugin.getColorUtil().parseColor("<red>Usage: /itemx trade title <gui-name> <title>")
            ));
            return true;
        }
        
        String guiName = args[2];
        String title = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        
        if (!plugin.getTradeManager().hasTradeGUI(guiName)) {
            sender.sendMessage(plugin.getPrefix().append(
                plugin.getColorUtil().parseColor("<red>Trade GUI '" + guiName + "' not found.")
            ));
            return true;
        }
        
        plugin.getTradeManager().setTradeGUITitle(guiName, title);
        sender.sendMessage(plugin.getPrefix().append(
            plugin.getColorUtil().parseColor("<green>Set title of trade GUI '" + guiName + "' to '" + title + "'")
        ));
        
        return true;
    }
    
    private boolean handleTradeEdit(CommandSender sender, String[] args) {
        if (!sender.hasPermission("itemx.trade.admin")) {
            sender.sendMessage(plugin.getPrefix().append(plugin.getMessage("no-permission")));
            return true;
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getPrefix().append(
                plugin.getColorUtil().parseColor("<red>Only players can use the trade editor.")
            ));
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage(plugin.getPrefix().append(
                plugin.getColorUtil().parseColor("<red>Usage: /itemx trade edit <gui-name>")
            ));
            return true;
        }
        
        String guiName = args[2];
        
        if (!plugin.getTradeManager().hasTradeGUI(guiName)) {
            sender.sendMessage(plugin.getPrefix().append(
                plugin.getColorUtil().parseColor("<red>Trade GUI '" + guiName + "' not found.")
            ));
            return true;
        }
        
        plugin.getTradeManager().openTradeManager((Player) sender, guiName);
        return true;
    }
    
    private boolean handleTradeReload(CommandSender sender) {
        if (!sender.hasPermission("itemx.trade.admin")) {
            sender.sendMessage(plugin.getPrefix().append(plugin.getMessage("no-permission")));
            return true;
        }
        
        plugin.getTradeManager().reload();
        sender.sendMessage(plugin.getPrefix().append(
            plugin.getColorUtil().parseColor("<green>Reloaded " + plugin.getTradeManager().getTradeGUICount() + " trade GUIs")
        ));
        
        return true;
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("give", "get", "trade", "reload"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give")) {
                completions.addAll(plugin.getItemManager().getItemIds());
            } else if (args[0].equalsIgnoreCase("get")) {
                // Add individual item IDs
                completions.addAll(plugin.getItemManager().getItemIds());
                // Add category options
                for (String category : plugin.getItemManager().getCategories()) {
                    completions.add("category:" + category);
                }
            } else if (args[0].equalsIgnoreCase("trade")) {
                completions.addAll(Arrays.asList("list", "create", "delete", "rename", "title", "edit", "reload"));
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("give")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            } else if (args[0].equalsIgnoreCase("trade")) {
                String subcommand = args[1].toLowerCase();
                if (Arrays.asList("delete", "rename", "title", "edit").contains(subcommand)) {
                    completions.addAll(plugin.getTradeManager().getTradeGUINames());
                }
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("trade") && args[1].equalsIgnoreCase("rename")) {
                // For rename, don't suggest existing names for the new name
                completions.add("<new-name>");
            }
        }
        
        return completions;
    }
    
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("itemx.reload")) {
            sender.sendMessage(plugin.getPrefix().append(plugin.getMessage("no-permission")));
            return true;
        }
        
        plugin.reload();
        sender.sendMessage(plugin.getPrefix().append(plugin.getMessage("reload-success")));
        return true;
    }
}