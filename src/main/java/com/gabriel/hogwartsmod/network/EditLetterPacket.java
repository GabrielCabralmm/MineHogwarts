package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.mail.LetterItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record EditLetterPacket(InteractionHand hand, String to, String text, boolean seal) {

    public static void encode(EditLetterPacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.hand);
        buf.writeUtf(packet.to, LetterItem.MAX_TO);
        buf.writeUtf(packet.text, LetterItem.MAX_TEXT * 4);
        buf.writeBoolean(packet.seal);
    }

    public static EditLetterPacket decode(FriendlyByteBuf buf) {
        return new EditLetterPacket(buf.readEnum(InteractionHand.class), buf.readUtf(LetterItem.MAX_TO), buf.readUtf(LetterItem.MAX_TEXT * 4), buf.readBoolean());
    }

    public static void handle(EditLetterPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            apply(player, packet);
        }
        ctx.get().setPacketHandled(true);
    }

    private static void apply(ServerPlayer player, EditLetterPacket packet) {
        ItemStack held = player.getItemInHand(packet.hand);
        if (!(held.getItem() instanceof LetterItem) || LetterItem.isSealed(held)) {
            return;
        }
        String to = packet.to.strip();
        String text = packet.text.length() > LetterItem.MAX_TEXT ? packet.text.substring(0, LetterItem.MAX_TEXT) : packet.text;
        if (!packet.seal && to.equals(LetterItem.to(held)) && text.equals(LetterItem.text(held))) {
            return;
        }
        ItemStack letter = held.getCount() > 1 ? held.split(1) : held;
        LetterItem.write(letter, to, text);
        if (packet.seal) {
            LetterItem.seal(letter, player.getGameProfile().getName());
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.HONEYCOMB_WAX_ON, SoundSource.PLAYERS, 1f, 0.9f);
            player.serverLevel().sendParticles(ParticleTypes.WAX_ON, player.getX(), player.getEyeY() - 0.3, player.getZ(), 8, 0.3, 0.2, 0.3, 0.5);
        }
        if (letter != held && !player.getInventory().add(letter)) {
            player.drop(letter, false);
        }
    }
}
