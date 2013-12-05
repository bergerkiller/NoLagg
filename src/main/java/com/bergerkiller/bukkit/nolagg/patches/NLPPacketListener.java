package com.bergerkiller.bukkit.nolagg.patches;

import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.controller.EntityNetworkController;
import com.bergerkiller.bukkit.common.conversion.Conversion;
import com.bergerkiller.bukkit.common.entity.CommonEntity;
import com.bergerkiller.bukkit.common.events.PacketReceiveEvent;
import com.bergerkiller.bukkit.common.events.PacketSendEvent;
import com.bergerkiller.bukkit.common.protocol.CommonPacket;
import com.bergerkiller.bukkit.common.protocol.PacketListener;
import com.bergerkiller.bukkit.common.protocol.PacketType;
import com.bergerkiller.bukkit.common.reflection.classes.WorldServerRef;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.common.utils.PacketUtil;
import com.bergerkiller.bukkit.common.wrappers.IntHashMap;

public class NLPPacketListener implements PacketListener {

	@Override
	public void onPacketReceive(PacketReceiveEvent arg0) {}

	@Override
	public void onPacketSend(PacketSendEvent event) {
		CommonPacket packet = event.getPacket();
		final Player player = event.getPlayer();

		if (NoLaggPatches.headRotationPatch && event.getType() == PacketType.OUT_ENTITY_SPAWN_NAMED) {
			final int EntityId = packet.read(PacketType.OUT_ENTITY_SPAWN_NAMED.entityId);
			final byte yaw = packet.read(PacketType.OUT_ENTITY_SPAWN_NAMED.yaw);
			final byte pitch = packet.read(PacketType.OUT_ENTITY_SPAWN_NAMED.pitch);
			
			CommonUtil.nextTick(new Runnable() {
				@Override
				public void run() {
					CommonPacket headPacket = new CommonPacket(PacketType.OUT_ENTITY_HEAD_ROTATION);
					headPacket.write(PacketType.OUT_ENTITY_HEAD_ROTATION.entityId, EntityId);
					headPacket.write(PacketType.OUT_ENTITY_HEAD_ROTATION.headYaw, yaw);
					
					CommonPacket bodyPacket = new CommonPacket(PacketType.OUT_ENTITY_LOOK);
					bodyPacket.write(PacketType.OUT_ENTITY_LOOK.entityId, EntityId);
					bodyPacket.write(PacketType.OUT_ENTITY_LOOK.yaw, yaw);
					bodyPacket.write(PacketType.OUT_ENTITY_LOOK.pitch, pitch);

					PacketUtil.sendPacket(player, headPacket);
					PacketUtil.sendPacket(player, bodyPacket);
				}
			});
		} else if (NoLaggPatches.entitySpawnPatch) {
			final boolean mob = event.getType() == PacketType.OUT_ENTITY_SPAWN_LIVING;
			final boolean vehicle = event.getType() == PacketType.OUT_ENTITY_SPAWN;
			if (mob || vehicle) {
				// Obtain the Entity Tracker Entry of this Entity
				int entityId;
				if (mob) {
					entityId = packet.read(PacketType.OUT_ENTITY_SPAWN_LIVING.entityId);
				} else if (vehicle) {
					entityId = packet.read(PacketType.OUT_ENTITY_SPAWN.entityId);
				} else {
					return;
				}
				IntHashMap<Object> entitiesById = WorldServerRef.entitiesById.get(Conversion.toWorldHandle.convert(event.getPlayer().getWorld()));
				Entity entity = Conversion.toEntity.convert(entitiesById.get(entityId));
				if (entity == null || !(entity instanceof Boat || entity instanceof Minecart || EntityUtil.isMob(entity))) {
					return;
				}
				EntityNetworkController<?> controller = CommonEntity.get(entity).getNetworkController();
				if (controller == null) {
					return;
				}
				// Let's go and fix up this packet!
				if (mob) {
					packet.write(PacketType.OUT_ENTITY_SPAWN_LIVING.x, controller.locSynched.getX());
					packet.write(PacketType.OUT_ENTITY_SPAWN_LIVING.y, controller.locSynched.getY());
					packet.write(PacketType.OUT_ENTITY_SPAWN_LIVING.z, controller.locSynched.getZ());
				} else if (vehicle) {
					packet.write(PacketType.OUT_ENTITY_SPAWN.x, controller.locSynched.getX());
					packet.write(PacketType.OUT_ENTITY_SPAWN.y, controller.locSynched.getY());
					packet.write(PacketType.OUT_ENTITY_SPAWN.z, controller.locSynched.getZ());
				}
			}
		}
	}
}
