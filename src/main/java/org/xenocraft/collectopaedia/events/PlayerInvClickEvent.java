package org.xenocraft.collectopaedia.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.xenocraft.collectopaedia.commands.OpenMenuCommand;

public class PlayerInvClickEvent implements Listener {
    @EventHandler
    public void InvClickEvent(InventoryClickEvent event){
        if(event.getCurrentItem() != null){
            Player p = (Player) event.getWhoClicked();
            if(event.getView().getTitle().contains("Collectopaedia")){
                event.setCancelled(true);
                OpenMenuCommand.menuClick(p, event.getCurrentItem());
            }
        }
    }
}
