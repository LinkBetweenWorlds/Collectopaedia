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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;


public final class Collectopaedia extends JavaPlugin implements Listener {

    public final Map<String, Map<String, List<String>>> areaDataCache = new HashMap<>();
    public FileConfiguration areasData;
    public FileConfiguration itemsData;
    public FileConfiguration rewardsData;

    @Override
    public void onEnable() {

        createDataFile();
        preloadCollectopaediaData();

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new PlayerInvClickEvent(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinLeaveListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerChangeWorldListener(this), this);

        Objects.requireNonNull(getCommand("collectopaedia")).setExecutor(new OpenMenuCommand(this));
        if (getCommand("reloadconfig") != null) {
            getCommand("reloadconfig").setExecutor(new ReloadCommand(this));
        } else {
            getLogger().severe("Failed to register the reloadconfig command. Please check your plugin.yml.");
        }
        //Means the plugin works somehow.
        getLogger().log(Level.INFO, "This message means that the Collectopaedia Plugin works somehow???\nHow? I have no clue. -Blink");
    }

    @Override
    public void onDisable() {
        getLogger().log(Level.INFO, "Disabling Collectopaedia plugin...");
        // Plugin shutdown logic
    }

    //Player file management
    public void createPlayerFile(Player p) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            String uuid = p.getUniqueId().toString();
            File newFile = new File(getDataFolder() + "/playerData", uuid + ".yml");
            if (!newFile.exists()) {
                try {
                    if (!newFile.createNewFile() && newFile.exists()) {
                        FileConfiguration playerFile = YamlConfiguration.loadConfiguration(newFile);
                        playerFile.set(uuid + ".name", p.getName());
                        playerFile.set(uuid + ".selectedArea", "colony9");
                        List<String> list = List.of("other", "colony9");
                        playerFile.set("unlockedArea", list);
                        playerFile.set("depositedItems", list);
                        playerFile.save(new File(getDataFolder() + "/playerData", uuid + ".yml"));
                    }
                } catch (IOException e) {
                    getLogger().log(Level.WARNING, e.toString());
                }
            }
        });
    }

    public void updatePlayerFile(Player p) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            FileConfiguration playerFile = loadPlayerData(p);
            List<String> areas = areasData.getStringList("areas");
            for (String a : areas) {
                String[] areaParts = a.split(",");
                if (!playerFile.contains("depositedItems." + areaParts[0].trim())) {
                    List<String> items = List.of();
                    playerFile.set("depositedItems." + areaParts[0].trim(), items);
                }
            }
            savePlayerFile(playerFile, p);
        });
    }

    public void updatePlayerItems(Player p, FileConfiguration playerFile, String item) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            String selectedArea = playerFile.getString(p.getUniqueId() + ".selectedArea");
            List<String> areaItems = playerFile.getStringList("depositedItems." + selectedArea);
            areaItems.add(item);
            playerFile.set("depositedItems." + selectedArea, areaItems);
            savePlayerFile(playerFile, p);
        });
    }

    public void updatePlayerArea(Player p, String area) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            FileConfiguration playerFile = loadPlayerData(p);
            playerFile.set(p.getUniqueId() + ".selectedArea", area);
            savePlayerFile(playerFile, p);
        });
    }

    public void savePlayerFile(FileConfiguration file, Player p) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                file.save(new File(getDataFolder() + "/playerData", p.getUniqueId() + ".yml"));
            } catch (IOException e) {
                getLogger().log(Level.WARNING, e.toString());
            }
        });
    }

    public FileConfiguration loadPlayerData(Player p) {
        File file = new File(getDataFolder() + "/playerData", p.getUniqueId() + ".yml");
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            if (!file.exists()) {
                createPlayerFile(p);
            }
        });
        return YamlConfiguration.loadConfiguration(file);
    }

    public boolean playerFileExists(Player p) {
        return new File(getDataFolder() + "/playerData", p.getUniqueId() + ".yml").exists();
    }

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

    //Static file management
    private void preloadCollectopaediaData() {
        // Assuming 'itemsData.getKeys(false)' gives all the `selectedArea` keys
        for (String area : itemsData.getKeys(false)) {
            // Cache data for each area
            areaDataCache.put(area, Map.of("veg", itemsData.getStringList(area + ".veg"), "fruit", itemsData.getStringList(area + ".fruit"), "flower", itemsData.getStringList(area + ".flower"), "animal", itemsData.getStringList(area + ".animal"), "bug", itemsData.getStringList(area + ".bug"), "nature", itemsData.getStringList(area + ".nature"), "parts", itemsData.getStringList(area + ".parts"), "strange", itemsData.getStringList(area + ".strange")));
        }
        getLogger().info("Collectopaedia data preloaded successfully.");
    }

    private void createDataFile() {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            File itemsFile = new File(getDataFolder(), "items.yml");
            if (!itemsFile.exists()) {
                if (itemsFile.getParentFile().mkdirs()) {
                    saveResource("items.yml", false);
                }

            }
            File areasFile = new File(getDataFolder(), "areas.yml");
            if (!areasFile.exists()) {
                if (areasFile.getParentFile().mkdirs()) {
                    saveResource("areas.yml", false);
                }
            }
            File rewardsFile = new File(getDataFolder(), "rewards.yml");
            if (!rewardsFile.exists()) {
                if (rewardsFile.getParentFile().mkdirs()) {
                    saveResource("rewards.yml", false);
                }
            }
            itemsData = YamlConfiguration.loadConfiguration(itemsFile);
            areasData = YamlConfiguration.loadConfiguration(areasFile);
            rewardsData = YamlConfiguration.loadConfiguration(rewardsFile);
        });
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        File itemsFile = new File(getDataFolder(), "items.yml");
        File areasFile = new File(getDataFolder(), "areas.yml");
        File rewardsFile = new File(getDataFolder(), "rewards.yml");

        itemsData = YamlConfiguration.loadConfiguration(itemsFile);
        areasData = YamlConfiguration.loadConfiguration(areasFile);
        rewardsData = YamlConfiguration.loadConfiguration(rewardsFile);
    }
}
