package com.bergerkiller.bukkit.nolagg.lighting;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;

import net.minecraft.server.Block;
import net.minecraft.server.Chunk;
import net.minecraft.server.ChunkCoordIntPair;
import net.minecraft.server.ChunkSection;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.EnumSkyBlock;
import net.minecraft.server.WorldServer;

import com.bergerkiller.bukkit.common.AsyncTask;
import com.bergerkiller.bukkit.common.Operation;
import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.common.utils.MathUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.nolagg.NoLagg;

public class LightingFixThread extends AsyncTask {

	private static AsyncTask task;
	private static LinkedHashSet<Chunk> toFix = new LinkedHashSet<Chunk>();
	public static int getPendingSize() {
		synchronized (toFix) {
			return toFix.size();
		}
	}
	public static void fix(org.bukkit.Chunk chunk) {
		fix(WorldUtil.getNative(chunk));
	}
	public static void fix(Chunk chunk) {
		synchronized (toFix) {
			toFix.add(chunk);
			if (task == null) {
				task = new LightingFixThread().start(true);
			}
		}
		//get rid of current sending requests for this chunk
		ChunkCoordIntPair p = new ChunkCoordIntPair(chunk.x, chunk.z);
		for (EntityPlayer ep : CommonUtil.getOnlinePlayers()) {
			if (ep != null && ep.world == chunk.world && ep.chunkCoordIntPairQueue != null) {
				if (EntityUtil.isNearChunk(ep, chunk.x, chunk.z, CommonUtil.view)) {
					ep.chunkCoordIntPairQueue.remove(p);
				}
			}
		}
	}
	public static boolean isFixing(org.bukkit.Chunk chunk) {
		return isFixing(WorldUtil.getNative(chunk));
	}
	public static boolean isFixing(Chunk chunk) {
		synchronized (toFix) {
			return toFix.contains(chunk);
		}
	}
	public static void finish() {
		executeAll();
		AsyncTask.stop(task);
		task = null;
	}		

	@Override
	public void run() {
		if (!executeAll()) {
			this.stop();
			task = null;
		}
	}

	private static List<FixOperation> next = new ArrayList<FixOperation>();
	private static boolean executeAll() {
		synchronized (next) {
			synchronized (toFix) {
				if (toFix.isEmpty()) return false;
				for (Chunk c : toFix) {
					next.add(new FixOperation(c));
				}
			}
			
			for (FixOperation fix : next) {
				fix.prepare();
			}
			
			final int redocount = 4;
			for (int i = 0; i < redocount; i++) {
				for (FixOperation fix : next) {
					fix.smooth(EnumSkyBlock.SKY);
					fix.smooth(EnumSkyBlock.BLOCK);
				}
			}
			
			
			for (FixOperation fix : next) {
				fix.finish();
			}
			
			//remove
			synchronized (toFix) {
				for (FixOperation fix : next) {
					toFix.remove(fix.chunk);
				}
			}
						
			next.clear();
			return true;
		}
	}

	private static class FixOperation {
		private final Chunk chunk;
		private final WorldServer world;
		public final ChunkSection[] sections;
		public FixOperation(final Chunk chunk) {
			this.chunk = chunk;
			this.world = (WorldServer) chunk.world;
			this.sections = this.chunk.i();
		}
		private Chunk getChunk(final int x, final int z) {
			return this.world.chunkProviderServer.chunks.get(x, z);
		}

		private int getLightLevel(EnumSkyBlock mode, int x, final int y, int z) {
			if (y <= 0 || y >= this.chunk.world.getHeight()) return 0;
			if (x >= 0 && z >= 0 && x < 16 && z < 16) {
				return this.chunk.getBrightness(mode, x, y, z);
			}
			Chunk chunk = this.getChunk(this.chunk.x + (x >> 4), this.chunk.z + (z >> 4));
			if (chunk == null) return 0;
			x -= (chunk.x - this.chunk.x) << 4;
			z -= (chunk.z - this.chunk.z) << 4;
			return chunk.getBrightness(mode, x, y, z);
		}	

		public void smooth(EnumSkyBlock mode) {
			int x, y, z, typeid, light, factor;
			int loops = 0;
			boolean haserror = true;
			int lasterrx, lasterry, lasterrz;
			lasterrx = lasterry = lasterrz = 0;
			while (haserror) {
				if (loops > 100) {
					lasterrx += this.chunk.x << 4;
					lasterrz += this.chunk.z << 4;
					StringBuilder msg = new StringBuilder();
					msg.append("Failed to fix all " + mode.toString().toLowerCase() + " lighting at [");
					msg.append(lasterrx).append('/').append(lasterry);
					msg.append('/').append(lasterrz).append(']');
					NoLaggLighting.plugin.log(Level.WARNING, msg.toString());
					break;
				}
				haserror = false;
				loops++;
				int inity;
				for (x = 0; x < 16; x++) {
					for (z = 0; z < 16; z++) {
						if (mode == EnumSkyBlock.SKY) {
							inity = this.chunk.b(x, z);
							if (inity >= this.world.getHeight()) {
								inity = this.world.getHeight() - 1;
							}
						} else {
							inity = this.world.getHeight() - 1;
						}
						for (y = inity; y > 0; --y) {
							if (this.chunk.i()[y >> 4] == null) {
								continue;
							}
							typeid = this.chunk.getTypeId(x, y, z);
							if (!Block.n[typeid]) {
								factor = Math.max(1, Block.lightBlock[typeid]);
								light = this.chunk.getBrightness(mode, x, y, z);
								//actual editing here
								int newlight = light + factor;
								//obtain lighting from all sides
								newlight = Math.max(newlight, getLightLevel(mode, x - 1, y, z));
								newlight = Math.max(newlight, getLightLevel(mode, x + 1, y, z));
								newlight = Math.max(newlight, getLightLevel(mode, x, y, z - 1));
								newlight = Math.max(newlight, getLightLevel(mode, x, y, z + 1));
								newlight = Math.max(newlight, getLightLevel(mode, x, y - 1, z));
								newlight = Math.max(newlight, getLightLevel(mode, x, y + 1, z));
								newlight -= factor;
								//pick the highest value
								if (newlight > light) {
									ChunkSection chunksection = this.chunk.i()[y >> 4];
									if (chunksection != null) {
										if (mode == EnumSkyBlock.SKY) {
											chunksection.c(x, y & 0xf, z, newlight);
										} else {
											chunksection.d(x, y & 0xf, z, newlight);
										}
										lasterrx = x;
										lasterry = y;
										lasterrz = z;
										haserror = true;
									}
								}
							}
						}
					}
				}
			}
		}

		public void prepare() {
			int x, y, z;
			int slicesLight = this.chunk.h();
			int maxheight = this.world.getHeight() - 1;
			ChunkSection sec;
			//initial calculation of sky light
			for (x = 0; x < 16; x++) {
				for (z = 0; z < 16; z++) {
					int ll = 15;
					int kk = slicesLight + 15;

					for (y = maxheight; y >= 0; y--) {						
						sec = this.sections[y >> 4];
						if (sec == null) continue;

						if (ll <= 0 || --kk <= 0 || (ll -= this.chunk.b(x, y, z)) <= 0) {
							ll = 0;
						}
						sec.c(x, y & 0xf, z, ll);
						sec.d(x, y & 0xf, z, Block.lightEmission[sec.a(x, y & 0xf, z)]);
					}
				}
			}
		}
		
		public void finish() {
			//transfer new block data
			final ChunkCoordIntPair pair = new ChunkCoordIntPair(chunk.x, chunk.z);
			new Task(NoLagg.plugin) {
				public void run() {
					new Operation() {
						public void run() {
							this.doPlayers(world);
						}
						@SuppressWarnings("unchecked")
						public void handle(EntityPlayer ep) {
							if (Math.abs(pair.x - MathUtil.locToChunk(ep.locX)) > CommonUtil.view) return;
							if (Math.abs(pair.z - MathUtil.locToChunk(ep.locZ)) > CommonUtil.view) return;
							ep.chunkCoordIntPairQueue.add(0, pair);
						}
					};
				}
			}.start();
		}

	}

}
