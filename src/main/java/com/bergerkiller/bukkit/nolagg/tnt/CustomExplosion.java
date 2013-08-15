package com.bergerkiller.bukkit.nolagg.tnt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.Location;

import com.bergerkiller.bukkit.common.Common;
import com.bergerkiller.bukkit.common.ToggledState;
import com.bergerkiller.bukkit.common.bases.IntVector3;
import com.bergerkiller.bukkit.common.entity.CommonEntity;
import com.bergerkiller.bukkit.common.reflection.SafeMethod;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.common.utils.MaterialUtil;
import com.bergerkiller.bukkit.common.utils.MathUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.common.wrappers.BlockInfo;

public class CustomExplosion {
	private static ExplosionSlot root;
	private static List<ExplosionLayer> explosionLayers = new ArrayList<ExplosionLayer>();
	private static List<ExplosionBlock> explosionBlocks = new ArrayList<ExplosionBlock>();
	private static Map<IntVector3, ExplosionBlock> explosionBlockMap = new HashMap<IntVector3, ExplosionBlock>();
	private static List<IntVector3> blocks = new ArrayList<IntVector3>(100);
	public static float factor = 1.0f;
	public static boolean useQuickDamageMode = false;
	public boolean fire;
	private Random h = new Random();
	private final Random worldRandom;
	private World world;
	public final Location pos;
	public Entity source;
	public float size;
	public boolean wasCanceled = false; // CraftBukkit

	static {
		explosionLayers.add(new ExplosionLayer());
		root = explosionLayers.get(0).slots.get(new IntVector3(0, 0, 0));
	}

	public CustomExplosion(org.bukkit.entity.Entity entity, Location location, float size, boolean fire) {
		this.world = location.getWorld();
		this.pos = location;
		this.source = entity;
		this.size = size;
		this.fire = fire;
		this.worldRandom = WorldUtil.getRandom(this.world);
	}

	public void doAll() {
		this.prepare();
		this.doBlocks();
	}

	public void prepare() {
		int xoff = pos.getBlockX();
		int yoff = pos.getBlockY();
		int zoff = pos.getBlockZ();

		root.sourcedamage = factor * this.size * (0.7F + worldRandom.nextFloat() * 0.6F);

		// Recursively operate on all blocks
		int i = 0;
		float damageFactor;
		int x, y, z;
		boolean hasDamage = true;
		while (hasDamage && i < explosionLayers.size()) {
			hasDamage = false;
			for (ExplosionSlot slot : explosionLayers.get(i).slotArray) {
				// generate the info for this block
				if (slot.block.initialized.set()) {
					x = slot.block.pos.x + xoff;
					y = slot.block.pos.y + yoff;
					z = slot.block.pos.z + zoff;
					slot.block.type = world.getBlockTypeIdAt(x, y, z);
					if (slot.block.type > 0) {
						slot.block.damagefactor = (MaterialUtil.getDamageResilience(slot.block.type, source) + 0.3F) * 0.3F;
						slot.block.damagefactor *= (2.0F + worldRandom.nextFloat()) / 3.0F;
					} else {
						slot.block.destroyed.set(); // prevent air getting destroyed by marking it as destroyed already
						slot.block.damagefactor = 0;
					}
				}

				// subtract damage factor
				damageFactor = slot.sourcedamage - slot.block.damagefactor;
				slot.sourcedamage = 0;
				if (damageFactor <= 0) {
					continue;
				}

				// mark the block for destroying
				if (slot.block.destroyed.set()) {
					blocks.add(slot.block.pos.add(xoff, yoff, zoff));
				}

				// one block layer further...
				if ((damageFactor -= 0.225F) <= 0.0F) {
					continue;
				}

				// force a new layer if needed
				if (slot.next == null) {
					// create a new layer
					ExplosionLayer nextLayer = new ExplosionLayer(explosionLayers.size());
					// convert to array
					for (ExplosionSlot slot2 : explosionLayers.get(explosionLayers.size() - 1).slots.values()) {
						slot2.next = slot2.nextSet.toArray(new ExplosionSlot[0]);
						slot2.nextSet = null;
					}
					explosionLayers.add(nextLayer);
				}

				// set source damage of next slots
				for (ExplosionSlot slot2 : slot.next) {
					if (damageFactor > slot2.sourcedamage) {
						slot2.sourcedamage = damageFactor;
					}
				}

				hasDamage = true;
			}
			i++;
		}

		// Restore blocks to old state
		for (ExplosionBlock block : explosionBlocks) {
			block.destroyed.clear();
			block.initialized.clear();
		}

		// =====================generate entities===================
		double tmpsize = (double) this.size * 2.0;
		double xmin = this.pos.getX() - tmpsize - 1.0;
		double ymin = this.pos.getY() - tmpsize - 1.0;
		double zmin = this.pos.getZ() - tmpsize - 1.0;

		double xmax = this.pos.getX() + tmpsize + 1.0;
		double ymax = this.pos.getY() + tmpsize + 1.0;
		double zmax = this.pos.getZ() + tmpsize + 1.0;

		List<Entity> list = WorldUtil.getEntities(this.world, this.source, xmin, ymin, zmin, xmax, ymax, zmax);
		// ==========================================================

		//Vec3D vec3d = Vec3D.a(this.posX, this.posY, this.posZ);
		double tmpX, tmpY, tmpZ;
		for (Entity bukkitEntity : list) {
			if (bukkitEntity == null || bukkitEntity.isDead()) {
				continue;
			}
			final CommonEntity<?> entity = CommonEntity.get(bukkitEntity);
			tmpX = entity.loc.getX() - this.pos.getX();
			tmpY = entity.loc.getY() - this.pos.getY();
			tmpZ = entity.loc.getZ() - this.pos.getZ();

			double length = MathUtil.lengthSquared(tmpX, tmpY, tmpZ);
			if (length >= (tmpsize * tmpsize))
				continue;
			length = Math.sqrt(length);

			// normalize
			tmpX /= length;
			tmpY /= length;
			tmpZ /= length;

			double distanceFactor = length / tmpsize;

			if (useQuickDamageMode) {
				// damage factor
				if (bukkitEntity instanceof Item) {
					damageFactor = 1.0F;
				} else if (bukkitEntity instanceof TNTPrimed) {
					damageFactor = 8.0F / 9.0F;
				} else {
					damageFactor = WorldUtil.getExplosionDamageFactor(pos, bukkitEntity);
				}
			} else {
				damageFactor = WorldUtil.getExplosionDamageFactor(pos, bukkitEntity);
			}
			double force = (1.0 - distanceFactor) * (double) damageFactor;
			double damageDone = force * (force + 1.0) * 4.0 * tmpsize + 1.0;

			// Send a damage event to Bukkit and deal the damage if not cancelled
			final EntityDamageEvent event;
			double damage;
			if (Common.MC_VERSION.equals("1.5.2")) {
				if (source == null) {
					event = new EntityDamageByBlockEvent(null, bukkitEntity, DamageCause.BLOCK_EXPLOSION, (int) damageDone);
				} else if (source instanceof TNTPrimed) {
					event = new EntityDamageByEntityEvent(source, bukkitEntity, DamageCause.BLOCK_EXPLOSION, (int) damageDone);
				} else {
					event = new EntityDamageByEntityEvent(source, bukkitEntity, DamageCause.ENTITY_EXPLOSION, (int) damageDone);
				}
				if (CommonUtil.callEvent(event).isCancelled()) {
					return;
				}
				damage = new SafeMethod<Integer>(event, "getDamage").invoke(event);
			} else {
				if (source == null) {
					event = new EntityDamageByBlockEvent(null, bukkitEntity, DamageCause.BLOCK_EXPLOSION, damageDone);
				} else if (source instanceof TNTPrimed) {
					event = new EntityDamageByEntityEvent(source, bukkitEntity, DamageCause.BLOCK_EXPLOSION, damageDone);
				} else {
					event = new EntityDamageByEntityEvent(source, bukkitEntity, DamageCause.ENTITY_EXPLOSION, damageDone);
				}
				if (CommonUtil.callEvent(event).isCancelled()) {
					return;
				}
				damage = event.getDamage();
			}
			if (!CommonUtil.callEvent(event).isCancelled()) {
				EntityUtil.damage(bukkitEntity, DamageCause.BLOCK_EXPLOSION, damage);
				entity.vel.add(tmpX * force, tmpY * force, tmpZ * force);
			}
		}
	}

	public void doBlocks() {
		// CraftBukkit start
		Location location = pos.clone();

		// generate org.bukkit Block array
		List<org.bukkit.block.Block> blockList = new ArrayList<org.bukkit.block.Block>(blocks.size());
		for (int i = blocks.size() - 1; i >= 0; i--) {
			IntVector3 cpos = blocks.get(i);
			blockList.add(this.world.getBlockAt(cpos.x, cpos.y, cpos.z));
		}

		EntityExplodeEvent event = new EntityExplodeEvent(this.source, location, blockList, 0.3F);
		CommonUtil.callEvent(event);

		blocks.clear();

		if (event.isCancelled()) {
			this.wasCanceled = true;
			return;
		} else if (NoLaggTNT.plugin.getTNTHandler().createExplosion(event)) {
			// handled by ourself
			return;
		}

		// CraftBukkit end
		int x;
		int y;
		int z;
		int type;
		List<org.bukkit.block.Block> blocks = event.blockList();
		for (int i = blocks.size() - 1; i >= 0; --i) {
			org.bukkit.block.Block block = blocks.get(i);
			x = block.getX();
			y = block.getY();
			z = block.getZ();
			type = block.getTypeId();

			double d0 = (double) ((float) x + worldRandom.nextFloat());
			double d1 = (double) ((float) y + worldRandom.nextFloat());
			double d2 = (double) ((float) z + worldRandom.nextFloat());
			double d3 = d0 - this.pos.getX();
			double d4 = d1 - this.pos.getY();
			double d5 = d2 - this.pos.getZ();
			double d6 = Math.sqrt(d3 * d3 + d4 * d4 + d5 * d5);

			d3 /= d6;
			d4 /= d6;
			d5 /= d6;
			double d7 = 0.5D / (d6 / (double) this.size + 0.1D);

			d7 *= (double) (worldRandom.nextFloat() * worldRandom.nextFloat() + 0.3F);
			d3 *= d7;
			d4 *= d7;
			d5 *= d7;

			// CraftBukkit - stop explosions from putting out fire
			if (type > 0 && type != Material.FIRE.getId()) {
				final BlockInfo info = BlockInfo.get(block);
				info.destroy(block, event.getYield());
				info.ignite(block);
			}
			if (this.fire) {
				int typeBelow = this.world.getBlockTypeIdAt(x, y - 1, z);
				if (type == 0 && MaterialUtil.ISSOLID.get(typeBelow) && this.h.nextInt(3) == 0) {
					block.setType(org.bukkit.Material.FIRE);
				}
			}
		}
	}

	private static class ExplosionBlock {
		public final IntVector3 pos;
		public final ToggledState initialized = new ToggledState();
		public final ToggledState destroyed = new ToggledState();
		public int type = 0;
		public float damagefactor;

		public ExplosionBlock(final IntVector3 pos) {
			this.pos = pos;
		}
	}

	private static class ExplosionLayer {
		private final int index;
		private final ExplosionSlot[] slotArray;
		private Map<IntVector3, ExplosionSlot> slots = new HashMap<IntVector3, ExplosionSlot>();

		public ExplosionLayer() {
			this.index = 0;
			this.slotArray = new ExplosionSlot[] { this.createSlot(0.0, 0.0, 0.0) };
		}

		public ExplosionLayer(final int index) {
			this(index, 16);
		}

		public ExplosionLayer(final int index, final int scale) {
			this.index = index;
			final int scaleminone = scale - 1;
			final double fact = (double) scaleminone / 2.0;
			int x, y, z;
			double dx, dy, dz;
			ExplosionLayer prev = explosionLayers.get(explosionLayers.size() - 1);
			for (x = 0; x < scale; ++x) {
				for (y = 0; y < scale; ++y) {
					for (z = 0; z < scale; ++z) {
						if (x == 0 || x == scaleminone || y == 0 || y == scaleminone || z == 0 || z == scaleminone) {
							dx = (double) x / fact - 1.0;
							dy = (double) y / fact - 1.0;
							dz = (double) z / fact - 1.0;
							double d = 0.3 / MathUtil.length(dx, dy, dz);
							dx *= d;
							dy *= d;
							dz *= d;
							// =============================================
							prev.createSlot(dx, dy, dz).nextSet.add(this.createSlot(dx, dy, dz));
						}
					}
				}
			}
			this.slotArray = slots.values().toArray(new ExplosionSlot[0]);
		}

		public ExplosionSlot createSlot(final double dx, final double dy, final double dz) {
			int x = MathUtil.floor(dx * (double) this.index + 0.5);
			int y = MathUtil.floor(dy * (double) this.index + 0.5);
			int z = MathUtil.floor(dz * (double) this.index + 0.5);
			IntVector3 pos = new IntVector3(x, y, z);
			ExplosionSlot slot = this.slots.get(pos);
			if (slot == null) {
				slot = new ExplosionSlot(pos);
				this.slots.put(pos, slot);
			}
			return slot;
		}
	}

	private static class ExplosionSlot {
		public ExplosionBlock block;
		public Set<ExplosionSlot> nextSet = new HashSet<ExplosionSlot>();
		public ExplosionSlot[] next;
		public float sourcedamage = 0;

		public ExplosionSlot(IntVector3 pos) {
			this.block = explosionBlockMap.get(pos);
			if (this.block == null) {
				this.block = new ExplosionBlock(pos);
				explosionBlocks.add(this.block);
				explosionBlockMap.put(pos, this.block);
			}
		}
	}
}
