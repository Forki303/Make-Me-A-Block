package com.makemeablock.client;

import com.makemeablock.MakeMeABlock;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.level.block.Block;

public final class BlockSkinState {
	public static final Identifier ACTIVE_TEXTURE = Identifier.fromNamespaceAndPath(MakeMeABlock.MOD_ID, "textures/skin/active");

	private static DynamicTexture texture;
	private static NativeImage image;
	private static Block block;
	private static boolean custom;
	private static final Map<Block, NativeImage> blockCache = new HashMap<>();

	private BlockSkinState() {
	}

	public static boolean isActive() {
		return texture != null && image != null;
	}

	public static boolean isCustom() {
		return custom;
	}

	public static Block getBlock() {
		return block;
	}

	public static PlayerSkin applyTo(final PlayerSkin original, final boolean isSelf) {
		if (!isSelf || !isActive()) {
			return original;
		}
		return new PlayerSkin(
			new ClientAsset.ResourceTexture(ACTIVE_TEXTURE, ACTIVE_TEXTURE),
			original.cape(),
			original.elytra(),
			original.model(),
			original.secure()
		);
	}

	public static PlayerSkin applyFor(final PlayerSkin original, final UUID uuid) {
		net.minecraft.client.player.LocalPlayer self = Minecraft.getInstance().player;
		if (self != null && self.getUUID().equals(uuid) && isActive()) {
			return new PlayerSkin(
				new ClientAsset.ResourceTexture(ACTIVE_TEXTURE, ACTIVE_TEXTURE),
				original.cape(),
				original.elytra(),
				original.model(),
				original.secure()
			);
		}
		return applyToRemote(original, uuid);
	}

	public static PlayerSkin applyToRemote(final PlayerSkin original, final UUID uuid) {
		Identifier textureId = RemoteSkinState.textureId(uuid);
		if (textureId == null) {
			return original;
		}
		return new PlayerSkin(
			new ClientAsset.ResourceTexture(textureId, textureId),
			original.cape(),
			original.elytra(),
			original.model(),
			original.secure()
		);
	}

	public static void selectBlock(final Block newBlock) {
		custom = false;
		block = newBlock;
		NativeImage cached = blockCache.get(newBlock);
		if (cached == null) {
			cached = BlockSkinGenerator.generate(newBlock);
			blockCache.put(newBlock, cached);
		}
		setImage(cached.mappedCopy(pixel -> pixel));
		SkinSyncClient.sendUpdate();
	}

	public static void selectCustom(final NativeImage newImage) {
		custom = true;
		block = null;
		setImage(newImage);
		SkinSyncClient.sendUpdate();
	}

	public static NativeImage currentImageCopy() {
		return isActive() ? image.mappedCopy(pixel -> pixel) : null;
	}

	public static void reset() {
		custom = false;
		block = null;
		image = null;
		if (texture != null) {
			texture.close();
			texture = null;
		}
		SkinSyncClient.sendUpdate();
	}

	private static void setImage(final NativeImage newImage) {
		ensureTexture();
		image = newImage;
		texture.setPixels(newImage);
		texture.upload();
	}

	private static void ensureTexture() {
		if (texture == null) {
			texture = new DynamicTexture(() -> "makemeablock active skin", new NativeImage(64, 64, true));
			Minecraft.getInstance().getTextureManager().register(ACTIVE_TEXTURE, texture);
		}
	}
}
