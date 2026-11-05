package com.gabriel.hogwartsmod.client;

import com.gabriel.hogwartsmod.magic.MagicData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.nbt.CompoundTag;

public final class ClientPacketHandler {

    private ClientPacketHandler() {
    }

    public static void handleSync(CompoundTag tag) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            MagicData data = MagicData.get(player);
            if (data != null) {
                data.load(tag);
            }
        }
    }

    public static void handleCastAnimation(int entityId, String animation) {
        var level = Minecraft.getInstance().level;
        if (level != null && level.getEntity(entityId) instanceof AbstractClientPlayer player) {
            CastAnimations.play(player, animation);
        }
    }
}
