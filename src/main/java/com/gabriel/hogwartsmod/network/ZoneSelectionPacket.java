package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.client.ClientPacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public record ZoneSelectionPacket(@Nullable BlockPos first, @Nullable BlockPos second) {

    public static void encode(ZoneSelectionPacket packet, FriendlyByteBuf buf) {
        buf.writeNullable(packet.first, FriendlyByteBuf::writeBlockPos);
        buf.writeNullable(packet.second, FriendlyByteBuf::writeBlockPos);
    }

    public static ZoneSelectionPacket decode(FriendlyByteBuf buf) {
        return new ZoneSelectionPacket(buf.readNullable(FriendlyByteBuf::readBlockPos), buf.readNullable(FriendlyByteBuf::readBlockPos));
    }

    public static void handle(ZoneSelectionPacket packet, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleZoneSelection(packet.first, packet.second));
        ctx.get().setPacketHandled(true);
    }
}
