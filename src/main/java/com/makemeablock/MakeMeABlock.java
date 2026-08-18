package com.makemeablock;

import com.makemeablock.network.SkinSyncPayload;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public final class MakeMeABlock implements ModInitializer {
	public static final String MOD_ID = "makemeablock";

	private static final Map<UUID, SkinSyncPayload.S2C> SKINS = new HashMap<>();

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.serverboundPlay().register(SkinSyncPayload.C2S_TYPE, SkinSyncPayload.C2S.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SkinSyncPayload.S2C_TYPE, SkinSyncPayload.S2C.STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(SkinSyncPayload.C2S_TYPE, (payload, context) -> {
			ServerPlayer sender = context.player();
			context.server().execute(() -> relay(sender, payload));
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			server.execute(() -> {
				for (SkinSyncPayload.S2C skin : SKINS.values()) {
					sendTo(handler.player, skin);
				}
			});
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			server.execute(() -> {
				UUID uuid = handler.player.getUUID();
				if (SKINS.remove(uuid) == null) {
					return;
				}
				SkinSyncPayload.S2C gone = new SkinSyncPayload.S2C(uuid, false, new byte[0]);
				for (ServerPlayer other : PlayerLookup.all(server)) {
					sendTo(other, gone);
				}
			});
		});
	}

	private static void relay(final ServerPlayer sender, final SkinSyncPayload.C2S payload) {
		UUID uuid = sender.getUUID();
		SkinSyncPayload.S2C sync = new SkinSyncPayload.S2C(uuid, payload.active(), payload.active() ? payload.data() : new byte[0]);
		if (payload.active()) {
			SKINS.put(uuid, sync);
		} else {
			SKINS.remove(uuid);
		}
		for (ServerPlayer other : PlayerLookup.all(sender.level().getServer())) {
			if (other != sender) {
				sendTo(other, sync);
			}
		}
	}

	private static void sendTo(final ServerPlayer player, final SkinSyncPayload.S2C payload) {
		if (ServerPlayNetworking.canSend(player, SkinSyncPayload.S2C_TYPE)) {
			ServerPlayNetworking.send(player, payload);
		}
	}
}
