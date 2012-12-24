package com.bergerkiller.bukkit.nolagg.tnt;

import net.minecraft.server.v1_4_5.AxisAlignedBB;
import net.minecraft.server.v1_4_5.Block;
import net.minecraft.server.v1_4_5.DamageSource;
import net.minecraft.server.v1_4_5.Entity;
import net.minecraft.server.v1_4_5.EntityItem;
import net.minecraft.server.v1_4_5.EntityTNTPrimed;
import net.minecraft.server.v1_4_5.Vec3D;
import net.minecraft.server.v1_4_5.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

// CraftBukkit start
import org.bukkit.Bukkit;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.Location;
// CraftBukkit end

import com.bergerkiller.bukkit.common.bases.IntVector3;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.MaterialUtil;
import com.bergerkiller.bukkit.common.utils.MathUtil;
import com.bergerkiller.bukkit.common.utils.NativeUtil;

public class CustomExplosion {
	public boolean fire;
	private Random h = new Random();
	private World world;
	public double posX;
	public double posY;
	public double posZ;
	public Entity source;
	public float size;
	private static List<IntVector3> blocks = new ArrayList<IntVector3>(100);

	public boolean wasCanceled = false; // CraftBukkit

	public CustomExplosion(org.bukkit.entity.Entity entity, Location location, float size, boolean fire) {
		this.world = NativeUtil.getNative(location.getWorld());
		this.posX = location.getX();
		this.posY = location.getY();
		this.posZ = location.getZ();
		this.source = NativeUtil.getNative(entity);
		this.size = size;
		this.fire = fire;
	}

	public void doAll() {
		this.prepare();
		this.doBlocks();
	}

	private static ExplosionSlot root;
	private static List<ExplosionLayer> explosionLayers = new ArrayList<ExplosionLayer>();
	private static List<ExplosionBlock> explosionBlocks = new ArrayList<ExplosionBlock>();
	private static Map<IntVector3, ExplosionBlock> explosionBlockMap = new HashMap<IntVector3, ExplosionBlock>();
	static {
		explosionLayers.add(new ExplosionLayer());
		root = explosionLayers.get(0).slots.get(new IntVector3(0, 0, 0));
	}

	private static class ExplosionBlock {
		public ExplosionBlock(final IntVector3 pos) {
			this.pos = pos;
		}

		public final IntVector3 pos;
		public boolean isSet = false;
		public boolean destroy = false;
		public int type = 0;
		public float damagefactor;
	}

	public static float factor = 1.0f;

	private static class ExplosionLayer {
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

		private final int index;
		private final ExplosionSlot[] slotArray;
		private Map<IntVector3, ExplosionSlot> slots = new HashMap<IntVector3, ExplosionSlot>();

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
		public ExplosionSlot(IntVector3 pos) {
			this.block = explosionBlockMap.get(pos);
			if (this.block == null) {
				this.block = new ExplosionBlock(pos);
				explosionBlocks.add(this.block);
				explosionBlockMap.put(pos, this.block);
			}
		}

		public ExplosionBlock block;
		public Set<ExplosionSlot> nextSet = new HashSet<ExplosionSlot>();
		public ExplosionSlot[] next;
		public float sourcedamage = 0;
	}

	public static boolean useQuickDamageMode = false;

	@SuppressWarnings("unchecked")
	public void prepare() {
		int xoff = MathUtil.floor(this.posX);
		int yoff = MathUtil.floor(this.posY);
		int zoff = MathUtil.floor(this.posZ);

		root.sourcedamage = 0.05f * factor * this.size * (0.7F + this.world.random.nextFloat() * 0.6F);

		// recursively operate on all blocks
		int i = 0;
		float damageFactor;
		int x, y, z;
		boolean hasDamage = true;
		while (hasDamage && i < explosionLayers.size()) {
			hasDamage = false;
			for (ExplosionSlot slot : explosionLayers.get(i).slotArray) {
				if (!slot.block.isSet) {
					// generate the info for this block
					x = slot.block.pos.x + xoff;
					y = slot.block.pos.y + yoff;
					z = slot.block.pos.z + zoff;
					slot.block.type = world.getTypeId(x, y, z);
					if (slot.block.type > 0) {
						slot.block.damagefactor = (Block.byId[slot.block.type].a(source) + 0.3F) * 0.3F;
						slot.block.damagefactor *= (2.0F + this.world.random.nextFloat()) / 3.0F;
					} else {
						slot.block.destroy = true; // prevent air getting
													// destroyed
						slot.block.damagefactor = 0;
					}
					slot.block.isSet = true;
				}

				// subtract damage factor
				damageFactor = slot.sourcedamage - slot.block.damagefactor;
				slot.sourcedamage = 0;
				if (damageFactor <= 0)
					continue;
				if (!slot.block.destroy) {
					slot.block.destroy = true;
					x = slot.block.pos.x + xoff;
					y = slot.block.pos.y + yoff;
					z = slot.block.pos.z + zoff;
					blocks.add(new IntVector3(x, y, z));
				}

				// one block layer further...
				if ((damageFactor -= 0.225) <= 0.0F)
					continue;

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

		double tmpX;
		double tmpY;
		double tmpZ;

		// =====================generate entities===================
		double tmpsize = (double) this.size * 2.0;
		double xmin = this.posX - tmpsize - 1.0;
		double ymin = this.posY - tmpsize - 1.0;
		double zmin = this.posZ - tmpsize - 1.0;

		double xmax = this.posX + tmpsize + 1.0;
		double ymax = this.posY + tmpsize + 1.0;
		double zmax = this.posZ + tmpsize + 1.0;

		List<Entity> list = this.world.getEntities(this.source, AxisAlignedBB.a(xmin, ymin, zmin, xmax, ymax, zmax));
		// ==========================================================

		Vec3D vec3d = Vec3D.a(this.posX, this.posY, this.posZ);

		for (i = 0; i < list.size(); i++) {
			Entity entity = list.get(i);
			if (entity == null || entity.dead)
				continue;
			tmpX = entity.locX - this.posX;
			tmpY = entity.locY - this.posY;
			tmpZ = entity.locZ - this.posZ;

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
				if (entity instanceof EntityItem) {
					damageFactor = 1.0F;
				} else if (entity instanceof EntityTNTPrimed) {
					damageFactor = 8.0F / 9.0F;
				} else {
					damageFactor = this.world.a(vec3d, entity.boundingBox);
				}
			} else {
				damageFactor = this.world.a(vec3d, entity.boundingBox);
			}
			double force = (1.0 - distanceFactor) * (double) damageFactor;

			// CraftBukkit start - explosion damage hook
			org.bukkit.entity.Entity damagee = entity.getBukkitEntity();
			int damageDone = (int) (force * (force + 1.0) * 4.0D * tmpsize + 1.0D);

			if (this.source == null) { // Block explosion
				// TODO: get the x/y/z of the tnt block?
				// does this even get called ever? @see EntityTNTPrimed - not
				// BlockTNT or whatever
				EntityDamageByBlockEvent event = new EntityDamageByBlockEvent(null, damagee, EntityDamageEvent.DamageCause.BLOCK_EXPLOSION, damageDone);
				Bukkit.getPluginManager().callEvent(event);

				if (!event.isCancelled()) {
					entity.damageEntity(DamageSource.EXPLOSION, event.getDamage());
					entity.motX += tmpX * force;
					entity.motY += tmpY * force;
					entity.motZ += tmpZ * force;
				}
			} else {
				final org.bukkit.entity.Entity damager = this.source.getBukkitEntity();
				final EntityDamageEvent.DamageCause damageCause;
				if (damager instanceof TNTPrimed) {
					damageCause = EntityDamageEvent.DamageCause.BLOCK_EXPLOSION;
				} else {
					damageCause = EntityDamageEvent.DamageCause.ENTITY_EXPLOSION;
				}
				EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(damager, damagee, damageCause, damageDone);
				Bukkit.getPluginManager().callEvent(event);

				if (!event.isCancelled()) {
					entity.damageEntity(DamageSource.EXPLOSION, event.getDamage());

					entity.motX += tmpX * force;
					entity.motY += tmpY * force;
					entity.motZ += tmpZ * force;
				}
			}
			// CraftBukkit end
		}

		// Restore blocks to old state
		for (ExplosionBlock block : explosionBlocks) {
			block.destroy = false;
			block.isSet = false;
		}

	}

	public void doBlocks() {
		// CraftBukkit start
		org.bukkit.World bworld = this.world.getWorld();
		org.bukkit.entity.Entity explode = this.source == null ? null : this.source.getBukkitEntity();
		Location location = new Location(bworld, this.posX, this.posY, this.posZ);

		// generate org.bukkit Block array
		List<org.bukkit.block.Block> blockList = new ArrayList<org.bukkit.block.Block>(blocks.size());
		for (int i = blocks.size() - 1; i >= 0; i--) {
			IntVector3 cpos = blocks.get(i);
			blockList.add(bworld.getBlockAt(cpos.x, cpos.y, cpos.z));
		}

		EntityExplodeEvent event = new EntityExplodeEvent(explode, location, blockList, 0.3F);
		CommonUtil.callEvent(event);

		blocks.clear();

		if (event.isCancelled()) {
			this.wasCanceled = true;
			return;
		} else if (TNTHandler.createExplosion(event)) {
			// handled by ourself
			return;
		}

		for (org.bukkit.block.Block block : event.blockList()) {
			IntVector3 coords = new IntVector3(block.getX(), block.getY(), block.getZ());
			blocks.add(coords);
		}

		// CraftBukkit end

		IntVector3 chunkposition;
		int x;
		int y;
		int z;
		int type;

		for (int i = blocks.size() - 1; i >= 0; --i) {
			chunkposition = blocks.get(i);
			x = chunkposition.x;
			y = chunkposition.y;
			z = chunkposition.z;
			type = this.world.getTypeId(x, y, z);

			double d0 = (double) ((float) x + this.world.random.nextFloat());
			double d1 = (double) ((float) y + this.world.random.nextFloat());
			double d2 = (double) ((float) z + this.world.random.nextFloat());
			double d3 = d0 - this.posX;
			double d4 = d1 - this.posY;
			double d5 = d2 - this.posZ;
			double d6 = Math.sqrt(d3 * d3 + d4 * d4 + d5 * d5);

			d3 /= d6;
			d4 /= d6;
			d5 /= d6;
			double d7 = 0.5D / (d6 / (double) this.size + 0.1D);

			d7 *= (double) (this.world.random.nextFloat() * this.world.random.nextFloat() + 0.3F);
			d3 *= d7;
			d4 *= d7;
			d5 *= d7;

			// CraftBukkit - stop explosions from putting out fire
			if (type > 0 && type != org.bukkit.Material.FIRE.getId()) {
				// CraftBukkit
				Block.byId[type].dropNaturally(this.world, x, y, z, this.world.getData(x, y, z), event.getYield(), 0);
				this.world.setTypeId(x, y, z, 0);
				Block.byId[type].wasExploded(this.world, x, y, z);
			}
			if (this.fire) {
				int typeBelow = this.world.getTypeId(x, y - 1, z);
				if (type == 0 && MaterialUtil.ISSOLID.get(typeBelow) && this.h.nextInt(3) == 0) {
					this.world.setTypeId(x, y, z, Block.FIRE.id);
				}
			}
		}
	}
}
