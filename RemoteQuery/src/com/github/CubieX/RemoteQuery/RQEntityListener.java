package com.github.CubieX.RemoteQuery;

import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public class RQEntityListener implements Listener
{
   private RemoteQuery plugin = null;

   UUID leashedEntity;

   public RQEntityListener(RemoteQuery plugin)
   {        
      this.plugin = plugin;

      plugin.getServer().getPluginManager().registerEvents(this, plugin);
   }

   //================================================================================================
   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onPlayerInteract(PlayerInteractEvent e)
   {
      
   }
}
