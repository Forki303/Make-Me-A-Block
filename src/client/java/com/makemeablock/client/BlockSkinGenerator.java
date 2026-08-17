package com.makemeablock.client;

import com.makemeablock.client.mixin.ModelPartAccessor;
import com.makemeablock.client.mixin.SpriteContentsAccessor;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3fc;

public final class BlockSkinGenerator {
	private static final int SIZE = 64;

	private record FaceData(TextureAtlasSprite sprite, int tintIndex) {
	}

	private BlockSkinGenerator() {
	}

	public static NativeImage generate(final Block block) {
		Minecraft minecraft = Minecraft.getInstance();
		BlockState state = block.defaultBlockState();
		List<BlockStateModelPart> parts = new ArrayList<>();
		minecraft.getModelManager().getBlockStateModelSet().get(state).collectParts(RandomSource.create(), parts);

		Map<Direction, FaceData> faces = new EnumMap<>(Direction.class);
		TextureAtlasSprite fallback = minecraft.getModelManager().getBlockStateModelSet().get(state).particleMaterial().sprite();
		for (Direction direction : Direction.values()) {
			FaceData data = findFace(parts, direction);
			if (data == null) {
				data = findFace(parts, null);
			}
			faces.put(direction, data != null ? data : new FaceData(fallback, -1));
		}

		BlockTintSource tintSource = minecraft.getBlockColors().getTintSource(state, 0);

		NativeImage result = new NativeImage(SIZE, SIZE, true);
		PlayerModel model = new PlayerModel(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
		for (ModelPart part : model.root().getAllParts()) {
			for (ModelPart.Cube cube : ((ModelPartAccessor) (Object) part).makemeablock$getCubes()) {
				for (ModelPart.Polygon polygon : cube.polygons) {
					Direction face = directionOf(polygon.normal());
					FaceData data = faces.get(face);
					if (data == null || data.sprite() == null) {
						continue;
					}
					paintPolygon(result, polygon, data);
				}
			}
		}

		BlockTintSource finalTint = tintSource;
		if (finalTint != null) {
			int tint = finalTint.color(state);
			for (int y = 0; y < SIZE; y++) {
				for (int x = 0; x < SIZE; x++) {
					int pixel = result.getPixel(x, y);
					if (ARGB.alpha(pixel) != 0) {
						result.setPixel(x, y, ARGB.multiply(pixel, tint));
					}
				}
			}
		}
		return result;
	}

	private static FaceData findFace(final List<BlockStateModelPart> parts, final Direction direction) {
		for (BlockStateModelPart part : parts) {
			List<BakedQuad> quads = part.getQuads(direction);
			if (!quads.isEmpty()) {
				BakedQuad quad = quads.get(0);
				return new FaceData(quad.materialInfo().sprite(), quad.materialInfo().tintIndex());
			}
		}
		return null;
	}

	private static Direction directionOf(final Vector3fc normal) {
		float x = normal.x();
		float y = normal.y();
		float z = normal.z();
		if (y > 0.5F) {
			return Direction.UP;
		}
		if (y < -0.5F) {
			return Direction.DOWN;
		}
		if (z > 0.5F) {
			return Direction.SOUTH;
		}
		if (z < -0.5F) {
			return Direction.NORTH;
		}
		if (x > 0.5F) {
			return Direction.EAST;
		}
		return Direction.WEST;
	}

	private static float shadeFor(final Direction face) {
		return switch (face) {
			case UP -> 1.0F;
			case DOWN -> 0.5F;
			case NORTH -> 0.8F;
			case SOUTH -> 0.6F;
			case WEST -> 0.6F;
			case EAST -> 0.8F;
		};
	}

	private static void paintPolygon(final NativeImage target, final ModelPart.Polygon polygon, final FaceData data) {
		float u0 = SIZE;
		float u1 = 0.0F;
		float v0 = SIZE;
		float v1 = 0.0F;
		for (ModelPart.Vertex vertex : polygon.vertices()) {
			float u = vertex.u() * SIZE;
			float v = vertex.v() * SIZE;
			u0 = Math.min(u0, u);
			u1 = Math.max(u1, u);
			v0 = Math.min(v0, v);
			v1 = Math.max(v1, v);
		}
		if (u1 - u0 < 0.001F || v1 - v0 < 0.001F) {
			return;
		}

		Direction face = directionOf(polygon.normal());
		float shade = shadeFor(face);
		int shadeColor = ARGB.color(Math.round(255.0F * shade), Math.round(255.0F * shade), Math.round(255.0F * shade));

		NativeImage source = ((SpriteContentsAccessor) (Object) data.sprite().contents()).makemeablock$getOriginalImage();
		int spriteWidth = data.sprite().contents().width();
		int spriteHeight = data.sprite().contents().height();

		int x0 = Math.max(0, (int) Math.floor(u0) - 1);
		int x1 = Math.min(SIZE - 1, (int) Math.ceil(u1) + 1);
		int y0 = Math.max(0, (int) Math.floor(v0) - 1);
		int y1 = Math.min(SIZE - 1, (int) Math.ceil(v1) + 1);
		for (int y = y0; y <= y1; y++) {
			float fy = (y + 0.5F - v0) / (v1 - v0);
			int sy = Math.max(0, Math.min(spriteHeight - 1, (int) (fy * spriteHeight)));
			for (int x = x0; x <= x1; x++) {
				float fx = (x + 0.5F - u0) / (u1 - u0);
				int sx = Math.max(0, Math.min(spriteWidth - 1, (int) (fx * spriteWidth)));
				int pixel = source.getPixel(sx, sy);
				if (ARGB.alpha(pixel) == 0) {
					continue;
				}
				pixel = ARGB.multiply(pixel, shadeColor);
				target.setPixel(x, y, pixel);
			}
		}
	}
}
