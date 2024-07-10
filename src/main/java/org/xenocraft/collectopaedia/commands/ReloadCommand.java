package org.xenocraft.collectopaedia.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.xenocraft.collectopaedia.Collectopaedia;

import java.util.List;

public class ReloadCommand implements TabExecutor {

    private final Collectopaedia collectopaedia;

    public ReloadCommand(Collectopaedia collectopaedia){
        this.collectopaedia = collectopaedia;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if (command.getName().equalsIgnoreCase("reloadconfig")) {
            collectopaedia.reloadConfig();
            sender.sendMessage("Configuration reloaded!");
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        return List.of();
    }
}
