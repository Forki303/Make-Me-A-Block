package com.makemeablock.client;

import com.makemeablock.client.screen.BlockPickerScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.resources.Identifier;

public final class MakeMeABlockClient implements ClientModInitializer {
	public static final String MOD_ID = "makemeablock";

	private KeyMapping openMenuKey;

	@Override
	public void onInitializeClient() {
		this.openMenuKey = new KeyMapping(
			"key." + MOD_ID + ".open",
			InputConstants.Type.KEYSYM,
			InputConstants.KEY_B,
			new KeyMapping.Category(Identifier.fromNamespaceAndPath(MOD_ID, "menu"))
		);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (this.openMenuKey.consumeClick() && client.player != null) {
				client.gui.setScreen(new BlockPickerScreen());
			}
		});
	}
}
