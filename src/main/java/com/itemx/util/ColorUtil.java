package com.itemx.util;

import com.itemx.ItemX;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtil {
    
    private final MiniMessage miniMessage;
    private final LegacyComponentSerializer legacySerializer;
    private final ItemX plugin;
    
    // Pattern for hex color codes like &#FF0048
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    
    public ColorUtil(ItemX plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        this.legacySerializer = LegacyComponentSerializer.legacyAmpersand();
    }
    
    /**
     * Parse a string with MiniMessage color codes, legacy codes, and hex codes into a Component
     * @param text The text to parse
     * @return Parsed Component
     */
    public Component parseColor(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        
        try {
            // Store original text for debugging
            String originalText = text;
            
            // First, convert hex codes to MiniMessage format
            text = convertHexCodes(text);
            
            // Convert legacy ampersand codes to MiniMessage format
            text = convertLegacyCodes(text);
            
            // Only add <!italic> if the text doesn't contain complex MiniMessage formatting
            boolean hasComplexFormatting = text.contains("gradient") || text.contains("rainbow") || 
                                         text.contains("click") || text.contains("hover") || 
                                         text.contains("transition") || text.contains("color:");
            
            if (!hasComplexFormatting && !text.contains("<!italic>")) {
                text = "<!italic>" + text;
            }
            
            // Parse with MiniMessage
            Component component = miniMessage.deserialize(text);
            
            // Debug output
            plugin.debug("Color parsing: '" + originalText + "' -> '" + text + "'");
            
            return component;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse color text: '" + text + "' - " + e.getMessage());
            
            // If parsing fails, try legacy parsing
            try {
                Component legacy = legacySerializer.deserialize(text);
                // Remove italic formatting from legacy text
                return legacy.decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
            } catch (Exception e2) {
                plugin.getLogger().warning("Legacy parsing also failed: " + e2.getMessage());
                // If all fails, return as plain text without italic
                return Component.text(text).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
            }
        }
    }
    
    /**
     * Convert hex color codes (&#FF0048) to MiniMessage format
     * @param text The text containing hex codes
     * @return Text with hex codes converted to MiniMessage format
     */
    private String convertHexCodes(String text) {
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String hexCode = matcher.group(1);
            matcher.appendReplacement(result, "<color:#" + hexCode + ">");
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * Convert legacy color codes (&6&l) to MiniMessage format
     * @param text The text containing legacy codes
     * @return Text with legacy codes converted to MiniMessage format
     */
    private String convertLegacyCodes(String text) {
        // Handle formatting codes
        text = text.replace("&l", "<bold>").replace("&L", "<bold>");
        text = text.replace("&o", "<italic>").replace("&O", "<italic>");
        text = text.replace("&n", "<underlined>").replace("&N", "<underlined>");
        text = text.replace("&m", "<strikethrough>").replace("&M", "<strikethrough>");
        text = text.replace("&k", "<obfuscated>").replace("&K", "<obfuscated>");
        text = text.replace("&r", "<reset>").replace("&R", "<reset>");
        
        // Handle color codes
        text = text.replace("&0", "<black>").replace("&0", "<black>");
        text = text.replace("&1", "<dark_blue>").replace("&1", "<dark_blue>");
        text = text.replace("&2", "<dark_green>").replace("&2", "<dark_green>");
        text = text.replace("&3", "<dark_aqua>").replace("&3", "<dark_aqua>");
        text = text.replace("&4", "<dark_red>").replace("&4", "<dark_red>");
        text = text.replace("&5", "<dark_purple>").replace("&5", "<dark_purple>");
        text = text.replace("&6", "<gold>").replace("&6", "<gold>");
        text = text.replace("&7", "<gray>").replace("&7", "<gray>");
        text = text.replace("&8", "<dark_gray>").replace("&8", "<dark_gray>");
        text = text.replace("&9", "<blue>").replace("&9", "<blue>");
        text = text.replace("&a", "<green>").replace("&A", "<green>");
        text = text.replace("&b", "<aqua>").replace("&B", "<aqua>");
        text = text.replace("&c", "<red>").replace("&C", "<red>");
        text = text.replace("&d", "<light_purple>").replace("&D", "<light_purple>");
        text = text.replace("&e", "<yellow>").replace("&E", "<yellow>");
        text = text.replace("&f", "<white>").replace("&F", "<white>");
        
        return text;
    }
    
    /**
     * Parse a string with MiniMessage color codes into a legacy string
     * @param text The text to parse
     * @return Legacy colored string
     */
    public String parseLegacy(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        try {
            // First convert hex and legacy codes
            text = convertHexCodes(text);
            text = convertLegacyCodes(text);
            
            Component component = miniMessage.deserialize(text);
            return legacySerializer.serialize(component);
        } catch (Exception e) {
            return text;
        }
    }
    
    /**
     * Strip all color codes from text
     * @param text The text to strip
     * @return Plain text without color codes
     */
    public String stripColor(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        try {
            // Convert all formats first
            text = convertHexCodes(text);
            text = convertLegacyCodes(text);
            
            Component component = miniMessage.deserialize(text);
            return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component);
        } catch (Exception e) {
            // Strip legacy codes manually as fallback
            return text.replaceAll("&[0-9a-fk-or]", "").replaceAll("&#[A-Fa-f0-9]{6}", "");
        }
    }
}