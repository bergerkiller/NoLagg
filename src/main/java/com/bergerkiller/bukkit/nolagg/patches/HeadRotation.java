package com.bergerkiller.bukkit.nolagg.patches;

import java.util.logging.Level;

import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.events.PacketReceiveEvent;
import com.bergerkiller.bukkit.common.events.PacketSendEvent;
import com.bergerkiller.bukkit.common.protocol.CommonPacket;
import com.bergerkiller.bukkit.common.protocol.PacketFields;
import com.bergerkiller.bukkit.common.protocol.PacketListener;
import com.bergerkiller.bukkit.common.protocol.PacketType;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.PacketUtil;
import com.bergerkiller.bukkit.nolagg.NoLagg;

public class HeadRotation implements PacketListener {
	private static HeadRotation instance;
	
	public static void init() {
		instance = new HeadRotation();
		PacketUtil.addPacketListener(NoLagg.plugin, instance,
				PacketType.NAMED_ENTITY_SPAWN);
		NoLaggPatches.plugin.log(Level.INFO, "Loaded head rotation fix");
	}
	
	public static void deinit() {
		if(instance != null) {
			PacketUtil.removePacketListener(instance);
			instance = null;
		}
	}

	@Override
	public void onPacketReceive(PacketReceiveEvent arg0) {}

	@Override
	public void onPacketSend(PacketSendEvent event) {
		CommonPacket packet = event.getPacket();
		PacketType type = packet.getType();
		final Player player = event.getPlayer();
		
		if(type == PacketType.NAMED_ENTITY_SPAWN) {
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
					
					PacketUtil.sendCommonPacket(player, headPacket);
					PacketUtil.sendCommonPacket(player, bodyPacket);
				}
			});
		}
	}
}
