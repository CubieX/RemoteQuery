package com.github.CubieX.RemoteQuery;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RQCommandHandler implements CommandExecutor
{
   private RemoteQuery plugin = null;
   private RQConfigHandler cHandler = null;
   private RQHTTPHandler mailHandler = null;

   public RQCommandHandler(RemoteQuery plugin, RQConfigHandler cHandler, RQHTTPHandler mailHandler) 
   {
      this.plugin = plugin;
      this.cHandler = cHandler;
      this.mailHandler = mailHandler;
   }

   @Override
   public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
   {
      Player player = null;

      if (sender instanceof Player) 
      {
         player = (Player) sender;
      }

      if (cmd.getName().equalsIgnoreCase("rq"))
      {
         if (args.length == 0)
         { //no arguments, so help will be displayed
            return false;
         }
         else if(args.length == 1)
         {
            if (args[0].equalsIgnoreCase("version"))
            {
               sender.sendMessage(RemoteQuery.logPrefix + ChatColor.GREEN + "This server is running " + plugin.getDescription().getName() + " version " + plugin.getDescription().getVersion());
               return true;
            }

            if (args[0].equalsIgnoreCase("reload"))
            {
               if(sender.isOp() || sender.hasPermission("remotequery.admin"))
               {                        
                  cHandler.reloadConfig(sender);
                  return true;
               }
               else
               {
                  sender.sendMessage(ChatColor.RED + "You do not have sufficient permission to reload " + plugin.getDescription().getName() + "!");
               }
            }
         }
         else if(args.length == 2)
         {
            if (args[0].equalsIgnoreCase("query"))
            {
               if(sender.isOp() || sender.hasPermission("remotequery.use"))
               {
                  String queryParam = args[1];

                  mailHandler.executeQueryHTTPasync(sender, queryParam);
               }
               else
               {
                  sender.sendMessage(ChatColor.RED + "You do not have sufficient permission to send an HTTP request!");
               }

               return true;
            }
         }  
         else
         {
            sender.sendMessage(ChatColor.YELLOW + "Falsche Parameteranzahl.");
         }                

      }         
      return false; // if false is returned, the help for the command stated in the plugin.yml will be displayed to the player
   }
}
