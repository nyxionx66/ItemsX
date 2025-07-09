package com.itemx.trade;

import com.itemx.ItemX;
import com.itemx.item.ItemDefinition;
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
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Completely redesigned Trade Editor system with clean architecture
 * and proper slot protection to prevent GUI item theft.
 */
public class TradeEditor implements Listener {

    private final ItemX plugin;
    private final Map<UUID, EditorSession> sessions;
    
    // GUI Layout Constants
    private static final int GUI_SIZE = 54;
    private static final String EDITOR_TITLE = "<gradient:#9146FF:#00D4FF>⚡ Trade Editor ⚡</gradient>";
    private static final String MANAGER_TITLE = "<gradient:#FF6B6B:#4ECDC4>✦ Trade Manager ✦</gradient>";
    
    // Slot Layout for Editor GUI
    private static final int INPUT1_SLOT = 19;
    private static final int INPUT2_SLOT = 21;
    private static final int OUTPUT_SLOT = 25;
    private static final int SAVE_BUTTON = 45;
    private static final int CANCEL_BUTTON = 46;
    private static final int CLEAR_BUTTON = 47;
    private static final int TRADE_ID_BUTTON = 4;
    
    // Manager GUI Layout
    private static final int ADD_TRADE_BUTTON = 7;
    private static final int CLOSE_MANAGER_BUTTON = 8;
    private static final int REORDER_BUTTON = 9;
    private static final int TRADES_START_SLOT = 18;
    private static final int TRADES_END_SLOT = 53;
    
    // Protected slots that cannot be interacted with
    private static final Set<Integer> PROTECTED_SLOTS = Set.of(
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
        20, 22, 23, 24, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38,
        39, 40, 41, 42, 43, 44, 48, 49, 50, 51, 52, 53
    );
    
    // Interactive slots where players can place items
    private static final Set<Integer> INTERACTIVE_SLOTS = Set.of(
        INPUT1_SLOT, INPUT2_SLOT, OUTPUT_SLOT
    );
    
    // Button slots for actions
    private static final Set<Integer> BUTTON_SLOTS = Set.of(
        SAVE_BUTTON, CANCEL_BUTTON, CLEAR_BUTTON, TRADE_ID_BUTTON
    );

    public TradeEditor(ItemX plugin) {
        this.plugin = plugin;
        this.sessions = new ConcurrentHashMap<>();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Opens the trade editor for a player
     */
    public void openTradeEditor(Player player, String guiName, String tradeId) {
        TradeGUI tradeGUI = plugin.getTradeManager().getTradeGUI(guiName);
        if (tradeGUI == null) {
            player.sendMessage(plugin.getPrefix().append(
                plugin.getColorUtil().parseColor("<red>Trade GUI '" + guiName + "' not found.")
            ));
            return;
        }

        // Create or get session
        EditorSession session = new EditorSession(player.getUniqueId(), guiName, tradeId);
        
        // If editing existing trade, load its data
        if (tradeId != null) {
            TradeDefinition existingTrade = tradeGUI.getTrade(tradeId);
            if (existingTrade != null) {
                session.setInput1(existingTrade.getInput1());
                session.setInput2(existingTrade.getInput2());
                session.setOutput(existingTrade.getOutput());
                session.setExistingTrade(existingTrade);
            }
        }
        
        sessions.put(player.getUniqueId(), session);
        openEditorGUI(player, session);
    }

    /**
     * Opens the enhanced trade manager for a player
     */
    public void openTradeManager(Player player, String guiName) {
        TradeGUI tradeGUI = plugin.getTradeManager().getTradeGUI(guiName);
        if (tradeGUI == null) {
            player.sendMessage(plugin.getPrefix().append(
                plugin.getColorUtil().parseColor("<red>Trade GUI '" + guiName + "' not found.")
            ));
            return;
        }

        EditorSession session = new EditorSession(player.getUniqueId(), guiName, null);
        session.setManagerMode(true);
        sessions.put(player.getUniqueId(), session);
        openManagerGUI(player, session);
    }

    /**
     * Creates and opens the trade editor GUI
     */
    private void openEditorGUI(Player player, EditorSession session) {
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, 
            plugin.getColorUtil().parseColor(EDITOR_TITLE));

        // Fill with background glass
        fillBackground(gui);
        
        // Add interactive slots
        setupInteractiveSlots(gui, session);
        
        // Add control buttons
        setupControlButtons(gui, session);
        
        // Add decorative elements
        setupDecorations(gui);
        
        player.openInventory(gui);
    }

    /**
     * Creates and opens the trade manager GUI
     */
    private void openManagerGUI(Player player, EditorSession session) {
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE,
            plugin.getColorUtil().parseColor(MANAGER_TITLE + " " + session.getGuiName()));

        // Fill with background glass
        fillBackground(gui);
        
        // Add header info
        setupManagerHeader(gui, session);
        
        // Add control buttons
        setupManagerButtons(gui);
        
        // Add trades
        setupTradeList(gui, session);
        
        player.openInventory(gui);
    }

    /**
     * Fills the GUI with background glass panes
     */
    private void fillBackground(Inventory gui) {
        ItemStack glass = createGlassPane(Material.BLACK_STAINED_GLASS_PANE, "");
        for (int i = 0; i < GUI_SIZE; i++) {
            if (PROTECTED_SLOTS.contains(i)) {
                gui.setItem(i, glass);
            }
        }
    }

    /**
     * Sets up the interactive slots for placing items
     */
    private void setupInteractiveSlots(Inventory gui, EditorSession session) {
        // Input 1 slot
        if (session.getInput1() != null) {
            gui.setItem(INPUT1_SLOT, createItemFromTradeItem(session.getInput1()));
        } else {
            gui.setItem(INPUT1_SLOT, createPlaceholderItem(Material.CHEST, 
                "<gradient:#FFD700:#FFA500>📥 Input Slot 1</gradient>", 
                "Place the required item here"));
        }

        // Input 2 slot
        if (session.getInput2() != null) {
            gui.setItem(INPUT2_SLOT, createItemFromTradeItem(session.getInput2()));
        } else {
            gui.setItem(INPUT2_SLOT, createPlaceholderItem(Material.ENDER_CHEST,
                "<gradient:#9370DB:#8A2BE2>📥 Input Slot 2</gradient>", 
                "Place the optional second item here"));
        }

        // Output slot
        if (session.getOutput() != null) {
            gui.setItem(OUTPUT_SLOT, createItemFromTradeItem(session.getOutput()));
        } else {
            gui.setItem(OUTPUT_SLOT, createPlaceholderItem(Material.SHULKER_BOX,
                "<gradient:#32CD32:#228B22>📤 Output Slot</gradient>", 
                "Place the resulting item here"));
        }
    }

    /**
     * Sets up the control buttons for the editor
     */
    private void setupControlButtons(Inventory gui, EditorSession session) {
        // Save button
        gui.setItem(SAVE_BUTTON, createButton(Material.EMERALD, 
            "<gradient:#00FF00:#32CD32>💾 Save Trade</gradient>", 
            Arrays.asList("<green>Click to save this trade", "<gray>Requires Input 1 and Output"), 
            "save_trade"));

        // Cancel button
        gui.setItem(CANCEL_BUTTON, createButton(Material.BARRIER, 
            "<gradient:#FF4444:#CC0000>❌ Cancel</gradient>", 
            Arrays.asList("<red>Click to cancel editing", "<gray>Returns to trade manager"), 
            "cancel_trade"));

        // Clear button
        gui.setItem(CLEAR_BUTTON, createButton(Material.BUCKET, 
            "<gradient:#FFA500:#FF8C00>🗑️ Clear All</gradient>", 
            Arrays.asList("<yellow>Click to clear all slots", "<gray>Removes all items from editor"), 
            "clear_all"));

        // Trade ID button
        String tradeIdText = session.getTradeId() != null ? session.getTradeId() : "auto_generate";
        gui.setItem(TRADE_ID_BUTTON, createButton(Material.NAME_TAG, 
            "<gradient:#4ECDC4:#44A08D>🏷️ Trade ID: " + tradeIdText + "</gradient>", 
            Arrays.asList("<aqua>Click to set custom trade ID", "<gray>Leave empty for auto-generation"), 
            "set_trade_id"));
    }

    /**
     * Sets up decorative elements for the editor
     */
    private void setupDecorations(Inventory gui) {
        // Plus symbol
        gui.setItem(20, createDecorationItem(Material.LIME_CONCRETE, 
            "<gradient:#00FF00:#32CD32>+</gradient>"));

        // Arrow symbol
        gui.setItem(23, createDecorationItem(Material.YELLOW_CONCRETE, 
            "<gradient:#FFD700:#FFA500>→</gradient>"));
        
        // Equals symbol
        gui.setItem(24, createDecorationItem(Material.ORANGE_CONCRETE, 
            "<gradient:#FF8C00:#FF4500>=</gradient>"));
    }

    /**
     * Sets up the header information for the manager GUI
     */
    private void setupManagerHeader(Inventory gui, EditorSession session) {
        TradeGUI tradeGUI = plugin.getTradeManager().getTradeGUI(session.getGuiName());
        if (tradeGUI == null) return;

        gui.setItem(0, createInfoItem(Material.DIAMOND_SWORD,
            "<gradient:#FFD700:#FF8C00>✦ Trade Manager</gradient>",
            Arrays.asList(
                "<gray>Managing trades for: <aqua>" + session.getGuiName(),
                "<gray>Total trades: <yellow>" + tradeGUI.getTradeCount(),
                "",
                "<green>✓ Left click to edit trade",
                "<green>✓ Right click to delete trade",
                "<green>✓ Shift+Right click to get output item"
            )));
    }

    /**
     * Sets up the control buttons for the manager GUI
     */
    private void setupManagerButtons(Inventory gui) {
        gui.setItem(ADD_TRADE_BUTTON, createButton(Material.EMERALD_BLOCK,
            "<gradient:#00FF00:#32CD32>✚ Add New Trade</gradient>",
            Arrays.asList("<green>Click to create a new trade", "<gray>Opens the trade editor"),
            "add_trade"));

        gui.setItem(CLOSE_MANAGER_BUTTON, createButton(Material.BARRIER,
            "<gradient:#FF4444:#CC0000>✖ Close Manager</gradient>",
            Arrays.asList("<red>Click to close the manager", "<gray>Returns to game"),
            "close_manager"));

        gui.setItem(REORDER_BUTTON, createButton(Material.CRAFTING_TABLE,
            "<gradient:#FFA500:#FF8C00>🔧 Reorder Trades</gradient>",
            Arrays.asList("<yellow>Click to reorder trades", "<gray>Change the order of trades"),
            "reorder_trades"));
    }

    /**
     * Sets up the trade list in the manager GUI
     */
    private void setupTradeList(Inventory gui, EditorSession session) {
        TradeGUI tradeGUI = plugin.getTradeManager().getTradeGUI(session.getGuiName());
        if (tradeGUI == null) return;

        List<TradeDefinition> trades = tradeGUI.getTrades();
        for (int i = 0; i < trades.size() && i < (TRADES_END_SLOT - TRADES_START_SLOT + 1); i++) {
            TradeDefinition trade = trades.get(i);
            ItemStack tradeDisplay = createTradeDisplay(trade, i + 1);
            gui.setItem(TRADES_START_SLOT + i, tradeDisplay);
        }
    }

    /**
     * Handles inventory click events
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();
        
        EditorSession session = sessions.get(playerId);
        if (session == null) return;

        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) return;

        int slot = event.getRawSlot();
        Inventory topInventory = event.getView().getTopInventory();

        // Check if clicking in our GUI
        if (clickedInventory.equals(topInventory)) {
            handleTopInventoryClick(event, player, session, slot);
        } else {
            // Clicking in player inventory
            handlePlayerInventoryClick(event, player, session);
        }
    }

    /**
     * Handles clicks in the top inventory (our GUI)
     */
    private void handleTopInventoryClick(InventoryClickEvent event, Player player, 
                                        EditorSession session, int slot) {
        if (session.isManagerMode()) {
            handleManagerClick(event, player, session, slot);
        } else {
            handleEditorClick(event, player, session, slot);
        }
    }

    /**
     * Handles clicks in the editor GUI
     */
    private void handleEditorClick(InventoryClickEvent event, Player player, 
                                  EditorSession session, int slot) {
        // Allow interaction with interactive slots
        if (INTERACTIVE_SLOTS.contains(slot)) {
            // Allow normal item placement/removal
            Bukkit.getScheduler().runTask(plugin, () -> updateSessionFromGUI(player, session));
            return;
        }

        // Cancel all other clicks
        event.setCancelled(true);

        // Handle button clicks
        if (BUTTON_SLOTS.contains(slot)) {
            handleButtonClick(event, player, session, slot);
        }
    }

    /**
     * Handles clicks in the manager GUI
     */
    private void handleManagerClick(InventoryClickEvent event, Player player, 
                                   EditorSession session, int slot) {
        event.setCancelled(true);

        // Handle control buttons
        if (slot == ADD_TRADE_BUTTON) {
            handleAddTrade(player, session);
        } else if (slot == CLOSE_MANAGER_BUTTON) {
            handleCloseManager(player);
        } else if (slot == REORDER_BUTTON) {
            handleReorderTrades(player, session);
        } else if (slot >= TRADES_START_SLOT && slot <= TRADES_END_SLOT) {
            handleTradeClick(event, player, session, slot - TRADES_START_SLOT);
        }
    }

    /**
     * Handles clicks in the player inventory
     */
    private void handlePlayerInventoryClick(InventoryClickEvent event, Player player, 
                                           EditorSession session) {
        if (session.isManagerMode()) {
            // No special handling for manager mode
            return;
        }

        // Handle shift-click to move items to editor
        if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
            event.setCancelled(true);
            handleShiftClick(player, session, event.getCurrentItem());
        }
    }

    /**
     * Handles button clicks in the editor
     */
    private void handleButtonClick(InventoryClickEvent event, Player player, 
                                  EditorSession session, int slot) {
        switch (slot) {
            case SAVE_BUTTON:
                handleSaveTrade(player, session);
                break;
            case CANCEL_BUTTON:
                handleCancelTrade(player, session);
                break;
            case CLEAR_BUTTON:
                handleClearAll(player, session);
                break;
            case TRADE_ID_BUTTON:
                handleSetTradeId(player, session);
                break;
        }
    }

    /**
     * Handles inventory drag events
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        EditorSession session = sessions.get(player.getUniqueId());
        if (session == null) return;

        // Check if any dragged slots are in our GUI
        Inventory topInventory = event.getView().getTopInventory();
        boolean draggedInGUI = false;
        
        for (int slot : event.getRawSlots()) {
            if (slot < topInventory.getSize()) {
                draggedInGUI = true;
                break;
            }
        }
        
        if (!draggedInGUI) return;

        // Only allow dragging to interactive slots
        for (int slot : event.getRawSlots()) {
            if (slot < topInventory.getSize() && !INTERACTIVE_SLOTS.contains(slot)) {
                event.setCancelled(true);
                return;
            }
        }

        // Update session after drag
        if (!session.isManagerMode()) {
            Bukkit.getScheduler().runTask(plugin, () -> updateSessionFromGUI(player, session));
        }
    }

    /**
     * Handles inventory close events
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        EditorSession session = sessions.get(playerId);
        if (session == null) return;

        // Return items to player if closing unexpectedly
        if (!session.isClosingNormally() && !session.isManagerMode()) {
            returnItemsToPlayer(player, event.getInventory());
        }
        
        // Clean up session
        sessions.remove(playerId);
    }

    /**
     * Handles chat events for trade ID setting
     */
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        EditorSession session = sessions.get(playerId);
        if (session == null || !session.isWaitingForInput()) return;

        event.setCancelled(true);
        
        String input = event.getMessage().trim();
        if (isValidTradeId(input)) {
            session.setTradeId(input);
            session.setWaitingForInput(false);
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage(plugin.getPrefix().append(
                    plugin.getColorUtil().parseColor("<green>Trade ID set to: <yellow>" + input)
                ));
                openEditorGUI(player, session);
            });
        } else {
            session.setWaitingForInput(false);
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage(plugin.getPrefix().append(
                    plugin.getColorUtil().parseColor("<red>Invalid trade ID! Use only letters, numbers, and underscores (1-32 chars).")
                ));
                openEditorGUI(player, session);
            });
        }
    }

    // Action Handlers

    private void handleSaveTrade(Player player, EditorSession session) {
        if (session.getInput1() == null || session.getOutput() == null) {
            player.sendMessage(plugin.getPrefix().append(
                plugin.getColorUtil().parseColor("<red>Input 1 and Output are required!")
            ));
            return;
        }

        String tradeId = session.getTradeId();
        if (tradeId == null || tradeId.isEmpty()) {
            tradeId = generateTradeId();
        }

        TradeDefinition trade = new TradeDefinition(tradeId, session.getInput1(), 
            session.getInput2(), session.getOutput());

        // Remove existing trade if editing
        if (session.getExistingTrade() != null) {
            plugin.getTradeManager().removeTrade(session.getGuiName(), 
                session.getExistingTrade().getId());
        }

        plugin.getTradeManager().addTrade(session.getGuiName(), trade);
        
        player.sendMessage(plugin.getPrefix().append(
            plugin.getColorUtil().parseColor("<green>Trade saved successfully with ID: <white>" + tradeId)
        ));

        session.setClosingNormally(true);
        player.closeInventory();
        openTradeManager(player, session.getGuiName());
    }

    private void handleCancelTrade(Player player, EditorSession session) {
        player.sendMessage(plugin.getPrefix().append(
            plugin.getColorUtil().parseColor("<yellow>Trade editing cancelled.")
        ));
        
        session.setClosingNormally(true);
        player.closeInventory();
        openTradeManager(player, session.getGuiName());
    }

    private void handleClearAll(Player player, EditorSession session) {
        session.setInput1(null);
        session.setInput2(null);
        session.setOutput(null);
        
        player.sendMessage(plugin.getPrefix().append(
            plugin.getColorUtil().parseColor("<yellow>All slots cleared.")
        ));
        
        openEditorGUI(player, session);
    }

    private void handleSetTradeId(Player player, EditorSession session) {
        session.setWaitingForInput(true);
        player.closeInventory();
        
        player.sendMessage(plugin.getPrefix().append(
            plugin.getColorUtil().parseColor("<yellow>Type the new trade ID in chat and press Enter.")
        ));
        player.sendMessage(plugin.getPrefix().append(
            plugin.getColorUtil().parseColor("<gray>(Letters, numbers, and underscores only. Max 32 characters)")
        ));
    }

    private void handleAddTrade(Player player, EditorSession session) {
        session.setClosingNormally(true);
        player.closeInventory();
        openTradeEditor(player, session.getGuiName(), null);
    }

    private void handleCloseManager(Player player) {
        EditorSession session = sessions.get(player.getUniqueId());
        if (session != null) {
            session.setClosingNormally(true);
        }
        player.closeInventory();
    }

    private void handleReorderTrades(Player player, EditorSession session) {
        // TODO: Implement reorder functionality
        player.sendMessage(plugin.getPrefix().append(
            plugin.getColorUtil().parseColor("<yellow>Reorder functionality will be implemented in Phase 3.")
        ));
    }

    private void handleTradeClick(InventoryClickEvent event, Player player, 
                                 EditorSession session, int tradeIndex) {
        TradeGUI tradeGUI = plugin.getTradeManager().getTradeGUI(session.getGuiName());
        if (tradeGUI == null) return;

        List<TradeDefinition> trades = tradeGUI.getTrades();
        if (tradeIndex >= trades.size()) return;

        TradeDefinition trade = trades.get(tradeIndex);
        
        switch (event.getClick()) {
            case LEFT:
                // Edit trade
                session.setClosingNormally(true);
                player.closeInventory();
                openTradeEditor(player, session.getGuiName(), trade.getId());
                break;
            case RIGHT:
                // Delete trade
                plugin.getTradeManager().removeTrade(session.getGuiName(), trade.getId());
                player.sendMessage(plugin.getPrefix().append(
                    plugin.getColorUtil().parseColor("<yellow>Deleted trade: <white>" + trade.getId())
                ));
                openManagerGUI(player, session);
                break;
            case SHIFT_RIGHT:
                // Get output item
                ItemStack outputItem = createItemFromTradeItem(trade.getOutput());
                if (outputItem != null) {
                    player.getInventory().addItem(outputItem);
                    player.sendMessage(plugin.getPrefix().append(
                        plugin.getColorUtil().parseColor("<green>Received output item from trade: <white>" + trade.getId())
                    ));
                } else {
                    player.sendMessage(plugin.getPrefix().append(
                        plugin.getColorUtil().parseColor("<red>Failed to create output item for trade: " + trade.getId())
                    ));
                }
                break;
        }
    }

    private void handleShiftClick(Player player, EditorSession session, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;

        Inventory gui = player.getOpenInventory().getTopInventory();
        ItemStack itemToAdd = item.clone();
        
        // Find first available slot
        if (gui.getItem(INPUT1_SLOT) == null || isPlaceholderItem(gui.getItem(INPUT1_SLOT))) {
            gui.setItem(INPUT1_SLOT, itemToAdd);
            item.setAmount(0);
        } else if (gui.getItem(INPUT2_SLOT) == null || isPlaceholderItem(gui.getItem(INPUT2_SLOT))) {
            gui.setItem(INPUT2_SLOT, itemToAdd);
            item.setAmount(0);
        } else if (gui.getItem(OUTPUT_SLOT) == null || isPlaceholderItem(gui.getItem(OUTPUT_SLOT))) {
            gui.setItem(OUTPUT_SLOT, itemToAdd);
            item.setAmount(0);
        } else {
            player.sendMessage(plugin.getPrefix().append(
                plugin.getColorUtil().parseColor("<red>All editor slots are full!")
            ));
            return;
        }
        
        updateSessionFromGUI(player, session);
    }

    // Utility Methods

    private void updateSessionFromGUI(Player player, EditorSession session) {
        Inventory gui = player.getOpenInventory().getTopInventory();
        
        ItemStack item1 = gui.getItem(INPUT1_SLOT);
        session.setInput1(isValidItem(item1) ? createTradeItemFromItemStack(item1) : null);
        
        ItemStack item2 = gui.getItem(INPUT2_SLOT);
        session.setInput2(isValidItem(item2) ? createTradeItemFromItemStack(item2) : null);
        
        ItemStack item3 = gui.getItem(OUTPUT_SLOT);
        session.setOutput(isValidItem(item3) ? createTradeItemFromItemStack(item3) : null);
        
        plugin.debug("Session updated for " + player.getName() + 
            " - Input1: " + (session.getInput1() != null) + 
            ", Input2: " + (session.getInput2() != null) + 
            ", Output: " + (session.getOutput() != null));
    }

    private void returnItemsToPlayer(Player player, Inventory inventory) {
        ItemStack[] itemsToReturn = {
            inventory.getItem(INPUT1_SLOT),
            inventory.getItem(INPUT2_SLOT),
            inventory.getItem(OUTPUT_SLOT)
        };
        
        for (ItemStack item : itemsToReturn) {
            if (isValidItem(item)) {
                player.getInventory().addItem(item);
            }
        }
    }

    private boolean isValidItem(ItemStack item) {
        return item != null && item.getType() != Material.AIR && !isPlaceholderItem(item);
    }

    private boolean isPlaceholderItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.hasLore() && meta.lore() != null && 
               meta.lore().stream().anyMatch(line -> 
                   net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                       .serialize(line).contains("Place") || 
                   net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                       .serialize(line).contains("Drag"));
    }

    private TradeDefinition.TradeItem createTradeItemFromItemStack(ItemStack item) {
        if (plugin.getItemManager().isCustomItem(item)) {
            String customId = plugin.getItemManager().getCustomItemId(item);
            return new TradeDefinition.TradeItem("itemx:" + customId, item.getAmount());
        } else {
            return new TradeDefinition.TradeItem(item.getType().name().toLowerCase(), item.getAmount());
        }
    }

    private ItemStack createItemFromTradeItem(TradeDefinition.TradeItem tradeItem) {
        if (tradeItem == null) return null;
        
        ItemStack item = null;
        if (tradeItem.isCustomItem()) {
            String customId = tradeItem.getCustomItemId();
            ItemDefinition definition = plugin.getItemManager().getItemDefinition(customId);
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

    private String generateTradeId() {
        return "trade_" + System.currentTimeMillis();
    }

    private boolean isValidTradeId(String id) {
        return id != null && id.matches("^[a-zA-Z0-9_]{1,32}$");
    }

    // Item Creation Methods

    private ItemStack createGlassPane(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createPlaceholderItem(Material material, String name, String description) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getColorUtil().parseColor(name));
            meta.lore(Arrays.asList(
                plugin.getColorUtil().parseColor("<gray>" + description),
                plugin.getColorUtil().parseColor("<yellow>Drag and drop or shift-click an item here")
            ));
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

    private ItemStack createDecorationItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getColorUtil().parseColor(name));
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

    private ItemStack createTradeDisplay(TradeDefinition trade, int position) {
        ItemStack displayItem = createItemFromTradeItem(trade.getOutput());
        if (displayItem == null) {
            displayItem = new ItemStack(Material.BARRIER);
        } else {
            displayItem = displayItem.clone();
            displayItem.setAmount(1);
        }

        ItemMeta meta = displayItem.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getColorUtil().parseColor(
                "<gradient:#4ECDC4:#44A08D>" + position + ". " + trade.getId() + "</gradient>"
            ));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(""));
            lore.add(plugin.getColorUtil().parseColor("<aqua>📥 Input 1: <white>" + 
                trade.getInput1().getItem() + " x" + trade.getInput1().getAmount()));
            if (trade.hasSecondInput()) {
                lore.add(plugin.getColorUtil().parseColor("<aqua>📥 Input 2: <white>" + 
                    trade.getInput2().getItem() + " x" + trade.getInput2().getAmount()));
            }
            lore.add(plugin.getColorUtil().parseColor("<green>📤 Output: <white>" + 
                trade.getOutput().getItem() + " x" + trade.getOutput().getAmount()));
            lore.add(Component.text(""));
            lore.add(plugin.getColorUtil().parseColor("<yellow>⚡ Actions:"));
            lore.add(plugin.getColorUtil().parseColor("<gray>• <green>Left Click</green> to Edit"));
            lore.add(plugin.getColorUtil().parseColor("<gray>• <red>Right Click</red> to Delete"));
            lore.add(plugin.getColorUtil().parseColor("<gray>• <gold>Shift+Right</gold> to Get Output Item"));
            
            meta.lore(lore);
            displayItem.setItemMeta(meta);
        }
        return displayItem;
    }

    /**
     * Clean, simplified session class
     */
    public static class EditorSession {
        private final UUID playerId;
        private final String guiName;
        private String tradeId;
        private TradeDefinition.TradeItem input1;
        private TradeDefinition.TradeItem input2;
        private TradeDefinition.TradeItem output;
        private TradeDefinition existingTrade;
        private boolean waitingForInput = false;
        private boolean closingNormally = false;
        private boolean managerMode = false;

        public EditorSession(UUID playerId, String guiName, String tradeId) {
            this.playerId = playerId;
            this.guiName = guiName;
            this.tradeId = tradeId;
        }

        // Getters and setters
        public UUID getPlayerId() { return playerId; }
        public String getGuiName() { return guiName; }
        public String getTradeId() { return tradeId; }
        public void setTradeId(String tradeId) { this.tradeId = tradeId; }
        public TradeDefinition.TradeItem getInput1() { return input1; }
        public void setInput1(TradeDefinition.TradeItem input1) { this.input1 = input1; }
        public TradeDefinition.TradeItem getInput2() { return input2; }
        public void setInput2(TradeDefinition.TradeItem input2) { this.input2 = input2; }
        public TradeDefinition.TradeItem getOutput() { return output; }
        public void setOutput(TradeDefinition.TradeItem output) { this.output = output; }
        public TradeDefinition getExistingTrade() { return existingTrade; }
        public void setExistingTrade(TradeDefinition existingTrade) { this.existingTrade = existingTrade; }
        public boolean isWaitingForInput() { return waitingForInput; }
        public void setWaitingForInput(boolean waitingForInput) { this.waitingForInput = waitingForInput; }
        public boolean isClosingNormally() { return closingNormally; }
        public void setClosingNormally(boolean closingNormally) { this.closingNormally = closingNormally; }
        public boolean isManagerMode() { return managerMode; }
        public void setManagerMode(boolean managerMode) { this.managerMode = managerMode; }
    }
}