/*
 * RemoteQuery - A CraftBukkit plugin that can query a remote DB via HTTP request by calling a PHP script via AES encrypted protocol.
 * Copyright (C) 2014  CubieX
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program; if not,
 * see <http://www.gnu.org/licenses/>.
 */
package com.github.CubieX.RemoteQuery;

import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class RemoteQuery extends JavaPlugin
{
   public static final Logger log = Bukkit.getServer().getLogger();
   static final String logPrefix = "[RemoteQuery] "; // Prefix to go in front of all log entries

   private RQCommandHandler comHandler = null;
   private RQConfigHandler cHandler = null;
   private RQHTTPHandler httpHandler = null;
   private RQEntityListener eListener = null;
   private RQSchedulerHandler schedHandler = null;
   
   // config values
   static boolean debug = false;
      
   // HTTP GET request
   static String scriptURL = " ";           // main URL of HTTP gateway to send the request to
   
   static String secretKey = "01234567890ABCDE"; // must have exactly 16 HEX chars (AES 128) or optional 32 HEX chars (AES 256)

   //*************************************************
   static String usedConfigVersion = "1"; // Update this every time the config file version changes, so the plugin knows, if there is a suiting config present
   //*************************************************

   @Override
   public void onEnable()
   {            
      cHandler = new RQConfigHandler(this);
      // FIXME wie die java.mail jar ins Plugin includen oder so machen, dass Bukkit sie findet beim start?? (lib-Verzeichnis z.B.)???
      if(!checkConfigFileVersion())
      {
         log.severe(logPrefix + "Outdated or corrupted config file(s). Please delete your config files."); 
         log.severe(logPrefix + "will generate a new config for you.");
         log.severe(logPrefix + "will be disabled now. Config file is outdated or corrupted.");
         getServer().getPluginManager().disablePlugin(this);
         return;
      }

      readConfigValues();

      schedHandler = new RQSchedulerHandler(this);
      httpHandler = new RQHTTPHandler(this, schedHandler);
      eListener = new RQEntityListener(this);
      comHandler = new RQCommandHandler(this, cHandler, httpHandler);      
      getCommand("rq").setExecutor(comHandler);      
     
      log.info(logPrefix + " version " + getDescription().getVersion() + " is enabled!");
   }   

   private boolean checkConfigFileVersion()
   {      
      boolean configOK = false;     

      if(cHandler.getConfig().isSet("config_version"))
      {
         String configVersion = getConfig().getString("config_version");

         if(configVersion.equals(usedConfigVersion))
         {
            configOK = true;
         }
      }

      return (configOK);
   }  

   public void readConfigValues()
   {
      boolean exceed = false;
      boolean invalid = false;

      if(getConfig().isSet("debug")){debug = getConfig().getBoolean("debug");}else{invalid = true;}
      
      // HTTP request
      if(getConfig().isSet("scriptURL")){scriptURL = getConfig().getString("scriptURL");}else{invalid = true;}
      if(getConfig().isSet("secretKey")){secretKey = getConfig().getString("secretKey");}else{invalid = true;}      
      
      if(exceed)
      {
         log.warning(logPrefix + "One or more config values are exceeding their allowed range. Please check your config file!");
      }

      if(invalid)
      {
         log.warning(logPrefix + "One or more config values are invalid. Please check your config file!");
      }
   }

   @Override
   public void onDisable()
   {     
      this.getServer().getScheduler().cancelTasks(this);
      cHandler = null;
      eListener = null;
      comHandler = null;
      httpHandler = null;
      schedHandler = null;
      log.info(logPrefix + "version " + getDescription().getVersion() + " is disabled!");
   }

   // #########################################################

   
}


