package com.bergerkiller.bukkit.nolagg.lighting;

import com.bergerkiller.bukkit.common.bases.NibbleArrayBase;
import com.bergerkiller.bukkit.common.reflection.classes.ChunkSectionRef;
import com.bergerkiller.bukkit.common.utils.MaterialUtil;

public class LightingChunkSection {
	public final LightingChunk owner;
	public final NibbleArrayBase skyLight;
	public final NibbleArrayBase blockLight;
	public final NibbleArrayBase opacity;

	public LightingChunkSection(LightingChunk owner, byte[] skyLight, byte[] blockLight, byte[] blockIds) {
		this.owner = owner;
		// Block light
		this.blockLight = new NibbleArrayBase(blockLight, 4);
		// Sky light
		this.skyLight = skyLight == null ? null : new NibbleArrayBase(skyLight, 4);
		// Fill opacity and initial block lighting values
		this.opacity = new NibbleArrayBase(4096, 4);
		int x, y, z, typeId, opacity, maxlight, light, blockEmission;
		boolean withinBounds;
		for (x = 0; x < 16; x++) {
			for (z = 0; z < 16; z++) {
				withinBounds = x >= owner.startX && x <= owner.endX && z >= owner.startZ && z <= owner.endZ;
				for (y = 0; y < 16; y++) {
					typeId = readBlockId(blockIds, x, y, z);
					opacity = MaterialUtil.OPACITY.get(typeId) & 0xf;
					blockEmission = MaterialUtil.EMISSION.get(typeId);
					if (withinBounds) {
						// Within bounds: Regenerate (skylight is regenerated elsewhere)
						this.opacity.set(x, y, z, opacity);
						this.blockLight.set(x, y, z, blockEmission);
					} else {
						// Outside bounds: only fix blatant errors in the light
						maxlight = 15 - opacity;

						// Sky light
						if (this.skyLight != null) {
							light = this.skyLight.get(x, y, z);
							if (light > maxlight) {
								this.skyLight.set(x, y, z, 0);
							}
						}

						// Block light (take in account light emission values)
						if (blockEmission > maxlight) {
							maxlight = blockEmission;
						}
						light = this.blockLight.get(x, y, z);
						if (light > maxlight) {
							this.blockLight.set(x, y, z, 0);
						}
					}
				}
			}
		}
	}

	private static int readBlockId(byte[] blockIds, int x, int y, int z) {
		return blockIds[y << 8 | z << 4 | x] & 255;
	}

	/**
	 * Sets a light level
	 * 
	 * @param x - coordinate
	 * @param y - coordinate
	 * @param z - coordinate
	 * @param skyLight state: True for skyLight, False for blockLight
	 * @param value of light to set to
	 */
	public void setLight(boolean skyLight, int x, int y, int z, int value) {
		(skyLight ? this.skyLight : this.blockLight).set(x, y, z, value);
	}

	/**
	 * Gets a light level
	 * 
	 * @param x - coordinate
	 * @param y - coordinate
	 * @param z - coordinate
	 * @param skyLight state: True for skyLight, False for blockLight
	 * @return the light level
	 */
	public int getLight(boolean skyLight, int x, int y, int z) {
		return (skyLight? this.skyLight : this.blockLight).get(x, y, z);
	}

	/**
	 * Applies the lighting information to a chunk section
	 * 
	 * @param chunkSection to save to
	 */
	public void saveToChunk(Object chunkSection) {
		ChunkSectionRef.blockLight.set(chunkSection, blockLight.toHandle());
		if (skyLight != null) {
			ChunkSectionRef.skyLight.set(chunkSection, skyLight.toHandle());
		}
	}
}
