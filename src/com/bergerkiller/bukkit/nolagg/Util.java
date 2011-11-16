package com.bergerkiller.bukkit.nolagg;

import java.util.Collection;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;

import net.minecraft.server.Chunk;
import net.minecraft.server.Entity;
import net.minecraft.server.EntityExperienceOrb;
import net.minecraft.server.EntityItem;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.Packet;
import net.minecraft.server.World;
import net.minecraft.server.WorldServer;

public class Util {
	public static void broadcast(Chunk chunk, Packet packet) {
		broadcast(chunk.world, chunk.x, chunk.z, packet);
	}
	public static void broadcast(Chunk chunk, Collection<Packet> packets) {
		broadcast(chunk.world, chunk.x, chunk.z, packets);
	}
	public static void broadcast(Chunk chunk, Packet[] packets) {
		broadcast(chunk.world, chunk.x, chunk.z, packets);
	}
	public static void broadcast(World world, int cx, int cz, Packet packet) {
		for (Object player : world.players) {
			if (player instanceof EntityPlayer) {
				EntityPlayer ep = (EntityPlayer) player;
				if (isNear(ep, cx, cz)) {
					ep.netServerHandler.sendPacket(packet);
				}
			}
		}
	}
	public static void broadcast(World world, int cx, int cz, Collection<Packet> packets) {
		for (Object player : world.players) {
			if (player instanceof EntityPlayer) {
				EntityPlayer ep = (EntityPlayer) player;
				if (isNear(ep, cx, cz)) {
					for (Packet p : packets) {
						ep.netServerHandler.sendPacket(p);
					}
				}
			}
		}
	}
	public static void broadcast(World world, int cx, int cz, Packet[] packets) {
		for (Object player : world.players) {
			if (player instanceof EntityPlayer) {
				EntityPlayer ep = (EntityPlayer) player;
				if (isNear(ep, cx, cz)) {
					for (Packet p : packets) {
						ep.netServerHandler.sendPacket(p);
					}
				}
			}
		}
	}
	public static boolean isNear(Entity e, int cx, int cz) {
		return isNear((int) e.locX >> 4, (int) e.locZ >> 4, cx, cz);
	}
	public static boolean isNear(int cx1, int cz1, int cx2, int cz2) {
		cx1 -= cx2;
		if (cx1 > 15) return false;
		if (cx1 < -15) return false;
		cz1 -= cz2;
		if (cz1 > 15) return false;
		if (cz1 < -15) return false;
		return true;
	}
	
	public static List<WorldServer> getWorlds() {
		return ((CraftServer) Bukkit.getServer()).getHandle().server.worlds;
	}
	public static void fillEntities(WorldServer world, List<Item> items, List<ExperienceOrb> orbs, List<org.bukkit.entity.Entity> all) {
		for (Object obj : world.entityList) {
			if (obj instanceof EntityItem) {
				Item item = (Item) ((EntityItem) obj).getBukkitEntity();
				if (ItemHandler.isShowcased(item)) continue;
				if (items != null) items.add(item);
			} else if (obj instanceof EntityExperienceOrb) {
				ExperienceOrb orb = (ExperienceOrb) ((EntityExperienceOrb) obj).getBukkitEntity();
				if (orbs != null) orbs.add(orb);
			}
			if (all != null) all.add(((Entity) obj).getBukkitEntity());
		}
	}
}
