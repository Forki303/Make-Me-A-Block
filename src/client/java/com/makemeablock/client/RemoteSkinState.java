package com.makemeablock.client;

import com.makemeablock.MakeMeABlock;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public final class RemoteSkinState {
	private static final Map<UUID, Entry> SKINS = new HashMap<>();

	private static final class Entry {
		private final DynamicTexture texture;
		private final Identifier textureId;

		private Entry(final DynamicTexture texture, final Identifier textureId) {
			this.texture = texture;
			this.textureId = textureId;
		}
	}

	private RemoteSkinState() {
	}

	public static void apply(final UUID uuid, final NativeImage image) {
		clear(uuid);
		Identifier textureId = Identifier.fromNamespaceAndPath(MakeMeABlock.MOD_ID, "skins/" + uuid);
		DynamicTexture texture = new DynamicTexture(() -> "makemeablock remote skin " + uuid, image);
		Minecraft.getInstance().getTextureManager().register(textureId, texture);
		texture.upload();
		SKINS.put(uuid, new Entry(texture, textureId));
	}

	public static void clear(final UUID uuid) {
		Entry entry = SKINS.remove(uuid);
		if (entry != null) {
			Minecraft.getInstance().getTextureManager().release(entry.textureId);
			entry.texture.close();
		}
	}

	public static void clearAll() {
		for (UUID uuid : new ArrayList<>(SKINS.keySet())) {
			clear(uuid);
		}
	}

	public static Identifier textureId(final UUID uuid) {
		Entry entry = SKINS.get(uuid);
		return entry != null ? entry.textureId : null;
	}
}
