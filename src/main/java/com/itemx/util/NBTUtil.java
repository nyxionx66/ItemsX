package com.itemx.util;

import com.itemx.ItemX;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class NBTUtil {
    
    private final ItemX plugin;
    private final NamespacedKey itemIdKey;
    
    public NBTUtil(ItemX plugin) {
        this.plugin = plugin;
        String keyName = plugin.getConfig().getString("nbt.key", "itemx:id");
        this.itemIdKey = NamespacedKey.fromString(keyName);
    }
    
    /**
     * Set the ItemX ID on an item
     * @param item The item to modify
     * @param id The ID to set
     */
    public void setItemId(ItemStack item, String id) {
        if (item == null || id == null) return;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(itemIdKey, PersistentDataType.STRING, id);
        item.setItemMeta(meta);
        
        plugin.debug("Set NBT ID '" + id + "' on item " + item.getType());
    }
    
    /**
     * Get the ItemX ID from an item
     * @param item The item to check
     * @return The ID or null if not found
     */
    public String getItemId(ItemStack item) {
        if (item == null) return null;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.get(itemIdKey, PersistentDataType.STRING);
    }
    
    /**
     * Check if an item has an ItemX ID
     * @param item The item to check
     * @return True if the item has an ItemX ID
     */
    public boolean hasItemId(ItemStack item) {
        if (item == null) return false;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(itemIdKey, PersistentDataType.STRING);
    }
    
    /**
     * Remove the ItemX ID from an item
     * @param item The item to modify
     */
    public void removeItemId(ItemStack item) {
        if (item == null) return;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.remove(itemIdKey);
        item.setItemMeta(meta);
        
        plugin.debug("Removed NBT ID from item " + item.getType());
    }
    
    /**
     * Set custom NBT data on an item
     * @param item The item to modify
     * @param key The key to set
     * @param value The value to set
     */
    public void setCustomData(ItemStack item, String key, String value) {
        if (item == null || key == null || value == null) return;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        
        String namespacePrefix = plugin.getConfig().getString("nbt.namespace-prefix", "itemx");
        NamespacedKey namespacedKey = NamespacedKey.fromString(namespacePrefix + ":" + key);
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(namespacedKey, PersistentDataType.STRING, value);
        item.setItemMeta(meta);
        
        plugin.debug("Set custom NBT data '" + key + "' = '" + value + "' on item " + item.getType());
    }
    
    /**
     * Get custom NBT data from an item
     * @param item The item to check
     * @param key The key to get
     * @return The value or null if not found
     */
    public String getCustomData(ItemStack item, String key) {
        if (item == null || key == null) return null;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        
        String namespacePrefix = plugin.getConfig().getString("nbt.namespace-prefix", "itemx");
        NamespacedKey namespacedKey = NamespacedKey.fromString(namespacePrefix + ":" + key);
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.get(namespacedKey, PersistentDataType.STRING);
    }
}