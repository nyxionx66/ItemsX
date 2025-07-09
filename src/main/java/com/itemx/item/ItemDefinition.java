package com.itemx.item;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemDefinition {
    
    private final String id;
    private final Material material;
    private final String name;
    private final List<String> lore;
    private final boolean unbreakable;
    private final boolean useVanillaLore;
    private final Map<Enchantment, Integer> enchantments;
    private final boolean disableUse;
    private final String nbtId;
    private final ArmorTrimData armorTrim;
    private final String category;
    
    public ItemDefinition(String id, Material material, String name, List<String> lore, 
                         boolean unbreakable, boolean useVanillaLore, 
                         Map<Enchantment, Integer> enchantments, boolean disableUse, 
                         String nbtId, ArmorTrimData armorTrim, String category) {
        this.id = id;
        this.material = material;
        this.name = name;
        this.lore = lore;
        this.unbreakable = unbreakable;
        this.useVanillaLore = useVanillaLore;
        this.enchantments = enchantments != null ? enchantments : new HashMap<>();
        this.disableUse = disableUse;
        this.nbtId = nbtId;
        this.armorTrim = armorTrim;
        this.category = category;
    }
    
    public String getId() {
        return id;
    }
    
    public Material getMaterial() {
        return material;
    }
    
    public String getName() {
        return name;
    }
    
    public List<String> getLore() {
        return lore;
    }
    
    public boolean isUnbreakable() {
        return unbreakable;
    }
    
    public boolean isUseVanillaLore() {
        return useVanillaLore;
    }
    
    public Map<Enchantment, Integer> getEnchantments() {
        return enchantments;
    }
    
    public boolean isDisableUse() {
        return disableUse;
    }
    
    public String getNbtId() {
        return nbtId;
    }
    
    public ArmorTrimData getArmorTrim() {
        return armorTrim;
    }
    
    public String getCategory() {
        return category;
    }
    
    public String getFullNbtId() {
        if (nbtId != null && !nbtId.isEmpty()) {
            return nbtId;
        }
        return "itemx:" + (category != null ? category + "/" : "") + id;
    }
    
    public static class ArmorTrimData {
        private final TrimPattern pattern;
        private final TrimMaterial material;
        
        public ArmorTrimData(TrimPattern pattern, TrimMaterial material) {
            this.pattern = pattern;
            this.material = material;
        }
        
        public TrimPattern getPattern() {
            return pattern;
        }
        
        public TrimMaterial getMaterial() {
            return material;
        }
        
        public ArmorTrim createArmorTrim() {
            return new ArmorTrim(material, pattern);
        }
    }
}