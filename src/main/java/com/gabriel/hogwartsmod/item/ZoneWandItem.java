package com.gabriel.hogwartsmod.item;

import com.gabriel.hogwartsmod.zone.ZoneSelections;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ZoneWandItem extends Item {

    public ZoneWandItem(Properties properties) {
        super(properties);
    }

    public static boolean canUse(Player player) {
        return player.hasPermissions(2);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (!canUse(player)) {
            denied(player);
            return InteractionResult.FAIL;
        }
        if (player instanceof ServerPlayer sp) {
            ZoneSelections.mark(sp, context.getClickedPos(), false, sp.isShiftKeyDown());
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!canUse(player)) {
            denied(player);
            return InteractionResultHolder.fail(stack);
        }
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> com.gabriel.hogwartsmod.client.gui.ZoneScreen::open);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public static void markFirst(Player player, BlockPos pos, boolean cut) {
        if (!canUse(player)) {
            denied(player);
            return;
        }
        if (player instanceof ServerPlayer sp) {
            ZoneSelections.mark(sp, pos, true, cut);
        }
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.hogwartsmod.varinha_zonas.tip1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.hogwartsmod.varinha_zonas.tip2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.hogwartsmod.varinha_zonas.tip3").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.hogwartsmod.varinha_zonas.tip4").withStyle(ChatFormatting.RED));
    }

    private static void denied(Player player) {
        if (!player.level().isClientSide) {
            player.displayClientMessage(Component.translatable("message.hogwartsmod.zone_staff_only").withStyle(ChatFormatting.GRAY), true);
        }
    }
}
