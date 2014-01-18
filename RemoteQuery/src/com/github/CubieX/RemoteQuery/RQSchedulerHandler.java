package com.github.CubieX.RemoteQuery;

import org.bukkit.command.CommandSender;

public class RQSchedulerHandler
{
   private RemoteQuery plugin = null;

   public RQSchedulerHandler(RemoteQuery plugin)
   {
      this.plugin = plugin;
   }

   public void sendSyncMessage(final CommandSender sender, final String message)
   {
      plugin.getServer().getScheduler().runTask(plugin, new Runnable()
      {
         @Override
         public void run()
         {
            if(null != sender)
            {
               sender.sendMessage(message);  
            }            
         }
      });
   }
}
