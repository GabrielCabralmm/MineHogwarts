package com.gabriel.hogwartsmod.house;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.magic.MagicData;
import com.gabriel.hogwartsmod.network.ModNetwork;
import com.gabriel.hogwartsmod.network.OpenSortingPacket;
import com.gabriel.hogwartsmod.network.SortingHatPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = HogwartsMod.MOD_ID)
public final class SortingManager {
    private static final int CHOOSE_TICKS = 20 * 90;
    private static final int AFTER_TICKS = 50;
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private static final class Session {
        final UUID sorter;
        long endAt;
        boolean decided;

        Session(UUID sorter, long endAt) {
            this.sorter = sorter;
            this.endAt = endAt;
        }
    }

    private SortingManager() {
    }

    public static boolean canSort(ServerPlayer player) {
        MagicData data = MagicData.get(player);
        return player.hasPermissions(2) || data != null && data.isProfessor();
    }

    public static void start(ServerPlayer sorter, ServerPlayer target) {
        if (!canSort(sorter)) {
            sorter.displayClientMessage(Component.translatable("message.hogwartsmod.hat_denied").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        Session current = SESSIONS.get(target.getUUID());
        if (current != null && !current.sorter.equals(sorter.getUUID())) {
            sorter.displayClientMessage(Component.translatable("message.hogwartsmod.hat_busy", target.getDisplayName()).withStyle(ChatFormatting.GRAY), true);
            return;
        }
        long now = target.level().getGameTime();
        SESSIONS.put(target.getUUID(), new Session(sorter.getUUID(), now + CHOOSE_TICKS));
        hat(target, true);
        target.level().playSound(null, target.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 1f, 0.8f);
        target.displayClientMessage(Component.translatable("message.hogwartsmod.hat_on_target").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), true);
        House currentHouse = HouseManager.of(target);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sorter),
                new OpenSortingPacket(target.getUUID(), target.getGameProfile().getName(), currentHouse == null ? "" : currentHouse.getId()));
    }

    public static void choose(ServerPlayer sorter, UUID targetId, @Nullable House house) {
        Session session = SESSIONS.get(targetId);
        if (session == null || session.decided || !session.sorter.equals(sorter.getUUID())) {
            return;
        }
        ServerPlayer target = sorter.server.getPlayerList().getPlayer(targetId);
        if (target == null) {
            SESSIONS.remove(targetId);
            return;
        }
        if (house == null) {
            end(target);
            return;
        }
        session.decided = true;
        session.endAt = target.level().getGameTime() + AFTER_TICKS;
        HouseManager.set(target, house);
        announce(sorter.server, target, house);
    }

    public static void announce(MinecraftServer server, ServerPlayer target, House house) {
        Component name = Component.literal(target.getGameProfile().getName()).withStyle(ChatFormatting.WHITE);
        Component house1 = house.getDisplayName().copy().withStyle(ChatFormatting.BOLD);
        server.getPlayerList().broadcastSystemMessage(Component.translatable("message.hogwartsmod.sorted", name, house1).withStyle(ChatFormatting.GRAY), false);
        target.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 20));
        target.connection.send(new ClientboundSetTitleTextPacket(house.getDisplayName().copy().withStyle(ChatFormatting.BOLD)));
        target.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("message.hogwartsmod.sorted_subtitle").withStyle(ChatFormatting.GRAY)));
        target.level().playSound(null, target.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.9f, 1f);
    }

    private static void hat(ServerPlayer target, boolean on) {
        ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target), new SortingHatPacket(target.getId(), on));
    }

    private static void end(ServerPlayer target) {
        SESSIONS.remove(target.getUUID());
        hat(target, false);
    }

    public static boolean isWearing(ServerPlayer target) {
        return SESSIONS.containsKey(target.getUUID());
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || SESSIONS.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        Iterator<Map.Entry<UUID, Session>> it = SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Session> entry = it.next();
            ServerPlayer target = server.getPlayerList().getPlayer(entry.getKey());
            if (target == null) {
                it.remove();
                continue;
            }
            Session session = entry.getValue();
            ServerPlayer sorter = server.getPlayerList().getPlayer(session.sorter);
            boolean sorterGone = !session.decided && (sorter == null || sorter.distanceToSqr(target) > 16 * 16);
            if (target.level().getGameTime() >= session.endAt || sorterGone) {
                it.remove();
                hat(target, false);
            }
        }
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && SESSIONS.containsKey(target.getUUID())
                && event.getEntity() instanceof ServerPlayer viewer) {
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer), new SortingHatPacket(target.getId(), true));
        }
    }
}
