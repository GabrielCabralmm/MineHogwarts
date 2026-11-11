package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.broom.BroomBenchBlock;
import com.gabriel.hogwartsmod.broom.BroomBenchMenu;
import com.gabriel.hogwartsmod.broom.BroomHandleItem;
import com.gabriel.hogwartsmod.broom.BroomType;
import com.gabriel.hogwartsmod.broom.Brooms;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record CraftBroomPacket(BroomType type) {

    public static void encode(CraftBroomPacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.type);
    }

    public static CraftBroomPacket decode(FriendlyByteBuf buf) {
        return new CraftBroomPacket(buf.readEnum(BroomType.class));
    }

    public static void handle(CraftBroomPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            craft(player, packet.type);
        }
        ctx.get().setPacketHandled(true);
    }

    public static boolean craft(ServerPlayer player, BroomType type) {
        if (type == BroomType.STANDARD || !(player.containerMenu instanceof BroomBenchMenu menu) || !BroomBenchBlock.canUse(player) || !menu.hasParts(type)) {
            return false;
        }
        BroomHandleItem handle = (BroomHandleItem) menu.handle().getItem();
        ItemStack broom = Brooms.create(type, handle.getWood(), player.getGameProfile().getName());
        menu.handle().shrink(1);
        menu.bristles().shrink(BroomBenchMenu.BRISTLES);
        if (type == BroomType.CARGO) {
            menu.chest().shrink(1);
        }
        menu.slotsChanged(null);
        menu.broadcastChanges();
        Component name = broom.getHoverName();
        if (!player.getInventory().add(broom)) {
            player.drop(broom, false);
        }
        menu.access().execute((level, pos) -> {
            level.playSound(null, pos, SoundEvents.VILLAGER_WORK_FLETCHER, SoundSource.BLOCKS, 1f, 1f);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8f, 1.2f);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 12, 0.3, 0.1, 0.3, 0.02);
                server.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 6, 0.3, 0.15, 0.3, 0.02);
            }
        });
        player.displayClientMessage(Component.translatable("message.hogwartsmod.broom_crafted", name).withStyle(ChatFormatting.GOLD), true);
        return true;
    }
}
