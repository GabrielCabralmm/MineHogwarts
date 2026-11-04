package com.gabriel.hogwartsmod.item;

import com.gabriel.hogwartsmod.spell.Spell;
import com.gabriel.hogwartsmod.spell.SpellCaster;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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

public class WandItem extends Item {
    private static final String SPELL_TAG = "SelectedSpell";

    public WandItem(Properties properties) {
        super(properties);
    }

    public static Spell getSelectedSpell(ItemStack stack) {
        return Spell.byId(stack.getOrCreateTag().getString(SPELL_TAG));
    }

    public static void setSelectedSpell(ItemStack stack, Spell spell) {
        stack.getOrCreateTag().putString(SPELL_TAG, spell.getId());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Spell spell = getSelectedSpell(stack);

        if (player.isShiftKeyDown()) {
            Spell next = spell.next();
            if (!level.isClientSide) {
                setSelectedSpell(stack, next);
                player.displayClientMessage(Component.translatable("message.hogwartsmod.spell_selected",
                        next.getDisplayName().copy().withStyle(ChatFormatting.GOLD)), true);
                level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
            return InteractionResultHolder.success(stack);
        }

        if (level instanceof ServerLevel server) {
            SpellCaster.cast(server, player, spell);
            player.displayClientMessage(spell.getDisplayName().copy().withStyle(ChatFormatting.ITALIC, ChatFormatting.AQUA), true);
        }
        player.getCooldowns().addCooldown(this, spell.getCooldownTicks());
        player.swing(hand, true);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.hogwartsmod.wand.spell",
                getSelectedSpell(stack).getDisplayName().copy().withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.hogwartsmod.wand.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
