package com.bergerkiller.bukkit.nolagg.chunks;

import java.util.Comparator;

import org.bukkit.block.BlockFace;

import com.bergerkiller.bukkit.common.utils.CommonUtil;

import net.minecraft.server.Chunk;
import net.minecraft.server.ChunkCoordIntPair;

/*
 * Warning: this comparator returns 0 when the coordinates match
 * It can return a zero value when the two objects do not equal
 * Out-of-reach coordinates are put at the back
 * Only works for Chunk, ChunkCoordIntPair and ChunkSendCommand
 */
public class ChunkCoordComparator implements Comparator<Object> {
	private static ChunkCoordComparator[] comparators = new ChunkCoordComparator[8];
	static {
		try {
			comparators[0] = new ChunkCoordComparator(BlockFace.NORTH);
			comparators[1] = new ChunkCoordComparator(BlockFace.NORTH_EAST);
			comparators[2] = new ChunkCoordComparator(BlockFace.EAST);
			comparators[3] = new ChunkCoordComparator(BlockFace.SOUTH_EAST);
			comparators[4] = new ChunkCoordComparator(BlockFace.SOUTH);
			comparators[5] = new ChunkCoordComparator(BlockFace.SOUTH_WEST);
			comparators[6] = new ChunkCoordComparator(BlockFace.WEST);
			comparators[7] = new ChunkCoordComparator(BlockFace.NORTH_WEST);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	private static ChunkCoordComparator get(final BlockFace direction) {
		switch (direction) {
		case NORTH : return comparators[0];
		case NORTH_EAST : return comparators[1];
		case EAST : return comparators[2];
		case SOUTH_EAST : return comparators[3];
		case SOUTH : return comparators[4];
		case SOUTH_WEST : return comparators[5];
		case WEST : return comparators[6];
		default : return comparators[7];
		}
	}
	public static ChunkCoordComparator get(final BlockFace direction, final ChunkCoordIntPair middle) {
		return new ChunkCoordComparator(get(direction), middle);
	}
		
	private int index = 0;
	private final BlockFace direction;
	private final int[][] indices;
	private final ChunkCoordIntPair middle;
	
	private ChunkCoordComparator(ChunkCoordComparator source, final ChunkCoordIntPair middle) {
		this.indices = source.indices;
		this.direction = source.direction;
		this.middle = middle;
	}
	private ChunkCoordComparator(final BlockFace direction) {
		this.direction = direction;
		this.indices = new int[CommonUtil.viewWidth][CommonUtil.viewWidth];
		this.middle = null;
		this.generate();
	}
	
	private void generate(int dx, int dz) {
		dx += CommonUtil.view;
		dz += CommonUtil.view;
		if (dx >= 0 && dx < this.indices.length) {
			int[] dzaint = this.indices[dx];
			if (dz >= 0 && dz < dzaint.length) {
				if (dzaint[dz] == 0) {
					dzaint[dz] = this.index++;
				}
			}
		}
	}
	private void generateLayer(final int layer, final double factor) {
		int count = (int) (layer * factor) + 1;
		//get modifiers from direction
		MoveMod[] mods = MoveMod.get(direction);
		//Get the chunk to start at
		int startx = this.direction.getModX() * layer;
		int startz = this.direction.getModZ() * layer;
		//Send starter chunk
		this.generate(startx, startz);
		//Peel
		int x1 = startx;
		int z1 = startz;
		int x2 = startx;
		int z2 = startz;
		while (--count > 0) {
			//offset the chunks
			x1 += mods[0].direction.getModX();
			z1 += mods[0].direction.getModZ();
			x2 += mods[1].direction.getModX();
			z2 += mods[1].direction.getModZ();
			//mod update
			mods[0].next(x1, z1, layer);
			mods[1].next(x2, z2, layer);
			//got till the end?
			this.generate(x1, z1);
			if (x1 == x2 && z1 == z2) {
				return;
			} else {
				this.generate(x2, z2);
			}
		}
	}
	private void generate() {		
		//main chunk
		this.generate(0, 0);
		
		final int threshold1 = 1; //to this layer full layers are sent, after half
		final int threshold2 = 5; //at this layer less than half are sent
		
		for (int layer = 1; layer <= CommonUtil.view; layer++) {
			if (layer <= threshold1) {
				this.generateLayer(layer, 4);
			} else if (layer <= threshold2) {
				this.generateLayer(layer, 2);
			} else {
				this.generateLayer(layer, 1.5);
			}
		}
		
		//end with only full layers
		for (int layer = 1; layer <= CommonUtil.view; layer++) {
			this.generateLayer(layer, 4);
		}
	}
	
	private static class MoveMod {
		private MoveMod(BlockFace direction, boolean right) {
			this.direction = direction;
			this.right = right;
		}
		
		public BlockFace direction;
		public boolean right;
		
		public void next(int dx, int dz, int limit) {
			if (Math.abs(dx) >= limit && Math.abs(dz) >= limit) {
				if (this.right) {
					if (direction == BlockFace.NORTH) {
						direction = BlockFace.EAST;
					} else if (direction == BlockFace.EAST) {
						direction = BlockFace.SOUTH;
					} else if (direction == BlockFace.SOUTH) {
						direction = BlockFace.WEST;
					} else if (direction == BlockFace.WEST) {
						direction = BlockFace.NORTH;
					}
				} else {
					if (direction == BlockFace.NORTH) {
						direction = BlockFace.WEST;
					} else if (direction == BlockFace.WEST) {
						direction = BlockFace.SOUTH;
					} else if (direction == BlockFace.SOUTH) {
						direction = BlockFace.EAST;
					} else if (direction == BlockFace.EAST) {
						direction = BlockFace.NORTH;
					}
				}
			}
		}
		
		public static MoveMod[] get(BlockFace direction) {
			MoveMod[] mods = new MoveMod[2];
			if (direction == BlockFace.NORTH) {
				mods[0] = new MoveMod(BlockFace.WEST, false);
				mods[1] = new MoveMod(BlockFace.EAST, true);
			} else if (direction == BlockFace.SOUTH) {
				mods[0] = new MoveMod(BlockFace.WEST, true);
				mods[1] = new MoveMod(BlockFace.EAST, false);
			} else if (direction == BlockFace.EAST) {
				mods[0] = new MoveMod(BlockFace.NORTH, false);
				mods[1] = new MoveMod(BlockFace.SOUTH, true);
			} else if (direction == BlockFace.WEST) {
				mods[0] = new MoveMod(BlockFace.NORTH, true);
				mods[1] = new MoveMod(BlockFace.SOUTH, false);
			} else if (direction == BlockFace.NORTH_EAST) {
				mods[0] = new MoveMod(BlockFace.WEST, false);
				mods[1] = new MoveMod(BlockFace.SOUTH, true);
			} else if (direction == BlockFace.SOUTH_EAST) {
				mods[0] = new MoveMod(BlockFace.WEST, true);
				mods[1] = new MoveMod(BlockFace.NORTH, false);
			} else if (direction == BlockFace.SOUTH_WEST) {
				mods[0] = new MoveMod(BlockFace.NORTH, true);
				mods[1] = new MoveMod(BlockFace.EAST, false);
			} else if (direction == BlockFace.NORTH_WEST) {
				mods[0] = new MoveMod(BlockFace.SOUTH, false);
				mods[1] = new MoveMod(BlockFace.EAST, true);
			}
			return mods;
		}	
	}
		
	public int getIndex(int x, int z) {
		x -= this.middle.x;
		z -= this.middle.z;
		if (Math.abs(x) > CommonUtil.view || Math.abs(z) > CommonUtil.view) {
			return Integer.MAX_VALUE;
		}
		return this.indices[x + CommonUtil.view][z + CommonUtil.view];
	}
	
	public int getIndex(Object coord) {
		if (coord instanceof ChunkCoordIntPair) {
			ChunkCoordIntPair pair = (ChunkCoordIntPair) coord;
			return getIndex(pair.x, pair.z);
		} else if (coord instanceof Chunk) {
			Chunk chunk = (Chunk) coord;
			return getIndex(chunk.x, chunk.z);
		} else if (coord instanceof ChunkSendCommand) {
			return getIndex(((ChunkSendCommand) coord).chunk);
		} else {
			return Integer.MAX_VALUE;
		}
	}
			
	@Override
	public int compare(Object coord1, Object coord2) {
		if (coord2.equals(coord1)) return 0;
		return getIndex(coord1) - getIndex(coord2);
	}

}
