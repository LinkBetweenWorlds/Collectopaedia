package org.xenocraft.collectopaedia;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.xenocraft.collectopaedia.commands.OpenMenuCommand;
import org.xenocraft.collectopaedia.commands.ReloadCommand;
import org.xenocraft.collectopaedia.events.PlayerInvClickEvent;
import org.xenocraft.collectopaedia.listener.PlayerChangeWorldListener;
import org.xenocraft.collectopaedia.listener.PlayerJoinLeaveListener;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class Collectopaedia extends JavaPlugin implements Listener {

    public final Map<String, Map<String, List<String>>> areaDataCache = new ConcurrentHashMap<>();
    public FileConfiguration areasData;
    public FileConfiguration itemsData;
    public FileConfiguration rewardsData;

    @Override
    public void onEnable() {
        // Schedule the plugin initialization on the main thread
        Bukkit.getScheduler().runTask(this, () -> {
            createDataFiles();

            registerListeners();
            registerCommands();

            getLogger().info("Collectopaedia enabled successfully.");
        });
    }

    // Register all event listeners
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new PlayerInvClickEvent(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinLeaveListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerChangeWorldListener(this), this);
    }

    // Register all commands
    private void registerCommands() {
        Objects.requireNonNull(getCommand("open")).setExecutor(new OpenMenuCommand(this));
        Objects.requireNonNull(getCommand("reload")).setExecutor(new ReloadCommand(this));
    }

    @Override
    public void onDisable() {
        getLogger().log(Level.INFO, "Disabling Collectopaedia plugin...");
        // Plugin shutdown logic
    }

    // Create a new player data file if it doesn't exist
    public void createPlayerFile(Player p) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            String uuid = p.getUniqueId().toString();
            File playerDataFolder = new File(getDataFolder(), "playerData");

            // Ensure the playerData folder exists
            if (!playerDataFolder.exists() && !playerDataFolder.mkdirs()) {
                getLogger().log(Level.WARNING, "Could not create player data folder.");
                return;
            }

            File newFile = new File(playerDataFolder, uuid + ".yml");

            // Check if the file already exists
            if (!newFile.exists()) {
                try {
                    // Create the file
                    if (newFile.createNewFile()) {
                        FileConfiguration playerFile = YamlConfiguration.loadConfiguration(newFile);

                        // Fill the file with default data
                        playerFile.set("name", p.getName());
                        playerFile.set("selectedArea", "colony9");
                        List<String> list = List.of("other", "colony9");
                        playerFile.set("unlockedArea", list);
                        playerFile.set("depositedItems", list);

                        // Save the data to the file
                        savePlayerFile(playerFile, p);

                        getLogger().info("Player file created for " + p.getName());
                    }
                } catch (IOException e) {
                    getLogger().log(Level.WARNING, "Could not create or save player file: " + e);
                }
            } else {
                getLogger().info("Player file already exists for " + p.getName());
            }
        });
    }

    // Update the player data file with the latest player information
    public void updatePlayerFile(Player p) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            FileConfiguration playerFile = loadPlayerData(p);
            playerFile.set("name", p.getName());
            playerFile.set("selectedArea", playerFile.getString("selectedArea"));
            playerFile.set("unlockedArea", playerFile.getStringList("unlockedArea"));

            // Ensure all areas have an entry in depositedItems
            List<String> areas = areasData.getStringList("areas");
            for (String a : areas) {
                String areaKey = a.split(",")[0].trim();
                if (!playerFile.contains("depositedItems." + areaKey)) {
                    playerFile.set("depositedItems." + areaKey, List.of());
                }
            }
            savePlayerFile(playerFile, p);
        });
    }

    // Update the items deposited by the player in a specific area
    public void updatePlayerItems(Player p, FileConfiguration playerFile, String item) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            String selectedArea = playerFile.getString("selectedArea");
            if (selectedArea == null) {
                getLogger().log(Level.WARNING, "Selected area not found for player " + p.getName());
                return;
            }

            List<String> areaItems = playerFile.getStringList("depositedItems." + selectedArea);
            areaItems.add(item);
            playerFile.set("depositedItems." + selectedArea, areaItems);
            savePlayerFile(playerFile, p);
        });
    }

    // Update the player's selected area
    public void updatePlayerArea(Player p, String area) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            FileConfiguration playerFile = loadPlayerData(p);
            playerFile.set("selectedArea", area);
            savePlayerFile(playerFile, p);
        });
    }

    // Save the player data file asynchronously
    public void savePlayerFile(FileConfiguration file, Player p) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                file.save(new File(getDataFolder() + "/playerData", p.getUniqueId() + ".yml"));
            } catch (IOException e) {
                getLogger().log(Level.WARNING, "Could not save player file for " + p.getName() + ": " + e);
            }
        });
    }

    // Load player data from the file
    public FileConfiguration loadPlayerData(Player p) {
        File file = new File(getDataFolder() + "/playerData", p.getUniqueId() + ".yml");
        if (!file.exists()) {
            createPlayerFile(p);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    // Check if the player data file exists
    public boolean playerFileExists(Player p) {
        return new File(getDataFolder() + "/playerData", p.getUniqueId() + ".yml").exists();
    }

    // Add a new area to the player's unlocked areas list
    public void addArea(Player p, String areaName) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            FileConfiguration playerFile = loadPlayerData(p);
            List<String> playerAreaList = playerFile.getStringList("unlockedArea");
            if (!playerAreaList.contains(areaName)) {
                playerAreaList.add(areaName);
                playerFile.set("unlockedArea", playerAreaList);
                savePlayerFile(playerFile, p);
            }
        });
    }

    // Preload data from area configuration files into cache
    public void preloadCollectopaediaData() {
        areaDataCache.clear();

        for (String area : areasData.getKeys(false)) {
            // Cache different types of items for each area
            areaDataCache.put(area, Map.of("fruit", itemsData.getStringList(area + ".fruit"), "vegetable", itemsData.getStringList(area + ".vegetable"), "flower", itemsData.getStringList(area + ".flower"), "animal", itemsData.getStringList(area + ".animal"), "bug", itemsData.getStringList(area + ".bug"), "nature", itemsData.getStringList(area + ".nature"), "parts", itemsData.getStringList(area + ".parts"), "strange", itemsData.getStringList(area + ".strange")));
        }
        getLogger().info("Collectopaedia data preloaded successfully.");
    }

    // Create a specific data file if it doesn't exist
    private void createDataFile(String fileName) {
        File file = new File(getDataFolder(), fileName);
        if (!file.exists() && !file.getParentFile().mkdirs()) {
            getLogger().log(Level.WARNING, "Could not create directory for " + fileName);
        }
        saveResource(fileName, false);
    }

    // Create all required data files
    private void createDataFiles() {
        createDataFile("items.yml");
        createDataFile("areas.yml");
        createDataFile("rewards.yml");
        itemsData = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "items.yml"));
        areasData = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "areas.yml"));
        rewardsData = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "rewards.yml"));
        preloadCollectopaediaData();
    }

    @Override
    public synchronized void reloadConfig() {
        // Reload the plugin's configuration and data files
        super.reloadConfig();
        File itemsFile = new File(getDataFolder(), "items.yml");
        File areasFile = new File(getDataFolder(), "areas.yml");
        File rewardsFile = new File(getDataFolder(), "rewards.yml");

        itemsData = YamlConfiguration.loadConfiguration(itemsFile);
        areasData = YamlConfiguration.loadConfiguration(areasFile);
        rewardsData = YamlConfiguration.loadConfiguration(rewardsFile);
    }
}