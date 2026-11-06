package com.gabriel.hogwartsmod.zone;

import com.gabriel.hogwartsmod.network.ModNetwork;
import com.gabriel.hogwartsmod.network.ZoneSelectionPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ZoneSelections {
    private static final Map<UUID, Selection> SELECTIONS = new HashMap<>();

    public record Selection(String dimension, @Nullable BlockPos first, @Nullable BlockPos second, boolean cut) {
        public boolean complete() {
            return first != null && second != null;
        }
    }

    private ZoneSelections() {
    }

    @Nullable
    public static Selection get(ServerPlayer player) {
        Selection selection = SELECTIONS.get(player.getUUID());
        return selection != null && selection.dimension().equals(dimensionOf(player)) ? selection : null;
    }

    public static void mark(ServerPlayer player, BlockPos pos, boolean first, boolean cut) {
        Selection old = get(player);
        if (old != null && old.cut() != cut) {
            old = null;
        }
        BlockPos a = first ? pos : old == null ? null : old.first();
        BlockPos b = first ? (old == null ? null : old.second()) : pos;
        Selection selection = new Selection(dimensionOf(player), a, b, cut);
        SELECTIONS.put(player.getUUID(), selection);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ZoneSelectionPacket(a, b, cut));
        Component coords = Component.literal(pos.getX() + " " + pos.getY() + " " + pos.getZ());
        Component message = Component.translatable(first ? "message.hogwartsmod.zone_first" : "message.hogwartsmod.zone_second", coords);
        if (cut) {
            message = Component.translatable("message.hogwartsmod.zone_cut_prefix").append(" · ").append(message);
        }
        if (selection.complete()) {
            message = message.copy().append(" · ").append(Component.translatable("message.hogwartsmod.zone_size",
                    Math.abs(a.getX() - b.getX()) + 1, Math.abs(a.getY() - b.getY()) + 1, Math.abs(a.getZ() - b.getZ()) + 1));
        }
        for (Zone zone : ZoneData.get(player.server).all()) {
            if (zone.contains(selection.dimension(), pos)) {
                message = message.copy().append(" · ").append(Component.translatable("message.hogwartsmod.zone_inside", zone.name()));
                break;
            }
        }
        player.displayClientMessage(message.copy().withStyle(cut ? ChatFormatting.RED : ChatFormatting.GOLD), true);
    }

    public static void clear(ServerPlayer player) {
        SELECTIONS.remove(player.getUUID());
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ZoneSelectionPacket(null, null, false));
    }

    public static String dimensionOf(ServerPlayer player) {
        return player.level().dimension().location().toString();
    }
}
