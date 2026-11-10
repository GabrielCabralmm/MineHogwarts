package com.gabriel.hogwartsmod.potion;

import com.gabriel.hogwartsmod.magic.MagicData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RecipeScrollItem extends Item {
    private final Brew brew;

    public RecipeScrollItem(Brew brew, Properties properties) {
        super(properties);
        this.brew = brew;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        MagicData data = MagicData.get(player);
        if (data == null) {
            return InteractionResultHolder.pass(stack);
        }
        if (!data.learnRecipe(brew)) {
            player.displayClientMessage(Component.translatable("message.hogwartsmod.recipe_known", brew.getDisplayName())
                    .withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 0.9f);
        level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.5f, 1.4f);
        player.displayClientMessage(Component.translatable("message.hogwartsmod.recipe_learned", brew.getDisplayName())
                .withStyle(ChatFormatting.GOLD), true);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.hogwartsmod.receita.hint", brew.getDisplayName())
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
