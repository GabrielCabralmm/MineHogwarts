package com.gabriel.hogwartsmod.potion;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PotionBookItem extends Item {
    private final int edition;

    public PotionBookItem(int edition, Properties properties) {
        super(properties.rarity(edition >= 5 ? Rarity.EPIC : edition >= 4 ? Rarity.RARE : edition >= 3 ? Rarity.UNCOMMON : Rarity.COMMON));
        this.edition = edition;
    }

    public int getEdition() {
        return edition;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            player.playSound(SoundEvents.BOOK_PAGE_TURN, 1f, 1f);
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.gabriel.hogwartsmod.client.gui.PotionBookScreen.open(edition));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int count = Brew.ofDifficulty(edition).size();
        tooltip.add(Component.translatable("item.hogwartsmod.livro_pocoes.level", edition).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.hogwartsmod.livro_pocoes.count", count).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
