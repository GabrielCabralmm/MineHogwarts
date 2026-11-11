package com.gabriel.hogwartsmod.network;

import com.gabriel.hogwartsmod.wand.BranchItem;
import com.gabriel.hogwartsmod.wand.WandBenchBlock;
import com.gabriel.hogwartsmod.wand.WandBenchMenu;
import com.gabriel.hogwartsmod.wand.WandCore;
import com.gabriel.hogwartsmod.wand.WandHandle;
import com.gabriel.hogwartsmod.wand.Wands;
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

public record CraftWandPacket(int quarters) {

    public static void encode(CraftWandPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.quarters);
    }

    public static CraftWandPacket decode(FriendlyByteBuf buf) {
        return new CraftWandPacket(buf.readVarInt());
    }

    public static void handle(CraftWandPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            craft(player, packet.quarters);
        }
        ctx.get().setPacketHandled(true);
    }

    public static boolean craft(ServerPlayer player, int quarters) {
        if (!(player.containerMenu instanceof WandBenchMenu menu) || !WandBenchBlock.canUse(player)) {
            return false;
        }
        ItemStack branch = menu.branch();
        WandCore core = WandCore.byItem(menu.core().getItem());
        if (!(branch.getItem() instanceof BranchItem item) || core == null) {
            return false;
        }
        ItemStack wand = com.gabriel.hogwartsmod.wand.WandBoxItem.filled(Wands.create(item.getWood(), core, quarters, WandHandle.NENHUM, player.getGameProfile().getName()));
        branch.shrink(1);
        menu.core().shrink(1);
        menu.slotsChanged(null);
        menu.broadcastChanges();
        Component name = wand.getHoverName();
        if (!player.getInventory().add(wand)) {
            player.drop(wand, false);
        }
        menu.access().execute((level, pos) -> {
            level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1f, 1.1f);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1f, 1.2f);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 30, 0.3, 0.3, 0.3, 0.6);
                server.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 6, 0.2, 0.15, 0.2, 0.02);
            }
        });
        player.displayClientMessage(Component.translatable("message.hogwartsmod.wand_crafted", name).withStyle(ChatFormatting.GOLD), true);
        return true;
    }
}
