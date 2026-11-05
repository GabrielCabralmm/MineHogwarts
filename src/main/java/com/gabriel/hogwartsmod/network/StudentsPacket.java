package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.client.ClientPacketHandler;
import com.gabriel.hogwartsmod.teach.StudentStatus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public record StudentsPacket(String spell, List<Entry> students) {
    public record Entry(UUID id, String name, StudentStatus status) {
    }

    public static void encode(StudentsPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.spell, 64);
        buf.writeVarInt(packet.students.size());
        for (Entry entry : packet.students) {
            buf.writeUUID(entry.id());
            buf.writeUtf(entry.name(), 64);
            buf.writeEnum(entry.status());
        }
    }

    public static StudentsPacket decode(FriendlyByteBuf buf) {
        String spell = buf.readUtf(64);
        int count = Math.min(buf.readVarInt(), 256);
        List<Entry> students = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            students.add(new Entry(buf.readUUID(), buf.readUtf(64), buf.readEnum(StudentStatus.class)));
        }
        return new StudentsPacket(spell, students);
    }

    public static void handle(StudentsPacket packet, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleStudents(packet.spell, packet.students));
        ctx.get().setPacketHandled(true);
    }
}
