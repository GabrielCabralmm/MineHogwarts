package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.block.PulpitBlockEntity;
import com.gabriel.hogwartsmod.spell.Spell;
import com.gabriel.hogwartsmod.spell.Spells;
import com.gabriel.hogwartsmod.teach.TeachingManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record PulpitLessonPacket(BlockPos pos, String spell, boolean start) {

    public static void encode(PulpitLessonPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeUtf(packet.spell, 64);
        buf.writeBoolean(packet.start);
    }

    public static PulpitLessonPacket decode(FriendlyByteBuf buf) {
        return new PulpitLessonPacket(buf.readBlockPos(), buf.readUtf(64), buf.readBoolean());
    }

    public static void handle(PulpitLessonPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        Spell spell = Spells.get(packet.spell);
        if (player != null && spell != null && player.level().isLoaded(packet.pos) && player.distanceToSqr(packet.pos.getCenter()) < 64 * 64
                && player.level().getBlockEntity(packet.pos) instanceof PulpitBlockEntity pulpit) {
            if (packet.start) {
                TeachingManager.startClass(player, pulpit, spell);
            } else {
                TeachingManager.sendInArea(player, pulpit, spell);
            }
        }
        ctx.get().setPacketHandled(true);
    }
}
