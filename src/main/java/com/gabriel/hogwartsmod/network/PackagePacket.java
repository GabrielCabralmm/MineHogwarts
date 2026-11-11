package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.mail.Mail;
import com.gabriel.hogwartsmod.mail.PackageItem;
import com.gabriel.hogwartsmod.mail.PackageMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record PackagePacket(InteractionHand hand, boolean seal) {

    public static void encode(PackagePacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.hand);
        buf.writeBoolean(packet.seal);
    }

    public static PackagePacket decode(FriendlyByteBuf buf) {
        return new PackagePacket(buf.readEnum(InteractionHand.class), buf.readBoolean());
    }

    public static void handle(PackagePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            if (packet.seal) {
                seal(player, packet.hand);
            } else {
                unpack(player, packet.hand);
            }
        }
        ctx.get().setPacketHandled(true);
    }

    public static boolean seal(ServerPlayer player, InteractionHand hand) {
        ItemStack box = player.getItemInHand(hand);
        if (!(box.getItem() instanceof PackageItem) || Mail.isSealed(box)) {
            return false;
        }
        if (player.containerMenu instanceof PackageMenu menu) {
            PackageItem.setContent(box, menu.content().copy());
        }
        if (PackageItem.content(box).isEmpty()) {
            return false;
        }
        PackageItem.seal(box, player.getGameProfile().getName());
        player.closeContainer();
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.HONEYCOMB_WAX_ON, SoundSource.PLAYERS, 1f, 0.9f);
        player.serverLevel().sendParticles(ParticleTypes.WAX_ON, player.getX(), player.getEyeY() - 0.3, player.getZ(), 8, 0.3, 0.2, 0.3, 0.5);
        player.displayClientMessage(Component.translatable("message.hogwartsmod.package_sealed").withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    public static boolean unpack(ServerPlayer player, InteractionHand hand) {
        ItemStack box = player.getItemInHand(hand);
        if (!(box.getItem() instanceof PackageItem) || !Mail.isSealed(box)) {
            return false;
        }
        ItemStack content = PackageItem.content(box);
        player.setItemInHand(hand, ItemStack.EMPTY);
        if (!content.isEmpty() && !player.getInventory().add(content)) {
            player.drop(content, false);
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.PLAYERS, 1f, 1f);
        return true;
    }
}
