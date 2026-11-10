package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.creature.OwlEntity;
import com.gabriel.hogwartsmod.mail.LetterItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record SendOwlPacket(int owl, InteractionHand hand, UUID recipient) {

    public static void encode(SendOwlPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.owl);
        buf.writeEnum(packet.hand);
        buf.writeUUID(packet.recipient);
    }

    public static SendOwlPacket decode(FriendlyByteBuf buf) {
        return new SendOwlPacket(buf.readVarInt(), buf.readEnum(InteractionHand.class), buf.readUUID());
    }

    public static void handle(SendOwlPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            send(player, packet);
        }
        ctx.get().setPacketHandled(true);
    }

    public static boolean send(ServerPlayer player, SendOwlPacket packet) {
        if (!(player.level().getEntity(packet.owl) instanceof OwlEntity owl) || !owl.isOwnedBy(player) || owl.isBusy()
                || owl.distanceToSqr(player) > 100) {
            return false;
        }
        ItemStack held = player.getItemInHand(packet.hand);
        ServerPlayer target = player.server.getPlayerList().getPlayer(packet.recipient);
        if (!(held.getItem() instanceof LetterItem) || !LetterItem.isSealed(held) || target == null) {
            return false;
        }
        owl.startDelivery(held.split(1), player, target);
        player.displayClientMessage(Component.translatable("message.hogwartsmod.owl_sent", target.getGameProfile().getName()).withStyle(ChatFormatting.GOLD), true);
        return true;
    }
}
