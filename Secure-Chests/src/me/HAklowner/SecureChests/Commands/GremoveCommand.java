package me.HAklowner.SecureChests.Commands;

import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.managers.ClanManager;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.HAklowner.SecureChests.SecureChests;

public class GremoveCommand {

	private final SecureChests plugin;

	public GremoveCommand() {
		plugin = SecureChests.getInstance();
	}


	// command status:
	// 0/null=none
	// 1= lock
	// 2= unlock
	// 3= add to chest access list
	// 4= remove from chest access list
	// 5= add to deny list
	// 6= lock for other (perms already checked).
	// 7= add clan to access list.
	// 8= remove clan from access list.
	// 9= add clan to deny list.
	// 10=toggle public status.

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

		Player player = null;
		if (sender instanceof Player) {
			player = (Player) sender;
		} else {
			sender.sendMessage("[SecureChests] This Command can only be used by a player");
			return true;
		}
		
	if (sender.hasPermission("securechests.lock")) {
		if (args.length != 1) {
			plugin.sendMessage(player, "Correct command usage: /sc gremove username");
		} else {
			if (args[0].toLowerCase().startsWith("c:") && plugin.usingSimpleClans) { //they want to add a clan not a player
				String clanTag = args[0].substring(2);
				ClanManager cm = plugin.simpleClans.getClanManager();
				if (cm.isClan(clanTag)) {
					Clan clan = cm.getClan(clanTag);
					plugin.sendMessage(player, "removing clan " + clan.getTagLabel() + ChatColor.WHITE + " from your global allow list.");
					plugin.getLockManager().addToGlobalList(player.getName(), clanTag, "clan");
				} else {
					if (!plugin.getLockManager().clanOnGlobalList(player.getName(), clanTag)){
						plugin.sendMessage(player, "clan " + clanTag + " Not on your global access list");
					} else {
						plugin.getLockManager().removeFromGlobalList(player.getName(), clanTag, "clan");
						plugin.sendMessage(player, "clan "+clanTag+" Removed from your global access list.");
					}
				}
			} else if (args[0].toLowerCase().startsWith("c:") && !plugin.usingSimpleClans) {
				plugin.sendMessage(player, "Server not using Simple Clans, unable to add clan to access list.");
			} else {
				String pName = plugin.myGetPlayerName(args[0]);
				if (!plugin.getLockManager().playerOnGlobalList(player.getName(), pName)){
					plugin.sendMessage(player, "Player " + pName + " Not on your global access list");
				} else {
					plugin.getLockManager().removeFromGlobalList(player.getName(), pName, "player");
					plugin.sendMessage(player, "Player "+pName+" Removed from your global access list.");
				}
			}
		}
	} else {
		plugin.sendMessage(player, "You don't have permission to use SecureChests. (securechests.lock)");
	}
	return true;
}
}
