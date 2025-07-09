package com.itemx.trade;

import com.itemx.ItemX;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Manages all trade GUIs, handling their loading, saving, creation, and modification.
 * This class acts as a central point of control for all trade-related configurations.
 */
public class TradeManager {

    private final ItemX plugin;
    private final TradeParser parser;
    private final Map<String, TradeGUI> tradeGUIs;

    public TradeManager(ItemX plugin) {
        this.plugin = plugin;
        this.parser = new TradeParser(plugin);
        this.tradeGUIs = new HashMap<>();
    }

    /**
     * Loads or reloads all trade GUIs from the configuration files.
     * This clears any existing GUIs in memory before loading.
     */
    public void loadTrades() {
        tradeGUIs.clear();
        Map<String, TradeGUI> loadedGUIs = parser.loadAllTradeGUIs();
        tradeGUIs.putAll(loadedGUIs);

        plugin.getLogger().info("Loaded " + tradeGUIs.size() + " trade GUIs.");
    }

    /**
     * Opens a specific trade GUI for a player.
     *
     * @param player  The player to open the GUI for.
     * @param guiName The unique name of the GUI to open.
     */
    public void openTradeGUI(Player player, String guiName) {
        TradeGUI tradeGUI = tradeGUIs.get(guiName);
        if (tradeGUI == null) {
            player.sendMessage(plugin.getPrefix().append(
                    plugin.getColorUtil().parseColor("<red>Trade GUI '" + guiName + "' not found.")
            ));
            return;
        }

        if (tradeGUI.getTradeCount() == 0) {
            player.sendMessage(plugin.getPrefix().append(
                    plugin.getColorUtil().parseColor("<yellow>Trade GUI '" + guiName + "' has no trades configured.")
            ));
            return;
        }

        tradeGUI.openFor(player);
    }

    /**
     * Retrieves a trade GUI by its name.
     *
     * @param guiName The name of the GUI.
     * @return The {@link TradeGUI} object, or null if not found.
     */
    public TradeGUI getTradeGUI(String guiName) {
        return tradeGUIs.get(guiName);
    }

    /**
     * Gets a set of all loaded trade GUI names.
     *
     * @return An unmodifiable set of GUI names.
     */
    public Set<String> getTradeGUINames() {
        return Collections.unmodifiableSet(tradeGUIs.keySet());
    }

    /**
     * Checks if a trade GUI with the given name exists.
     *
     * @param guiName The name to check.
     * @return true if the GUI exists, false otherwise.
     */
    public boolean hasTradeGUI(String guiName) {
        return tradeGUIs.containsKey(guiName);
    }

    /**
     * Creates a new, empty trade GUI and saves it to a file.
     *
     * @param guiName The unique name for the new GUI.
     * @param title   The display title for the new GUI.
     */
    public void createTradeGUI(String guiName, String title) {
        if (hasTradeGUI(guiName)) {
            plugin.getLogger().warning("Attempted to create a trade GUI that already exists: " + guiName);
            return;
        }
        TradeGUI tradeGUI = new TradeGUI(guiName, title, plugin);
        tradeGUIs.put(guiName, tradeGUI);
        parser.saveTradeGUI(tradeGUI);
        plugin.debug("Created new trade GUI: " + guiName);
    }

    /**
     * Deletes a trade GUI from memory and its corresponding file.
     *
     * @param guiName The name of the GUI to delete.
     */
    public void deleteTradeGUI(String guiName) {
        if (tradeGUIs.remove(guiName) != null) {
            parser.deleteTradeGUI(guiName);
            plugin.debug("Deleted trade GUI: " + guiName);
        }
    }

    /**
     * Renames a trade GUI. This involves updating it in memory, deleting the old
     * configuration file, and saving a new one with the new name.
     *
     * @param oldName The current name of the GUI.
     * @param newName The desired new name for the GUI.
     */
    public void renameTradeGUI(String oldName, String newName) {
        TradeGUI tradeGUI = tradeGUIs.remove(oldName);
        if (tradeGUI != null) {
            parser.deleteTradeGUI(oldName); // Delete the old file

            tradeGUI.setName(newName); // Assumes TradeGUI has a setName method

            tradeGUIs.put(newName, tradeGUI); // Re-add to map with the new name
            parser.saveTradeGUI(tradeGUI); // Save to the new file
            plugin.debug("Renamed trade GUI from '" + oldName + "' to '" + newName + "'.");
        }
    }

    /**
     * Updates the title of a specific trade GUI.
     *
     * @param guiName  The name of the GUI to update.
     * @param newTitle The new title to set.
     */
    public void setTradeGUITitle(String guiName, String newTitle) {
        TradeGUI tradeGUI = tradeGUIs.get(guiName);
        if (tradeGUI != null) {
            tradeGUI.setTitle(newTitle); // Assumes TradeGUI has a setTitle method
            parser.saveTradeGUI(tradeGUI); // Save the changes
            plugin.debug("Changed title of trade GUI '" + guiName + "' to '" + newTitle + "'.");
        }
    }

    /**
     * Adds a new trade definition to a GUI and saves the changes.
     *
     * @param guiName The name of the GUI to add the trade to.
     * @param trade   The {@link TradeDefinition} to add.
     */
    public void addTrade(String guiName, TradeDefinition trade) {
        TradeGUI tradeGUI = tradeGUIs.get(guiName);
        if (tradeGUI != null) {
            tradeGUI.addTrade(trade);
            parser.saveTradeGUI(tradeGUI);
            plugin.debug("Added trade '" + trade.getId() + "' to GUI '" + guiName + "'.");
        }
    }

    /**
     * Removes a trade from a GUI by its ID and saves the changes.
     *
     * @param guiName The name of the GUI to remove the trade from.
     * @param tradeId The ID of the trade to remove.
     */
    public void removeTrade(String guiName, String tradeId) {
        TradeGUI tradeGUI = tradeGUIs.get(guiName);
        if (tradeGUI != null) {
            tradeGUI.removeTrade(tradeId);
            parser.saveTradeGUI(tradeGUI);
            plugin.debug("Removed trade '" + tradeId + "' from GUI '" + guiName + "'.");
        }
    }

    /**
     * Moves a trade within a GUI's list to a new position for reordering.
     *
     * @param guiName   The name of the GUI.
     * @param fromIndex The current index of the trade.
     * @param toIndex   The target index for the trade.
     */
    public void moveTrade(String guiName, int fromIndex, int toIndex) {
        TradeGUI tradeGUI = tradeGUIs.get(guiName);
        if (tradeGUI != null) {
            tradeGUI.moveTrade(fromIndex, toIndex);
            parser.saveTradeGUI(tradeGUI);
            plugin.debug("Moved trade in GUI '" + guiName + "' from index " + fromIndex + " to " + toIndex + ".");
        }
    }

    /**
     * Gets the total number of loaded trade GUIs.
     *
     * @return The count of trade GUIs.
     */
    public int getTradeGUICount() {
        return tradeGUIs.size();
    }

    /**
     * Opens the trade manager interface for a player
     *
     * @param player  The player to open the manager for
     * @param guiName The unique name of the GUI to manage
     */
    public void openTradeManager(Player player, String guiName) {
        TradeGUI tradeGUI = tradeGUIs.get(guiName);
        if (tradeGUI == null) {
            player.sendMessage(plugin.getPrefix().append(
                    plugin.getColorUtil().parseColor("<red>Trade GUI '" + guiName + "' not found.")
            ));
            return;
        }

        plugin.getTradeEditor().openTradeManager(player, guiName);
    }

    /**
     * Opens the trade editor interface for a player
     *
     * @param player  The player to open the editor for
     * @param guiName The unique name of the GUI to edit
     * @param tradeId The ID of the trade to edit (null for new trade)
     */
    public void openTradeEditor(Player player, String guiName, String tradeId) {
        TradeGUI tradeGUI = tradeGUIs.get(guiName);
        if (tradeGUI == null) {
            player.sendMessage(plugin.getPrefix().append(
                    plugin.getColorUtil().parseColor("<red>Trade GUI '" + guiName + "' not found.")
            ));
            return;
        }

        plugin.getTradeEditor().openTradeEditor(player, guiName, tradeId);
    }

    /**
     * Opens the reorder GUI for a player
     *
     * @param player  The player to open the reorder GUI for
     * @param guiName The unique name of the GUI to reorder
     */
    public void openReorderGUI(Player player, String guiName) {
        TradeGUI tradeGUI = tradeGUIs.get(guiName);
        if (tradeGUI == null) {
            player.sendMessage(plugin.getPrefix().append(
                    plugin.getColorUtil().parseColor("<red>Trade GUI '" + guiName + "' not found.")
            ));
            return;
        }

        plugin.getTradeReorderGUI().openReorderGUI(player, guiName);
    }

    /**
     * Alias for {@link #loadTrades()}, providing a clear reload entry point.
     */
    public void reload() {
        loadTrades();
    }
}