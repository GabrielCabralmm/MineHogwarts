package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.client.ClientPacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncMagicPacket(CompoundTag tag) {

    public static void encode(SyncMagicPacket packet, FriendlyByteBuf buf) {
        buf.writeNbt(packet.tag);
    }

    public static SyncMagicPacket decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new SyncMagicPacket(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(SyncMagicPacket packet, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleSync(packet.tag));
        ctx.get().setPacketHandled(true);
    }
}
