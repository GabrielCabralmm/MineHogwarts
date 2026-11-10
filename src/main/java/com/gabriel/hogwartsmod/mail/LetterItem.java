package com.gabriel.hogwartsmod.mail;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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

public class LetterItem extends Item {
    public static final int MAX_TO = 32;
    public static final int MAX_TEXT = 640;

    public LetterItem(Properties properties) {
        super(properties);
    }

    // -- Dados da carta

    public static boolean isSealed(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().getBoolean("Selada");
    }

    public static String to(ItemStack stack) {
        return stack.getTag() == null ? "" : stack.getTag().getString("Para");
    }

    public static String text(ItemStack stack) {
        return stack.getTag() == null ? "" : stack.getTag().getString("Texto");
    }

    public static String author(ItemStack stack) {
        return stack.getTag() == null ? "" : stack.getTag().getString("Autor");
    }

    public static void write(ItemStack stack, String to, String text) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("Para", to);
        tag.putString("Texto", text);
        if (to.isEmpty() && text.isEmpty()) {
            stack.setTag(null);
        }
    }

    public static void seal(ItemStack stack, String author) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean("Selada", true);
        tag.putString("Autor", author);
    }

    // -- Item

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.gabriel.hogwartsmod.client.gui.LetterScreen.open(hand, stack));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public Component getName(ItemStack stack) {
        String to = to(stack);
        if (!to.isEmpty()) {
            return Component.translatable("item.hogwartsmod.envelope.to", to);
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (isSealed(stack)) {
            tooltip.add(Component.translatable("item.hogwartsmod.envelope.sealed_by", author(stack)).withStyle(ChatFormatting.GOLD));
        } else if (stack.hasTag()) {
            tooltip.add(Component.translatable("item.hogwartsmod.envelope.draft").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        } else {
            tooltip.add(Component.translatable("item.hogwartsmod.envelope.hint").withStyle(ChatFormatting.GRAY));
        }
    }
}
