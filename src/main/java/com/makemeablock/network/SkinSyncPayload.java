package com.makemeablock.network;

import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class SkinSyncPayload {
	public static final int SIZE = 64 * 64 * 4;

	public static final CustomPacketPayload.Type<C2S> C2S_TYPE = new CustomPacketPayload.Type<>(
		Identifier.fromNamespaceAndPath("makemeablock", "skin_update")
	);
	public static final CustomPacketPayload.Type<S2C> S2C_TYPE = new CustomPacketPayload.Type<>(
		Identifier.fromNamespaceAndPath("makemeablock", "skin_sync")
	);

	private static final StreamCodec<ByteBuf, UUID> UUID_CODEC = StreamCodec.ofMember(
		(uuid, buf) -> {
			buf.writeLong(uuid.getMostSignificantBits());
			buf.writeLong(uuid.getLeastSignificantBits());
		},
		buf -> new UUID(buf.readLong(), buf.readLong())
	);

	private SkinSyncPayload() {
	}

	public record C2S(boolean active, byte[] data) implements CustomPacketPayload {
		public static final StreamCodec<ByteBuf, C2S> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, C2S::active,
			ByteBufCodecs.BYTE_ARRAY, C2S::data,
			C2S::new
		);

		@Override
		public Type<C2S> type() {
			return C2S_TYPE;
		}
	}

	public record S2C(UUID uuid, boolean active, byte[] data) implements CustomPacketPayload {
		public static final StreamCodec<ByteBuf, S2C> STREAM_CODEC = StreamCodec.composite(
			UUID_CODEC, S2C::uuid,
			ByteBufCodecs.BOOL, S2C::active,
			ByteBufCodecs.BYTE_ARRAY, S2C::data,
			S2C::new
		);

		@Override
		public Type<S2C> type() {
			return S2C_TYPE;
		}
	}
}
