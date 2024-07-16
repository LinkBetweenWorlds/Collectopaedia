package org.xenocraft.collectopaedia;


import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.xenocraft.collectopaedia.commands.OpenMenuCommand;
import org.xenocraft.collectopaedia.commands.ReloadCommand;
import org.xenocraft.collectopaedia.events.PlayerInvClickEvent;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.xenocraft.collectopaedia.listener.PlayerChangeWorldListener;
import org.xenocraft.collectopaedia.listener.PlayerJoinLeaveListener;

import java.awt.image.AreaAveragingScaleFilter;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;


public final class Collectopaedia extends JavaPlugin implements Listener {

    public FileConfiguration areasData;
    public FileConfiguration itemsData;

    @Override
    public void onEnable() {

        createDataFile();

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new PlayerInvClickEvent(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinLeaveListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerChangeWorldListener(this), this);

        Objects.requireNonNull(getCommand("open")).setExecutor(new OpenMenuCommand(this));
        if (getCommand("reloadconfig") != null) {
            getCommand("reloadconfig").setExecutor(new ReloadCommand(this));
        } else {
            getLogger().severe("Failed to register the reloadconfig command. Please check your plugin.yml.");
        }
        //Means the plugin work somehow.
        getLogger().log(Level.INFO, "This means that the Collectopaedia Plugin works somehow???");

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    // Method to create a new YAML file
    public void createPlayerFile(Player p) {
        String uuid = p.getUniqueId().toString();
        File newFile = new File(getDataFolder() + "/playerData", uuid + ".yml");
        if (!newFile.exists()) {
            try {
                newFile.getParentFile().mkdirs();
                newFile.createNewFile();
                FileConfiguration playerFile = YamlConfiguration.loadConfiguration(newFile);
                playerFile.set(uuid + ".name", p.getName());
                List<String> list = List.of("other", "colony9");
                playerFile.set("unlockedArea", list);
                playerFile.set("depositedItems", list);
                playerFile.save(new File(getDataFolder() + "/playerData", uuid + ".yml"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void updatePlayerFile(Player p){
        FileConfiguration playerFile = loadPlayerData(p);
        List<String> areas = areasData.getStringList("areas");
        for(String a : areas){
            String[] areaParts = a.split(",");
            if(!playerFile.contains("depositedItems." + areaParts[0].trim())){
                List<String> items = List.of();
                playerFile.set("depositedItems." + areaParts[0].trim(), items);
            }
        }
        savePlayerFile(playerFile, p);
    }

    public void savePlayerFile(FileConfiguration file, Player p) {
        try {
            file.save(new File(getDataFolder() + "/playerData", p.getUniqueId() + ".yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public FileConfiguration loadPlayerData(Player p) {
        File file = new File(getDataFolder() + "/playerData", p.getUniqueId() + ".yml");
        if(!file.exists()){
            createPlayerFile(p);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public void addArea(Player p, String areaName){
        FileConfiguration playerFile = loadPlayerData(p);
        List<String> playerAreaList = playerFile.getStringList("unlockedArea");
        if(!playerAreaList.contains(areaName)){
            playerAreaList.add(areaName);
        }
        playerFile.set("unlockedArea", playerAreaList);
        savePlayerFile(playerFile, p);
    }

    // Method to check if a YAML file exists
    public boolean playerFileExists(Player p) {
        File file = new File(getDataFolder() + "/playerData", p.getUniqueId() + ".yml");
        return file.exists();
    }

    private void createDataFile() {
        File itemsFile = new File(getDataFolder(), "items.yml");
        if (!itemsFile.exists()) {
            itemsFile.getParentFile().mkdirs();
            saveResource("items.yml", false);
        }
        File areasFile = new File(getDataFolder(), "areas.yml");
        if (!areasFile.exists()) {
            areasFile.getParentFile().mkdirs();
            saveResource("areas.yml", false);
        }
        itemsData = YamlConfiguration.loadConfiguration(itemsFile);
        areasData = YamlConfiguration.loadConfiguration(areasFile);
    }

    @Override
    public void reloadConfig(){
        super.reloadConfig();
        File itemsFile = new File(getDataFolder(), "items.yml");
        File areasFile = new File(getDataFolder(), "areas.yml");

        itemsData = YamlConfiguration.loadConfiguration(itemsFile);
        areasData = YamlConfiguration.loadConfiguration(areasFile);
    }
}
