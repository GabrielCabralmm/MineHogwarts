package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.magic.MagicData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String VERSION = "1";
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
    }

    public static void playAnimation(ServerPlayer player, String animation) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new CastAnimationPacket(player.getId(), animation));
    }

    public static void stopAnimation(ServerPlayer player) {
        playAnimation(player, "");
    }

    public static void sync(ServerPlayer player, MagicData data) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncMagicPacket(data.save()));
        data.clearSync();
    }
}
