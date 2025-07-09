package com.itemx;

import com.itemx.command.ItemXCommand;
import com.itemx.item.ItemManager;
import com.itemx.util.ColorUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class ItemX extends JavaPlugin {
    
    private static ItemX instance;
    private ItemManager itemManager;
    private ColorUtil colorUtil;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize utilities
        colorUtil = new ColorUtil(this);
        
        // Save default config
        saveDefaultConfig();
        
        // Initialize item manager
        itemManager = new ItemManager(this);
        
        // Register commands
        getCommand("itemx").setExecutor(new ItemXCommand(this));
        
        // Load items
        itemManager.loadItems();
        
        getLogger().info("ItemX has been enabled!");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("ItemX has been disabled!");
    }
    
    public static ItemX getInstance() {
        return instance;
    }
    
    public ItemManager getItemManager() {
        return itemManager;
    }
    
    public ColorUtil getColorUtil() {
        return colorUtil;
    }
    
    public TradeManager getTradeManager() {
        return tradeManager;
    }
    
    public TradeEditor getTradeEditor() {
        return tradeEditor;
    }
    
    public TradeReorderGUI getTradeReorderGUI() {
        return tradeReorderGUI;
    }
    
    public void reload() {
        reloadConfig();
        itemManager.loadItems();
        tradeManager.loadTrades();
    }
    
    public void debug(String message) {
        if (getConfig().getBoolean("debug", false)) {
            getLogger().log(Level.INFO, "[DEBUG] " + message);
        }
    }
    
    public Component getPrefix() {
        return colorUtil.parseColor(getConfig().getString("prefix", "<gray>[<aqua>ItemX</aqua>]</gray> "));
    }
    
    public Component getMessage(String key) {
        return colorUtil.parseColor(getConfig().getString("messages." + key, "<red>Missing message: " + key));
    }
    
    public Component getMessage(String key, String... replacements) {
        String message = getConfig().getString("messages." + key, "<red>Missing message: " + key);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }
        return colorUtil.parseColor(message);
    }
}