package com.gabriel.hogwartsmod.wand;

import com.gabriel.hogwartsmod.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WandBoxItem extends Item {

    public WandBoxItem(Properties properties) {
        super(properties);
    }

    public static ItemStack filled(ItemStack wand) {
        ItemStack box = new ItemStack(ModItems.CAIXA_VARINHA.get());
        Wands.copyParts(wand, box);
        return box;
    }

    public static boolean isBlank(ItemStack box) {
        return Wands.wood(box) == null || Wands.core(box) == null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack box = player.getItemInHand(hand);
        if (isBlank(box)) {
            if (level.isClientSide) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.gabriel.hogwartsmod.client.gui.WandBoxScreen.open(hand));
            }
            return InteractionResultHolder.sidedSuccess(box, level.isClientSide);
        }
        if (player instanceof ServerPlayer sp) {
            ItemStack wand = new ItemStack(ModItems.WAND.get());
            Wands.copyParts(box, wand);
            open(sp, hand, wand);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    public static void open(ServerPlayer player, InteractionHand hand, ItemStack wand) {
        Wands.setOwner(wand, player);
        player.setItemInHand(hand, wand);
        player.level().playSound(null, player.blockPosition(), SoundEvents.CHEST_OPEN, SoundSource.PLAYERS, 0.5f, 1.6f);
        player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.3f);
        if (player.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getEyeY() - 0.3, player.getZ(), 30, 0.4, 0.3, 0.4, 0.8);
            server.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getEyeY() - 0.3, player.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
        }
        player.displayClientMessage(Component.translatable("message.hogwartsmod.wand_chosen").withStyle(ChatFormatting.GOLD), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (isBlank(stack)) {
            tooltip.add(Component.translatable("tooltip.hogwartsmod.wand_box.blank").withStyle(ChatFormatting.GRAY));
            return;
        }
        Wands.appendTooltip(stack, tooltip);
        tooltip.add(Component.translatable("tooltip.hogwartsmod.wand_box.open").withStyle(ChatFormatting.DARK_GRAY));
    }
}
