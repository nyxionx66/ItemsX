package com.itemx.trade;

import com.itemx.ItemX;
import com.itemx.item.ItemDefinition;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a single trading interface, similar to a villager's.
 * It holds a collection of trade definitions and is responsible for creating
 * and displaying the merchant GUI to a player.
 */
public class TradeGUI {

    private String name;
    private String title;
    private final List<TradeDefinition> trades;
    private final ItemX plugin;

    public TradeGUI(String name, String title, ItemX plugin) {
        this.name = Objects.requireNonNull(name, "GUI name cannot be null");
        this.title = Objects.requireNonNull(title, "GUI title cannot be null");
        this.plugin = Objects.requireNonNull(plugin, "Plugin instance cannot be null");
        this.trades = new ArrayList<>();
    }

    /**
     * Gets the unique identifier name for this GUI.
     *
     * @return The GUI's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the unique identifier name for this GUI.
     * Intended for use by the {@link TradeManager} during renames.
     *
     * @param name The new name for the GUI.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the display title of the merchant inventory.
     *
     * @return The GUI's title, with color codes.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the display title of the merchant inventory.
     *
     * @param title The new title for the GUI.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Gets a mutable list of all trade definitions in this GUI.
     *
     * @return The list of trades.
     */
    public List<TradeDefinition> getTrades() {
        return trades;
    }

    /**
     * Overwrites the current list of trades with a new one.
     *
     * @param trades The new list of trade definitions.
     */
    public void setTrades(List<TradeDefinition> trades) {
        this.trades.clear();
        if (trades != null) {
            this.trades.addAll(trades);
        }
    }

    /**
     * Adds a single trade definition to this GUI.
     *
     * @param trade The trade to add.
     */
    public void addTrade(TradeDefinition trade) {
        trades.add(trade);
    }

    /**
     * Removes a trade from the GUI based on its unique ID.
     *
     * @param tradeId The ID of the trade to remove.
     */
    public void removeTrade(String tradeId) {
        trades.removeIf(trade -> trade.getId().equals(tradeId));
    }

    /**
     * Retrieves a trade by its unique ID.
     *
     * @param tradeId The ID of the trade to find.
     * @return The {@link TradeDefinition} if found, otherwise null.
     */
    public TradeDefinition getTrade(String tradeId) {
        return trades.stream()
                .filter(trade -> trade.getId().equals(tradeId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Moves a trade from one position to another in the list for reordering.
     *
     * @param fromIndex The current index of the trade.
     * @param toIndex   The target index for the trade.
     */
    public void moveTrade(int fromIndex, int toIndex) {
        if (fromIndex < 0 || fromIndex >= trades.size() || toIndex < 0 || toIndex >= trades.size()) {
            return;
        }
        // Use Collections.swap for efficiency, or the remove/add for more complex list types
        TradeDefinition trade = trades.remove(fromIndex);
        trades.add(toIndex, trade);
    }

    /**
     * Opens the merchant trading interface for a specific player.
     *
     * @param player The player to open the GUI for.
     */
    public void openFor(Player player) {
        Merchant merchant = Bukkit.createMerchant(plugin.getColorUtil().parseColor(title));

        List<MerchantRecipe> recipes = new ArrayList<>();
        for (TradeDefinition trade : trades) {
            MerchantRecipe recipe = createRecipe(trade);
            if (recipe != null) {
                recipes.add(recipe);
            }
        }

        merchant.setRecipes(recipes);
        player.openMerchant(merchant, true);
        plugin.debug("Opened trade GUI '" + name + "' for player " + player.getName());
    }

    /**
     * Converts a {@link TradeDefinition} into a usable {@link MerchantRecipe}.
     *
     * @param trade The trade definition to convert.
     * @return A valid MerchantRecipe, or null if any item in the trade is invalid.
     */
    private MerchantRecipe createRecipe(TradeDefinition trade) {
        try {
            ItemStack result = createItemStack(trade.getOutput());
            if (result == null) {
                plugin.getLogger().warning("Failed to create result item for trade '" + trade.getId() + "' in GUI '" + name + "'. Skipping recipe.");
                return null;
            }

            // A recipe's "uses" are not relevant here, so we set it to max to never lock.
            MerchantRecipe recipe = new MerchantRecipe(result, Integer.MAX_VALUE);

            ItemStack input1 = createItemStack(trade.getInput1());
            if (input1 == null) {
                plugin.getLogger().warning("Failed to create input1 item for trade '" + trade.getId() + "' in GUI '" + name + "'. Skipping recipe.");
                return null;
            }
            recipe.addIngredient(input1);

            ItemStack input2 = null;
            if (trade.hasSecondInput()) {
                input2 = createItemStack(trade.getInput2());
                if (input2 == null) {
                    plugin.getLogger().warning("Failed to create input2 item for trade '" + trade.getId() + "' in GUI '" + name + "'. Skipping recipe.");
                    return null;
                }
                recipe.addIngredient(input2);
            }

            plugin.debug("Created recipe for trade " + trade.getId() +
                    " - Input1: " + input1.getType() + "x" + input1.getAmount() +
                    (input2 != null ? " + Input2: " + input2.getType() + "x" + input2.getAmount() : "") +
                    " -> Output: " + result.getType() + "x" + result.getAmount());

            return recipe;
        } catch (Exception e) {
            plugin.getLogger().severe("An unexpected error occurred while creating recipe for trade '" + trade.getId() + "': " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Creates a Bukkit {@link ItemStack} from a {@link TradeDefinition.TradeItem}.
     * This handles both custom ItemX items and vanilla items.
     *
     * @param tradeItem The trade item definition.
     * @return The created ItemStack, or null if the item is invalid.
     */
    private ItemStack createItemStack(TradeDefinition.TradeItem tradeItem) {
        if (tradeItem == null) return null;

        ItemStack item;
        if (tradeItem.isCustomItem()) {
            String customId = tradeItem.getCustomItemId();
            ItemDefinition definition = plugin.getItemManager().getItemDefinition(customId);
            if (definition == null) {
                plugin.getLogger().warning("Custom item definition not found: " + customId);
                return null;
            }
            item = plugin.getItemManager().createItem(definition);
        } else {
            try {
                Material material = Material.valueOf(tradeItem.getItem().toUpperCase());
                item = new ItemStack(material);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid vanilla material: " + tradeItem.getItem());
                return null;
            }
        }

        item.setAmount(tradeItem.getAmount());
        return item;
    }

    /**
     * Removes all trades from this GUI.
     */
    public void clearTrades() {
        trades.clear();
    }

    /**
     * Gets the number of trades configured in this GUI.
     *
     * @return The total count of trades.
     */
    public int getTradeCount() {
        return trades.size();
    }

    /**
     * Checks if a trade with the specified ID exists in this GUI.
     *
     * @param tradeId The ID to check for.
     * @return true if the trade exists, false otherwise.
     */
    public boolean hasTrade(String tradeId) {
        return trades.stream().anyMatch(trade -> trade.getId().equals(tradeId));
    }
}