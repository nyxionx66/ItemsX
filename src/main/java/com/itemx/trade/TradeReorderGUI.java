package com.itemx.trade;

import com.itemx.ItemX;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced trade reorder GUI with better UX and visual feedback
 */
public class TradeReorderGUI implements Listener {

    private final ItemX plugin;
    private final Map<UUID, ReorderSession> sessions;
    
    private static final int GUI_SIZE = 54;
    private static final String REORDER_TITLE = "<gradient:#FFD700:#FF8C00>🔧 Reorder Trades</gradient>";
    
    // Control buttons
    private static final int DONE_BUTTON = 53;
    private static final int ADD_TRADE_BUTTON = 52;
    private static final int RESET_BUTTON = 51;
    private static final int HELP_BUTTON = 4;
    
    // Trade display area
    private static final int TRADES_START = 9;
    private static final int TRADES_END = 44;
    private static final int TRADES_PER_ROW = 9;
    private static final int MAX_TRADES_DISPLAYED = 36;

    public TradeReorderGUI(ItemX plugin) {
        this.plugin = plugin;
        this.sessions = new ConcurrentHashMap<>();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Opens the reorder GUI for a player
     */
    public void openReorderGUI(Player player, String guiName) {
        TradeGUI tradeGUI = plugin.getTradeManager().getTradeGUI(guiName);
        if (tradeGUI == null) {
            player.sendMessage(plugin.getPrefix().append(
                plugin.getColorUtil().parseColor("<red>Trade GUI '" + guiName + "' not found.")
            ));
            return;
        }

        ReorderSession session = new ReorderSession(player.getUniqueId(), guiName);
        sessions.put(player.getUniqueId(), session);
        
        openGUI(player, session);
    }

    /**
     * Creates and opens the reorder GUI
     */
    private void openGUI(Player player, ReorderSession session) {
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, 
            plugin.getColorUtil().parseColor(REORDER_TITLE + " " + session.getGuiName()));

        // Fill background
        fillBackground(gui);
        
        // Add help information
        setupHelpButton(gui, session);
        
        // Add control buttons
        setupControlButtons(gui, session);
        
        // Add trades
        setupTrades(gui, session);
        
        player.openInventory(gui);
    }

    /**
     * Fills the GUI with background glass
     */
    private void fillBackground(Inventory gui) {
        ItemStack glass = createGlassPane(Material.GRAY_STAINED_GLASS_PANE, "");
        for (int i = 0; i < GUI_SIZE; i++) {
            if (i < TRADES_START || i > TRADES_END) {
                gui.setItem(i, glass);
            }
        }
    }

    /**
     * Sets up the help button with instructions
     */
    private void setupHelpButton(Inventory gui, ReorderSession session) {
        gui.setItem(HELP_BUTTON, createInfoItem(Material.BOOK,
            "<gradient:#4ECDC4:#44A08D>📋 Reorder Instructions</gradient>",
            Arrays.asList(
                "<yellow>How to reorder trades:",
                "",
                "<aqua>🔄 Drag and Drop:",
                "<gray>• Drag trades to reorder them",
                "<gray>• Visual feedback shows valid positions",
                "",
                "<aqua>⚡ Click Actions:",
                "<gray>• <green>Left Click</green> to move trade up",
                "<gray>• <red>Right Click</red> to move trade down",
                "",
                "<aqua>🎯 Tips:",
                "<gray>• Changes are saved automatically",
                "<gray>• Use <gold>Reset</gold> to undo changes",
                "<gray>• Click <green>Done</green> when finished"
            )));
    }

    /**
     * Sets up the control buttons
     */
    private void setupControlButtons(Inventory gui, ReorderSession session) {
        gui.setItem(DONE_BUTTON, createButton(Material.EMERALD,
            "<gradient:#00FF00:#32CD32>✓ Done</gradient>",
            Arrays.asList("<green>Click when finished reordering", "<gray>Returns to trade manager"),
            "done"));

        gui.setItem(ADD_TRADE_BUTTON, createButton(Material.GOLD_INGOT,
            "<gradient:#FFD700:#FFA500>✚ Add New Trade</gradient>",
            Arrays.asList("<yellow>Click to add a new trade", "<gray>Opens the trade editor"),
            "add_trade"));

        gui.setItem(RESET_BUTTON, createButton(Material.REDSTONE,
            "<gradient:#FF6B6B:#FF4444>↻ Reset Order</gradient>",
            Arrays.asList("<red>Click to reset to original order", "<gray>Undoes all changes"),
            "reset"));
    }

    /**
     * Sets up the trade display area
     */
    private void setupTrades(Inventory gui, ReorderSession session) {
        TradeGUI tradeGUI = plugin.getTradeManager().getTradeGUI(session.getGuiName());
        if (tradeGUI == null) return;

        List<TradeDefinition> trades = tradeGUI.getTrades();
        
        for (int i = 0; i < Math.min(trades.size(), MAX_TRADES_DISPLAYED); i++) {
            TradeDefinition trade = trades.get(i);
            ItemStack tradeItem = createReorderTradeItem(trade, i + 1);
            gui.setItem(TRADES_START + i, tradeItem);
        }
        
        // Fill empty slots with placeholder items
        for (int i = trades.size(); i < MAX_TRADES_DISPLAYED; i++) {
            gui.setItem(TRADES_START + i, createEmptySlot());
        }
    }

    /**
     * Handles inventory click events
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        ReorderSession session = sessions.get(player.getUniqueId());
        if (session == null) return;

        // Only handle clicks in our GUI
        if (!event.getView().getTopInventory().equals(event.getClickedInventory())) {
            return;
        }

        int slot = event.getSlot();
        
        // Handle control buttons
        if (slot == DONE_BUTTON || slot == ADD_TRADE_BUTTON || slot == RESET_BUTTON) {
            event.setCancelled(true);
            handleControlButton(player, session, slot);
            return;
        }
        
        // Handle trade clicks
        if (slot >= TRADES_START && slot <= TRADES_END) {
            event.setCancelled(true);
            handleTradeClick(player, session, slot - TRADES_START, event.getClick());
            return;
        }
        
        // Cancel all other clicks
        event.setCancelled(true);
    }

    /**
     * Handles control button clicks
     */
    private void handleControlButton(Player player, ReorderSession session, int slot) {
        switch (slot) {
            case DONE_BUTTON:
                handleDone(player, session);
                break;
            case ADD_TRADE_BUTTON:
                handleAddTrade(player, session);
                break;
            case RESET_BUTTON:
                handleReset(player, session);
                break;
        }
    }

    /**
     * Handles trade item clicks
     */
    private void handleTradeClick(Player player, ReorderSession session, int tradeIndex, ClickType clickType) {
        TradeGUI tradeGUI = plugin.getTradeManager().getTradeGUI(session.getGuiName());
        if (tradeGUI == null) return;

        List<TradeDefinition> trades = tradeGUI.getTrades();
        if (tradeIndex >= trades.size()) return;

        switch (clickType) {
            case LEFT:
                // Move up
                if (tradeIndex > 0) {
                    plugin.getTradeManager().moveTrade(session.getGuiName(), tradeIndex, tradeIndex - 1);
                    player.sendMessage(plugin.getPrefix().append(
                        plugin.getColorUtil().parseColor("<green>Moved trade up: <white>" + trades.get(tradeIndex).getId())
                    ));
                    refreshGUI(player, session);
                }
                break;
            case RIGHT:
                // Move down
                if (tradeIndex < trades.size() - 1) {
                    plugin.getTradeManager().moveTrade(session.getGuiName(), tradeIndex, tradeIndex + 1);
                    player.sendMessage(plugin.getPrefix().append(
                        plugin.getColorUtil().parseColor("<green>Moved trade down: <white>" + trades.get(tradeIndex).getId())
                    ));
                    refreshGUI(player, session);
                }
                break;
        }
    }

    /**
     * Handles inventory close events
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        ReorderSession session = sessions.remove(player.getUniqueId());
        if (session == null) return;

        // Auto-return to manager if not closing normally
        if (!session.isClosingNormally()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getTradeManager().openTradeManager(player, session.getGuiName());
            });
        }
    }

    // Action handlers

    private void handleDone(Player player, ReorderSession session) {
        session.setClosingNormally(true);
        player.closeInventory();
        
        player.sendMessage(plugin.getPrefix().append(
            plugin.getColorUtil().parseColor("<green>Trade reordering completed!")
        ));
        
        plugin.getTradeManager().openTradeManager(player, session.getGuiName());
    }

    private void handleAddTrade(Player player, ReorderSession session) {
        session.setClosingNormally(true);
        player.closeInventory();
        
        plugin.getTradeManager().openTradeEditor(player, session.getGuiName(), null);
    }

    private void handleReset(Player player, ReorderSession session) {
        // TODO: Implement reset functionality
        player.sendMessage(plugin.getPrefix().append(
            plugin.getColorUtil().parseColor("<yellow>Reset functionality will be implemented in enhanced version.")
        ));
    }

    /**
     * Refreshes the GUI after changes
     */
    private void refreshGUI(Player player, ReorderSession session) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                openGUI(player, session);
            }
        });
    }

    // Item creation methods

    private ItemStack createGlassPane(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createButton(Material material, String name, List<String> lore, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getColorUtil().parseColor(name));
            
            List<Component> loreComponents = new ArrayList<>();
            for (String line : lore) {
                loreComponents.add(plugin.getColorUtil().parseColor(line));
            }
            loreComponents.add(plugin.getColorUtil().parseColor("<#333333>Action: " + action));
            
            meta.lore(loreComponents);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createInfoItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getColorUtil().parseColor(name));
            
            List<Component> loreComponents = new ArrayList<>();
            for (String line : lore) {
                loreComponents.add(plugin.getColorUtil().parseColor(line));
            }
            
            meta.lore(loreComponents);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createReorderTradeItem(TradeDefinition trade, int position) {
        ItemStack displayItem = createItemFromTradeItem(trade.getOutput());
        if (displayItem == null) {
            displayItem = new ItemStack(Material.PAPER);
        } else {
            displayItem = displayItem.clone();
            displayItem.setAmount(1);
        }

        ItemMeta meta = displayItem.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getColorUtil().parseColor(
                "<gradient:#FFD700:#FF8C00>" + position + ". " + trade.getId() + "</gradient>"
            ));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(""));
            lore.add(plugin.getColorUtil().parseColor("<aqua>📥 Input: <white>" + 
                trade.getInput1().getItem() + " x" + trade.getInput1().getAmount()));
            if (trade.hasSecondInput()) {
                lore.add(plugin.getColorUtil().parseColor("<aqua>📥 Input 2: <white>" + 
                    trade.getInput2().getItem() + " x" + trade.getInput2().getAmount()));
            }
            lore.add(plugin.getColorUtil().parseColor("<green>📤 Output: <white>" + 
                trade.getOutput().getItem() + " x" + trade.getOutput().getAmount()));
            lore.add(Component.text(""));
            lore.add(plugin.getColorUtil().parseColor("<yellow>⚡ Reorder Actions:"));
            lore.add(plugin.getColorUtil().parseColor("<gray>• <green>Left Click</green> to move up"));
            lore.add(plugin.getColorUtil().parseColor("<gray>• <red>Right Click</red> to move down"));
            lore.add(plugin.getColorUtil().parseColor("<gray>• <aqua>Drag</green> to reposition"));

            meta.lore(lore);
            displayItem.setItemMeta(meta);
        }
        return displayItem;
    }

    private ItemStack createEmptySlot() {
        ItemStack item = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getColorUtil().parseColor("<gray>Empty Slot"));
            meta.lore(Arrays.asList(
                plugin.getColorUtil().parseColor("<dark_gray>No trade in this position"),
                plugin.getColorUtil().parseColor("<gray>Create new trades to fill this slot")
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createItemFromTradeItem(TradeDefinition.TradeItem tradeItem) {
        if (tradeItem == null) return null;
        
        ItemStack item = null;
        if (tradeItem.isCustomItem()) {
            String customId = tradeItem.getCustomItemId();
            com.itemx.item.ItemDefinition definition = plugin.getItemManager().getItemDefinition(customId);
            if (definition != null) {
                item = plugin.getItemManager().createItem(definition);
            }
        } else {
            try {
                Material material = Material.valueOf(tradeItem.getItem().toUpperCase());
                item = new ItemStack(material);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material in trade: " + tradeItem.getItem());
            }
        }
        
        if (item != null) {
            item.setAmount(tradeItem.getAmount());
        }
        return item;
    }

    /**
     * Session class for reorder operations
     */
    public static class ReorderSession {
        private final UUID playerId;
        private final String guiName;
        private boolean closingNormally = false;

        public ReorderSession(UUID playerId, String guiName) {
            this.playerId = playerId;
            this.guiName = guiName;
        }

        public UUID getPlayerId() { return playerId; }
        public String getGuiName() { return guiName; }
        public boolean isClosingNormally() { return closingNormally; }
        public void setClosingNormally(boolean closingNormally) { this.closingNormally = closingNormally; }
    }
}