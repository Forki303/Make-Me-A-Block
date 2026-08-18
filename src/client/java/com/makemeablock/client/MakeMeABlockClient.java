package com.makemeablock.client;

import com.makemeablock.MakeMeABlock;
import com.makemeablock.client.screen.BlockPickerScreen;
import com.makemeablock.network.SkinSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.resources.Identifier;

public final class MakeMeABlockClient implements ClientModInitializer {
	private KeyMapping openMenuKey;

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(SkinSyncPayload.S2C_TYPE, (payload, context) -> {
			context.client().execute(() -> SkinSyncClient.handle(payload));
		});

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			RemoteSkinState.clearAll();
			SkinSyncClient.sendUpdate();
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			RemoteSkinState.clearAll();
		});

		this.openMenuKey = new KeyMapping(
			"key." + MakeMeABlock.MOD_ID + ".open",
			InputConstants.Type.KEYSYM,
			InputConstants.KEY_B,
			new KeyMapping.Category(Identifier.fromNamespaceAndPath(MakeMeABlock.MOD_ID, "menu"))
		);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (this.openMenuKey.consumeClick() && client.player != null) {
				client.gui.setScreen(new BlockPickerScreen());
			}
		});
	}
}
