package com.itemx.trade;

import com.itemx.ItemX;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles the serialization and deserialization of TradeGUI objects to and from YAML files.
 * This class is responsible for all file I/O related to trades.
 */
public class TradeParser {

    private final ItemX plugin;
    private final File tradesDir;

    // Constants for YAML keys to prevent typos and ease maintenance.
    private static final String KEY_GUI_NAME = "gui-name";
    private static final String KEY_GUI_TITLE = "gui-title";
    private static final String KEY_TRADES = "trades";
    private static final String KEY_TRADE_ID = "trade-id";
    private static final String KEY_INPUT_1 = "input1";
    private static final String KEY_INPUT_2 = "input2";
    private static final String KEY_OUTPUT = "output";
    private static final String KEY_ITEM = "item";
    private static final String KEY_AMOUNT = "amount";


    public TradeParser(ItemX plugin) {
        this.plugin = plugin;
        this.tradesDir = new File(plugin.getDataFolder(), "trades");
    }

    /**
     * Loads all trade GUI configurations from the '/trades' directory.
     *
     * @return A map of GUI names to their corresponding TradeGUI objects.
     */
    public Map<String, TradeGUI> loadAllTradeGUIs() {
        Map<String, TradeGUI> tradeGUIs = new HashMap<>();

        if (!tradesDir.exists()) {
            if (tradesDir.mkdirs()) {
                createExampleTrades();
            } else {
                plugin.getLogger().severe("Could not create 'trades' directory!");
                return tradeGUIs;
            }
        }

        File[] files = tradesDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return tradeGUIs;

        for (File file : files) {
            TradeGUI tradeGUI = loadTradeGUIFromFile(file);
            if (tradeGUI != null) {
                tradeGUIs.put(tradeGUI.getName(), tradeGUI);
                plugin.debug("Loaded trade GUI: " + tradeGUI.getName() + " with " + tradeGUI.getTradeCount() + " trades.");
            }
        }
        return tradeGUIs;
    }

    private TradeGUI loadTradeGUIFromFile(File file) {
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

            String guiName = config.getString(KEY_GUI_NAME, file.getName().replace(".yml", ""));
            String guiTitle = config.getString(KEY_GUI_TITLE, guiName);

            TradeGUI tradeGUI = new TradeGUI(guiName, guiTitle, plugin);

            // Correctly load a list of maps, which is the standard YAML list format.
            if (config.isList(KEY_TRADES)) {
                for (Map<?, ?> tradeMap : config.getMapList(KEY_TRADES)) {
                    TradeDefinition trade = parseTradeFromMap(tradeMap, guiName);
                    if (trade != null) {
                        tradeGUI.addTrade(trade);
                    }
                }
            }
            return tradeGUI;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load trade GUI from " + file.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private TradeDefinition parseTradeFromMap(Map<?, ?> tradeMap, String guiName) {
        // Use .get() and check for null, as ConfigurationSection methods are not available on a raw map.
        String tradeId = (String) tradeMap.get(KEY_TRADE_ID);
        if (tradeId == null) {
            plugin.getLogger().warning("Skipping a trade in '" + guiName + ".yml' because it's missing a 'trade-id'.");
            return null;
        }

        try {
            TradeDefinition.TradeItem input1 = parseItemFromMap((Map<?, ?>) tradeMap.get(KEY_INPUT_1));
            if (input1 == null) {
                plugin.getLogger().warning("Trade '" + tradeId + "' in '" + guiName + ".yml' is missing or has an invalid 'input1'.");
                return null;
            }

            TradeDefinition.TradeItem output = parseItemFromMap((Map<?, ?>) tradeMap.get(KEY_OUTPUT));
            if (output == null) {
                plugin.getLogger().warning("Trade '" + tradeId + "' in '" + guiName + ".yml' is missing or has an invalid 'output'.");
                return null;
            }

            // Optional second input
            TradeDefinition.TradeItem input2 = null;
            if (tradeMap.containsKey(KEY_INPUT_2)) {
                input2 = parseItemFromMap((Map<?, ?>) tradeMap.get(KEY_INPUT_2));
            }

            return new TradeDefinition(tradeId, input1, input2, output);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to parse trade '" + tradeId + "' in '" + guiName + ".yml': " + e.getMessage());
            return null;
        }
    }

    private TradeDefinition.TradeItem parseItemFromMap(Map<?, ?> itemMap) {
        if (itemMap == null) return null;

        String item = (String) itemMap.get(KEY_ITEM);
        if (item == null || item.isEmpty()) {
            plugin.getLogger().warning("Trade item is missing its 'item' field.");
            return null;
        }

        // Handle amount, which could be an Integer or String from the YAML parser
        int amount = 1;
        Object amountObj = itemMap.get(KEY_AMOUNT);
        if (amountObj instanceof Number) {
            amount = ((Number) amountObj).intValue();
        }

        return new TradeDefinition.TradeItem(item, amount);
    }

    /**
     * Saves a TradeGUI object to its corresponding .yml file in the '/trades' directory.
     *
     * @param tradeGUI The TradeGUI object to save.
     */
    public void saveTradeGUI(TradeGUI tradeGUI) {
        try {
            if (!tradesDir.exists()) {
                tradesDir.mkdirs();
            }

            File file = new File(tradesDir, tradeGUI.getName() + ".yml");
            YamlConfiguration config = new YamlConfiguration();

            config.set(KEY_GUI_NAME, tradeGUI.getName());
            config.set(KEY_GUI_TITLE, tradeGUI.getTitle());

            // Serialize trades into a List of Maps for clean YAML output
            List<Map<String, Object>> serializedTrades = tradeGUI.getTrades().stream()
                    .map(this::serializeTrade)
                    .collect(Collectors.toList());

            config.set(KEY_TRADES, serializedTrades);

            config.save(file);
            plugin.debug("Saved trade GUI: " + tradeGUI.getName());
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save trade GUI " + tradeGUI.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Map<String, Object> serializeTrade(TradeDefinition trade) {
        Map<String, Object> tradeMap = new LinkedHashMap<>(); // Use LinkedHashMap to preserve key order
        tradeMap.put(KEY_TRADE_ID, trade.getId());
        tradeMap.put(KEY_INPUT_1, serializeItem(trade.getInput1()));
        if (trade.hasSecondInput()) {
            tradeMap.put(KEY_INPUT_2, serializeItem(trade.getInput2()));
        }
        tradeMap.put(KEY_OUTPUT, serializeItem(trade.getOutput()));
        return tradeMap;
    }

    private Map<String, Object> serializeItem(TradeDefinition.TradeItem item) {
        Map<String, Object> itemMap = new LinkedHashMap<>();
        itemMap.put(KEY_ITEM, item.getItem());
        itemMap.put(KEY_AMOUNT, item.getAmount());
        return itemMap;
    }

    /**
     * Deletes the .yml file for a given GUI name.
     *
     * @param guiName The name of the GUI file to delete (without .yml).
     */
    public void deleteTradeGUI(String guiName) {
        File file = new File(tradesDir, guiName + ".yml");
        if (file.exists()) {
            if (!file.delete()) {
                plugin.getLogger().warning("Failed to delete trade GUI file: " + file.getName());
            }
        }
    }

    private void createExampleTrades() {
        // --- Magic Shop Example ---
        TradeGUI magicShop = new TradeGUI("magic_shop", "<gradient:#9146FF:#00D4FF>Magic Items Exchange</gradient>", plugin);
        addExampleTrade(magicShop, "flame_sword", "diamond_sword", 1, "emerald", 10, "itemx:sword_flame", 1);
        addExampleTrade(magicShop, "ice_sword", "diamond_sword", 1, "blue_ice", 5, "itemx:sword_ice", 1);
        addExampleTrade(magicShop, "miner_pickaxe", "diamond_pickaxe", 1, "emerald", 15, "itemx:pickaxe_miner", 1);
        saveTradeGUI(magicShop);

        // --- Tools Shop Example ---
        TradeGUI toolsShop = new TradeGUI("tools_shop", "<gradient:#8B4513:#DAA520>Tools Exchange</gradient>", plugin);
        addExampleTrade(toolsShop, "excavator_shovel", "diamond_shovel", 1, "gold_ingot", 8, "itemx:shovel_excavator", 1);
        addExampleTrade(toolsShop, "rainbow_shovel", "netherite_shovel", 1, "diamond", 5, "itemx:shovel_rainbow", 1);
        saveTradeGUI(toolsShop);

        plugin.getLogger().info("Created example trading GUIs: magic_shop.yml and tools_shop.yml");
    }

    // Helper to reduce repetition in example creation
    private void addExampleTrade(TradeGUI gui, String tradeId, String in1Item, int in1Amt, String in2Item, int in2Amt, String outItem, int outAmt) {
        TradeDefinition.TradeItem input1 = new TradeDefinition.TradeItem(in1Item, in1Amt);
        TradeDefinition.TradeItem input2 = (in2Item != null) ? new TradeDefinition.TradeItem(in2Item, in2Amt) : null;
        TradeDefinition.TradeItem output = new TradeDefinition.TradeItem(outItem, outAmt);
        gui.addTrade(new TradeDefinition(tradeId, input1, input2, output));
    }
}