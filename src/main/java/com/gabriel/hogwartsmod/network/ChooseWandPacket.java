package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.wand.WandBoxItem;
import com.gabriel.hogwartsmod.wand.WandCore;
import com.gabriel.hogwartsmod.wand.WandHandle;
import com.gabriel.hogwartsmod.wand.WandWood;
import com.gabriel.hogwartsmod.wand.Wands;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ChooseWandPacket(InteractionHand hand, WandWood wood, WandCore core, int quarters) {

    public static void encode(ChooseWandPacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.hand);
        buf.writeEnum(packet.wood);
        buf.writeEnum(packet.core);
        buf.writeVarInt(packet.quarters);
    }

    public static ChooseWandPacket decode(FriendlyByteBuf buf) {
        return new ChooseWandPacket(buf.readEnum(InteractionHand.class), buf.readEnum(WandWood.class), buf.readEnum(WandCore.class), buf.readVarInt());
    }

    public static void handle(ChooseWandPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            choose(player, packet.hand, packet.wood, packet.core, packet.quarters);
        }
        ctx.get().setPacketHandled(true);
    }

    public static boolean choose(ServerPlayer player, InteractionHand hand, WandWood wood, WandCore core, int quarters) {
        ItemStack box = player.getItemInHand(hand);
        if (!(box.getItem() instanceof WandBoxItem) || !WandBoxItem.isBlank(box) || !wood.isCommon() || !core.isCommon()) {
            return false;
        }
        WandBoxItem.open(player, hand, Wands.create(wood, core, quarters, WandHandle.NENHUM, null));
        return true;
    }
}
