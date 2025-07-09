package com.itemx.item;

import com.itemx.ItemX;
import com.itemx.util.NBTUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.trim.ArmorTrim;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ItemManager implements Listener {

    private final ItemX plugin;
    private final ItemParser parser;
    private final NBTUtil nbtUtil;
    private final Map<String, ItemDefinition> itemDefinitions;

    public ItemManager(ItemX plugin) {
        this.plugin = plugin;
        this.parser = new ItemParser(plugin);
        this.nbtUtil = new NBTUtil(plugin);
        this.itemDefinitions = new ConcurrentHashMap<>();

        // Register event listener
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void loadItems() {
        itemDefinitions.clear();

        File itemsDir = new File(plugin.getDataFolder(), "items");
        if (!itemsDir.exists()) {
            itemsDir.mkdirs();
            createExampleItems();
        }

        loadItemsFromDirectory(itemsDir, "");

        plugin.getLogger().info("Loaded " + itemDefinitions.size() + " custom items");
    }

    private void loadItemsFromDirectory(File directory, String category) {
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                String newCategory = category.isEmpty() ? file.getName() : category + "/" + file.getName();
                loadItemsFromDirectory(file, newCategory);
            } else if (file.getName().endsWith(".yml")) {
                Map<String, ItemDefinition> definitions = parser.parseFile(file, category);
                itemDefinitions.putAll(definitions);
                plugin.debug("Loaded " + definitions.size() + " items from " + file.getName());
            }
        }
    }

    private void createExampleItems() {
        // Create tools category with multiple files
        createToolsExamples();

        // Create weapons category with multiple files
        createWeaponsExamples();

        // Create misc category
        createMiscExamples();
    }

    private void createToolsExamples() {
        // Create pickaxes.yml in tools category
        File pickaxesFile = new File(plugin.getDataFolder(), "items/tools/pickaxes.yml");
        pickaxesFile.getParentFile().mkdirs();

        String pickaxesContent = """
            pickaxe_miner:
              material: DIAMOND_PICKAXE
              name: "<gradient:#00ffff:#0000ff>Miner's Pickaxe</gradient>"
              lore:
                - "&7Efficient and powerful."
                - "<gradient:#ffaa00:#ff0000>Handle with care!</gradient>"
              unbreakable: true
              use-vanilla-lore: false
              enchants:
                EFFICIENCY: 5
                UNBREAKING: 3
              disable-use: false
              nbt-id: miners_pick
            
            pickaxe_starter:
              material: STONE_PICKAXE
              name: "&7Starter Pickaxe"
              lore:
                - "&8Basic mining tool"
                - "&#FF0048&lP&#FD0249&lU&#FB044A&lN&#F9064C&lC&#F7084D&lH&#F50A4E&lI &#F10E50&lS&#EF1051&lI&#ED1353&lT&#EB1554&lH&#E91755&lE &#E51B57&lP&#E31D58&lU&#E11F5A&lN&#DF215B&lC&#DD235C&lH&#DC255D&lI"
              enchants:
                EFFICIENCY: 1
            
            pickaxe_legacy:
              material: GOLDEN_PICKAXE
              name: "&6&lLegacy Pickaxe"
              lore:
                - "&eSupports old color codes"
                - "&a&lGreen and Bold"
                - "&c&nRed and Underlined"
              use-vanilla-lore: true
              enchants:
                FORTUNE: 3
            """;

        // Create shovels.yml in tools category
        File shovelsFile = new File(plugin.getDataFolder(), "items/tools/shovels.yml");

        String shovelsContent = """
            shovel_excavator:
              material: DIAMOND_SHOVEL
              name: "<gradient:#8B4513:#DAA520>Excavator Shovel</gradient>"
              lore:
                - "&eDigg faster than ever!"
                - "&#DAA520Mixed color formats work!"
              enchants:
                EFFICIENCY: 4
                UNBREAKING: 2
            
            shovel_rainbow:
              material: NETHERITE_SHOVEL
              name: "&4R&6a&ei&an&bb&9o&dw &5S&ch&4o&6v&ee&al"
              lore:
                - "&7This shovel has rainbow colors"
                - "&#FF0000R&#FF8000a&#FFFF00i&#80FF00n&#00FF00b&#00FF80o&#00FFFFw &#0080FFt&#0000FFe&#8000FFx&#FF00FFt"
              enchants:
                EFFICIENCY: 5
                SILK_TOUCH: 1
            """;

        try {
            java.nio.file.Files.write(pickaxesFile.toPath(), pickaxesContent.getBytes());
            java.nio.file.Files.write(shovelsFile.toPath(), shovelsContent.getBytes());
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create tools examples: " + e.getMessage());
        }
    }

    private void createWeaponsExamples() {
        // Create swords.yml in weapons category
        File swordsFile = new File(plugin.getDataFolder(), "items/weapons/swords.yml");
        swordsFile.getParentFile().mkdirs();

        String swordsContent = """
            sword_flame:
              material: DIAMOND_SWORD
              name: "<gradient:#FF6B35:#F7931E>Flame Sword</gradient>"
              lore:
                - "&cBurns enemies on contact"
                - "&7Legendary weapon"
                - "<gradient:#FF0000:#FFFF00>Fire damage boost</gradient>"
              enchants:
                SHARPNESS: 5
                FIRE_ASPECT: 2
                UNBREAKING: 3
            
            sword_ice:
              material: DIAMOND_SWORD
              name: "<gradient:#87CEEB:#4682B4>Ice Sword</gradient>"
              lore:
                - "&bFreezes enemies"
                - "&7Legendary weapon"
                - "<gradient:#00FFFF:#FFFFFF>Frost power</gradient>"
              enchants:
                SHARPNESS: 4
                KNOCKBACK: 2
            
            sword_hex:
              material: NETHERITE_SWORD
              name: "&#FF0000H&#FF4000e&#FF8000x &#FFC000S&#FFFF00w&#C0FF00o&#80FF00r&#40FF00d"
              lore:
                - "&#FF0000This sword uses hex colors"
                - "&6&lLegacy &r&7and &#00FF00hex &rcombined"
                - "<gradient:#FF0000:#00FF00:#0000FF>RGB Gradient</gradient>"
              use-vanilla-lore: true
              enchants:
                SHARPNESS: 5
                LOOTING: 3
            
            rainbow_sword:
              material: NETHERITE_SWORD
              name: "<rainbow>Rainbow Sword</rainbow>"
              lore:
                - "<rainbow>Every color of the rainbow!</rainbow>"
                - "<gradient:#FF0000:#FF7F00:#FFFF00:#00FF00:#0000FF:#4B0082:#9400D3>Full spectrum</gradient>"
              enchants:
                SHARPNESS: 5
                SWEEPING: 3
            """;

        // Create bows.yml in weapons category
        File bowsFile = new File(plugin.getDataFolder(), "items/weapons/bows.yml");

        String bowsContent = """
            bow_hunter:
              material: BOW
              name: "<gradient:#8B4513:#228B22>Hunter's Bow</gradient>"
              lore:
                - "&aPerfect for hunting"
                - "&#228B22Supports all color formats"
                - "<gradient:#00FF00:#FFFF00>Nature's power</gradient>"
              enchants:
                POWER: 3
                PUNCH: 1
                INFINITY: 1
            
            bow_legacy:
              material: BOW
              name: "&d&lMagic Bow"
              lore:
                - "&5&oEnchanted with old magic"
                - "&e&nLegacy color codes work!"
                - "<gradient:#9400D3:#FF1493>Magic energy</gradient>"
              enchants:
                POWER: 5
                FLAME: 1
            
            bow_gradient:
              material: CROSSBOW
              name: "<gradient:#FF69B4:#00CED1:#98FB98>Tri-Color Crossbow</gradient>"
              lore:
                - "<gradient:#FF1493:#00BFFF>Piercing shots</gradient>"
                - "<rainbow>Rainbow ammunition</rainbow>"
              enchants:
                QUICK_CHARGE: 3
                PIERCING: 2
            """;

        try {
            java.nio.file.Files.write(swordsFile.toPath(), swordsContent.getBytes());
            java.nio.file.Files.write(bowsFile.toPath(), bowsContent.getBytes());
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create weapons examples: " + e.getMessage());
        }
    }

    private void createMiscExamples() {
        File miscFile = new File(plugin.getDataFolder(), "items/misc/decorative.yml");
        miscFile.getParentFile().mkdirs();

        String miscContent = """
            builder_sunflower:
              material: SUNFLOWER
              name: "<yellow>Builder's Marker</yellow>"
              lore:
                - "<gray>Cannot be placed</gray>"
                - "<gold>Decorative item only</gold>"
              disable-use: true
            
            magic_chestplate:
              material: DIAMOND_CHESTPLATE
              name: "<gradient:#9146FF:#00D4FF>Enchanted Chestplate</gradient>"
              lore:
                - "<gray>Magical protection</gray>"
                - "<gradient:#FF6B6B:#4ECDC4>Gradient colors work!</gradient>"
              enchants:
                PROTECTION: 4
              armor-trim:
                pattern: rib
                material: emerald
            
            netherite_leggings:
              material: NETHERITE_LEGGINGS
              name: "<gradient:#8B0000:#FFD700>Flame Leggings</gradient>"
              lore:
                - "<red>Fire resistance</red>"
                - "<gradient:#FF4500:#FFD700>Blazing protection</gradient>"
              enchants:
                FIRE_PROTECTION: 4
                UNBREAKING: 3
              armor-trim:
                pattern: silence
                material: redstone
            
            golden_helmet:
              material: GOLDEN_HELMET
              name: "<gradient:#FFD700:#FF69B4:#00CED1>Rainbow Helmet</gradient>"
              lore:
                - "<gradient:#FF0000:#00FF00:#0000FF>RGB Gradient Test</gradient>"
                - "<rainbow>Rainbow text works too!</rainbow>"
              armor-trim:
                pattern: snout
                material: gold
            
            iron_boots:
              material: IRON_BOOTS
              name: "&8&lIron Boots"
              lore:
                - "&7Standard protection"
                - "&6With emerald trim"
              armor-trim:
                pattern: vex
                material: emerald
            
            chainmail_helmet:
              material: CHAINMAIL_HELMET
              name: "&7Chainmail Helmet"
              lore:
                - "&8Light protection"
                - "&bWith diamond trim"
              use-vanilla-lore: true
              armor-trim:
                pattern: wild
                material: diamond
            """;

        try {
            java.nio.file.Files.write(miscFile.toPath(), miscContent.getBytes());
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create misc examples: " + e.getMessage());
        }
    }

    public ItemDefinition getItemDefinition(String id) {
        return itemDefinitions.get(id);
    }

    public Set<String> getItemIds() {
        return itemDefinitions.keySet();
    }

    public Set<String> getCategories() {
        return itemDefinitions.values().stream()
                .map(ItemDefinition::getCategory)
                .filter(Objects::nonNull)
                .filter(cat -> !cat.isEmpty())
                .collect(Collectors.toSet());
    }

    public Set<ItemDefinition> getItemsByCategory(String category) {
        return itemDefinitions.values().stream()
                .filter(def -> Objects.equals(def.getCategory(), category))
                .collect(Collectors.toSet());
    }

    public ItemStack createItem(ItemDefinition definition) {
        ItemStack item = new ItemStack(definition.getMaterial());
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            plugin.getLogger().warning("Failed to get ItemMeta for " + definition.getId());
            return item;
        }

        // Set display name
        if (definition.getName() != null && !definition.getName().isEmpty()) {
            Component nameComponent = plugin.getColorUtil().parseColor(definition.getName());
            meta.displayName(nameComponent);
            plugin.debug("Set display name for " + definition.getId() + ": " + definition.getName());
        }

        // Set unbreakable
        if (definition.isUnbreakable()) {
            meta.setUnbreakable(true);
        }

        // Add enchantments
        for (Map.Entry<Enchantment, Integer> entry : definition.getEnchantments().entrySet()) {
            meta.addEnchant(entry.getKey(), entry.getValue(), true);
        }

        // Apply the meta first to ensure it's properly set
        item.setItemMeta(meta);

        // Add armor trim if applicable - do this after setting the meta
        if (definition.getArmorTrim() != null) {
            plugin.debug("Attempting to apply armor trim to " + definition.getId());

            // Get fresh meta after setting it
            ItemMeta freshMeta = item.getItemMeta();
            if (freshMeta instanceof ArmorMeta) {
                ArmorMeta armorMeta = (ArmorMeta) freshMeta;
                try {
                    ArmorTrim trim = definition.getArmorTrim().createArmorTrim();
                    armorMeta.setTrim(trim);

                    // Apply the armor meta back to the item
                    item.setItemMeta(armorMeta);

                    plugin.debug("Successfully applied armor trim to " + definition.getId() + ": " +
                            definition.getArmorTrim().getPattern().key() + " + " +
                            definition.getArmorTrim().getMaterial().key());

                    // Verify the trim was applied
                    ArmorMeta verifyMeta = (ArmorMeta) item.getItemMeta();
                    if (verifyMeta.hasTrim()) {
                        plugin.debug("Armor trim verified on " + definition.getId());
                    } else {
                        plugin.getLogger().warning("Armor trim not found after applying to " + definition.getId());
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to apply armor trim to " + definition.getId() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                plugin.getLogger().warning("Cannot apply armor trim to " + definition.getId() +
                        " - not an armor item (meta type: " + (freshMeta != null ? freshMeta.getClass().getSimpleName() : "null") + ")");
            }
        }

        // Apply the meta before handling lore to get vanilla lore if needed
        // Note: Don't set meta here if we have armor trim - it's already set above

        // Handle lore based on use-vanilla-lore setting
        if (definition.isUseVanillaLore()) {
            // Use vanilla lore - preserve all vanilla tooltips and add custom lore
            handleVanillaLore(item, definition);
        } else {
            // Use custom lore only - hide vanilla tooltips
            handleCustomLore(item, definition);
        }

        // Add NBT identification
        nbtUtil.setItemId(item, definition.getFullNbtId());

        return item;
    }

    private void handleVanillaLore(ItemStack item, ItemDefinition definition) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // Don't hide any vanilla information - let Minecraft show everything
        // This includes enchantments, armor trims, attributes, etc.

        // Add custom lore if defined, but preserve vanilla lore
        if (definition.getLore() != null && !definition.getLore().isEmpty()) {
            List<Component> existingLore = meta.lore();
            List<Component> customLore = new ArrayList<>();

            // Add custom lore first
            for (String loreLine : definition.getLore()) {
                customLore.add(plugin.getColorUtil().parseColor(loreLine));
            }

            // Add vanilla lore if it exists
            if (existingLore != null && !existingLore.isEmpty()) {
                customLore.addAll(existingLore);
            }

            meta.lore(customLore);
            // Apply the updated meta
            item.setItemMeta(meta);
        }

        // Don't add any ItemFlags - let vanilla display everything
    }

    private void handleCustomLore(ItemStack item, ItemDefinition definition) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // Set custom lore
        if (definition.getLore() != null && !definition.getLore().isEmpty()) {
            List<Component> loreComponents = new ArrayList<>();
            for (String loreLine : definition.getLore()) {
                loreComponents.add(plugin.getColorUtil().parseColor(loreLine));
            }
            meta.lore(loreComponents);
        }

        // Hide vanilla information EXCEPT armor trim
        meta.addItemFlags(
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_UNBREAKABLE,
                ItemFlag.HIDE_DESTROYS,
                ItemFlag.HIDE_PLACED_ON
                // Note: We're NOT adding ItemFlag.HIDE_ARMOR_TRIM so armor trims are visible
        );

        // Apply the updated meta
        item.setItemMeta(meta);
    }

    public boolean isCustomItem(ItemStack item) {
        return nbtUtil.hasItemId(item);
    }

    public String getCustomItemId(ItemStack item) {
        return nbtUtil.getItemId(item);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (!isCustomItem(item)) return;

        String itemId = getCustomItemId(item);
        if (itemId == null) return;

        // Find the definition to check if use is disabled
        ItemDefinition definition = findDefinitionByNbtId(itemId);
        if (definition != null && definition.isDisableUse()) {
            event.setCancelled(true);
            plugin.debug("Blocked placement of " + itemId + " (use disabled)");
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null || !isCustomItem(item)) return;

        String itemId = getCustomItemId(item);
        if (itemId == null) return;

        // Find the definition to check if use is disabled
        ItemDefinition definition = findDefinitionByNbtId(itemId);
        if (definition != null && definition.isDisableUse()) {
            event.setCancelled(true);
            plugin.debug("Blocked interaction with " + itemId + " (use disabled)");
        }
    }

    private ItemDefinition findDefinitionByNbtId(String nbtId) {
        return itemDefinitions.values().stream()
                .filter(def -> def.getFullNbtId().equals(nbtId))
                .findFirst()
                .orElse(null);
    }
}