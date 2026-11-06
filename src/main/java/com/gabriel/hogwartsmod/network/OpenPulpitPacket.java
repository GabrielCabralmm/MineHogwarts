package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.client.ClientPacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record OpenPulpitPacket(BlockPos pos, boolean canEdit) {

    public static void encode(OpenPulpitPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeBoolean(packet.canEdit);
    }

    public static OpenPulpitPacket decode(FriendlyByteBuf buf) {
        return new OpenPulpitPacket(buf.readBlockPos(), buf.readBoolean());
    }

    public static void handle(OpenPulpitPacket packet, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleOpenPulpit(packet.pos, packet.canEdit));
        ctx.get().setPacketHandled(true);
    }
}
