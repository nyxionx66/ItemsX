package com.itemx.item;

import com.itemx.ItemX;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.io.File;
import java.util.*;

public class ItemParser {

    private final ItemX plugin;

    public ItemParser(ItemX plugin) {
        this.plugin = plugin;
    }

    public Map<String, ItemDefinition> parseFile(File file, String category) {
        Map<String, ItemDefinition> definitions = new HashMap<>();

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

            for (String key : config.getKeys(false)) {
                if (config.isConfigurationSection(key)) {
                    ItemDefinition definition = parseItemDefinition(key, config.getConfigurationSection(key), category);
                    if (definition != null) {
                        definitions.put(key, definition);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to parse item file " + file.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }

        return definitions;
    }

    private ItemDefinition parseItemDefinition(String id, org.bukkit.configuration.ConfigurationSection section, String category) {
        try {
            // Parse material
            String materialName = section.getString("material");
            if (materialName == null) {
                plugin.getLogger().warning("Item " + id + " is missing material");
                return null;
            }

            Material material;
            try {
                material = Material.valueOf(materialName.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material '" + materialName + "' for item " + id);
                return null;
            }

            // Parse basic properties
            String name = section.getString("name", "");
            List<String> lore = section.getStringList("lore");
            boolean unbreakable = section.getBoolean("unbreakable", false);
            boolean useVanillaLore = section.getBoolean("use-vanilla-lore", false);
            boolean disableUse = section.getBoolean("disable-use", false);
            String nbtId = section.getString("nbt-id");

            // Parse enchantments
            Map<Enchantment, Integer> enchantments = new HashMap<>();
            if (section.isConfigurationSection("enchants")) {
                org.bukkit.configuration.ConfigurationSection enchantSection = section.getConfigurationSection("enchants");
                for (String enchantName : enchantSection.getKeys(false)) {
                    try {
                        Enchantment enchantment = Registry.ENCHANTMENT.get(org.bukkit.NamespacedKey.minecraft(enchantName.toLowerCase()));
                        if (enchantment != null) {
                            int level = enchantSection.getInt(enchantName, 1);
                            enchantments.put(enchantment, level);
                        } else {
                            plugin.getLogger().warning("Unknown enchantment '" + enchantName + "' for item " + id);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to parse enchantment '" + enchantName + "' for item " + id + ": " + e.getMessage());
                    }
                }
            }

            // Parse armor trim
            ItemDefinition.ArmorTrimData armorTrim = null;
            if (section.isConfigurationSection("armor-trim")) {
                org.bukkit.configuration.ConfigurationSection trimSection = section.getConfigurationSection("armor-trim");
                String patternName = trimSection.getString("pattern");
                String trimMaterialName = trimSection.getString("material");

                plugin.debug("Parsing armor trim for " + id + ": pattern=" + patternName + ", material=" + trimMaterialName);

                if (patternName != null && trimMaterialName != null) {
                    try {
                        // Create NamespacedKey for pattern
                        org.bukkit.NamespacedKey patternKey = org.bukkit.NamespacedKey.minecraft(patternName.toLowerCase());
                        TrimPattern pattern = Registry.TRIM_PATTERN.get(patternKey);

                        // Create NamespacedKey for material
                        org.bukkit.NamespacedKey materialKey = org.bukkit.NamespacedKey.minecraft(trimMaterialName.toLowerCase());
                        TrimMaterial trimMaterial = Registry.TRIM_MATERIAL.get(materialKey);

                        if (pattern != null && trimMaterial != null) {
                            armorTrim = new ItemDefinition.ArmorTrimData(pattern, trimMaterial);
                            plugin.debug("Successfully parsed armor trim: " + pattern.key() + " + " + trimMaterial.key());
                        } else {
                            plugin.getLogger().warning("Invalid armor trim for item " + id +
                                    " - pattern: " + (pattern != null ? "found" : "not found") +
                                    ", material: " + (trimMaterial != null ? "found" : "not found"));

                            // List available options
                            plugin.getLogger().info("Available trim patterns:");
                            Registry.TRIM_PATTERN.forEach(p -> plugin.getLogger().info("  - " + p.key().value()));
                            plugin.getLogger().info("Available trim materials:");
                            Registry.TRIM_MATERIAL.forEach(m -> plugin.getLogger().info("  - " + m.key().value()));
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to parse armor trim for item " + id + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }

            return new ItemDefinition(id, material, name, lore, unbreakable, useVanillaLore,
                    enchantments, disableUse, nbtId, armorTrim, category);

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to parse item definition " + id + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}