package com.makemeablock.client.mixin;

import com.makemeablock.client.BlockSkinState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
	@Redirect(
		method = {"renderMapHand", "renderPlayerArm"},
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getSkin()Lnet/minecraft/world/entity/player/PlayerSkin;")
	)
	private PlayerSkin makemeablock$useBlockSkin(final LocalPlayer player) {
		return BlockSkinState.applyFor(player.getSkin(), player.getUUID());
	}

	@Redirect(
		method = {"renderMapHand", "renderPlayerArm"},
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;getSkin()Lnet/minecraft/world/entity/player/PlayerSkin;")
	)
	private PlayerSkin makemeablock$useBlockSkin(final AbstractClientPlayer player) {
		return BlockSkinState.applyFor(player.getSkin(), player.getUUID());
	}
}
