package com.gabriel.hogwartsmod.item;

import com.gabriel.hogwartsmod.house.SortingManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SortingHatItem extends Item {

    public SortingHatItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Player)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer sorter && target instanceof ServerPlayer sorted) {
            SortingManager.start(sorter, sorted);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.hogwartsmod.chapeu_seletor.tip").withStyle(ChatFormatting.GRAY));
    }
}
