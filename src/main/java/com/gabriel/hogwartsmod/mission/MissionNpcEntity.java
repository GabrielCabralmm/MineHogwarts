package com.gabriel.hogwartsmod.mission;

import com.gabriel.hogwartsmod.npc.HogwartsNpc;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MissionNpcEntity extends HogwartsNpc {
    public static final int MAX_MISSIONS = 8;
    private final List<Mission> missions = new ArrayList<>();

    public MissionNpcEntity(EntityType<? extends MissionNpcEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected String defaultModel() {
        return "mercador";
    }

    public List<Mission> getMissions() {
        return List.copyOf(missions);
    }

    public void setMissions(List<Mission> list) {
        missions.clear();
        for (Mission mission : list) {
            if (missions.size() < MAX_MISSIONS) {
                missions.add(mission.sanitized());
            }
        }
    }

    @Nullable
    public Mission mission(String id) {
        return missions.stream().filter(m -> m.id().equals(id)).findFirst().orElse(null);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            getLookControl().setLookAt(player);
            if (serverPlayer.isShiftKeyDown() && serverPlayer.hasPermissions(2)) {
                MissionManager.openEditor(serverPlayer, this);
            } else {
                talk();
                MissionManager.open(serverPlayer, this);
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ListTag list = new ListTag();
        missions.forEach(m -> list.add(m.save()));
        tag.put("Missoes", list);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        missions.clear();
        ListTag list = tag.getList("Missoes", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            missions.add(Mission.load(list.getCompound(i)));
        }
    }
}
