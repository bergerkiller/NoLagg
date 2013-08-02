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
import com.bergerkiller.bukkit.common.protocol.PacketFields;
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

		if (NoLaggPatches.headRotationPatch && event.getType() == PacketType.NAMED_ENTITY_SPAWN) {
			final int EntityId = packet.read(PacketFields.NAMED_ENTITY_SPAWN.entityId);
			final byte yaw = packet.read(PacketFields.NAMED_ENTITY_SPAWN.yaw);
			final byte pitch = packet.read(PacketFields.NAMED_ENTITY_SPAWN.pitch);
			
			CommonUtil.nextTick(new Runnable() {
				@Override
				public void run() {
					CommonPacket headPacket = new CommonPacket(PacketType.ENTITY_HEAD_ROTATION);
					headPacket.write(PacketFields.ENTITY_HEAD_ROTATION.entityId, EntityId);
					headPacket.write(PacketFields.ENTITY_HEAD_ROTATION.headYaw, yaw);
					
					CommonPacket bodyPacket = new CommonPacket(PacketType.ENTITY_LOOK);
					bodyPacket.write(PacketFields.ENTITY_LOOK.entityId, EntityId);
					bodyPacket.write(PacketFields.ENTITY_LOOK.dyaw, yaw);
					bodyPacket.write(PacketFields.ENTITY_LOOK.dpitch, pitch);
					
					PacketUtil.sendPacket(player, headPacket);
					PacketUtil.sendPacket(player, bodyPacket);
				}
			});
		} else if (NoLaggPatches.entitySpawnPatch) {
			final boolean mob = event.getType() == PacketType.MOB_SPAWN;
			final boolean vehicle = event.getType() == PacketType.VEHICLE_SPAWN;
			if (mob || vehicle) {
				// Obtain the Entity Tracker Entry of this Entity
				int entityId;
				if (mob) {
					entityId = packet.read(PacketFields.MOB_SPAWN.entityId);
				} else if (vehicle) {
					entityId = packet.read(PacketFields.VEHICLE_SPAWN.entityId);
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
					packet.write(PacketFields.MOB_SPAWN.x, controller.locSynched.getX());
					packet.write(PacketFields.MOB_SPAWN.y, controller.locSynched.getY());
					packet.write(PacketFields.MOB_SPAWN.z, controller.locSynched.getZ());
				} else if (vehicle) {
					packet.write(PacketFields.VEHICLE_SPAWN.x, controller.locSynched.getX());
					packet.write(PacketFields.VEHICLE_SPAWN.y, controller.locSynched.getY());
					packet.write(PacketFields.VEHICLE_SPAWN.z, controller.locSynched.getZ());
				}
			}
		}
	}
}
