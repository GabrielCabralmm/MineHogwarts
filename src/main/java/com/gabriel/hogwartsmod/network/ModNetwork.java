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
    private static final String VERSION = "21";
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
        CHANNEL.messageBuilder(OpenNpcPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenNpcPacket::encode).decoder(OpenNpcPacket::decode).consumerMainThread(OpenNpcPacket::handle).add();
        CHANNEL.messageBuilder(NpcLearnPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(NpcLearnPacket::encode).decoder(NpcLearnPacket::decode).consumerMainThread(NpcLearnPacket::handle).add();
        CHANNEL.messageBuilder(DodgePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DodgePacket::encode).decoder(DodgePacket::decode).consumerMainThread(DodgePacket::handle).add();
        CHANNEL.messageBuilder(DodgeMovePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DodgeMovePacket::encode).decoder(DodgeMovePacket::decode).consumerMainThread(DodgeMovePacket::handle).add();
        CHANNEL.messageBuilder(PublicLocationsPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PublicLocationsPacket::encode).decoder(PublicLocationsPacket::decode).consumerMainThread(PublicLocationsPacket::handle).add();
        CHANNEL.messageBuilder(EditLetterPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(EditLetterPacket::encode).decoder(EditLetterPacket::decode).consumerMainThread(EditLetterPacket::handle).add();
        CHANNEL.messageBuilder(SendOwlPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SendOwlPacket::encode).decoder(SendOwlPacket::decode).consumerMainThread(SendOwlPacket::handle).add();
        CHANNEL.messageBuilder(PackagePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PackagePacket::encode).decoder(PackagePacket::decode).consumerMainThread(PackagePacket::handle).add();
        CHANNEL.messageBuilder(ApparatePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ApparatePacket::encode).decoder(ApparatePacket::decode).consumerMainThread(ApparatePacket::handle).add();
        CHANNEL.messageBuilder(PlaceEditPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PlaceEditPacket::encode).decoder(PlaceEditPacket::decode).consumerMainThread(PlaceEditPacket::handle).add();
        CHANNEL.messageBuilder(BroomDismountPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(BroomDismountPacket::encode).decoder(BroomDismountPacket::decode).consumerMainThread(BroomDismountPacket::handle).add();
        CHANNEL.messageBuilder(BroomBoostPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(BroomBoostPacket::encode).decoder(BroomBoostPacket::decode).consumerMainThread(BroomBoostPacket::handle).add();
        CHANNEL.messageBuilder(PulpitLessonPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PulpitLessonPacket::encode).decoder(PulpitLessonPacket::decode).consumerMainThread(PulpitLessonPacket::handle).add();
        CHANNEL.messageBuilder(ZonesPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ZonesPacket::encode).decoder(ZonesPacket::decode).consumerMainThread(ZonesPacket::handle).add();
        CHANNEL.messageBuilder(ZoneSelectionPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ZoneSelectionPacket::encode).decoder(ZoneSelectionPacket::decode).consumerMainThread(ZoneSelectionPacket::handle).add();
        CHANNEL.messageBuilder(ZoneMarkPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ZoneMarkPacket::encode).decoder(ZoneMarkPacket::decode).consumerMainThread(ZoneMarkPacket::handle).add();
        CHANNEL.messageBuilder(ZoneEditPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ZoneEditPacket::encode).decoder(ZoneEditPacket::decode).consumerMainThread(ZoneEditPacket::handle).add();
        CHANNEL.messageBuilder(HousePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(HousePacket::encode).decoder(HousePacket::decode).consumerMainThread(HousePacket::handle).add();
        CHANNEL.messageBuilder(SortingHatPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SortingHatPacket::encode).decoder(SortingHatPacket::decode).consumerMainThread(SortingHatPacket::handle).add();
        CHANNEL.messageBuilder(OpenSortingPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenSortingPacket::encode).decoder(OpenSortingPacket::decode).consumerMainThread(OpenSortingPacket::handle).add();
        CHANNEL.messageBuilder(SortingChoosePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SortingChoosePacket::encode).decoder(SortingChoosePacket::decode).consumerMainThread(SortingChoosePacket::handle).add();
        CHANNEL.messageBuilder(HousePointsPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(HousePointsPacket::encode).decoder(HousePointsPacket::decode).consumerMainThread(HousePointsPacket::handle).add();
        CHANNEL.messageBuilder(PouchActionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PouchActionPacket::encode).decoder(PouchActionPacket::decode).consumerMainThread(PouchActionPacket::handle).add();
        CHANNEL.messageBuilder(OpenMissionsPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenMissionsPacket::encode).decoder(OpenMissionsPacket::decode).consumerMainThread(OpenMissionsPacket::handle).add();
        CHANNEL.messageBuilder(OpenMissionEditorPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenMissionEditorPacket::encode).decoder(OpenMissionEditorPacket::decode).consumerMainThread(OpenMissionEditorPacket::handle).add();
        CHANNEL.messageBuilder(MissionActionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(MissionActionPacket::encode).decoder(MissionActionPacket::decode).consumerMainThread(MissionActionPacket::handle).add();
        CHANNEL.messageBuilder(MissionJournalPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(MissionJournalPacket::encode).decoder(MissionJournalPacket::decode).consumerMainThread(MissionJournalPacket::handle).add();
        CHANNEL.messageBuilder(OpenShopPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenShopPacket::encode).decoder(OpenShopPacket::decode).consumerMainThread(OpenShopPacket::handle).add();
        CHANNEL.messageBuilder(OpenShopEditorPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenShopEditorPacket::encode).decoder(OpenShopEditorPacket::decode).consumerMainThread(OpenShopEditorPacket::handle).add();
        CHANNEL.messageBuilder(ShopBuyPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ShopBuyPacket::encode).decoder(ShopBuyPacket::decode).consumerMainThread(ShopBuyPacket::handle).add();
        CHANNEL.messageBuilder(SaveShopNpcPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SaveShopNpcPacket::encode).decoder(SaveShopNpcPacket::decode).consumerMainThread(SaveShopNpcPacket::handle).add();
        CHANNEL.messageBuilder(SummonCreaturePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SummonCreaturePacket::encode).decoder(SummonCreaturePacket::decode).consumerMainThread(SummonCreaturePacket::handle).add();
        CHANNEL.messageBuilder(SaveMissionNpcPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SaveMissionNpcPacket::encode).decoder(SaveMissionNpcPacket::decode).consumerMainThread(SaveMissionNpcPacket::handle).add();
        CHANNEL.messageBuilder(RemoveNpcPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RemoveNpcPacket::encode).decoder(RemoveNpcPacket::decode).consumerMainThread(RemoveNpcPacket::handle).add();
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
        tag.putBoolean("VidasAtivas", com.gabriel.hogwartsmod.lives.LivesManager.isActive(player.server));
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncMagicPacket(tag));
        data.clearSync();
    }
}
