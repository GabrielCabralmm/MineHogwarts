package com.gabriel.hogwartsmod.mission;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public record ActiveMission(UUID npc, String npcName, Mission mission, int progress) {

    public String key() {
        return MissionProgress.key(npc, mission.id());
    }

    public ActiveMission withProgress(int value) {
        return new ActiveMission(npc, npcName, mission, value);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Npc", npc);
        tag.putString("NomeNpc", npcName);
        tag.put("Missao", mission.save());
        tag.putInt("Progresso", progress);
        return tag;
    }

    public static ActiveMission load(CompoundTag tag) {
        return new ActiveMission(tag.getUUID("Npc"), tag.getString("NomeNpc"), Mission.load(tag.getCompound("Missao")), tag.getInt("Progresso"));
    }
}
