package com.gabriel.hogwartsmod.creature;

import com.gabriel.hogwartsmod.magic.MagicData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CreatureBagItem extends Item {

    public CreatureBagItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!player.isShiftKeyDown() || !(target instanceof TamableAnimal creature) || Species.of(target) == null) {
            return InteractionResult.PASS;
        }
        if (!creature.isOwnedBy(player)) {
            if (!player.level().isClientSide) {
                player.displayClientMessage(Component.translatable("message.hogwartsmod.creature_not_yours").withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            CreatureManager.store(serverPlayer, creature);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        if (level.isClientSide) {
            MagicData data = MagicData.get(player);
            if (data == null || data.getCreatures().all().isEmpty()) {
                player.displayClientMessage(Component.translatable("message.hogwartsmod.creature_bag_empty").withStyle(ChatFormatting.GRAY), true);
            } else {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.gabriel.hogwartsmod.client.gui.CreatureWheelScreen.open(hand));
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.hogwartsmod.bolsa_criaturas.hint_use").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("item.hogwartsmod.bolsa_criaturas.hint_store").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
