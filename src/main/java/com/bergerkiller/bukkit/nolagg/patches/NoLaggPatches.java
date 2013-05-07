package com.bergerkiller.bukkit.nolagg.patches;

import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.common.protocol.PacketType;
import com.bergerkiller.bukkit.common.utils.PacketUtil;
import com.bergerkiller.bukkit.nolagg.NoLagg;
import com.bergerkiller.bukkit.nolagg.NoLaggComponent;

public class NoLaggPatches extends NoLaggComponent {
	public static boolean headRotationPatch;
	public static boolean entitySpawnPatch;
	public static NoLaggPatches plugin;
	private NLPPacketListener headRotationFixListener = new NLPPacketListener();

	@Override
	public void onReload(ConfigurationNode config) {
		config.setHeader("headRotationOnSpawn", "\nSets whether to automatically fix the player head rotation when spawning");
		config.addHeader("headRotationOnSpawn", "This is done by sending additional rotation information after spawning the player at the client");
		headRotationPatch = config.get("headRotationOnSpawn", true);

		config.setHeader("entitySpawnFix", "\nSets whether to fix up the vehicle (minecart) and mob spawn positions when spawning, avoiding glitched positions");
		config.addHeader("entitySpawnFix", "In-depth: Minecraft server contains a bug in the EntityTrackerEntry which results in spawn packets sent with 'too new' positions");
		config.addHeader("entitySpawnFix", "This 'too new' position is then further updated with relative updates, resulting in entities spawning with a slight offset");
		config.addHeader("entitySpawnFix", "In the case of minecarts, you see minecarts spawned moving next to the rails, floating. This is fixed");
		entitySpawnPatch = config.get("entitySpawnFix", true);
	}

	@Override
	public void onDisable(ConfigurationNode config) {
		PacketUtil.removePacketListener(headRotationFixListener);
	}

	@Override
	public void onEnable(ConfigurationNode config) {
		plugin = this;
		PacketUtil.addPacketListener(NoLagg.plugin, headRotationFixListener, PacketType.NAMED_ENTITY_SPAWN, PacketType.VEHICLE_SPAWN, PacketType.MOB_SPAWN);
		this.onReload(config);
	}
}
