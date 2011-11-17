package com.bergerkiller.bukkit.nolagg;

import net.minecraft.server.Chunk;
import net.minecraft.server.EnumSkyBlock;

import org.bukkit.block.Block;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerPickupItemEvent;

public class NLPlayerListener extends PlayerListener {

	@Override
	public void onPlayerPickupItem(PlayerPickupItemEvent event) {
		if (!event.isCancelled()) {
			ItemHandler.removeSpawnedItem(event.getItem());
		}
	}
	
//	@Override
//	public void onPlayerInteract(PlayerInteractEvent event) {
//		Block b = event.getClickedBlock();
//		if (b != null) {
//			b = b.getRelative(event.getBlockFace());
//			Chunk c = ChunkHandler.getNative(b.getChunk());
//			c.world.a(EnumSkyBlock.SKY, b.getX(), b.getY(), b.getZ());
//		}
//	}
								
}
