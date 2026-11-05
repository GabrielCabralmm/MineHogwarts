package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.spell.Spell;
import com.gabriel.hogwartsmod.spell.Spells;
import com.gabriel.hogwartsmod.teach.TeachingManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record InviteStudentPacket(String spell, UUID student) {

    public static void encode(InviteStudentPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.spell, 64);
        buf.writeUUID(packet.student);
    }

    public static InviteStudentPacket decode(FriendlyByteBuf buf) {
        return new InviteStudentPacket(buf.readUtf(64), buf.readUUID());
    }

    public static void handle(InviteStudentPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer teacher = ctx.get().getSender();
        Spell spell = Spells.get(packet.spell);
        if (teacher != null && spell != null) {
            ServerPlayer student = teacher.server.getPlayerList().getPlayer(packet.student);
            if (student != null) {
                TeachingManager.invite(teacher, student, spell);
            }
        }
        ctx.get().setPacketHandled(true);
    }
}
