package com.bergerkiller.bukkit.nolagg;

import java.util.logging.Level;

import com.bergerkiller.bukkit.nolaggchunks.BufferedChunk;
import com.bergerkiller.bukkit.nolaggchunks.PlayerChunkBuffer;
import com.bergerkiller.bukkit.nolaggchunks.PlayerChunkLoader;

import net.minecraft.server.Chunk;
import net.minecraft.server.ChunkProviderServer;
import net.minecraft.server.Packet;

@SuppressWarnings("rawtypes")
public class ChunkOperation implements Comparable {
	private int mode = 0;
	public Chunk c;
	public ChunkOperation(Chunk chunk, Type type) {
		this.c = chunk;
		switch(type) {
		case LIGHTING : this.mode = 1; break;
		case UNLOAD : this.mode = 2; break;
		case AUTOSAVE : this.mode = 3; break;
		}
	}
	
	public static enum Type{LIGHTING, UNLOAD, AUTOSAVE};
	
	public int compareTo(Object object) {
		if (object instanceof ChunkOperation) {
			return ((ChunkOperation) object).mode - this.mode;
		} else {
			return 0;
		}
	}
	
	public int getMode() {
		return this.mode;
	}
		
	/*
	 * Whatever needs to happen when this object is being scheduled
	 */
	public void preexecute() {
		if (this.mode == 0) {
			if (NoLagg.isAddonEnabled) {
				try {
					for (PlayerChunkBuffer pcb : PlayerChunkLoader.getBuffersNear(c)) {
						pcb.get(c.x, c.z).setLocked(true);
					}
				} catch (Throwable t) {
					NoLagg.log(Level.SEVERE, "An error occured while using NoLaggChunks (update needed?):");
					t.printStackTrace();
					NoLagg.isAddonEnabled = false;
				}
			}
		}
	}
	
	/*
	 * What needs to happen when the scheduler finds this object
	 */
	public void execute(boolean isFinal) {
		//1 = fix lighting
		//2 = save - unload mode
		//3 = save - autosave mode
		//save
		ChunkProviderServer cps = (ChunkProviderServer) this.c.world.chunkProvider;
		if (this.mode == 1) {
			c.h();
			c.initLighting();
			
			if (isFinal) return; //Skip packet handling when reloading!
			
			//prepare the packets to send
			Packet[] toSend = ChunkHandler.getChunkPackets(c);
			
			//send data to clients
			//if the add-on is enabled: unlock the chunk and send
			//else just send it right away
			boolean send = true;
			if (NoLagg.isAddonEnabled) {
				try {
					for (PlayerChunkBuffer pcb : PlayerChunkLoader.getBuffersNear(c)) {
						BufferedChunk bc = pcb.get(c.x, c.z);
						bc.setLocked(false);
						bc.clear();
						for (Packet p : toSend) bc.queue(p);
					}
					send = false;
				} catch (Throwable t) {
					NoLagg.log(Level.SEVERE, "An error occured while using NoLaggChunks (update needed?):");
					t.printStackTrace();
					NoLagg.isAddonEnabled = false;
				}
			}
			if (send) {
				//send it using native coding
				Util.broadcast(c, toSend);
			}
		} else if (this.mode == 2) {
			//unload
			cps.saveChunk(this.c);
			cps.saveChunkNOP(this.c);
		} else if (this.mode == 3) {
			//autosave - detach entities!
			this.c = ChunkHandler.cloneChunk(this.c);
			if (this.c != null) {
				this.c.bukkitChunk = null;
				cps.saveChunk(this.c);
			}
		}
	}
}
