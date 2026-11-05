package com.gabriel.hogwartsmod.item;

import com.gabriel.hogwartsmod.magic.MagicData;
import com.gabriel.hogwartsmod.network.ModNetwork;
import com.gabriel.hogwartsmod.spell.Spell;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class WandItem extends Item {

    public WandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        MagicData data = MagicData.get(player);
        if (data == null) {
            return InteractionResultHolder.pass(stack);
        }
        Spell spell = data.getActiveSpell();
        long now = level.getGameTime();
        if (spell == null) {
            notify(player, level, Component.translatable("message.hogwartsmod.no_spell"));
            return InteractionResultHolder.fail(stack);
        }
        if (!data.knows(spell)) {
            notify(player, level, Component.translatable("message.hogwartsmod.not_learned", spell.getDisplayName()));
            return InteractionResultHolder.fail(stack);
        }
        if (data.hasPendingCast()) {
            return InteractionResultHolder.fail(stack);
        }
        if (spell.isFreeCast(data, now)) {
            if (player instanceof ServerPlayer sp) {
                spell.cast(sp, data);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        int cooldown = data.getCooldownRemaining(spell, now);
        if (cooldown > 0) {
            notify(player, level, Component.translatable("message.hogwartsmod.cooldown", spell.getDisplayName(),
                    String.format("%.1f", cooldown / 20f)));
            return InteractionResultHolder.fail(stack);
        }
        if (data.getReserve() < spell.getCost()) {
            notify(player, level, Component.translatable("message.hogwartsmod.no_reserve"));
            return InteractionResultHolder.fail(stack);
        }
        if (spell.isSustained()) {
            if (player instanceof ServerPlayer sp) {
                if (!spell.startHold(sp, data)) {
                    return InteractionResultHolder.fail(stack);
                }
                data.spend(spell.getCost());
                ModNetwork.playAnimation(sp, spell.getDelivery().getAnimation());
            }
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        if (player instanceof ServerPlayer sp) {
            data.spend(spell.getCost());
            data.setCooldown(spell, now, spell.getCooldownTicks());
            ModNetwork.playAnimation(sp, spell.getDelivery().getAnimation());
            int release = spell.getDelivery().getReleaseTick();
            if (release <= 0) {
                spell.cast(sp, data);
            } else {
                data.schedule(spell, now + release);
            }
            data.markForSync();
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        MagicData data = MagicData.get(player);
        Spell spell = data == null ? null : data.getActiveSpell();
        if (spell == null || !spell.isSustained()) {
            player.stopUsingItem();
            return;
        }
        int held = getUseDuration(stack) - remaining;
        if (!spell.tickHold(player, data, held)) {
            player.stopUsingItem();
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        MagicData data = MagicData.get(player);
        Spell spell = data == null ? null : data.getActiveSpell();
        if (spell != null && spell.isSustained()) {
            spell.stopHold(player, data);
            data.setCooldown(spell, level.getGameTime(), spell.getCooldownTicks());
            ModNetwork.stopAnimation(player);
        }
        if (data != null) {
            data.stopShield();
        }
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    private static void notify(Player player, Level level, Component message) {
        if (!level.isClientSide) {
            player.displayClientMessage(message.copy().withStyle(ChatFormatting.GRAY), true);
        }
    }
}
