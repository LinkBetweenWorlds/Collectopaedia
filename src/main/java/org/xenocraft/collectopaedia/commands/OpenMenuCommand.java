package org.xenocraft.collectopaedia.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.xenocraft.collectopaedia.Collectopaedia;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class OpenMenuCommand implements TabExecutor {

    private static final Map<String, String> groupDisplayMap = Map.of("veg", "Veg", "fruit", "Fruit", "flower", "Flower", "animal", "Animal", "bug", "Bug", "nature", "Nature", "parts", "Parts", "strange", "Strange");

    // Static item stacks for reuse, avoiding repeated creation
    private static final ItemStack INFILL = createStaticItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " ");
    private static final ItemStack BACK_BUTTON = createStaticItemStack(Material.BARRIER, ChatColor.RED + "Back");
    private static final ItemStack LOCKED_AREA = createStaticItemStack(Material.RED_STAINED_GLASS_PANE, ChatColor.DARK_RED + "Locked Area");
    private static final ItemStack NEXT_PAGE = createStaticItemStack(Material.PAPER, ChatColor.GREEN + "Next");

    public static int page = 1;
    private final Collectopaedia collectopaedia;
    private final ConcurrentHashMap<UUID, FileConfiguration> playerDataCache = new ConcurrentHashMap<>();

    public OpenMenuCommand(Collectopaedia collectopaedia) {
        this.collectopaedia = collectopaedia;
    }

    // Helper method to create a static ItemStack with a specific material and display name
    private static ItemStack createStaticItemStack(Material material, String displayName) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        Objects.requireNonNull(meta).setDisplayName(displayName);
        item.setItemMeta(meta);
        return item;
    }

    // Handles player clicking in the menu
    public void menuClick(Player p, ItemStack item) {
        Bukkit.getScheduler().runTask(collectopaedia, () -> {
            String itemName = item.getItemMeta().getDisplayName().trim();
            Material itemType = item.getType();
            Inventory inv = p.getOpenInventory().getTopInventory();
            Set<String> areaList = new HashSet<>(collectopaedia.areasData.getStringList("areas"));

            // Handle clicking on different items in the menu
            if (itemName.contains("Back")) {
                p.closeInventory();
            } else if (itemName.contains("Next")) {
                page = (page == 2) ? 1 : 2;
                updatePlayerInv(p, inv);
            } else if (!areaList.contains(itemName)) {
                if (itemType == Material.EMERALD || itemType == Material.BLUE_STAINED_GLASS_PANE) {
                    //TODO Update so that player can submit item for inv and menu.
                    submitItem(p, item);
                }
            } else {
                areaList.forEach(s -> {
                    String[] areaParts = s.split(",");
                    String areaId = areaParts[0].trim();
                    String areaName = areaParts[1].trim();

                    if (itemName.contains(areaName)) {
                        collectopaedia.updatePlayerArea(p, areaId);
                        updatePlayerInv(p, inv);
                    }
                });
            }
        });
    }

    // Handles submitting an item from the player's inventory
    //TODO rewrite both submitItem and checkCollectopaedia
    // Make it so that the player can select the item in their inv or the tile in the menu.
    // Make sure that the player can't enter an item if they have already submitted it.
    // Close inv only if the player completed a row/page, otherwise keep the menu open.
    // Make sure one one item is taken.
    // Only give reward once.
    //TODO submitItem logic
    // Check if player has already submitted item, if so break.
    // Check if item is a collectopaedia item that can be submitted.
    // If not take item, and update player files.
    // Give player the reward.
    // If need to give reward close inv to display fancy message.
    // If not update inv to allow player to submit multiple item with having to reopen the menu.

    private void submitItem(Player p, ItemStack item) {
        Bukkit.getScheduler().runTaskAsynchronously(collectopaedia, () -> {
            try {
                FileConfiguration playerFile = getPlayerData(p);
                String itemName = formatItemName(item);
                String selectedArea = playerFile.getString(p.getUniqueId() + ".selectedArea");
                Map<String, List<String>> areaItems = collectopaedia.areaDataCache.get(selectedArea);
                List<String> playerCollectedItems = playerFile.getStringList("depositedItems." + selectedArea);

                // Check if the player has not already submitted the item
                if (playerCollectedItems.contains(itemName)) {
                    return; // Player has already submitted the item, so do nothing
                }

                for (Map.Entry<String, List<String>> entry : areaItems.entrySet()) {
                    String group = entry.getKey();
                    List<String> groupItems = entry.getValue();
                    if (groupItems.contains(itemName)) {
                        collectopaedia.updatePlayerItems(p, playerFile, itemName);
                        Bukkit.getScheduler().runTask(collectopaedia, () -> {
                            PlayerInventory inventory = p.getInventory();
                            inventory.removeItem(item);
                        });
                        checkReward(p, selectedArea);
                        updatePlayerInv(p, p.getInventory());
                        return; // Exit once the item is submitted
                    }
                }
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.SEVERE, "Error processing async task", e);
            }
        });
    }

    // Utility method to format item names consistently
    private String formatItemName(ItemStack item) {
        return item.getItemMeta().getDisplayName().trim().replace(ChatColor.AQUA + "", "");
    }

    //TODO Give player items based off area and group.
    // Logic
    // Get a list of fill with the rewards that player has already claimed for that area.
    // Check if the player has completed any groups that they have not claimed the reward for.
    // If player has completed the group give them that reward and update files.
    private void checkReward(Player p, String selectedArea) {
        Bukkit.getScheduler().runTaskAsynchronously(collectopaedia, () -> {
            try {
                FileConfiguration playerFile = getPlayerData(p);
                synchronized (playerFile) {
                    List<String> playerDepositedItems = playerFile.getStringList("depositedItems." + selectedArea);
                    List<String> playerClaimedRewards = playerFile.getStringList("rewards." + selectedArea);
                    Map<String, List<String>> areaItems = collectopaedia.areaDataCache.get(selectedArea);

                    for (Map.Entry<String, List<String>> entry : areaItems.entrySet()) {
                        String group = entry.getKey();
                        List<String> groupItems = entry.getValue();
                        if (!playerClaimedRewards.contains(group) && playerDepositedItems.containsAll(groupItems)) {
                            // TODO: Give player the reward
                            collectopaedia.updatePlayerReward(p, selectedArea, group);
                            Bukkit.getLogger().info("Reward given to player: " + p.getName() + " for group: " + group);
                        }
                    }
                }
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.SEVERE, "Error processing async task", e);
            }
        });
    }

    // Updates the player's inventory view
    private void updatePlayerInv(Player p, Inventory gui) {
        // Fetch data asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(collectopaedia, () -> {
            FileConfiguration playerFile = getPlayerData(p);
            String selectedArea;
            Map<String, List<String>> cachedAreaData;
            Set<String> playerCollectedItems;
            List<String> playerAreas;

            // Minimize the synchronized block to improve performance
            synchronized (playerFile) {
                selectedArea = playerFile.getString(p.getUniqueId() + ".selectedArea");
                playerCollectedItems = new HashSet<>(playerFile.getStringList("depositedItems." + selectedArea));
                playerAreas = playerFile.getStringList("unlockedArea");
            }

            cachedAreaData = collectopaedia.areaDataCache.get(selectedArea);

            // Player data
            PlayerInventory playerInv = p.getInventory();
            Map<Integer, String> playerItemNamesMap = getPlayerItemNames(playerInv);
            List<String> playerItemNames = new ArrayList<>(playerItemNamesMap.values());
            int totalItems = collectopaedia.itemsData.getInt(selectedArea + ".count");
            List<String> areaList = collectopaedia.areasData.getStringList("areas");

            // After async processing, switch back to the main thread for GUI updates
            Bukkit.getScheduler().runTask(collectopaedia, () -> {
                // Update inventory safely on the main thread
                updateAreaItems(gui, areaList, playerAreas, selectedArea);
                updatePercentMeter(gui, totalItems, playerCollectedItems);
                updateCategoryItems(gui, page, cachedAreaData, playerCollectedItems, playerItemNames);

                // Update the player inventory view if necessary
                Inventory currentInv = p.getOpenInventory().getTopInventory();
                if (!gui.equals(currentInv)) {
                    p.updateInventory();
                }
            });
        });
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("This command can only be run by a player.");
            return false;
        }
        Bukkit.getScheduler().runTaskAsynchronously(collectopaedia, () -> {
            int rows = 6;
            int cols = 9;
            int invSize = (rows * cols);

            Bukkit.getScheduler().runTask(collectopaedia, () -> {
                Inventory gui = Bukkit.createInventory(p, invSize, ChatColor.DARK_GREEN + "Collectopaedia");

                // Fill inventory slots efficiently
                for (int slot = 0; slot < invSize; slot++) {
                    // Fill lockedArea for border slots
                    if (slot < 9 || slot % 9 == 0 || slot >= 45) {
                        gui.setItem(slot, LOCKED_AREA);
                    } else {
                        // Fill infill for all other slots
                        gui.setItem(slot, INFILL);
                    }
                }

                gui.setItem(36, NEXT_PAGE);
                gui.setItem(45, BACK_BUTTON);

                p.openInventory(gui);
                updatePlayerInv(p, gui);
            });
        });
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        return List.of();
    }

    //Helper Methods

    // Gets the player data from the cache or loads it if not already present
    private FileConfiguration getPlayerData(Player p) {
        return playerDataCache.computeIfAbsent(p.getUniqueId(), id -> collectopaedia.loadPlayerData(p));
    }

    // Retrieves item names from the player's inventory
    private Map<Integer, String> getPlayerItemNames(PlayerInventory playerInv) {
        Map<Integer, String> playerItemNames = new HashMap<>();
        String aquaColorCode = ChatColor.AQUA.toString();

        for (int i = 0; i < playerInv.getSize(); i++) {
            ItemStack item = playerInv.getItem(i);
            if (item != null) {
                ItemMeta itemMeta = item.getItemMeta();
                if (itemMeta != null) {
                    String itemName = itemMeta.getDisplayName();
                    if (itemName.contains(aquaColorCode)) {
                        playerItemNames.put(i, itemName.replace(aquaColorCode, ""));
                    }
                }
            }
        }
        return playerItemNames;
    }

    // Creates an item stack representing the percent completion meter
    private ItemStack getPercentMeter(int totalItems, Set<String> playerCollectedItems) {
        ItemStack percentMeter = new ItemStack(Material.CLOCK);
        ItemMeta meterMeta = percentMeter.getItemMeta();
        if (totalItems > 0) {
            int percent = playerCollectedItems.size() * 100 / totalItems;
            meterMeta.setDisplayName(ChatColor.RED + "" + percent + " %");
            if (percent > 25 && percent < 50) {
                meterMeta.setDisplayName(ChatColor.GOLD + "" + percent + " %");
            } else if (percent > 50 && percent < 75) {
                meterMeta.setDisplayName(ChatColor.YELLOW + "" + percent + " %");
            } else if (percent > 75) {
                meterMeta.setDisplayName(ChatColor.GREEN + "" + percent + " %");
            }
        } else {
            meterMeta.setDisplayName(ChatColor.RED + "0 %");
        }
        percentMeter.setItemMeta(meterMeta);
        return percentMeter;
    }

    // Updates the percentage meter item in the inventory GUI
    private void updatePercentMeter(Inventory gui, int totalItems, Set<String> playerCollectedItems) {
        ItemStack percentMeter = getPercentMeter(totalItems, playerCollectedItems);
        gui.setItem(9, percentMeter);
    }

    // Updates the category items in the inventory GUI
    // Updates the category items in the inventory GUI
    private void updateCategoryItems(Inventory gui, int page, Map<String, List<String>> cachedAreaData, Set<String> playerCollectedItems, List<String> playerItemNames) {

        // Item settings for different categories
        Map<String, ItemStack> categoryItems = Map.of(
                "veg", createCategoryItem(Material.EMERALD, 4, "Veg"),
                "fruit", createCategoryItem(Material.EMERALD, 5, "Fruit"),
                "flower", createCategoryItem(Material.EMERALD, 6, "Flower"),
                "animal", createCategoryItem(Material.EMERALD, 7, "Animal"),
                "bug", createCategoryItem(Material.EMERALD, 8, "Bug"),
                "nature", createCategoryItem(Material.EMERALD, 9, "Nature"),
                "parts", createCategoryItem(Material.EMERALD, 10, "Parts"),
                "strange", createCategoryItem(Material.EMERALD, 11, "Strange")
        );

        ItemStack neededItem = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta neededMeta = neededItem.getItemMeta();
        Objects.requireNonNull(neededMeta).setDisplayName(ChatColor.RED + "Needed Item");
        neededItem.setItemMeta(neededMeta);

        ItemStack hasItem = new ItemStack(Material.BLUE_STAINED_GLASS_PANE);
        ItemMeta hasMeta = hasItem.getItemMeta();
        Objects.requireNonNull(hasMeta).setDisplayName(ChatColor.GREEN + "Have Item");
        hasItem.setItemMeta(hasMeta);

        // Set items based on a page (page 1 or 2)
        String[] categories = page == 1 ? new String[]{"veg", "fruit", "flower", "animal"} : new String[]{"bug", "nature", "parts", "strange"};
        int[] baseSlots = new int[]{10, 19, 28, 37};

        // Loop over categories and update inventory
        for (int i = 0; i < categories.length; i++) {
            String category = categories[i];
            List<String> itemList = cachedAreaData.get(category);
            int baseSlot = baseSlots[i];

            if (itemList == null || itemList.contains("None")) {
                fillEmptySlots(gui, baseSlot, 5);
                continue;
            }

            ItemStack categoryItem = categoryItems.get(category);
            int slotOffset = 0;

            for (String item : itemList) {
                if (playerCollectedItems.contains(item)) {
                    gui.setItem(baseSlot + slotOffset, categoryItem);
                } else if (playerItemNames.contains(item)) {
                    hasMeta.setDisplayName(ChatColor.GREEN + item);
                    hasItem.setItemMeta(hasMeta);
                    gui.setItem(baseSlot + slotOffset, hasItem);
                } else {
                    neededMeta.setDisplayName(ChatColor.RED + item);
                    neededItem.setItemMeta(neededMeta);
                    gui.setItem(baseSlot + slotOffset, neededItem);
                }
                slotOffset++;
            }
        }
    }

    // Updates the area items in the inventory GUI
    private void updateAreaItems(Inventory gui, List<String> areaList, List<String> playerAreas, String selectedArea) {
        ItemStack areaItem = new ItemStack(Material.PAPER);
        ItemMeta areaMeta = areaItem.getItemMeta();
        int invSlot = 0;

        for (String s : areaList) {
            String[] areaParts = s.split(",");
            if (playerAreas.contains(areaParts[0].trim())) {
                areaMeta.setDisplayName(areaParts[1].trim());
                areaItem.setType(selectedArea.equals(areaParts[0].trim()) ? Material.MAP : Material.PAPER);
                areaItem.setItemMeta(areaMeta);
                gui.setItem(invSlot++, areaItem);
                if (invSlot > 8 && invSlot < 44) {
                    invSlot += 8;
                } else if (invSlot == 45) {
                    invSlot = gui.getSize() - 1;
                } else if (invSlot > 45) {
                    invSlot -= 2;
                }
            }
        }
    }

    // Helper method to create a category item with specific properties
    private ItemStack createCategoryItem(Material material, int customModelData, String displayName) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        Objects.requireNonNull(meta).setCustomModelData(customModelData);
        meta.setDisplayName(ChatColor.WHITE + displayName);
        item.setItemMeta(meta);
        return item;
    }

    // Fills empty slots in the inventory with the infill item
    private void fillEmptySlots(Inventory gui, int baseSlot, int count) {
        for (int i = 0; i < count; i++) {
            gui.setItem(baseSlot + i, INFILL);
        }
    }
}
