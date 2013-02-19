package com.bergerkiller.bukkit.nolagg.chunks;

import java.util.Comparator;

import org.bukkit.Chunk;
import org.bukkit.block.BlockFace;

import com.bergerkiller.bukkit.common.bases.IntVector2;
import com.bergerkiller.bukkit.common.conversion.Conversion;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.FaceUtil;

/*
 * Warning: this comparator returns 0 when the coordinates match
 * It can return a zero value when the two objects do not equal
 * Out-of-reach coordinates are put at the back
 * Only works for Chunk, ChunkCoordIntPair and ChunkSendCommand
 */
public class ChunkCoordComparator implements Comparator<Object> {
	private static ChunkCoordComparator[] comparators = new ChunkCoordComparator[8];

	public static void init(ChunkSendMode mode) {
		try {
			for (int i = 0; i < 8; i++) {
				comparators[i] = new ChunkCoordComparator(FaceUtil.notchToFace(i));
				switch (mode) {
					case SLOPE : 
						comparators[i].generateSlope();
						break;
					default :
						comparators[i].generateSpiral();
						break;
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public static ChunkCoordComparator get(final BlockFace direction, final IntVector2 middle) {
		return new ChunkCoordComparator(comparators[FaceUtil.faceToNotch(direction)], middle);
	}

	private int index = 0;
	private final BlockFace direction;
	private final int[][] indices;
	private final IntVector2 middle;

	private ChunkCoordComparator(ChunkCoordComparator source, final IntVector2 middle) {
		this.indices = source.indices;
		this.direction = source.direction;
		this.middle = middle;
	}

	private ChunkCoordComparator(final BlockFace direction) {
		this.direction = direction;
		this.indices = new int[CommonUtil.VIEWWIDTH][CommonUtil.VIEWWIDTH];
		this.middle = null;
	}

	private void generate(int dx, int dz) {
		dx += CommonUtil.VIEW;
		dz += CommonUtil.VIEW;
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
		// get modifiers from direction
		MoveMod[] mods = MoveMod.get(direction);
		// Get the chunk to start at
		int startx = this.direction.getModX() * layer;
		int startz = this.direction.getModZ() * layer;
		// Send starter chunk
		this.generate(startx, startz);
		// Peel
		int x1 = startx;
		int z1 = startz;
		int x2 = startx;
		int z2 = startz;
		while (--count > 0) {
			// offset the chunks
			x1 += mods[0].direction.getModX();
			z1 += mods[0].direction.getModZ();
			x2 += mods[1].direction.getModX();
			z2 += mods[1].direction.getModZ();
			// mod update
			mods[0].next(x1, z1, layer);
			mods[1].next(x2, z2, layer);
			// got till the end?
			this.generate(x1, z1);
			if (x1 == x2 && z1 == z2) {
				return;
			} else {
				this.generate(x2, z2);
			}
		}
	}

	private void generateSpiral() {
		// main chunk
		this.generate(0, 0);
		// Only full layers
		for (int layer = 1; layer <= CommonUtil.VIEW; layer++) {
			this.generateLayer(layer, 4);
		}
	}

	private void generateSlope() {
		// main chunk
		this.generate(0, 0);

		// to this layer full layers are sent, after  half
		final int threshold1 = 2;
		// at this layer less than half are sent
		final int threshold2 = 5;

		for (int layer = 1; layer <= CommonUtil.VIEW; layer++) {
			if (layer <= threshold1) {
				this.generateLayer(layer, 4);
			} else if (layer <= threshold2) {
				this.generateLayer(layer, 2);
			} else {
				this.generateLayer(layer, 1.5);
			}
		}

		// end with only full layers
		for (int layer = 1; layer <= CommonUtil.VIEW; layer++) {
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
				this.direction = FaceUtil.rotate(this.direction, right ? 2 : -2);
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
		if (Math.abs(x) > CommonUtil.VIEW || Math.abs(z) > CommonUtil.VIEW) {
			return Integer.MAX_VALUE;
		}
		return this.indices[x + CommonUtil.VIEW][z + CommonUtil.VIEW];
	}

	public int getIndex(Object coord) {
		if (coord instanceof Chunk) {
			Chunk chunk = (Chunk) coord;
			return getIndex(chunk.getX(), chunk.getZ());
		} else if (coord instanceof ChunkSendCommand) {
			ChunkSendCommand cmd = (ChunkSendCommand) coord;
			return getIndex(cmd.chunk.getX(), cmd.chunk.getZ());
		} else {
			final IntVector2 i2coord = Conversion.toIntVector2.convert(coord);
			if (coord != null) {
				return getIndex(i2coord.x, i2coord.z);
			} else {
				return Integer.MAX_VALUE;
			}
		}
	}

	@Override
	public int compare(Object coord1, Object coord2) {
		if (coord2.equals(coord1)) {
			return 0;
		}
		return getIndex(coord1) - getIndex(coord2);
	}
}
