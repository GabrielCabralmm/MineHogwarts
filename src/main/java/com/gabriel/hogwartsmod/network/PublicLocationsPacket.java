package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.apparition.ApparitionPoint;
import com.gabriel.hogwartsmod.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public record PublicLocationsPacket(List<ApparitionPoint> points) {

    public static void encode(PublicLocationsPacket packet, FriendlyByteBuf buf) {
        buf.writeCollection(packet.points, (b, p) -> p.write(b));
    }

    public static PublicLocationsPacket decode(FriendlyByteBuf buf) {
        return new PublicLocationsPacket(buf.readList(ApparitionPoint::read));
    }

    public static void handle(PublicLocationsPacket packet, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handlePublicLocations(packet.points));
        ctx.get().setPacketHandled(true);
    }
}
