package com.creeperevents.oggehej.obsidianbreaker;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import cn.nukkit.block.Block;
import cn.nukkit.block.BlockAir;
import cn.nukkit.block.BlockLiquid;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.entity.EntityExplodeEvent;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemBlock;
import cn.nukkit.level.Location;
import cn.nukkit.level.Position;
import cn.nukkit.math.Vector3;
import cn.nukkit.utils.BlockIterator;

/**
 * Block listener class
 * 
 * @author oggehej
 */
public class BlockListener implements Listener {
	private ObsidianBreaker plugin;
	BlockListener(ObsidianBreaker instance) {
		this.plugin = instance;
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onEntityExplode(EntityExplodeEvent event) {
		if(event.getEntity() == null)
			return;

		Iterator<Block> it = event.getBlockList().iterator();
		while(it.hasNext()) {
			Block block = it.next();
			if(plugin.getStorage().isValidBlock(block))
				it.remove();
		}

		float unalteredRadius = (float) plugin.getConfig().getDouble("BlastRadius");
		int radius = (int) Math.ceil(unalteredRadius);
		Position detonatorLoc = event.getPosition();
		for (int x = -radius; x <= radius; x++)
			for (int y = -radius; y <= radius; y++)
				for (int z = -radius; z <= radius; z++) {
					Location targetLoc = new Location(detonatorLoc.getX() + x, detonatorLoc.getY() + y, detonatorLoc.getZ() + z, 0,0, detonatorLoc.getLevel());;
					if (detonatorLoc.distance(targetLoc) <= unalteredRadius)
						explodeBlock(targetLoc, detonatorLoc, event.getEntity());
				}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onBlockBreak(BlockBreakEvent event) {
		// Remove BlockStatus if block is broken
		StorageHandler storage = plugin.getStorage();
		Block block = event.getBlock();
		BlockStatus status = storage.getBlockStatus(block, false);
		if(status != null) {
			storage.removeBlockStatus(status);
		}
	}

	/**
	 * Explode a block
	 * 
	 * @param loc {@code Location} of the block
	 * @param source {@code Location} of the explosion source
	 * @param explosive The {@code EntityType} of the explosion cause
	 */
	void explodeBlock(Location loc, Position source, Entity explosive) {
		if(!loc.getLevel().getChunk(loc.getFloorX() >>4, loc.getFloorZ()>>4).isLoaded() || (loc.getFloorY() == 0 && plugin.getConfig().getBoolean("VoidProtector")))
			return;
		Block block = loc.getLevel().getBlock(loc);
		if(plugin.getStorage().isValidBlock(block)) {
			try {
				float rawDamage = explosive == null ? 1 : (float) plugin.getConfig().getDouble("ExplosionSources." + explosive.getName());
				if(plugin.getStorage().addDamage(block, rawDamage)) {
					@SuppressWarnings("unchecked")
					List<String> list = (List<String>) plugin.getConfig().getList("Drops.DontDrop");
					for(Object section : list) {
						if(section instanceof Integer)
							section = Integer.toString((Integer) section);

						String[] s = ((String) section).split(":");
						if(block.getId() == Integer.parseInt(s[0]) && (s.length == 1 || block.getDamage() == Integer.parseInt((s[1])))) {
							block.getLevel().setBlockIdAt(block.getFloorX(), block.getFloorY(), block.getFloorZ(), Item.AIR);
							return;
						}
					}
					if(new Random().nextInt(100) + 1 >= plugin.getConfig().getInt("Drops.DropChance")){
						block.getLevel().setBlockIdAt(block.getFloorX(), block.getFloorY(), block.getFloorZ(), Item.AIR);
					}else{
						block.getDrops(new ItemBlock(new BlockAir()));
					block.getLevel().setBlockIdAt(block.getFloorX(), block.getFloorY(), block.getFloorZ(), Item.AIR);
					}
				}
			} catch (UnknownBlockTypeException e) {}
		}
	}
}
