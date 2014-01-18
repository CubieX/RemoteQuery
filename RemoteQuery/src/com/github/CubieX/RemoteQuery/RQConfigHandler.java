package com.github.CubieX.RemoteQuery;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

public class RQConfigHandler 
{
   private FileConfiguration config;
   private final RemoteQuery plugin;

   public RQConfigHandler(RemoteQuery plugin) 
   {
      this.plugin = plugin;      

      initConfig();
   }

   private void initConfig()
   {
      plugin.saveDefaultConfig(); //creates a copy of the provided config.yml in the plugins data folder, if it does not exist
      config = plugin.getConfig(); //re-reads config out of memory. (Reads the config from file only, when invoked the first time!)
   }
   
   public FileConfiguration getConfig()
   {
      return (config);
   }

   public void saveConfig() //saves the config to disc (needed when entries have been altered via the plugin in-game)
   {
      // get and set values here!
      plugin.saveConfig();
   }

   //reloads the config from disc (used if user made manual changes to the config.yml file)
   public void reloadConfig(CommandSender sender)
   {
      plugin.reloadConfig();
      config = plugin.getConfig(); // new assignment necessary when returned value is assigned to a variable or static field(!)
      plugin.readConfigValues();

      sender.sendMessage(RemoteQuery.logPrefix + " " + plugin.getDescription().getVersion() + " reloaded!");      
   } 
}
