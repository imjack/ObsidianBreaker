package com.creeperevents.oggehej.obsidianbreaker;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.event.player.PlayerTeleportEvent;
import cn.nukkit.level.Location;

/**
 * Listener for player actions
 * 
 * @author oggehej
 */
public class PlayerListener implements Listener {
	private ObsidianBreaker plugin;
	PlayerListener(ObsidianBreaker instance) {
		this.plugin = instance;
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if(event.getAction() == PlayerInteractEvent.RIGHT_CLICK_BLOCK) {
			Player player = event.getPlayer();
			String[] s = plugin.getConfig().getString("DurabilityChecker").split(":");
			if(player.getInventory().getItemInHand().getId() == Integer.parseInt(s[0])
					&& (s.length == 1 || player.getInventory().getItemInHand().getDamage() == Integer.parseInt(s[1]))
					&& player.hasPermission("obsidianbreaker.test")) {
				try {
					Block block = event.getBlock();
					BlockStatus status = plugin.getStorage().getBlockStatus(block, false);
					float totalDurability;
					float remainingDurability;
					if(status == null) {
						totalDurability = plugin.getStorage().getTotalDurabilityFromConfig(block);
						remainingDurability = totalDurability;
					} else {
						totalDurability = status.getTotalDurability();
						remainingDurability = totalDurability - status.getDamage();
					}

					if(block.getLocation().getFloorY() == 0 && plugin.getConfig().getBoolean("VoidProtector")) {
						player.sendMessage(Locale.DURABILITY + " " + Locale.UNLIMITED_VOID);
					} else if(totalDurability > 0) {
						// Round numbers for fancy output
						DecimalFormat format = new DecimalFormat("##.##");
						DecimalFormatSymbols symbol = new DecimalFormatSymbols();
						symbol.setDecimalSeparator('.');
						format.setDecimalFormatSymbols(symbol);

						String durability = format.format(totalDurability);
						String durabilityLeft = format.format(remainingDurability);
						player.sendMessage(Locale.DURABILITY + " " + Locale.DURABILITY_LEFT.toString().replace("{0}", durabilityLeft).replace("{1}", durability));
					} else
						player.sendMessage(Locale.DURABILITY + " " + Locale.UNLIMITED);
				} catch (UnknownBlockTypeException e) {}
			}
		}
	}
}
