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

public class TradeEditor implements Listener {

    private final ItemX plugin;
    private final Map<UUID, EditorSession> editorSessions;
    private final Map<UUID, ReorderSession> reorderSessions;
    private final Map<UUID, EnhancedManagerSession> enhancedSessions;

    // Slot constants for better organization
    private static final int INPUT1_SLOT = 10;
    private static final int INPUT2_SLOT = 12;
    private static final int OUTPUT_SLOT = 16;
    private static final int SAVE_SLOT = 22;
    private static final int CANCEL_SLOT = 23;
    private static final int TRADE_ID_SLOT = 4;

    public TradeEditor(ItemX plugin) {
        this.plugin = plugin;
        this.editorSessions = new HashMap<>();
        this.reorderSessions = new HashMap<>();
        this.enhancedSessions = new HashMap<>();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openTradeEditor(Player player, String guiName, String tradeId) {
        TradeGUI tradeGUI = plugin.getTradeManager().getTradeGUI(guiName);
        if (tradeGUI == null) {
            player.sendMessage(plugin.getPrefix().append(
                    plugin.getColorUtil().parseColor("<red>Trade GUI '" + guiName + "' not found.")
            ));
            return;
        }

        TradeDefinition existingTrade = tradeId != null ? tradeGUI.getTrade(tradeId) : null;
        EditorSession session = new EditorSession(guiName, tradeId, existingTrade);

        // If editing, populate session with existing trade items
        if (existingTrade != null) {
            session.input1 = existingTrade.getInput1();
            session.input2 = existingTrade.getInput2();
            session.output = existingTrade.getOutput();
        }

        editorSessions.put(player.getUniqueId(), session);
        openEditorGUI(player, session);
    }

    public void openEnhancedTradeManager(Player player, String guiName) {
        TradeGUI tradeGUI = plugin.getTradeManager().getTradeGUI(guiName);
        if (tradeGUI == null) {
            player.sendMessage(plugin.getPrefix().append(
                    plugin.getColorUtil().parseColor("<red>Trade GUI '" + guiName + "' not found.")
            ));
            return;
        }

        EnhancedManagerSession session = new EnhancedManagerSession(guiName);
        enhancedSessions.put(player.getUniqueId(), session);

        openEnhancedManagerGUI(player, session);
    }

    private void openEnhancedManagerGUI(Player player, EnhancedManagerSession session) {
        Inventory gui = Bukkit.createInventory(null, 54,
                plugin.getColorUtil().parseColor("<gradient:#FF6B6B:#4ECDC4>✦ Trade Manager: " + session.guiName + " ✦</gradient>"));

        gui.setItem(0, createHeaderItem(Material.DIAMOND_SWORD,
                "<gradient:#FFD700:#FF8C00>✦ Trade Manager</gradient>",
                Arrays.asList(
                        "<gray>Managing trades for: <aqua>" + session.guiName,
                        "<gray>Total trades: <yellow>" + plugin.getTradeManager().getTradeGUI(session.guiName).getTradeCount(),
                        "",
                        "<green>✓ Left click to edit trade",
                        "<green>✓ Right click to delete trade",
                        "<green>✓ Shift+Right click to get output item"
                )));

        gui.setItem(7, createControlButton(Material.EMERALD_BLOCK,
                "<gradient:#00FF00:#32CD32>✚ Add New Trade</gradient>", "enhanced_add_trade"));
        gui.setItem(8, createControlButton(Material.BARRIER,
                "<gradient:#FF4444:#CC0000>✖ Close Manager</gradient>", "enhanced_close"));
        gui.setItem(9, createControlButton(Material.CRAFTING_TABLE,
                "<gradient:#FFA500:#FF8C00>🔧 Reorder Trades</gradient>", "enhanced_reorder"));

        for (int i = 10; i < 18; i++) {
            gui.setItem(i, createSeparator());
        }

        List<TradeDefinition> trades = plugin.getTradeManager().getTradeGUI(session.guiName).getTrades();
        for (int i = 0; i < trades.size() && i < 36; i++) {
            TradeDefinition trade = trades.get(i);
            ItemStack tradeDisplay = createEnhancedTradeDisplay(trade, i + 1);
            gui.setItem(18 + i, tradeDisplay);
        }

        player.openInventory(gui);
    }

    public void openReorderGUI(Player player, String guiName) {
        TradeGUI tradeGUI = plugin.getTradeManager().getTradeGUI(guiName);
        if (tradeGUI == null) {
            player.sendMessage(plugin.getPrefix().append(
                    plugin.getColorUtil().parseColor("<red>Trade GUI '" + guiName + "' not found.")
            ));
            return;
        }

        Inventory gui = Bukkit.createInventory(null, 54,
                plugin.getColorUtil().parseColor("<gradient:#FFD700:#FF8C00>🔧 Reorder Trades: " + guiName + "</gradient>"));

        gui.setItem(4, createHeaderItem(Material.BOOK,
                "<gradient:#4ECDC4:#44A08D>📋 Reorder Instructions</gradient>",
                Arrays.asList(
                        "<yellow>How to reorder trades:",
                        "<gray>• Use the arrow buttons to move trades",
                        "<gray>• <green>↑ Move Up</green> - Move trade up in order",
                        "<gray>• <red>↓ Move Down</red> - Move trade down in order",
                        "<gray>• Click <gold>Add New Trade</gold> to create new",
                        "<gray>• Click <green>Done</green> when finished"
                )));

        List<TradeDefinition> trades = tradeGUI.getTrades();
        for (int i = 0; i < trades.size() && i < 9; i++) {
            TradeDefinition trade = trades.get(i);
            ItemStack tradeItem = createReorderTradeDisplay(trade, i + 1);
            gui.setItem(i, tradeItem);

            if (i > 0) {
                gui.setItem(9 + i, createControlButton(Material.LIME_CONCRETE,
                        "<gradient:#00FF00:#32CD32>↑ Move Up</gradient>", "move_up_" + i));
            }
            if (i < trades.size() - 1) {
                gui.setItem(18 + i, createControlButton(Material.RED_CONCRETE,
                        "<gradient:#FF4444:#CC0000>↓ Move Down</gradient>", "move_down_" + i));
            }
        }

        gui.setItem(52, createControlButton(Material.EMERALD,
                "<gradient:#00FF00:#32CD32>✓ Done</gradient>", "done"));
        gui.setItem(53, createControlButton(Material.GOLD_INGOT,
                "<gradient:#FFD700:#FFA500>✚ Add New Trade</gradient>", "add_trade"));

        ReorderSession session = new ReorderSession(guiName);
        reorderSessions.put(player.getUniqueId(), session);

        player.openInventory(gui);
    }

    private void openEditorGUI(Player player, EditorSession session) {
        Inventory gui = Bukkit.createInventory(null, 27,
                plugin.getColorUtil().parseColor("<gradient:#9146FF:#00D4FF>⚡ Trade Editor ⚡</gradient>"));

        for (int i = 0; i < 27; i++) {
            if (i != INPUT1_SLOT && i != INPUT2_SLOT && i != OUTPUT_SLOT &&
                    i != SAVE_SLOT && i != CANCEL_SLOT && i != TRADE_ID_SLOT &&
                    i != 11 && i != 14) {
                gui.setItem(i, createBackgroundPane());
            }
        }

        // --- FIX: Populate slots based on session data, showing placeholders if empty ---
        if (session.input1 != null) {
            gui.setItem(INPUT1_SLOT, createItemFromTradeItem(session.input1));
        } else {
            gui.setItem(INPUT1_SLOT, createInputSlot(Material.CHEST, "<gradient:#FFD700:#FFA500>📥 Input Slot 1</gradient>", "Place the required item here."));
        }

        if (session.input2 != null) {
            gui.setItem(INPUT2_SLOT, createItemFromTradeItem(session.input2));
        } else {
            gui.setItem(INPUT2_SLOT, createInputSlot(Material.ENDER_CHEST, "<gradient:#9370DB:#8A2BE2>📥 Input Slot 2</gradient>", "Place the optional second item here."));
        }

        if (session.output != null) {
            gui.setItem(OUTPUT_SLOT, createItemFromTradeItem(session.output));
        } else {
            gui.setItem(OUTPUT_SLOT, createInputSlot(Material.SHULKER_BOX, "<gradient:#32CD32:#228B22>📤 Output Slot</gradient>", "Place the resulting item here."));
        }

        gui.setItem(11, createIndicator(Material.LIME_CONCRETE, "<gradient:#00FF00:#32CD32>+</gradient>", "Plus"));
        gui.setItem(14, createIndicator(Material.YELLOW_CONCRETE, "<gradient:#FFD700:#FFA500>→</gradient>", "Arrow"));

        gui.setItem(SAVE_SLOT, createControlButton(Material.EMERALD, "<gradient:#00FF00:#32CD32>💾 Save Trade</gradient>", "save_trade"));
        gui.setItem(CANCEL_SLOT, createControlButton(Material.BARRIER, "<gradient:#FF4444:#CC0000>❌ Cancel</gradient>", "cancel_trade"));

        String tradeIdText = session.tradeId != null ? session.tradeId : session.generateTradeId();
        gui.setItem(TRADE_ID_SLOT, createControlButton(Material.NAME_TAG, "<gradient:#4ECDC4:#44A08D>🏷️ Trade ID: " + tradeIdText + "</gradient>", "set_trade_id"));

        player.openInventory(gui);
    }

    // --- FIX: Rewrote inventory click handling logic for proper drag-and-drop ---
    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();

        // Handle Editor, Reorder, and Enhanced Manager sessions
        if (editorSessions.containsKey(playerId)) {
            handleEditorClick(player, editorSessions.get(playerId), event);
        } else if (reorderSessions.containsKey(playerId)) {
            event.setCancelled(true);
            handleReorderClick(player, reorderSessions.get(playerId), event);
        } else if (enhancedSessions.containsKey(playerId)) {
            event.setCancelled(true);
            handleEnhancedClick(player, enhancedSessions.get(playerId), event);
        }
    }

    private void handleEditorClick(Player player, EditorSession session, InventoryClickEvent event) {
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) return;

        int rawSlot = event.getRawSlot();
        Inventory topInventory = player.getOpenInventory().getTopInventory();

        // Check if the click is in the editor GUI
        if (clickedInventory.equals(topInventory)) {
            // Check if it's one of our special item slots
            if (rawSlot == INPUT1_SLOT || rawSlot == INPUT2_SLOT || rawSlot == OUTPUT_SLOT) {
                // DO NOT CANCEL THE EVENT. Let Bukkit handle the item moving.
                // Schedule a task to update our session state after the inventory has visually updated.
                Bukkit.getScheduler().runTask(plugin, () -> updateSessionFromInventory(session, topInventory));
                return;
            }

            // It's a click on a button or background pane in our GUI, so cancel it.
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && clickedItem.hasItemMeta()) {
                String action = getActionFromItem(clickedItem);
                switch (action) {
                    case "save_trade":
                        saveTrade(player, session);
                        break;
                    case "cancel_trade":
                        cancelAndClose(player);
                        break;
                    case "set_trade_id":
                        handleSetTradeId(player, session);
                        break;
                }
            }
        } else {
            // Click is in the player's inventory. Handle shift-clicking.
            if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
                event.setCancelled(true); // Cancel default shift-click behavior
                handleShiftClick(player, session, event.getCurrentItem());
            }
        }
    }

    // --- FIX: Rewrote inventory drag handling to allow dragging into specified slots ---
    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();

        if (editorSessions.containsKey(playerId)) {
            EditorSession session = editorSessions.get(playerId);
            Inventory topInventory = player.getOpenInventory().getTopInventory();

            // Check if any part of the drag is outside our allowed item slots
            Set<Integer> allowedSlots = Set.of(INPUT1_SLOT, INPUT2_SLOT, OUTPUT_SLOT);
            for (int slot : event.getRawSlots()) {
                if (slot < topInventory.getSize()) { // Make sure the slot is in the top inventory
                    if (!allowedSlots.contains(slot)) {
                        event.setCancelled(true); // Cancel if dragging over a restricted slot (e.g., glass pane)
                        return;
                    }
                }
            }

            // The drag is valid (only over our item slots). Let it happen.
            // Schedule a task to update our session state after the items have been placed.
            Bukkit.getScheduler().runTask(plugin, () -> updateSessionFromInventory(session, topInventory));

        } else if (reorderSessions.containsKey(playerId) || enhancedSessions.containsKey(playerId)) {
            event.setCancelled(true); // Prevent dragging in other UIs
        }
    }

    /**
     * NEW: Reads the current items in the editor GUI and updates the session state.
     * This is called 1 tick after a click or drag to ensure the inventory has updated.
     */
    private void updateSessionFromInventory(EditorSession session, Inventory inventory) {
        ItemStack item1 = inventory.getItem(INPUT1_SLOT);
        session.input1 = (item1 != null && item1.getType() != Material.AIR) ? createTradeItemFromItemStack(item1) : null;

        ItemStack item2 = inventory.getItem(INPUT2_SLOT);
        session.input2 = (item2 != null && item2.getType() != Material.AIR) ? createTradeItemFromItemStack(item2) : null;

        ItemStack item3 = inventory.getItem(OUTPUT_SLOT);
        session.output = (item3 != null && item3.getType() != Material.AIR) ? createTradeItemFromItemStack(item3) : null;

        plugin.debug("Session updated: Input1=" + (session.input1 != null) + ", Input2=" + (session.input2 != null) + ", Output=" + (session.output != null));
    }

    /**
     * NEW: Handles shift-clicking items from the player's inventory into the editor.
     */
    private void handleShiftClick(Player player, EditorSession session, ItemStack clickedItem) {
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        Inventory gui = player.getOpenInventory().getTopInventory();
        ItemStack itemToAdd = clickedItem.clone();

        // Logic to place the item in the first available slot, respecting existing items
        if (gui.getItem(INPUT1_SLOT) == null || gui.getItem(INPUT1_SLOT).getType() == Material.AIR) {
            gui.setItem(INPUT1_SLOT, itemToAdd);
            clickedItem.setAmount(0); // "Consume" the item from player's inventory
        } else if (gui.getItem(INPUT2_SLOT) == null || gui.getItem(INPUT2_SLOT).getType() == Material.AIR) {
            gui.setItem(INPUT2_SLOT, itemToAdd);
            clickedItem.setAmount(0);
        } else if (gui.getItem(OUTPUT_SLOT) == null || gui.getItem(OUTPUT_SLOT).getType() == Material.AIR) {
            gui.setItem(OUTPUT_SLOT, itemToAdd);
            clickedItem.setAmount(0);
        } else {
            // Optional: send a message if all slots are full
            player.sendMessage(plugin.getPrefix().append(plugin.getColorUtil().parseColor("<red>All editor slots are full!")));
            return; // Don't proceed to update if nothing changed
        }

        // Immediately update the session state
        updateSessionFromInventory(session, gui);
    }


    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (editorSessions.containsKey(playerId)) {
            EditorSession session = editorSessions.get(playerId);
            if (session.waitingForId) {
                event.setCancelled(true);

                String newId = event.getMessage().trim();
                if (isValidTradeId(newId)) {
                    session.tradeId = newId;
                    session.waitingForId = false;

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage(plugin.getPrefix().append(
                                plugin.getColorUtil().parseColor("<green>Trade ID set to: <yellow>" + newId)
                        ));
                        openEditorGUI(player, session);
                    });
                } else {
                    session.waitingForId = false; // Stop waiting even on failure
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage(plugin.getPrefix().append(
                                plugin.getColorUtil().parseColor("<red>Invalid trade ID! Use only letters, numbers, and underscores (1-32 chars).")
                        ));
                        openEditorGUI(player, session);
                    });
                }
            }
        }
    }

    private void handleSetTradeId(Player player, EditorSession session) {
        session.waitingForId = true;
        player.closeInventory();
        player.sendMessage(plugin.getPrefix().append(
                plugin.getColorUtil().parseColor("<yellow>Type the new trade ID in chat and press Enter.")
        ));
        player.sendMessage(plugin.getPrefix().append(
                plugin.getColorUtil().parseColor("<gray>(Letters, numbers, and underscores only. Max 32 characters)")
        ));
    }

    private boolean isValidTradeId(String id) {
        return id != null && id.matches("^[a-zA-Z0-9_]{1,32}$");
    }

    // Remaining handlers (reorder, enhanced) are mostly fine as they cancel all clicks.
    private void handleReorderClick(Player player, ReorderSession session, InventoryClickEvent event) {
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) return;

        String action = getActionFromItem(clickedItem);

        switch (action) {
            case "done":
                player.closeInventory();
                // reorderSessions.remove is handled in onInventoryClose
                player.sendMessage(plugin.getPrefix().append(
                        plugin.getColorUtil().parseColor("<green>Trade reordering completed!")
                ));
                openEnhancedTradeManager(player, session.guiName); // Go back to manager
                break;
            case "add_trade":
                player.closeInventory();
                openTradeEditor(player, session.guiName, null);
                break;
            default:
                if (action.startsWith("move_up_")) {
                    int tradeIndex = Integer.parseInt(action.substring(8));
                    if (tradeIndex > 0) {
                        plugin.getTradeManager().moveTrade(session.guiName, tradeIndex, tradeIndex - 1);
                        openReorderGUI(player, session.guiName); // Refresh GUI
                    }
                } else if (action.startsWith("move_down_")) {
                    int tradeIndex = Integer.parseInt(action.substring(10));
                    List<TradeDefinition> trades = plugin.getTradeManager().getTradeGUI(session.guiName).getTrades();
                    if (tradeIndex < trades.size() - 1) {
                        plugin.getTradeManager().moveTrade(session.guiName, tradeIndex, tradeIndex + 1);
                        openReorderGUI(player, session.guiName); // Refresh GUI
                    }
                }
                break;
        }
    }

    private void handleEnhancedClick(Player player, EnhancedManagerSession session, InventoryClickEvent event) {
        int slot = event.getSlot();
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) return;

        String action = getActionFromItem(clickedItem);

        switch (action) {
            case "enhanced_add_trade":
                player.closeInventory();
                openTradeEditor(player, session.guiName, null);
                break;
            case "enhanced_close":
                player.closeInventory();
                break;
            case "enhanced_reorder":
                player.closeInventory();
                openReorderGUI(player, session.guiName);
                break;
            default:
                if (slot >= 18 && slot <= 53) {
                    handleTradeClick(player, session, slot - 18, event.getClick());
                }
                break;
        }
    }

    private void handleTradeClick(Player player, EnhancedManagerSession session, int tradeIndex, org.bukkit.event.inventory.ClickType clickType) {
        List<TradeDefinition> trades = plugin.getTradeManager().getTradeGUI(session.guiName).getTrades();
        if (tradeIndex >= trades.size()) return;

        TradeDefinition trade = trades.get(tradeIndex);

        switch (clickType) {
            case LEFT:
                player.closeInventory();
                openTradeEditor(player, session.guiName, trade.getId());
                break;
            case RIGHT:
                plugin.getTradeManager().removeTrade(session.guiName, trade.getId());
                player.sendMessage(plugin.getPrefix().append(
                        plugin.getColorUtil().parseColor("<yellow>Deleted trade: <white>" + trade.getId())
                ));
                openEnhancedManagerGUI(player, session); // Refresh
                break;
            case SHIFT_RIGHT:
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

    private void saveTrade(Player player, EditorSession session) {
        if (session.input1 == null || session.output == null) {
            player.sendMessage(plugin.getPrefix().append(
                    plugin.getColorUtil().parseColor("<red>Input 1 and Output are required!")
            ));
            return;
        }

        String tradeId = session.tradeId != null ? session.tradeId : session.generateTradeId();
        TradeDefinition trade = new TradeDefinition(tradeId, session.input1, session.input2, session.output);

        // Remove existing trade if editing (use the original ID)
        if (session.existingTrade != null) {
            plugin.getTradeManager().removeTrade(session.guiName, session.existingTrade.getId());
        }

        plugin.getTradeManager().addTrade(session.guiName, trade);
        player.sendMessage(plugin.getPrefix().append(
                plugin.getColorUtil().parseColor("<green>Trade saved successfully with ID: <white>" + tradeId)
        ));

        player.closeInventory(); // Will trigger onInventoryClose to clean up session
        openEnhancedTradeManager(player, session.guiName); // Go back to manager
    }

    private void cancelAndClose(Player player) {
        player.sendMessage(plugin.getPrefix().append(
                plugin.getColorUtil().parseColor("<yellow>Trade editing cancelled.")
        ));
        player.closeInventory();

        EditorSession session = editorSessions.get(player.getUniqueId());
        if (session != null) {
            openEnhancedTradeManager(player, session.guiName);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            UUID playerId = player.getUniqueId();

            if (editorSessions.containsKey(playerId)) {
                EditorSession session = editorSessions.get(playerId);
                // Return items to player if inventory is closed unexpectedly
                if (!session.isClosingNormally) {
                    Inventory inv = event.getInventory();
                    ItemStack[] itemsToReturn = {
                            inv.getItem(INPUT1_SLOT),
                            inv.getItem(INPUT2_SLOT),
                            inv.getItem(OUTPUT_SLOT)
                    };
                    for(ItemStack item : itemsToReturn) {
                        if(item != null && item.getType() != Material.AIR && getActionFromItem(item).isEmpty()) {
                            player.getInventory().addItem(item);
                        }
                    }
                }
                editorSessions.remove(playerId);
            } else {
                // Clean up other sessions
                reorderSessions.remove(playerId);
                enhancedSessions.remove(playerId);
            }
        }
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
                plugin.getLogger().warning("Invalid material in trade config: " + tradeItem.getItem());
            }
        }

        if (item != null) {
            item.setAmount(tradeItem.getAmount());
        }
        return item;
    }

    private String getActionFromItem(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
            List<Component> lore = item.getItemMeta().lore();
            if (lore != null && !lore.isEmpty()) {
                String loreText = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(lore.get(lore.size() -1));
                if (loreText.startsWith("Action: ")) {
                    return loreText.substring(8);
                }
            }
        }
        return "";
    }

    // --- UI Creation Methods (Unchanged) ---
    private ItemStack createHeaderItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getColorUtil().parseColor(name));
            List<Component> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(plugin.getColorUtil().parseColor(line));
            }
            meta.lore(coloredLore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSeparator() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createBackgroundPane() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createInputSlot(Material material, String name, String description) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getColorUtil().parseColor(name));
            meta.lore(Arrays.asList(
                    plugin.getColorUtil().parseColor("<gray>" + description),
                    plugin.getColorUtil().parseColor("<yellow>Drag and drop or shift-click an item here.")
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createIndicator(Material material, String name, String description) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getColorUtil().parseColor(name));
            meta.lore(List.of(plugin.getColorUtil().parseColor("<gray>" + description)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createControlButton(Material material, String name, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getColorUtil().parseColor(name));
            if (action != null && !action.isEmpty()) {
                meta.lore(List.of(plugin.getColorUtil().parseColor("<#333333>Action: " + action)));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createEnhancedTradeDisplay(TradeDefinition trade, int position) {
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
            lore.add(plugin.getColorUtil().parseColor("<aqua>📥 Input 1: <white>" + trade.getInput1().getItem() + " x" + trade.getInput1().getAmount()));
            if (trade.hasSecondInput()) {
                lore.add(plugin.getColorUtil().parseColor("<aqua>📥 Input 2: <white>" + trade.getInput2().getItem() + " x" + trade.getInput2().getAmount()));
            }
            lore.add(plugin.getColorUtil().parseColor("<green>📤 Output: <white>" + trade.getOutput().getItem() + " x" + trade.getOutput().getAmount()));
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

    private ItemStack createReorderTradeDisplay(TradeDefinition trade, int position) {
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
            lore.add(plugin.getColorUtil().parseColor("<aqua>📥 Input: <white>" + trade.getInput1().getItem() + " x" + trade.getInput1().getAmount()));
            if (trade.hasSecondInput()) {
                lore.add(plugin.getColorUtil().parseColor("<aqua>📥 Input 2: <white>" + trade.getInput2().getItem() + " x" + trade.getInput2().getAmount()));
            }
            lore.add(Component.text(""));
            lore.add(plugin.getColorUtil().parseColor("<gray>Use buttons below to reorder."));

            meta.lore(lore);
            displayItem.setItemMeta(meta);
        }
        return displayItem;
    }


    // --- Session Classes ---
    private static class EditorSession {
        final String guiName;
        String tradeId;
        final TradeDefinition existingTrade;
        boolean waitingForId = false;
        boolean isClosingNormally = false; // Flag to prevent item return on normal close
        TradeDefinition.TradeItem input1;
        TradeDefinition.TradeItem input2;
        TradeDefinition.TradeItem output;

        EditorSession(String guiName, String tradeId, TradeDefinition existingTrade) {
            this.guiName = guiName;
            this.tradeId = tradeId;
            this.existingTrade = existingTrade;
        }

        String generateTradeId() {
            return "trade_" + (System.currentTimeMillis() / 1000);
        }
    }

    private static class EnhancedManagerSession {
        final String guiName;
        EnhancedManagerSession(String guiName) { this.guiName = guiName; }
    }

    private static class ReorderSession {
        final String guiName;
        ReorderSession(String guiName) { this.guiName = guiName; }
    }
}