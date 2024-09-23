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

public class OpenMenuCommand implements TabExecutor {

    public static int page = 1;
    private final Collectopaedia collectopaedia;
    private final ConcurrentHashMap<UUID, FileConfiguration> playerDataCache = new ConcurrentHashMap<>();

    public OpenMenuCommand(Collectopaedia collectopaedia) {
        this.collectopaedia = collectopaedia;
    }

    private static ItemStack getPercentMeter(int totalItems, Set<String> playerCollectedItems) {
        ItemStack percentMeter = new ItemStack(Material.CLOCK);
        ItemMeta meterMeta = percentMeter.getItemMeta();
        if (totalItems > 0) {
            int percent = playerCollectedItems.size() / totalItems;
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

    public void menuClick(Player p, ItemStack item) {
        String itemName = item.getItemMeta().getDisplayName().trim();
        Material itemType = item.getType();
        Inventory inv = p.getOpenInventory().getTopInventory();
        Set<String> areaList = new HashSet<>(collectopaedia.areasData.getStringList("areas"));
        if (itemName.contains("Back")) {
            p.closeInventory();
        } else if (itemName.contains("Next")) {
            page = (page == 2) ? 1 : 2;
            updatePlayerInv(p, inv);
        } else if (!areaList.contains(itemName) && (itemType == Material.BLUE_STAINED_GLASS_PANE)) {
            submitItem(p, item);
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
    }

    private void submitItem(Player p, ItemStack item) {
        Bukkit.getScheduler().runTaskAsynchronously(collectopaedia, () -> {
            FileConfiguration playerFile = getPlayerData(p);
            PlayerInventory playerInv = p.getInventory();
            String submitItemName = (item.getItemMeta().getDisplayName()).replace(ChatColor.AQUA + "", "");
            Map<Integer, String> playerItems = getPlayerItemNames(playerInv);

            //Go thru player's inv to find item and remove it.
            //Update player's file with submitted item.
            //Update gui
            int[] i = {0};
            for (i[0] = 0; i[0] < playerInv.getSize(); i[0]++) {
                if (playerItems.containsKey(i[0])) {
                    if (playerItems.get(i[0]).equals(submitItemName)) {
                        collectopaedia.updatePlayerItems(p, playerFile, submitItemName);
                        Bukkit.getScheduler().runTask(collectopaedia, () -> {
                            ItemStack itemRemove = playerInv.getItem(i[0]);
                            int[] itemAmount = {itemRemove.getAmount()};
                            if (itemAmount[0] > 1) {
                                itemRemove.setAmount(itemAmount[0] - 1);
                            } else {
                                playerInv.setItem(i[0], null);
                            }
                            updatePlayerInv(p, playerInv);
                        });
                        break;
                    }
                }
            }

        });


    }

    private FileConfiguration getPlayerData(Player p) {
        return playerDataCache.computeIfAbsent(p.getUniqueId(), _ -> collectopaedia.loadPlayerData(p));
    }

    private void updatePlayerInv(Player p, Inventory gui) {
        Bukkit.getScheduler().runTaskAsynchronously(collectopaedia, () -> {
            FileConfiguration playerFile = getPlayerData(p);
            String selectedArea = playerFile.getString(p.getUniqueId() + ".selectedArea");

            List<String> vegList = collectopaedia.itemsData.getStringList(selectedArea + ".veg");
            List<String> fruitList = collectopaedia.itemsData.getStringList(selectedArea + ".fruit");
            List<String> flowerList = collectopaedia.itemsData.getStringList(selectedArea + ".flower");
            List<String> animalList = collectopaedia.itemsData.getStringList(selectedArea + ".animal");
            List<String> bugList = collectopaedia.itemsData.getStringList(selectedArea + ".bug");
            List<String> natureList = collectopaedia.itemsData.getStringList(selectedArea + ".nature");
            List<String> partsList = collectopaedia.itemsData.getStringList(selectedArea + ".parts");
            List<String> strangeList = collectopaedia.itemsData.getStringList(selectedArea + ".strange");
            PlayerInventory playerInv = p.getInventory();

            Map<Integer, String> playerItemNamesMap = getPlayerItemNames(playerInv);
            List<String> playerItemNames = new ArrayList<>(playerItemNamesMap.values());

            List<String> playerAreas = playerFile.getStringList("unlockedArea");
            Set<String> playerCollectedItems = new HashSet<>(playerFile.getStringList("depositedItems." + selectedArea));
            int totalItems = collectopaedia.itemsData.getInt(selectedArea + ".count");
            List<String> areaList = collectopaedia.areasData.getStringList("areas");

            //Area Items
            int invSlot = 0;
            ItemStack areaItem = new ItemStack(Material.PAPER);
            ItemMeta areaMeta = areaItem.getItemMeta();
            for (String s : areaList) {
                String[] areaParts = s.split(",");
                if (playerAreas.contains(areaParts[0].trim())) {
                    areaMeta.setDisplayName(areaParts[1].trim());
                    if (selectedArea.equals(areaParts[0].trim())) {
                        areaItem.setType(Material.MAP);
                    } else {
                        areaItem.setType(Material.PAPER);
                    }
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


            //Percent Meter
            ItemStack percentMeter = getPercentMeter(totalItems, playerCollectedItems);
            gui.setItem(9, percentMeter);

            //Cat Items
            ItemStack vegItem = new ItemStack(Material.EMERALD);
            ItemMeta vegMeta = vegItem.getItemMeta();
            vegMeta.setCustomModelData(4);
            vegMeta.setDisplayName(ChatColor.WHITE + "Veg");
            vegMeta.setHideTooltip(false);
            vegItem.setItemMeta(vegMeta);

            ItemStack fruitItem = new ItemStack(Material.EMERALD);
            ItemMeta fruitMeta = fruitItem.getItemMeta();
            fruitMeta.setCustomModelData(5);
            fruitMeta.setDisplayName(ChatColor.WHITE + "Fruit");
            fruitMeta.setHideTooltip(false);
            fruitItem.setItemMeta(fruitMeta);

            ItemStack flowerItem = new ItemStack(Material.EMERALD);
            ItemMeta flowerMeta = flowerItem.getItemMeta();
            flowerMeta.setCustomModelData(6);
            flowerMeta.setDisplayName(ChatColor.WHITE + "Flower");
            flowerMeta.setHideTooltip(false);
            flowerItem.setItemMeta(flowerMeta);

            ItemStack animalItem = new ItemStack(Material.EMERALD);
            ItemMeta animalMeta = animalItem.getItemMeta();
            animalMeta.setCustomModelData(7);
            animalMeta.setDisplayName(ChatColor.WHITE + "Animal");
            animalMeta.setHideTooltip(false);
            animalItem.setItemMeta(animalMeta);

            ItemStack bugItem = new ItemStack(Material.EMERALD);
            ItemMeta bugMeta = bugItem.getItemMeta();
            bugMeta.setCustomModelData(8);
            bugMeta.setDisplayName(ChatColor.WHITE + "Bug");
            bugMeta.setHideTooltip(false);
            bugItem.setItemMeta(bugMeta);

            ItemStack natureItem = new ItemStack(Material.EMERALD);
            ItemMeta natureMeta = natureItem.getItemMeta();
            natureMeta.setCustomModelData(9);
            natureMeta.setDisplayName(ChatColor.WHITE + "Nature");
            natureMeta.setHideTooltip(false);
            natureItem.setItemMeta(natureMeta);

            ItemStack partsItem = new ItemStack(Material.EMERALD);
            ItemMeta partsMeta = partsItem.getItemMeta();
            partsMeta.setCustomModelData(10);
            partsMeta.setDisplayName(ChatColor.WHITE + "Parts");
            partsMeta.setHideTooltip(false);
            partsItem.setItemMeta(partsMeta);

            ItemStack strangeItem = new ItemStack(Material.EMERALD);
            ItemMeta strangeMeta = strangeItem.getItemMeta();
            strangeMeta.setCustomModelData(11);
            strangeMeta.setDisplayName(ChatColor.WHITE + "Strange");
            strangeMeta.setHideTooltip(false);
            strangeItem.setItemMeta(strangeMeta);

            ItemStack infill = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
            ItemMeta infillMeta = infill.getItemMeta();
            Objects.requireNonNull(infillMeta).setDisplayName(" ");
            infillMeta.setHideTooltip(true);
            infill.setItemMeta(infillMeta);

            if (page == 1) {
                gui.setItem(10, vegItem);
                gui.setItem(19, fruitItem);
                gui.setItem(28, flowerItem);
                gui.setItem(37, animalItem);
            } else if (page == 2) {
                gui.setItem(10, bugItem);
                gui.setItem(19, natureItem);
                gui.setItem(28, partsItem);
                gui.setItem(37, strangeItem);
            }

            //Items
            ItemStack neededItem = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            ItemMeta neededMeta = neededItem.getItemMeta();
            neededMeta.setHideTooltip(true);
            neededItem.setItemMeta(neededMeta);

            ItemStack hasItem = new ItemStack(Material.BLUE_STAINED_GLASS_PANE);
            ItemMeta hasMeta = hasItem.getItemMeta();

            int slotOffset = 0;

            if (page == 1) {
                if (!vegList.contains("None")) {
                    for (String s : vegList) {
                        if (playerCollectedItems.contains(s)) {
                            vegMeta.setDisplayName(ChatColor.AQUA + s);
                            vegItem.setItemMeta(vegMeta);
                            gui.setItem(11 + slotOffset, vegItem);
                            slotOffset++;
                        } else if (playerItemNames.contains(s)) {
                            hasMeta.setDisplayName(ChatColor.GREEN + s);
                            hasItem.setItemMeta(hasMeta);
                            gui.setItem(11 + slotOffset, hasItem);
                            slotOffset++;
                        } else {
                            gui.setItem(11 + slotOffset, neededItem);
                            slotOffset++;
                        }
                    }
                } else {
                    for (int t = 0; t < 5; t++) {
                        gui.setItem(20 + t, infill);
                    }
                }
                slotOffset = 0;
                if (!fruitList.contains("None")) {
                    for (String s : fruitList) {
                        if (playerCollectedItems.contains(s)) {
                            fruitMeta.setDisplayName(ChatColor.AQUA + s);
                            fruitItem.setItemMeta(fruitMeta);
                            gui.setItem(20 + slotOffset, fruitItem);
                            slotOffset++;
                        } else if (playerItemNames.contains(s)) {
                            hasMeta.setDisplayName(ChatColor.GREEN + s);
                            hasItem.setItemMeta(hasMeta);
                            gui.setItem(20 + slotOffset, hasItem);
                            slotOffset++;
                        } else {
                            gui.setItem(20 + slotOffset, neededItem);
                            slotOffset++;
                        }
                    }
                } else {
                    for (int t = 0; t < 5; t++) {
                        gui.setItem(20 + t, infill);
                    }
                }
                slotOffset = 0;
                if (!flowerList.contains("None")) {
                    for (String s : flowerList) {
                        if (playerCollectedItems.contains(s)) {
                            flowerMeta.setDisplayName(ChatColor.AQUA + s);
                            flowerItem.setItemMeta(flowerMeta);
                            gui.setItem(29 + slotOffset, flowerItem);
                            slotOffset++;
                        } else if (playerItemNames.contains(s)) {
                            hasMeta.setDisplayName(ChatColor.GREEN + s);
                            hasItem.setItemMeta(hasMeta);
                            gui.setItem(29 + slotOffset, hasItem);
                            slotOffset++;
                        } else {
                            gui.setItem(29 + slotOffset, neededItem);
                            slotOffset++;
                        }
                    }
                } else {
                    for (int t = 0; t < 5; t++) {
                        gui.setItem(29 + t, infill);
                    }
                }
                slotOffset = 0;
                if (!animalList.contains("None")) {
                    for (String s : animalList) {
                        if (playerCollectedItems.contains(s)) {
                            animalMeta.setDisplayName(ChatColor.AQUA + s);
                            animalItem.setItemMeta(animalMeta);
                            gui.setItem(38 + slotOffset, animalItem);
                            slotOffset++;
                        } else if (playerItemNames.contains(s)) {
                            hasMeta.setDisplayName(ChatColor.GREEN + s);
                            hasItem.setItemMeta(hasMeta);
                            gui.setItem(38 + slotOffset, hasItem);
                            slotOffset++;
                        } else {
                            gui.setItem(38 + slotOffset, neededItem);
                            slotOffset++;
                        }
                    }
                } else {
                    for (int t = 0; t < 5; t++) {
                        gui.setItem(38 + t, infill);
                    }
                }
            } else if (page == 2) {
                if (!bugList.contains("None")) {
                    for (String s : bugList) {
                        if (playerCollectedItems.contains(s)) {
                            bugMeta.setDisplayName(ChatColor.AQUA + s);
                            bugItem.setItemMeta(bugMeta);
                            gui.setItem(11 + slotOffset, bugItem);
                            slotOffset++;
                        } else if (playerItemNames.contains(s)) {
                            hasMeta.setDisplayName(ChatColor.GREEN + s);
                            hasItem.setItemMeta(hasMeta);
                            gui.setItem(11 + slotOffset, hasItem);
                            slotOffset++;
                        } else {
                            gui.setItem(11 + slotOffset, neededItem);
                            slotOffset++;
                        }
                    }
                } else {
                    for (int t = 0; t < 5; t++) {
                        gui.setItem(20 + t, infill);
                    }
                }
                slotOffset = 0;
                if (!natureList.contains("None")) {
                    for (String s : natureList) {
                        if (playerCollectedItems.contains(s)) {
                            natureMeta.setDisplayName(ChatColor.AQUA + s);
                            natureItem.setItemMeta(natureMeta);
                            gui.setItem(20 + slotOffset, natureItem);
                            slotOffset++;
                        } else if (playerItemNames.contains(s)) {
                            hasMeta.setDisplayName(ChatColor.GREEN + s);
                            hasItem.setItemMeta(hasMeta);
                            gui.setItem(20 + slotOffset, hasItem);
                            slotOffset++;
                        } else {
                            gui.setItem(20 + slotOffset, neededItem);
                            slotOffset++;
                        }
                    }
                } else {
                    for (int t = 0; t < 5; t++) {
                        gui.setItem(20 + t, infill);
                    }
                }
                slotOffset = 0;
                if (!partsList.contains("None")) {
                    for (String s : partsList) {
                        if (playerCollectedItems.contains(s)) {
                            partsMeta.setDisplayName(ChatColor.AQUA + s);
                            partsItem.setItemMeta(flowerMeta);
                            gui.setItem(29 + slotOffset, partsItem);
                            slotOffset++;
                        } else if (playerItemNames.contains(s)) {
                            hasMeta.setDisplayName(ChatColor.GREEN + s);
                            hasItem.setItemMeta(hasMeta);
                            gui.setItem(29 + slotOffset, hasItem);
                            slotOffset++;
                        } else {
                            gui.setItem(29 + slotOffset, neededItem);
                            slotOffset++;
                        }
                    }
                } else {
                    for (int t = 0; t < 5; t++) {
                        gui.setItem(29 + t, infill);
                    }
                }
                slotOffset = 0;
                if (!strangeList.contains("None")) {
                    for (String s : strangeList) {
                        if (playerCollectedItems.contains(s)) {
                            strangeMeta.setDisplayName(ChatColor.AQUA + s);
                            strangeItem.setItemMeta(strangeMeta);
                            gui.setItem(38 + slotOffset, strangeItem);
                            slotOffset++;
                        } else if (playerItemNames.contains(s)) {
                            hasMeta.setDisplayName(ChatColor.GREEN + s);
                            hasItem.setItemMeta(hasMeta);
                            gui.setItem(38 + slotOffset, hasItem);
                            slotOffset++;
                        } else {
                            gui.setItem(38 + slotOffset, neededItem);
                            slotOffset++;
                        }
                    }
                } else {
                    for (int t = 0; t < 5; t++) {
                        gui.setItem(38 + t, infill);
                    }
                }
            }

            Bukkit.getScheduler().runTask(collectopaedia, () -> {
                Inventory currentInv = p.getOpenInventory().getTopInventory();
                if (!gui.equals(currentInv)) {
                    p.updateInventory();
                }
            });
        });
    }

    private Map<Integer, String> getPlayerItemNames(PlayerInventory playerInv) {
        Map<Integer, String> playerItemNames = new HashMap<>();
        String aquaColorCode = ChatColor.AQUA + "";
        for (int i = 0; i < playerInv.getSize(); i++) {
            ItemStack item = playerInv.getItem(i);
            if (item != null) {
                ItemMeta itemMeta = item.getItemMeta();
                if (itemMeta != null) {
                    String itemName = itemMeta.getDisplayName();
                    if (itemName.contains(aquaColorCode)) {
                        playerItemNames.put(i, itemName.replace(ChatColor.AQUA + "", ""));
                    }
                }
            }
        }
        return playerItemNames;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        Bukkit.getScheduler().runTaskAsynchronously(collectopaedia, () -> {
            if (sender instanceof Player p) {
                int rows = 6;
                int cols = 9;
                int invSize = (rows * cols);

                Inventory gui = Bukkit.createInventory(p, invSize, ChatColor.DARK_GREEN + "Collectopaedia");

                //Create infill and back items.
                ItemStack infill = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
                ItemMeta infillMeta = infill.getItemMeta();
                Objects.requireNonNull(infillMeta).setDisplayName(" ");
                infillMeta.setHideTooltip(true);
                infill.setItemMeta(infillMeta);

                ItemStack backButton = new ItemStack(Material.BARRIER);
                ItemMeta backMeta = backButton.getItemMeta();
                Objects.requireNonNull(backMeta).setDisplayName(ChatColor.RED + "Back");
                backMeta.setHideTooltip(false);
                backButton.setItemMeta(backMeta);

                ItemStack lockedArea = new ItemStack(Material.RED_STAINED_GLASS_PANE);
                ItemMeta lockedAreaMeta = lockedArea.getItemMeta();
                Objects.requireNonNull(lockedAreaMeta).setDisplayName(ChatColor.DARK_RED + "Locked Area");
                lockedAreaMeta.setHideTooltip(false);
                lockedArea.setItemMeta(lockedAreaMeta);

                ItemStack nextPage = new ItemStack(Material.PAPER);
                ItemMeta nextPageMeta = nextPage.getItemMeta();
                Objects.requireNonNull(nextPageMeta).setDisplayName(ChatColor.GREEN + "Next");
                nextPageMeta.setHideTooltip(false);
                nextPage.setItemMeta(nextPageMeta);

                //Fills in the inv.
                for (int slot = 8; slot < invSize; slot++) {
                    gui.setItem(slot, infill);
                }
                for (int slot = 0; slot < 9; slot++) {
                    gui.setItem(slot, lockedArea);
                }
                for (int slot = 8; slot < invSize; slot += 9) {
                    gui.setItem(slot, lockedArea);
                }
                for (int slot = 46; slot < invSize; slot++) {
                    gui.setItem(slot, lockedArea);
                }

                gui.setItem(36, nextPage);
                gui.setItem(45, backButton);

                updatePlayerInv(p, gui);
            }
        });
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        return List.of();
    }
}
