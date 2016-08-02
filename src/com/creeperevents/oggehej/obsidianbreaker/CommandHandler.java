package com.creeperevents.oggehej.obsidianbreaker;

import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.utils.TextFormat;

/**
 * Handles the commands
 * 
 * @author oggehej
 */
public class CommandHandler extends Command {
	private ObsidianBreaker plugin;
	public CommandHandler(ObsidianBreaker instance) {
		super("obsidianbreaker");
		this.plugin = instance;
	}

	@Override
	public boolean execute(CommandSender sender, String label, String[] args) {
		if(args.length == 0) {
			sender.sendMessage(TextFormat.AQUA + " -- [" + plugin.getName() + " v" + plugin.getDescription().getVersion() + "] --");
			if(sender.hasPermission("obsidianbreaker.reload"))
				sender.sendMessage(TextFormat.GOLD + "/" + label + " reload" + TextFormat.WHITE + " - " + Locale.RELOAD_CONFIG);
		} else if(args[0].equalsIgnoreCase("reload"))
			if(sender.hasPermission("obsidianbreaker.reload")) {
				plugin.reloadConfig();
				Locale.setupLocale(plugin);
				plugin.scheduleRegenRunner();
				sender.sendMessage(Locale.CONFIG_RELOADED.toString());
			} else
				sender.sendMessage(Locale.NO_PERMISSION.toString());
		else
			sender.sendMessage(Locale.INVALID_ARGUMENTS.toString());
		return true;
	}
}
