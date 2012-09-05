package com.bergerkiller.bukkit.nolagg.threadcheck;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.painting.PaintingBreakEvent;
import org.bukkit.event.painting.PaintingPlaceEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.SpawnChangeEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public class Listener implements org.bukkit.event.Listener {

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerJoin(PlayerJoinEvent event) {
		ThreadCheck.check("PLAYER_JOIN");
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {
		ThreadCheck.check("PLAYER_QUIT");
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerKick(PlayerKickEvent event) {
		ThreadCheck.check("PLAYER_KICK");
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerMove(PlayerMoveEvent event) {
		ThreadCheck.check("PLAYER_MOVE");
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		ThreadCheck.check("PLAYER_TELEPORT");
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
		ThreadCheck.check("PLAYER_CHANGED_WORLD");
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onItemHeldChange(PlayerItemHeldEvent event) {
		ThreadCheck.check("PLAYER_ITEM_HELD");
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockPhysics(BlockPhysicsEvent event) {
		ThreadCheck.check("BLOCK_PHYSICS");
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockFromTo(BlockFromToEvent event) {
		ThreadCheck.check("BLOCK_FROMTO");
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockRedstoneChange(BlockRedstoneEvent event) {
		ThreadCheck.check("REDSTONE_CHANGE");
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		ThreadCheck.check("CREATURE_SPAWN");
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onItemSpawn(ItemSpawnEvent event) {
		ThreadCheck.check("ITEM_SPAWN");
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPaintingBreak(PaintingBreakEvent event) {
		ThreadCheck.check("PAINTING_BREAK");
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPaintingPlace(PaintingPlaceEvent event) {
		ThreadCheck.check("PAINTING_PLACE");
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onChunkLoad(ChunkLoadEvent event) {
		ThreadCheck.check("CHUNK_LOAD");
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onChunkUnload(ChunkUnloadEvent event) {
		ThreadCheck.check("CHUNK_UNLOAD");
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onWorldLoad(WorldLoadEvent event) {
		ThreadCheck.check("WORLD_LOAD");
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onChunkPopulate(ChunkPopulateEvent event) {
		ThreadCheck.check("CHUNK_POPULATED");
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onWorldUnload(WorldUnloadEvent event) {
		ThreadCheck.check("WORLD_UNLOAD");
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onWorldInit(WorldInitEvent event) {
		ThreadCheck.check("WORLD_INIT");
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onWorldSave(WorldSaveEvent event) {
		ThreadCheck.check("WORLD_SAVE");
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onSpawnChange(SpawnChangeEvent event) {
		ThreadCheck.check("SPAWN_CHANGE");
	}

}
