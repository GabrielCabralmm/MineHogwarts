package com.gabriel.hogwartsmod.mission;

import net.minecraft.network.FriendlyByteBuf;

public record MissionView(Mission mission, State state, int progress, long cooldown) {

    public enum State { AVAILABLE, ACTIVE, READY, COOLDOWN, DONE }

    public void write(FriendlyByteBuf buf) {
        mission.write(buf);
        buf.writeEnum(state);
        buf.writeVarInt(progress);
        buf.writeVarLong(cooldown);
    }

    public static MissionView read(FriendlyByteBuf buf) {
        return new MissionView(Mission.read(buf), buf.readEnum(State.class), buf.readVarInt(), buf.readVarLong());
    }
}
