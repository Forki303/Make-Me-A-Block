package com.makemeablock.client;

import com.makemeablock.network.SkinSyncPayload;
import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.ARGB;

public final class SkinSyncClient {
	private SkinSyncClient() {
	}

	public static void sendUpdate() {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		if (connection == null) {
			return;
		}
		NativeImage image = BlockSkinState.currentImageCopy();
		if (image != null) {
			ClientPlayNetworking.send(new SkinSyncPayload.C2S(true, toBytes(image)));
			image.close();
		} else {
			ClientPlayNetworking.send(new SkinSyncPayload.C2S(false, new byte[0]));
		}
	}

	public static void handle(final SkinSyncPayload.S2C payload) {
		LocalPlayer self = Minecraft.getInstance().player;
		if (self == null || payload.uuid().equals(self.getUUID())) {
			return;
		}
		if (payload.active() && payload.data().length == SkinSyncPayload.SIZE) {
			RemoteSkinState.apply(payload.uuid(), toImage(payload.data()));
		} else {
			RemoteSkinState.clear(payload.uuid());
		}
	}

	private static byte[] toBytes(final NativeImage image) {
		byte[] bytes = new byte[SkinSyncPayload.SIZE];
		int i = 0;
		for (int y = 0; y < 64; y++) {
			for (int x = 0; x < 64; x++) {
				int pixel = image.getPixel(x, y);
				bytes[i++] = (byte) ARGB.red(pixel);
				bytes[i++] = (byte) ARGB.green(pixel);
				bytes[i++] = (byte) ARGB.blue(pixel);
				bytes[i++] = (byte) ARGB.alpha(pixel);
			}
		}
		return bytes;
	}

	private static NativeImage toImage(final byte[] bytes) {
		NativeImage image = new NativeImage(64, 64, true);
		int i = 0;
		for (int y = 0; y < 64; y++) {
			for (int x = 0; x < 64; x++) {
				int r = bytes[i++] & 0xFF;
				int g = bytes[i++] & 0xFF;
				int b = bytes[i++] & 0xFF;
				int a = bytes[i++] & 0xFF;
				image.setPixel(x, y, ARGB.color(a, r, g, b));
			}
		}
		return image;
	}
}
