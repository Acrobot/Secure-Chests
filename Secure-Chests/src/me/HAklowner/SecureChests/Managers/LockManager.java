package me.HAklowner.SecureChests.Managers;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.HAklowner.SecureChests.Lock;
import me.HAklowner.SecureChests.SecureChests;
import me.HAklowner.SecureChests.Storage.DBCore;
import me.HAklowner.SecureChests.Storage.SQLite;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class LockManager {

	private SecureChests plugin;
	private Logger logger;
	private DBCore core;

	public LockManager() {
		plugin = SecureChests.getInstance();
		logger = logger;
		initalizeDB();

	}

	private void initalizeDB() {
		core = new SQLite(plugin.getDataFolder().getPath());

		if(core.checkConnection()) {
			logger.info("[Secure Chests] SQLite database connection successful.");
			if(!core.tableExists("SC_Locks")) {

				SecureChests.log("[Secure Chests] Creating Table: SC_Locks");

				String query = "" +
						"CREATE TABLE IF NOT EXISTS `SC_Locks` (" +
						"	`id` INTEGER PRIMARY KEY," +
						"	`World` varchar(30)," +
						"	`Owner` varchar(30)," +
						"	`PosX` int(11)," +
						"	`PosY` int(11)," +
						"	`PosZ` int(11)," +
						"	`Public` tinyint(1) DEFAULT '0'" +
						")";
				core.execute(query);
			}

			if(!core.tableExists("SC_Access")) {

				SecureChests.log("[Secure Chests] Creating Table: SC_Access");

				String query = "" +
						"CREATE TABLE IF NOT EXISTS `SC_Access` (" +
						" `id` INTEGER PRIMARY KEY," +
						" `Lock ID` int(11)," +
						" `Type` varchar(10)," +
						" `Name` varchar(30)," +
						" `Access` tinyint(1)" +
						")";
				core.execute(query);
			}

			if(!core.tableExists("SC_Global")) {
				SecureChests.log("[Secure Chests] Creating Table: SC_Global");

				String query = "" +
						"CREATE TABLE IF NOT EXISTS `SC_Global` (" +
						"  `id` INTEGER PRIMARY KEY," +
						"  `Player` varchar(30)," +
						"  `Type` varchar(10)," +
						"  `Name` varchar(30)" +
						")";
				core.execute(query);
			}
		} else {
			logger.info("[Secure Chests] SQLite database connection failed. :(");
		}
	}

	public Lock getLock(Location loc) {
		Lock lock = new Lock(loc);	
		String query = "SELECT * FROM `SC_Locks` WHERE" +
				" `World` = '" + loc.getWorld().getName() + "' AND " +
				" `PosX` = " + loc.getBlockX() + " AND " +
				" `PosY` = " + loc.getBlockY() + " AND " +
				" `PosZ` = " + loc.getBlockZ() + ";";
		ResultSet res = core.select(query);

		try {
			if(res.next()) {

				int lockID = res.getInt("id");

				lock.setID(lockID);
				lock.setOwner(res.getString("Owner"));
				lock.setPublic(res.getBoolean("Public"));

				//Get Local access list.
				String accessQuery = "SELECT * FROM `SC_Access` WHERE" +
						"`Lock ID` = " + lockID;
				ResultSet aRes = core.select(accessQuery);

				Map<String, Boolean> playerAccessList = new HashMap<String, Boolean>();
				Map<String, Boolean> clanAccessList = new HashMap<String, Boolean>();

				while(aRes.next()) {
					String type = aRes.getString("Type");
					String name = aRes.getString("Name");
					Boolean ac = aRes.getBoolean("Access");
					if (type.equals("player"))
						playerAccessList.put(name, ac);
					if (type.equals("clan"))
						clanAccessList.put(name, ac);
				}

				lock.setPlayerAccessList(playerAccessList);
				lock.setClanAccessList(clanAccessList);

			} else { //empty lock don't execute any more queries.
				return lock;
			}
		} catch (Exception ex) {
			for (StackTraceElement el : ex.getStackTrace()) {
				System.out.print(el.toString());
			}
		}
		return lock;	
	}

	public void newLock(Lock lock) {
		String query = "INSERT INTO `SC_Locks` (`World`, `owner`, `PosX`, `PosY`, `PosZ`, `Public`) VALUES ('"+lock.getLocation().getWorld().getName()+"', '"+lock.getOwner()+"', '"+lock.getLocation().getBlockX()+"', '"+lock.getLocation().getBlockY()+"', '"+lock.getLocation().getBlockZ()+"', '0')";
		//logger.info(query);
		core.execute(query);
	}

	public void updateLock(Lock lock) {
		int pub = 0;
		if (lock.isPublic())
			pub = 1;
		String query = "UPDATE `SC_Locks` SET `owner` = '"+lock.getOwner()+"', `Public` = '"+pub+"', `PosX` = '"+lock.getLocation().getBlockX()+"', `PosY` = '"+lock.getLocation().getBlockY()+"', `PosZ` = '"+lock.getLocation().getBlockZ()+"' WHERE `id` =" + lock.getID();
		core.execute(query);
	}

	//action == true, add
	//action == false, remove.
	public void addToAcessList(Lock lock, String name, String type, Boolean access) {
		int na = 0; //false
		if (access)
			na = 1;
		String query = "INSERT INTO `SC_Access` (`Lock ID`,`Type`,`Name`,`Access`) VALUES ('"+lock.getID()+"','"+type+"','"+name+"','"+na+"')";
		core.execute(query);
	}

	public void removeFromAccessList(Lock lock, String name, String type) {
		String query = "DELETE FROM `SC_Access` WHERE `Lock ID` = "+lock.getID()+" AND `Name` = '"+name+"' AND `type` = '"+type+"'";
		core.execute(query);
	}

	public void removeLock(Lock lock) {
		String query = "DELETE FROM `SC_Locks` WHERE `id` = " + lock.getID();
		core.execute(query);
		query = "DELETE FROM `SC_Access` WHERE `Lock ID` = " + lock.getID();
		core.execute(query);
	}

	public Boolean playerOnGlobalList(String owner, String user) {
		String query = "SELECT `Name` FROM `SC_Global` WHERE `Player` = '"+owner+"' AND `Type` = 'player'";
		ResultSet result = core.select(query);

		try {
			while(result.next()) {
				if(user.equals(result.getString("Name")))
					return true;
			}
		} catch (SQLException e) {
			//ohh no access list empty!
			return false;
		}
		return false;
	}

	public Boolean clanOnGlobalList(String owner, String clantag) {
		String query = "SELECT `Name` FROM `SC_Global` WHERE `Player` = '"+owner+"' AND `Type` = 'clan'";
		ResultSet result = core.select(query);

		try {
			while(result.next()) {
				if(clantag.equals(result.getString("Name")))
					return true;
			}
		} catch (SQLException e) {
			//ohh no access list empty!
			return false;
		}
		return false;
	}

	public void addToGlobalList(String owner, String name, String type) {
		String query = "INSERT INTO `SC_Global` (`Player`,`Type`,`Name`) VALUES ('"+owner+"','"+type+"','"+name+"')";
		core.execute(query);
	}

	public void removeFromGlobalList(String owner, String name, String type) {
		String query = "DELETE FROM `SC_Global` WHERE `Player` = '"+owner+"' AND `Type` = '"+type+"' AND `Name` = '"+name+"'";
		core.execute(query);
	}


	private boolean purgeConsole = false;
	private Player purgePlayer;
	private void purgeMessage(String msg) {
		if (!purgeConsole) { //send to player if player started command
			plugin.sendMessage(purgePlayer, msg);
		}
		//send to console regardless of who started it.
		SecureChests.log("[" + plugin.getDescription().getName() + "] "+msg);
	}

	public void purgeGhostEntry(Player player) {
		purgeConsole = false;
		purgePlayer = player;
		purgeGhostEntry();
	}

	public void purgeGhostEntry(boolean fromconsole) {
		if (fromconsole) { 
			purgeConsole = true;
			purgeGhostEntry();
		}
	}

	private void purgeGhostEntry() {
		purgeMessage("Starting Ghost Purge");

		//limit to 200 locks loaded at a time.
		int pass = 0;
		boolean keepgoing = true;
		int total = 0;
		while (keepgoing) {
			try {
				String query = "SELECT * FROM `SC_Locks` ORDER BY `id` LIMIT 200 OFFSET "+ ((pass*200)-total);
				ResultSet result = core.select(query);
				int count = 0;
				while(result.next()) {
					Location loc = new Location(plugin.getServer().getWorld(result.getString("world")), result.getDouble("PosX"), result.getDouble("PosY"), result.getDouble("PosZ"));
					if (!plugin.blockStatus.containsKey(loc.getBlock().getTypeId())) {
						//ohh no its a ghost entry! squash it!
						String delquery = "DELETE FROM `SC_Locks` WHERE `id` = " + result.getInt("id");
						core.execute(delquery);
						total++;
					}
				}
				if (count == 0) { //no rows returned we reached the end!
					keepgoing = false;
				}
			} catch (SQLException e) {
				purgeMessage("there has been an error while attempting to purge ghost entries. :(");
				keepgoing = false;
			}
			pass++;
			purgeMessage("purged " + total + " ghost locks so far!");
		}
		purgeMessage("Purge Complete " +total+ " Ghost locks purged");
	}

	public void updateFromFlatFile() {
		logger.info("[SecureChests] Starting Upgrade (note server will hang while upgrade is taking place");
		File storageConfigFile = new File("plugins/SecureChests", "storage.yml");
		FileConfiguration storageConfig =  YamlConfiguration.loadConfiguration(storageConfigFile);
		Set<String> worldList = storageConfig.getConfigurationSection("").getKeys(false);
		int total = 0;
		for(String world:worldList) {
			logger.log(Level.INFO, "[SecureChests] starting import of world {0}", world);
			Set<String> locationList = storageConfig.getConfigurationSection(world).getKeys(false);
			int worldtotal = 0;
			for(String location:locationList) {
				String[] loc = location.split("_");
				String owner = storageConfig.getString(world+"."+location+".owner");
				boolean ispublic = storageConfig.getBoolean(world+"."+location+".public");
				int pub = 0;
				if (ispublic)
					pub = 1;
				total++;
				worldtotal++;
				if (worldtotal % 50 == 0) {
					logger.log(Level.INFO, "[SecureChests] {0}/{1} Processed in world {2}", new Object[]{worldtotal, locationList.size(), world});
					logger.log(Level.INFO, "[SecureChests] {0} across all worlds", total);
				}
				core.execute("INSERT INTO `SC_Locks` (`World`, `owner`, `PosX`, `PosY`, `PosZ`, `Public`) VALUES ('"+world+"', '"+owner+"', '"+loc[0]+"', '"+loc[1]+"', '"+loc[2]+"', '"+pub+"')");
				String query = "SELECT `id` FROM `SC_Locks` WHERE" +
						" `World` = '" + world + "' AND " +
						" `PosX` = " + loc[0] + " AND " +
						" `PosY` = " + loc[1] + " AND " +
						" `PosZ` = " + loc[2] + ";";
				ResultSet idres = core.select(query);
				int id = 0;
				try {
					while(idres.next()) {
						id = idres.getInt("id");
					}
				} catch (SQLException e) {
					return;
				}
				ConfigurationSection acs = storageConfig.getConfigurationSection(world+"."+location+".access");
				if (acs != null) {
					Set<String> access = acs.getKeys(false);
					for (String name : access) {
						boolean hasaccess = storageConfig.getBoolean(world+"."+location+".access."+name);
						int ac = 0;
						if (hasaccess)
							ac = 1;
						String pquery = "INSERT INTO `SC_Access` (`Lock ID`,`Type`,`Name`,`Access`) VALUES ('"+id+"','player','"+name+"','"+ac+"')";
						core.execute(pquery);
					}
				}

				ConfigurationSection ccs = storageConfig.getConfigurationSection(world+"."+location+".access");
				if(ccs != null) {
					Set<String> caccess = ccs.getKeys(false);
					for (String clantag : caccess) {
						boolean hasaccess = storageConfig.getBoolean(world+"."+location+".access."+clantag);
						int ac = 0;
						if (hasaccess)
							ac = 1;
						String pquery = "INSERT INTO `SC_Access` (`Lock ID`,`Type`,`Name`,`Access`) VALUES ('"+id+"','player','"+clantag+"','"+ac+"')";
						core.execute(pquery);
					}
				}
			}
			logger.log(Level.INFO, "[SecureChests] {0}/{1} Processed in world {2}", new Object[]{worldtotal, locationList.size(), world});
			logger.log(Level.INFO, "[SecureChests]{0} across all worlds", total);
		}	
		logger.log(Level.INFO, "[SecureChests] Upgrade Complete! Processed {0} entries", total);
	}

	public void closeConnection()
	{
		core.close();
	}

}
