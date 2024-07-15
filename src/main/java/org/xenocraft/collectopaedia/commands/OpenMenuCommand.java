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

import java.util.List;
import java.util.Objects;

public class OpenMenuCommand implements TabExecutor {

    private final Collectopaedia collectopaedia;
    public int page = 0;
    public String selectedArea = "colony9";

    //TODO Check player items, if they match give option to add to Collectopaedia
    //TODO Save file content: Player Name, unlocked area, deposited items in each area.
    //TODO Save the player current Collectopaedia to YAML, under each area.
    //TODO When menu is opened default to Colony 9
    //TODO When player first goes to new area, add to unlocked area list.


    public OpenMenuCommand(Collectopaedia collectopaedia) {
        this.collectopaedia = collectopaedia;
    }

    public static void menuClick(Player p, ItemStack item) {
        p.sendMessage("You clicked " + item.getItemMeta().getDisplayName());
    }


    public void updatePlayerInv(Player p, Inventory gui) {
        PlayerInventory playerInv = p.getInventory();
        ItemStack[] itemList = playerInv.getContents();
        FileConfiguration playerFile = collectopaedia.loadPlayerData(p);
        List<String> playerAreas = playerFile.getStringList("unlockedArea");
        List<String> areaList = collectopaedia.areasData.getStringList("areas");

        ItemStack selectedAreaItem = new ItemStack(Material.MAP);
        ItemStack areaItem = new ItemStack(Material.PAPER);
        ItemMeta areaMeta = selectedAreaItem.getItemMeta();

        int invSlot = 0;

        for (String s : areaList) {
            //TODO Change loop so its is dynamic and update based off the selected area
            String[] areaParts = s.split(",");
            if (playerAreas.contains(areaParts[0].trim())) {
                areaMeta.setDisplayName(areaParts[1].trim());
                areaItem.setItemMeta(areaMeta);
                System.out.println(s);
                System.out.println(invSlot);
                if (invSlot == 1) {
                    areaMeta.setDisplayName(selectedArea);
                    selectedAreaItem.setItemMeta(areaMeta);
                    gui.setItem(invSlot, selectedAreaItem);
                } else {
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


        for (ItemStack itemStack : itemList) {
            if (itemStack != null) {
            }
        }
        p.openInventory(gui);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {

        if (sender instanceof Player p) {
            int rows = 6;
            int cols = 9;
            int invSize = (rows * cols);

            int currentInvSquare = 0;
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

            //TODO Grab player's unlocked area and replace the locked areas.
            //TODO Areas

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

            ItemStack bugItem = new ItemStack(Material.EMERALD);
            ItemMeta bugMeta = bugItem.getItemMeta();
            bugMeta.setCustomModelData(7);
            bugMeta.setDisplayName(ChatColor.WHITE + "Bug");
            bugMeta.setHideTooltip(false);
            bugItem.setItemMeta(bugMeta);

            gui.setItem(10, vegItem);
            gui.setItem(19, fruitItem);
            gui.setItem(28, flowerItem);
            gui.setItem(37, bugItem);

            //TODO Display first four group on one page(veg, fruit, flower, animal) and the next four on second page(bug, nature, parts, strange)
            gui.setItem(36, nextPage);
            gui.setItem(45, backButton);

            //p.openInventory(gui);

            updatePlayerInv(p, gui);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        return List.of();
    }
}
