package com.gabriel.hogwartsmod.mission;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public record ActiveMission(UUID npc, String npcName, Mission mission, int progress, boolean tracked) {

    public ActiveMission(UUID npc, String npcName, Mission mission, int progress) {
        this(npc, npcName, mission, progress, true);
    }

    public String key() {
        return MissionProgress.key(npc, mission.id());
    }

    public ActiveMission withProgress(int value) {
        return new ActiveMission(npc, npcName, mission, value, tracked);
    }

    public ActiveMission withTracked(boolean value) {
        return new ActiveMission(npc, npcName, mission, progress, value);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Npc", npc);
        tag.putString("NomeNpc", npcName);
        tag.put("Missao", mission.save());
        tag.putInt("Progresso", progress);
        tag.putBoolean("Acompanhar", tracked);
        return tag;
    }

    public static ActiveMission load(CompoundTag tag) {
        return new ActiveMission(tag.getUUID("Npc"), tag.getString("NomeNpc"), Mission.load(tag.getCompound("Missao")), tag.getInt("Progresso"),
                !tag.contains("Acompanhar") || tag.getBoolean("Acompanhar"));
    }
}
