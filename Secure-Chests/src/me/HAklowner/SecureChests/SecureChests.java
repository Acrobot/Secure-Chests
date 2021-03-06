package me.HAklowner.SecureChests;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.HAklowner.SecureChests.Commands.LockCommand;
import me.HAklowner.SecureChests.Commands.SCCommand;
import me.HAklowner.SecureChests.Commands.UnLockCommand;
import me.HAklowner.SecureChests.Listeners.SecureChestsBlockListener;
import me.HAklowner.SecureChests.Listeners.SecureChestsExplosionListener;
import me.HAklowner.SecureChests.Listeners.SecureChestsPlayerListener;
import me.HAklowner.SecureChests.Listeners.SecureChestsRedstoneListener;
import me.HAklowner.SecureChests.Managers.LockManager;
import me.HAklowner.SecureChests.Utils.MetricsSC;
import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.FileConfigurationOptions;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class SecureChests extends JavaPlugin {

	private static SecureChests instance;

	//ClassListeners
	private final SecureChestsPlayerListener playerListener = new SecureChestsPlayerListener(this);
	private final SecureChestsBlockListener blockListener = new SecureChestsBlockListener(this);
	private final SecureChestsRedstoneListener redstoneListener = new SecureChestsRedstoneListener(this);
	private final SecureChestsExplosionListener explosionListener = new SecureChestsExplosionListener(this);

	//Define the logger
	static final Logger logger = Logger.getLogger("SecureChests");


	//simpleClan vars.
	public SimpleClans simpleClans;
	public boolean usingSimpleClans;


	//block list stuffs
	public static final Map<Integer, String> BLOCK_LIST = createBlockListMap(); //make block list name
	public static final Map<Integer, String> BLOCK_PERMS = createBlockPermMap(); //make block perm list
	public static final Map<Integer, String> BLOCK_CONFIG = createBlockConfigMap(); //make block perm list
	public Map<Integer, Boolean> blockStatus = new HashMap<Integer, Boolean>();
	public Map<Integer, Boolean> blockExplosion = new HashMap<Integer, Boolean>();

	//managers
	private LockManager LockManager;

	private static Map<Integer, String> createBlockListMap() {
		Map<Integer, String> result = new HashMap<Integer, String>();
		result.put(23, "dispenser");
		result.put(54, "chest");
		result.put(61, "furnace");
		result.put(62, "furnace");
		result.put(64, "door");
		result.put(96, "trapdoor");
		result.put(107, "gate");
		result.put(117, "potion stand");
		result.put(84, "jukebox");
		return Collections.unmodifiableMap(result);	
	}

	private static Map<Integer, String> createBlockConfigMap() {
		Map<Integer, String> result = new HashMap<Integer, String>();
		result.put(23, "Dispenser");
		result.put(54, "Chest");
		result.put(61, "Furnace");
		result.put(62, "Furnace");
		result.put(64, "Door");
		result.put(96, "Trapdoor");
		result.put(107, "Gate");
		result.put(117, "Potion");
		result.put(84, "Jukebox");
		return Collections.unmodifiableMap(result);	
	}

	private static Map<Integer, String> createBlockPermMap() {
		Map<Integer, String> result = new HashMap<Integer, String>();
		result.put(23, "dispenser");
		result.put(54, "chest");
		result.put(61, "furnace");
		result.put(62, "furnace");
		result.put(64, "door");
		result.put(96, "trapdoor");
		result.put(107, "gate");
		result.put(117, "potionstand");
		result.put(84, "jukebox");
		return Collections.unmodifiableMap(result);	
	}


	public Map<Player, Integer> scCmd = new HashMap<Player, Integer>();
	public Map<Player, String> scAList = new HashMap<Player, String>();	
	public Map<Player, Clan> scClan = new HashMap<Player, Clan>();


	/*/begin chest storage config commands

	private FileConfiguration storage = null;
	private File storageConfFile = new File("plugins/SecureChests/", "storage.yml");

	public FileConfiguration getStorageConfig() {
		if (storage == null) {
			reloadStorageConfig();
		}
		return storage;
	}
	
	public void reloadStorageConfig() {
		storage = YamlConfiguration.loadConfiguration(storageConfFile);
	}
	
	public void saveStorageConfig() {
		try {
			storage.save(storageConfFile);
		} catch(IOException ex) {
			logger.log(Level.SEVERE, "Could not save config to " + storageConfFile, ex);
		}
	}

	//end chest storage config commands

	//begin player global access list config commands

	private FileConfiguration aList = null;
	private File aListConfFile = new File("plugins/SecureChests/", "accesslist.yml");

	public FileConfiguration getAListConfig() {
		if (aList == null) {
			reloadAListConfig();
		}
		return aList;
	}

	public void reloadAListConfig() {
		aList = YamlConfiguration.loadConfiguration(aListConfFile);
	}

	public void saveAListConfig() {
		try {
			aList.save(aListConfFile);
		} catch(IOException ex) {
			logger.log(Level.SEVERE, "Could not save config to " + aListConfFile, ex);
		}
	}//end player global access list config commands
	*/
	
	public static Logger getLog() {
		return logger;
	}

	public static SecureChests getInstance() {
		return instance;
	}

	public static void log(String msg, Object... arg) {
		if (arg == null || arg.length == 0) {
			logger.log(Level.INFO, msg);
		} else {
			logger.log(Level.INFO, new StringBuilder().append(MessageFormat.format(msg, arg)).toString());
		}
	}


	public void sendMessage(Player player, String Message) {
		player.sendMessage(ChatColor.DARK_BLUE + "[Secure Chests]" + ChatColor.WHITE + " " + Message);
	}

	public void sendMessage(CommandSender sender, String Message) {
		sender.sendMessage(ChatColor.DARK_BLUE + "[Secure Chests]" + ChatColor.WHITE + " " + Message);
	}


	public void displayHelp(Player player) {
		displayHelp(player, 1);
	}

	public void displayHelp(Player player, Integer page) {

		List<String> helpList = new ArrayList<String>();

		if (player.hasPermission("securechests.lock")) {
			helpList.add(ChatColor.WHITE + "/sc lock (/lock)" + ChatColor.GRAY + " - lock your chests/furnaces/doors/etc...");
			helpList.add(ChatColor.WHITE + "/sc unlock (/unlock)" + ChatColor.GRAY + " - unlock your chests/furnaces/doors/etc...");
			helpList.add(ChatColor.WHITE + "/sc add username" + ChatColor.GRAY + " - Add a user to container/door access list");
			helpList.add(ChatColor.WHITE + "/sc deny username" + ChatColor.GRAY + " - Add a user to chest container/door list (will override global access list)");
			helpList.add(ChatColor.WHITE + "/sc remove username" + ChatColor.GRAY + " - remove a user from container/door access list");
			helpList.add(ChatColor.WHITE + "/sc gadd username" + ChatColor.GRAY + " - Add a user to your global allow");
			helpList.add(ChatColor.WHITE + "/sc gremove username" + ChatColor.GRAY + " - remove user from global allow list");
		}

		if (player.hasPermission("securechests.lock.public")) {
			helpList.add(ChatColor.WHITE + "/sc public" + ChatColor.GRAY + " - Toggle public status.");
		}

		if (player.hasPermission("securechests.bypass.lock")) {
			helpList.add(ChatColor.WHITE + "/sc lock Name (/lock Name)" + ChatColor.GRAY + " - lock chest for someone else.");
		}
		
		if (player.hasPermission("securechests.bypass.chagneowner")) {
			helpList.add(ChatColor.WHITE + "/sc newowner username" + ChatColor.GRAY + " - changes locks ownership to \"username\" ");
		}

		if (player.hasPermission("securechests.admin.reload")) {
			helpList.add(ChatColor.WHITE + "/sc reload" + ChatColor.GRAY + " - reload config files");
		}
		
		if (player.hasPermission("securechests.admin.purge")) {
			helpList.add(ChatColor.WHITE + "/sc purge" + ChatColor.GRAY + " - purge ghost locks from database");
		}

		if (helpList.isEmpty() ) {
			helpList.add("You don't have access to use SecureChests. :( (securechests.lock)");
		}

		int totalPageNum = (int) Math.ceil( (double)helpList.size() / (double)7 );

		page -= 1;

		if (page > totalPageNum-1 || page <= 0) {
			page = 0;
		}

		player.sendMessage(ChatColor.GOLD + "----- Secure Chests " + getDescription().getVersion() + " - Page " + (page+1) + "/" + totalPageNum + " -----");

		for (int i = (page * 7); i <= (page * 7) + 6 ;i++) {
			if (i >= helpList.size())
				break;
			player.sendMessage(helpList.get(i));
		}

		player.sendMessage(ChatColor.GOLD + "----- use '/sc help #' to get to other pages -----");



	}

	private void initBlockData() {
		// Get current active block
		blockStatus.clear(); // clear the enable/disabled for all block during initialization.
		for (Integer key : BLOCK_LIST.keySet() ) { //start all blocks disabled then get proper status from config file
			blockStatus.put(key, false); //default to not allow locking.
			blockStatus.put(key, this.getConfig().getBoolean("Active." + BLOCK_CONFIG.get(key)));
			blockExplosion.put(key, true);	//Default to block all explosions.
			blockExplosion.put(key, this.getConfig().getBoolean("Block_Explosions." + BLOCK_CONFIG.get(key)));
		}
	}

    @Override
	public void onEnable() {
		instance = this;
		PluginManager pm = this.getServer().getPluginManager();
		pm.registerEvents(blockListener, this);
		pm.registerEvents(playerListener, this);
		pm.registerEvents(redstoneListener, this);
		pm.registerEvents(explosionListener, this);

		// load / create config
		FileConfiguration cfg = getConfig();
		FileConfigurationOptions cfgOptions = cfg.options();
		cfgOptions.copyDefaults(true).copyHeader(true);
		saveConfig();      


		initBlockData(); //call this to get on/off status of lockable blocks.

		registerCommands();

		Plugin plug = pm.getPlugin("SimpleClans");

		if (plug != null)
		{
			simpleClans = ((SimpleClans) plug);
			usingSimpleClans = true;
			logger.log(Level.INFO, "[{0}] SimpleClans found.", getDescription().getName());    
		}

		metrics();

		//if (getConfig().getBoolean("ghost_purge_on_startup")) {
		//	GhostPurge gp = new GhostPurge();
		//	gp.purge(true);
		//}

		LockManager = new LockManager();
		
		
		logger.log(Level.INFO, "[{0}] {1} enabled.", new Object[]{getDescription().getName(), getDescription().getVersion()});    


	}
	
	public LockManager getLockManager() {
		return LockManager;
	}
	

	private void registerCommands() {
		getCommand("lock").setExecutor(new LockCommand());
		getCommand("unlock").setExecutor(new UnLockCommand());
		getCommand("sc").setExecutor(new SCCommand());
		getCommand("securechest").setExecutor(new SCCommand());
		getCommand("securechests").setExecutor(new SCCommand());
	}

	private void metrics() {
		try {
			MetricsSC metrics = new MetricsSC(this);
			metrics.start();
		} catch (IOException e) {
			logger.severe("Problems submitting plugin stats");
		}
	}

	public void reloadPlugin() {
		reloadConfig();
		initBlockData();
		logger.log(Level.INFO, "[{0}] Reload complete", getDescription().getName());
	}

    @Override
	public void onDisable() {
		getLockManager().closeConnection();
		logger.log(Level.INFO, "[{0}] {1} Disabled.", new Object[]{getDescription().getName(), getDescription().getVersion()}); 
	}

	// will return :
	// 1. exact name if online
	// 2. partial name if online
	// 3. if neither are true then return same name given
	public String myGetPlayerName(String name) { 
		Player caddPlayer = getServer().getPlayerExact(name);
		String pName;
		if(caddPlayer == null) {
			caddPlayer = getServer().getPlayer(name);
			if(caddPlayer == null) {
				pName = name;
			} else {
				pName = caddPlayer.getName();
			}
		} else {
			pName = caddPlayer.getName();
		}
		return pName;
	}
}
