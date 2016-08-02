package com.creeperevents.oggehej.obsidianbreaker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.level.Location;
import cn.nukkit.level.format.generic.BaseFullChunk;

/**
 * Storage handler
 * 
 * @author oggehej
 */
public class StorageHandler {
	private ObsidianBreaker plugin;
	StorageHandler(ObsidianBreaker instance) {
		this.plugin = instance;
	}

	// The HashMaps inside the HashMap represent chunks
	ConcurrentHashMap<String, ConcurrentHashMap<String, BlockStatus>> damage = new ConcurrentHashMap<String, ConcurrentHashMap<String, BlockStatus>>();

	/**
	 * Generate a unique {@code String} for the {@code Block} {@code Location}
	 * 
	 * @param loc Block location
	 * @return Unique string
	 */
	private String generateBlockHash(Location loc) {
		return loc.getLevel().getName() + ":" + loc.getX() + ":" + loc.getY() + ":" + loc.getZ();
	}

	/**
	 * Generate a unique {@code String} for the {@code Chunk} {@code Location}
	 * 
	 * @param loc Block location
	 * @return Unique string
	 */
	private String generateChunkHash(BaseFullChunk chunk) {
		return chunk.getProvider().getLevel().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
	}

	/**
	 * Generate a {@code Location} from the unique {@code String}
	 * 
	 * @param blockHash
	 * @return Location
	 */
	public Location generateLocation(String blockHash) {
		try {
			String[] s = blockHash.split(":");
			return new Location(Integer.parseInt(s[1]), Integer.parseInt(s[2]), Integer.parseInt(s[3]), 0,0,Server.getInstance().getLevelByName(s[0]));
		} catch(Exception e) {
			plugin.printError("Couldn't generate hash from location (hash: " + blockHash + ")", e);
			return null;
		}
	}

	/**
	 * Check if we even handle the explosion for the specified block
	 * 
	 * @param block Block to check
	 * @return Whether we're handling these kind of blocks
	 */
	@SuppressWarnings("deprecation")
	public boolean isValidBlock(Block block) {
		try {
			for(String section : plugin.getConfig().getSection("Blocks").getKeys(false)) {
				String[] s = section.split(":");
				if(block.getId() == Integer.parseInt(s[0]) && (s.length == 1 || block.getDamage() == Integer.parseInt(s[1]))) {
					return true;
				}
			}
		} catch(Exception e) {}

		return false;
	}

	/**
	 * Get the total durability of the block type
	 * 
	 * @param block Block
	 * @return Total durability of block type
	 * @throws UnknownBlockTypeException There's no durability data for this block type
	 */
	@SuppressWarnings("deprecation")
	float getTotalDurabilityFromConfig(Block block) throws UnknownBlockTypeException {
		try {
			for(String section : plugin.getConfig().getSection("Blocks").getKeys(false)) {
				String[] s = section.split(":");
				if(block.getId() == Integer.parseInt(s[0]) && (s.length == 1 || block.getDamage() == Integer.parseInt(s[1]))) {
					return (float) plugin.getConfig().getDouble("Blocks." + section);
				}
			}
		} catch(Exception e) {}

		throw new UnknownBlockTypeException();
	}

	/**
	 * Add damage to the specified block
	 * 
	 * @param block Block
	 * @param addDamage Damage to add
	 * @return Return true if the durability left is <= 0
	 * @throws UnknownBlockTypeException There's no durability data for this block type
	 */
	public boolean addDamage(Block block, float addDamage) throws UnknownBlockTypeException {
		BlockStatus status = getBlockStatus(block, false);
		if(addDamage <= 0) {
			return false;
		} else if(status == null) {
			if(getTotalDurabilityFromConfig(block) <= 0){
				return false;
			}
			status = getBlockStatus(block, true);
			if(status == null){
				throw new UnknownBlockTypeException();
			}
		}

		status.setDamage(status.getDamage() + addDamage);
		if(status.getDamage() >= status.getTotalDurability() - 0.001f) {
			removeBlockStatus(status);
			return true;
		} else{
			return false;
		}
	}

	/**
	 * Get the {@code BlockStatus} object of the block
	 * 
	 * @param block The block
	 * @param create Whether we should create the object if it doesn't exist
	 * @return The {@code BlockStatus}, or null if it doesn't exist and create==false OR invalid block
	 */
	BlockStatus getBlockStatus(Block block, boolean create) {
		try {
			String chunkHash = generateChunkHash(block.getLocation().getLevel().getChunk(block.getLocation().getFloorX()>>4, block.getLocation().getFloorZ()>>4));
			Map<String, BlockStatus> chunkMap = null;

			if(damage.containsKey(chunkHash))
				chunkMap = damage.get(chunkHash);
			else if(create) {
				damage.put(chunkHash, new ConcurrentHashMap<String, BlockStatus>());
				chunkMap = damage.get(chunkHash);
			} else
				return null;

			String blockHash = generateBlockHash(block.getLocation());

			if(chunkMap.containsKey(blockHash))
				return chunkMap.get(blockHash);
			else if(create) {
				chunkMap.put(blockHash, new BlockStatus(blockHash, chunkHash, getTotalDurabilityFromConfig(block)));
				return chunkMap.get(blockHash);
			} else
				return null;
		} catch (UnknownBlockTypeException e) {
			return null;
		}
	}

	/**
	 * Remove the {@code BlockStatus} object from the map
	 * 
	 * @param blockStatus
	 */
	void removeBlockStatus(BlockStatus blockStatus) {
		String chunkHash = blockStatus.getChunkHash();
		Map<String, BlockStatus> chunk = damage.get(chunkHash);
		if(chunk == null)
			return;

		chunk.remove(blockStatus.getBlockHash());

		if(chunk.isEmpty())
			damage.remove(chunkHash);
	}


	/**
	 * Get the blocks within a certain radius of given chunk
	 * <p>Currently not used by this plugin
	 * 
	 * @param chunk Centre chunk
	 * @param chunkRadius Chunk radius
	 * @return List of blocks represented in BlockStatus
	 */
	public List<BlockStatus> getNearbyBlocks(BaseFullChunk chunk, int chunkRadius) {
		ArrayList<BlockStatus> status = new ArrayList<BlockStatus>();

		for(int x = chunk.getX() - chunkRadius; x <= chunk.getX() + chunkRadius; x++) {
			for(int z = chunk.getZ() - chunkRadius; z <= chunk.getZ() + chunkRadius; z++) {
				String hash = chunk.getProvider().getLevel().getName() + ":" + x + ":" + z;
				Map<String, BlockStatus> map = damage.get(hash);
				if(map != null) {
					for(BlockStatus s : map.values())
						status.add(s);
				}
			}
		}

		return status;
	}
}
