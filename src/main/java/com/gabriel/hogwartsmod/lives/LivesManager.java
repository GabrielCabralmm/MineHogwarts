package com.gabriel.hogwartsmod.lives;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.magic.MagicConfig;
import com.gabriel.hogwartsmod.magic.MagicData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = HogwartsMod.MOD_ID)
public final class LivesManager {

    private LivesManager() {
    }

    public static boolean isActive(MinecraftServer server) {
        return MagicConfig.LIVES_ENABLED.get() && (server.isDedicatedServer() || Boolean.getBoolean("hogwartsmod.vidasLocal"));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player) || !isActive(player.server)) {
            return;
        }
        MagicData data = MagicData.get(player);
        if (data == null) {
            return;
        }
        int left = data.getLives() - 1;
        data.setLives(left);
        if (left > 0) {
            player.sendSystemMessage(Component.translatable("message.hogwartsmod.lives_left", left).withStyle(ChatFormatting.RED));
            return;
        }
        finalDeath(player);
    }

    // -- Morte definitiva

    public static void finalDeath(ServerPlayer dead) {
        markWitnesses(dead);
        MinecraftServer server = dead.server;
        Component reason = Component.translatable("message.hogwartsmod.lives_final_kick");
        server.getPlayerList().broadcastSystemMessage(
                Component.translatable("message.hogwartsmod.lives_final", dead.getDisplayName()).withStyle(ChatFormatting.DARK_RED), false);
        server.tell(new TickTask(server.getTickCount() + 1, () -> {
            server.getPlayerList().getBans().add(new UserBanListEntry(dead.getGameProfile(), null, "Hogwarts", null, reason.getString()));
            dead.connection.disconnect(reason);
        }));
    }

    public static int markWitnesses(ServerPlayer dead) {
        int range = MagicConfig.LIVES_WITNESS_RANGE.get();
        int count = 0;
        for (ServerPlayer other : dead.serverLevel().players()) {
            if (other == dead || other.isSpectator() || other.distanceToSqr(dead) > range * range || !other.hasLineOfSight(dead)) {
                continue;
            }
            MagicData data = MagicData.get(other);
            if (data != null && !data.hasSeenDeath()) {
                data.setSeenDeath(true);
                other.sendSystemMessage(Component.translatable("message.hogwartsmod.thestral_sight").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
                count++;
            }
        }
        return count;
    }
}
