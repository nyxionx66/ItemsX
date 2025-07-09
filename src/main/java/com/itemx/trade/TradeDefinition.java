package com.itemx.trade;

import org.bukkit.inventory.ItemStack;

import java.util.Objects;

/**
 * Represents a single, immutable trade recipe.
 * This is a data class that holds the definition of a trade, including
 * its unique ID, inputs, and output.
 */
public final class TradeDefinition {

    private final String id;
    private final TradeItem input1;
    private final TradeItem input2; // Can be null for single-item trades
    private final TradeItem output;

    /**
     * Constructs a new TradeDefinition.
     *
     * @param id     The unique identifier for this trade.
     * @param input1 The primary required item for the trade.
     * @param input2 The optional secondary item for the trade (can be null).
     * @param output The resulting item from the trade.
     */
    public TradeDefinition(String id, TradeItem input1, TradeItem input2, TradeItem output) {
        this.id = Objects.requireNonNull(id, "Trade ID cannot be null");
        this.input1 = Objects.requireNonNull(input1, "Input1 cannot be null");
        this.input2 = input2; // input2 is nullable
        this.output = Objects.requireNonNull(output, "Output cannot be null");
    }

    public String getId() {
        return id;
    }

    public TradeItem getInput1() {
        return input1;
    }

    public TradeItem getInput2() {
        return input2;
    }

    public TradeItem getOutput() {
        return output;
    }

    /**
     * Checks if this trade requires a second input item.
     *
     * @return true if a second input is defined, false otherwise.
     */
    public boolean hasSecondInput() {
        return input2 != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TradeDefinition that = (TradeDefinition) o;
        return id.equals(that.id) &&
                input1.equals(that.input1) &&
                Objects.equals(input2, that.input2) &&
                output.equals(that.output);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, input1, input2, output);
    }

    @Override
    public String toString() {
        return "TradeDefinition{" +
                "id='" + id + '\'' +
                ", input1=" + input1 +
                ", input2=" + input2 +
                ", output=" + output +
                '}';
    }

    /**
     * Represents a single item stack within a trade, defined by its type and amount.
     * The item can be a vanilla material name or a custom "itemx:" prefixed ID.
     */
    public static final class TradeItem {
        private final String item;
        private final int amount;

        /**
         * Constructs a new TradeItem.
         *
         * @param item   The item identifier (e.g., "DIAMOND" or "itemx:super_sword").
         * @param amount The quantity of the item, must be greater than 0.
         */
        public TradeItem(String item, int amount) {
            this.item = Objects.requireNonNull(item, "Item identifier cannot be null");
            if (amount <= 0) {
                throw new IllegalArgumentException("Item amount must be positive.");
            }
            this.amount = amount;
        }

        /**
         * Gets the item identifier string.
         *
         * @return The item identifier.
         */
        public String getItem() {
            return item;
        }

        /**
         * Gets the amount for this item stack.
         *
         * @return The item amount.
         */
        public int getAmount() {
            return amount;
        }

        /**
         * Checks if this item is a custom ItemX item.
         *
         * @return true if the item identifier starts with "itemx:", false otherwise.
         */
        public boolean isCustomItem() {
            return item.startsWith("itemx:");
        }

        /**
         * Gets the custom item ID by stripping the "itemx:" prefix.
         *
         * @return The custom item ID, or null if this is not a custom item.
         */
        public String getCustomItemId() {
            return isCustomItem() ? item.substring(6) : null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TradeItem tradeItem = (TradeItem) o;
            return amount == tradeItem.amount &&
                    item.equals(tradeItem.item);
        }

        @Override
        public int hashCode() {
            return Objects.hash(item, amount);
        }

        @Override
        public String toString() {
            return "TradeItem{" +
                    "item='" + item + '\'' +
                    ", amount=" + amount +
                    '}';
        }
    }
}