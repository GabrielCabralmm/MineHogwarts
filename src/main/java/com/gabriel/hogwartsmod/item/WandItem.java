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
        if (player.getVehicle() instanceof com.gabriel.hogwartsmod.entity.FlyingMount) {
            notify(player, level, Component.translatable("message.hogwartsmod.no_cast_flying"));
            return InteractionResultHolder.fail(stack);
        }
        data.setWandStats(com.gabriel.hogwartsmod.wand.Wands.effective(player, stack));
        Spell spell = data.getActiveSpell();
        long now = level.getGameTime();
        if (spell == null) {
            notify(player, level, Component.translatable("message.hogwartsmod.no_spell"));
            return InteractionResultHolder.fail(stack);
        }
        if (isCombat(spell) && com.gabriel.hogwartsmod.zone.Zones.blocks(player, com.gabriel.hogwartsmod.zone.ZoneRule.COMBATE)) {
            com.gabriel.hogwartsmod.zone.Zones.warn(player, com.gabriel.hogwartsmod.zone.ZoneRule.COMBATE);
            return InteractionResultHolder.fail(stack);
        }
        if (player instanceof ServerPlayer duelist && com.gabriel.hogwartsmod.duel.DuelManager.blocksSpell(duelist, spell)) {
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
        if (data.getReserve() < data.scaledCost(spell)) {
            notify(player, level, Component.translatable("message.hogwartsmod.no_reserve"));
            return InteractionResultHolder.fail(stack);
        }
        if (spell.isSustained()) {
            if (player instanceof ServerPlayer sp) {
                if (!spell.startHold(sp, data)) {
                    return InteractionResultHolder.fail(stack);
                }
                data.spend(data.scaledCost(spell));
                ModNetwork.playAnimation(sp, spell.getDelivery().getAnimation());
            }
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        if (player instanceof ServerPlayer sp) {
            data.spend(data.scaledCost(spell));
            data.setCooldown(spell, now, data.scaledCooldown(spell));
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
        data.setWandStats(com.gabriel.hogwartsmod.wand.Wands.effective(player, stack));
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
            data.setCooldown(spell, level.getGameTime(), data.scaledCooldown(spell));
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

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack wand, ItemStack carried, net.minecraft.world.inventory.Slot slot, net.minecraft.world.inventory.ClickAction action,
                                            Player player, net.minecraft.world.entity.SlotAccess access) {
        if (action != net.minecraft.world.inventory.ClickAction.SECONDARY || !(carried.getItem() instanceof com.gabriel.hogwartsmod.wand.HandleItem item)
                || !com.gabriel.hogwartsmod.wand.Wands.isCustom(wand) || !slot.allowModification(player)) {
            return false;
        }
        com.gabriel.hogwartsmod.wand.WandHandle old = com.gabriel.hogwartsmod.wand.Wands.handle(wand);
        if (old == item.getHandle()) {
            return true;
        }
        com.gabriel.hogwartsmod.wand.Wands.setHandle(wand, item.getHandle());
        slot.setChanged();
        carried.shrink(1);
        net.minecraft.world.item.Item oldItem = old.getItem();
        if (oldItem != null) {
            ItemStack back = new ItemStack(oldItem);
            if (carried.isEmpty()) {
                access.set(back);
            } else if (!player.getInventory().add(back)) {
                player.drop(back, false);
            }
        }
        player.playSound(net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER, 0.8f, 1.3f);
        return true;
    }

    @Override
    public Component getName(ItemStack stack) {
        com.gabriel.hogwartsmod.wand.WandWood wood = com.gabriel.hogwartsmod.wand.Wands.wood(stack);
        if (wood != null && com.gabriel.hogwartsmod.wand.Wands.isCustom(stack)) {
            String owner = com.gabriel.hogwartsmod.wand.Wands.owner(stack);
            return Component.translatable("item.hogwartsmod.wand.custom", owner.isEmpty() ? wood.getDisplayName() : Component.literal(owner));
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @org.jetbrains.annotations.Nullable Level level, java.util.List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        com.gabriel.hogwartsmod.wand.Wands.appendTooltip(stack, tooltip);
    }

    public static boolean isCombat(Spell spell) {
        return switch (spell.getCategory()) {
            case CONTROLE, FORCA, DANO, IMPERDOAVEL -> true;
            default -> false;
        };
    }

    private static void notify(Player player, Level level, Component message) {
        if (!level.isClientSide) {
            player.displayClientMessage(message.copy().withStyle(ChatFormatting.GRAY), true);
        }
    }
}
