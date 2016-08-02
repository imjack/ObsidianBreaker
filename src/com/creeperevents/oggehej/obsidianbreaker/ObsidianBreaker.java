package com.creeperevents.oggehej.obsidianbreaker;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ConcurrentHashMap;

import cn.nukkit.block.Block;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.plugin.PluginManager;
import cn.nukkit.scheduler.PluginTask;

/**
 * The main class of ObsidianBreaker
 * 
 * @author oggehej
 */
public class ObsidianBreaker extends PluginBase {
	BlockListener blockListener;
	private PlayerListener playerListener;
	private StorageHandler storage;
	int regenRunner = -1;

	/**
	 * To be run on enable
	 */
	public void onEnable() {
		blockListener = new BlockListener(this);
		playerListener = new PlayerListener(this);
		storage = new StorageHandler(this);

		// Register listeners
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(blockListener, this);
		pm.registerEvents(playerListener, this);

		// Load configuration file
		saveDefaultConfig();
		Locale.setupLocale(this);

		// Initialise command
		getServer().getCommandMap().register("obsidianbreaker", new CommandHandler(this));

		// Schedule runners
		scheduleRegenRunner();
	}

	/**
	 * To be run on disable
	 */
	public void onDisable() {
		storage = null;
		blockListener = null;
		playerListener = null;
	}

	/**
	 * Get the storage handler of ObsidianBreaker
	 * 
	 * @return Storage handler
	 */
	public StorageHandler getStorage() {
		return storage;
	}

	/**
	 * Schedule the regen runner
	 */
	void scheduleRegenRunner() {
		if(regenRunner != -1) {
			getServer().getScheduler().cancelTask(regenRunner);
			regenRunner = -1;
		}

		// Configuration can be set to a negative frequency in order to disable
		int freq = getConfig().getInt("Regen.Frequency") * 20 * 60;
		if(freq > 0) {
			getServer().getScheduler().scheduleDelayedRepeatingTask(new RegenRunnable(this), freq, freq, true);
		}
	}

	/**
	 * Print a formatted error message
	 * 
	 * @param message Error message
	 * @param e The {@code Exception} object or null if none
	 */
	public void printError(String message, Exception e) {
		String s = "<-- Start -->\n" + "[" + getName() + " v" + getDescription().getVersion() + "] " + message + "\n"
				+ "If you've decided to post this error message, "
				+ "please include everything between the \"Start\" and \"End\" tag PLUS your config.yml and lang.yml\n"
				+ "<-- Stack trace -->\n";
		if (e != null) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			s += sw.toString() + "\n";
		} else {
			s += "None provided\n";
		}
		s += "<-- End -->";
		getLogger().critical(s);
	}

	public static boolean isMatch(Block block, String string) {
		try {
			String[] s = string.split(":");
			if(block.getId() == Integer.parseInt(s[0]) && (s.length == 1 || block.getDamage() == Integer.parseInt(s[1])))
				return true;
		} catch(Exception e) {}

		return false;
	}

	/**
	 * Will regenerate blocks when run
	 * 
	 * @author oggehej
	 */
	class RegenRunnable extends PluginTask<ObsidianBreaker> {
		public RegenRunnable(ObsidianBreaker owner) {
			super(owner);
		}

		@Override
		public void onRun(int arg0) {
			try {
				for (ConcurrentHashMap<String, BlockStatus> map : storage.damage.values()) {
					for (BlockStatus status : map.values()) {
						if (status.isModified()) {
							status.setModified(false);
						} else {
							status.setDamage(status.getDamage() - (float) getConfig().getDouble("Regen.Amount"));
							if (status.getDamage() < 0.001f) {
								getStorage().removeBlockStatus(status);
							}
						}
					}
				}
			} catch (Exception e) {
				printError("Error occured while trying to regen block (task " + getTaskId() + ")", e);
			}
		}
	}
}
