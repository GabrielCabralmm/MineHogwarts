package com.gabriel.hogwartsmod.mission;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.magic.MagicData;
import com.gabriel.hogwartsmod.money.MoneyHelper;
import com.gabriel.hogwartsmod.network.ModNetwork;
import com.gabriel.hogwartsmod.network.OpenMissionEditorPacket;
import com.gabriel.hogwartsmod.network.OpenMissionsPacket;
import com.gabriel.hogwartsmod.zone.Zone;
import com.gabriel.hogwartsmod.zone.Zones;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = HogwartsMod.MOD_ID)
public final class MissionManager {
    private static final double REACH = 6.0;

    public enum Action { ACCEPT, ABANDON, COMPLETE }

    private MissionManager() {
    }

    // -- Telas

    public static void open(ServerPlayer player, MissionNpcEntity npc) {
        MagicData data = MagicData.get(player);
        if (data == null) {
            return;
        }
        List<MissionView> views = new ArrayList<>();
        for (Mission mission : npc.getMissions()) {
            views.add(view(player, data, npc.getUUID(), mission));
        }
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new OpenMissionsPacket(npc.getId(), npc.getNpcName(), views, data.getMissions().active().size()));
    }

    public static void openEditor(ServerPlayer player, MissionNpcEntity npc) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new OpenMissionEditorPacket(npc.getId(), npc.getNpcName(), npc.getModel(), npc.getMissions()));
    }

    public static MissionView view(ServerPlayer player, MagicData data, UUID npc, Mission mission) {
        ActiveMission active = data.getMissions().find(npc, mission.id());
        if (active != null) {
            int progress = progress(player, active);
            return new MissionView(active.mission(), progress >= active.mission().amount() ? MissionView.State.READY : MissionView.State.ACTIVE, progress, 0);
        }
        long done = data.getMissions().completedAt(npc, mission.id());
        if (done >= 0) {
            if (!mission.repeatable()) {
                return new MissionView(mission, MissionView.State.DONE, 0, 0);
            }
            long wait = done + mission.cooldownHours() * 3_600_000L - System.currentTimeMillis();
            if (wait > 0) {
                return new MissionView(mission, MissionView.State.COOLDOWN, 0, wait);
            }
        }
        return new MissionView(mission, MissionView.State.AVAILABLE, 0, 0);
    }

    public static int progress(ServerPlayer player, ActiveMission active) {
        if (active.mission().type() == ObjectiveType.ENTREGAR) {
            return Math.min(active.mission().amount(), count(player, active.mission().item()));
        }
        return Math.min(active.mission().amount(), active.progress());
    }

    private static int count(ServerPlayer player, Item item) {
        int n = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                n += stack.getCount();
            }
        }
        return n;
    }

    // -- Ações do jogador

    public static void action(ServerPlayer player, int entityId, String missionId, Action action) {
        if (!(player.level().getEntity(entityId) instanceof MissionNpcEntity npc) || npc.distanceTo(player) > REACH) {
            return;
        }
        MagicData data = MagicData.get(player);
        Mission mission = npc.mission(missionId);
        if (data == null || mission == null) {
            return;
        }
        MissionProgress progress = data.getMissions();
        MissionView view = view(player, data, npc.getUUID(), mission);
        switch (action) {
            case ACCEPT -> {
                if (view.state() != MissionView.State.AVAILABLE) {
                    return;
                }
                if (progress.active().size() >= MissionProgress.MAX_ACTIVE) {
                    player.displayClientMessage(Component.translatable("message.hogwartsmod.mission_full", MissionProgress.MAX_ACTIVE)
                            .withStyle(ChatFormatting.GRAY), true);
                    return;
                }
                progress.start(new ActiveMission(npc.getUUID(), npc.getNpcName(), mission, 0));
                player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1f);
                player.displayClientMessage(Component.translatable("message.hogwartsmod.mission_accepted", title(mission))
                        .withStyle(ChatFormatting.GOLD), true);
            }
            case ABANDON -> {
                if (progress.find(npc.getUUID(), mission.id()) == null) {
                    return;
                }
                progress.remove(npc.getUUID(), mission.id());
            }
            case COMPLETE -> {
                if (view.state() != MissionView.State.READY) {
                    return;
                }
                ActiveMission active = progress.find(npc.getUUID(), mission.id());
                Mission done = active == null ? mission : active.mission();
                if (done.type() == ObjectiveType.ENTREGAR) {
                    int left = done.amount();
                    Item item = done.item();
                    for (ItemStack stack : player.getInventory().items) {
                        if (left > 0 && stack.is(item)) {
                            int take = Math.min(left, stack.getCount());
                            stack.shrink(take);
                            left -= take;
                        }
                    }
                }
                progress.complete(npc.getUUID(), mission.id(), System.currentTimeMillis());
                MoneyHelper.give(player, done.galeoes(), done.sicles(), done.nuques());
                npc.talk();
                player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.3f);
                player.displayClientMessage(Component.translatable("message.hogwartsmod.mission_done", title(done))
                        .withStyle(ChatFormatting.GOLD), false);
            }
        }
        data.markForSync();
        open(player, npc);
    }

    private static Component title(Mission mission) {
        return Component.literal(mission.title().isBlank() ? "?" : mission.title()).withStyle(ChatFormatting.BOLD);
    }

    // -- Edição da staff

    public static void save(ServerPlayer player, int entityId, String name, String model, List<Mission> missions) {
        if (!player.hasPermissions(2) || !(player.level().getEntity(entityId) instanceof MissionNpcEntity npc)) {
            return;
        }
        String clean = name.strip();
        if (!clean.isEmpty()) {
            npc.setCustomName(Component.literal(clean.length() > 32 ? clean.substring(0, 32) : clean));
            npc.setCustomNameVisible(true);
        }
        npc.setModel(model);
        npc.setMissions(missions);
        player.displayClientMessage(Component.translatable("message.hogwartsmod.mission_npc_saved", npc.getNpcName())
                .withStyle(ChatFormatting.GOLD), true);
    }

    // -- Progresso

    @SubscribeEvent
    public static void kill(LivingDeathEvent event) {
        Entity killer = event.getSource().getEntity();
        if (!(killer instanceof ServerPlayer player)) {
            return;
        }
        MagicData data = MagicData.get(player);
        if (data == null) {
            return;
        }
        String type = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).toString();
        boolean changed = false;
        for (ActiveMission active : data.getMissions().active()) {
            Mission m = active.mission();
            if (m.type() == ObjectiveType.DERROTAR && m.target().equals(type) && active.progress() < m.amount()) {
                ActiveMission next = active.withProgress(active.progress() + 1);
                data.getMissions().update(next);
                changed = true;
                Component msg = next.progress() >= m.amount()
                        ? Component.translatable("message.hogwartsmod.mission_ready", Component.literal(m.title()), active.npcName())
                        : Component.translatable("message.hogwartsmod.mission_progress", m.targetName(), next.progress(), m.amount());
                player.displayClientMessage(msg.copy().withStyle(ChatFormatting.GOLD), true);
            }
        }
        if (changed) {
            data.markForSync();
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        MagicData data = MagicData.get(player);
        if (data == null || data.getMissions().active().isEmpty()) {
            return;
        }
        List<Zone> here = null;
        for (ActiveMission active : data.getMissions().active()) {
            Mission m = active.mission();
            if (m.type() != ObjectiveType.IR || active.progress() >= 1) {
                continue;
            }
            if (here == null) {
                here = Zones.at(player.level(), player.position());
            }
            for (Zone zone : here) {
                if (zone.name().equalsIgnoreCase(m.target())) {
                    data.getMissions().update(active.withProgress(1));
                    data.markForSync();
                    player.displayClientMessage(Component.translatable("message.hogwartsmod.mission_ready",
                            Component.literal(m.title()), active.npcName()).withStyle(ChatFormatting.GOLD), true);
                    player.level().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.7f, 1.4f);
                    break;
                }
            }
        }
    }
}
