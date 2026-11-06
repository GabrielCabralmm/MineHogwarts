package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.magic.MagicConfig;
import com.gabriel.hogwartsmod.magic.MagicData;
import com.gabriel.hogwartsmod.teach.TeachRules;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String VERSION = "3";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private ModNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(SyncMagicPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncMagicPacket::encode).decoder(SyncMagicPacket::decode).consumerMainThread(SyncMagicPacket::handle).add();
        CHANNEL.messageBuilder(SelectSlotPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SelectSlotPacket::encode).decoder(SelectSlotPacket::decode).consumerMainThread(SelectSlotPacket::handle).add();
        CHANNEL.messageBuilder(CastAnimationPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CastAnimationPacket::encode).decoder(CastAnimationPacket::decode).consumerMainThread(CastAnimationPacket::handle).add();
        CHANNEL.messageBuilder(UpdateSlotsPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UpdateSlotsPacket::encode).decoder(UpdateSlotsPacket::decode).consumerMainThread(UpdateSlotsPacket::handle).add();
        CHANNEL.messageBuilder(PlayerFxPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PlayerFxPacket::encode).decoder(PlayerFxPacket::decode).consumerMainThread(PlayerFxPacket::handle).add();
        CHANNEL.messageBuilder(RequestStudentsPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestStudentsPacket::encode).decoder(RequestStudentsPacket::decode).consumerMainThread(RequestStudentsPacket::handle).add();
        CHANNEL.messageBuilder(StudentsPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(StudentsPacket::encode).decoder(StudentsPacket::decode).consumerMainThread(StudentsPacket::handle).add();
        CHANNEL.messageBuilder(InviteStudentPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(InviteStudentPacket::encode).decoder(InviteStudentPacket::decode).consumerMainThread(InviteStudentPacket::handle).add();
        CHANNEL.messageBuilder(OpenTracePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenTracePacket::encode).decoder(OpenTracePacket::decode).consumerMainThread(OpenTracePacket::handle).add();
        CHANNEL.messageBuilder(TraceResultPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TraceResultPacket::encode).decoder(TraceResultPacket::decode).consumerMainThread(TraceResultPacket::handle).add();
        CHANNEL.messageBuilder(OpenPulpitPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenPulpitPacket::encode).decoder(OpenPulpitPacket::decode).consumerMainThread(OpenPulpitPacket::handle).add();
        CHANNEL.messageBuilder(PulpitAreaPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PulpitAreaPacket::encode).decoder(PulpitAreaPacket::decode).consumerMainThread(PulpitAreaPacket::handle).add();
        CHANNEL.messageBuilder(PulpitLessonPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PulpitLessonPacket::encode).decoder(PulpitLessonPacket::decode).consumerMainThread(PulpitLessonPacket::handle).add();
    }

    public static void playAnimation(ServerPlayer player, String animation) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new CastAnimationPacket(player.getId(), animation));
    }

    public static void stopAnimation(ServerPlayer player) {
        playAnimation(player, "");
    }

    public static void sync(ServerPlayer player, MagicData data) {
        CompoundTag tag = data.save();
        TeachRules.Limits limits = TeachRules.Limits.fromConfig();
        tag.putInt("TeachMinLevel", limits.minLevel());
        tag.putInt("TeachMaxSpellLevel", limits.maxSpellLevel());
        tag.putInt("TeachRange", MagicConfig.INVITE_RANGE.get());
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncMagicPacket(tag));
        data.clearSync();
    }
}
