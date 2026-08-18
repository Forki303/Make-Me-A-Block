package com.makemeablock.client.mixin;

import com.makemeablock.client.BlockSkinState;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {
	@Redirect(
		method = "extractRenderState",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/ClientAvatarEntity;getSkin()Lnet/minecraft/world/entity/player/PlayerSkin;")
	)
	private PlayerSkin makemeablock$useBlockSkin(final ClientAvatarEntity entity) {
		return BlockSkinState.applyFor(entity.getSkin(), ((Avatar) entity).getUUID());
	}
}
