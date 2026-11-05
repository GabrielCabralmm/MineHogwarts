package com.gabriel.hogwartsmod.spell.impl;

import com.gabriel.hogwartsmod.entity.SpellProjectileEntity;
import com.gabriel.hogwartsmod.magic.MagicData;
import com.gabriel.hogwartsmod.spell.SpellCategory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ExpelliarmusSpell extends ProjectileSpell {

    public ExpelliarmusSpell() {
        super("expelliarmus", SpellCategory.CONTROLE, 2, 15f, 80, 3, 20, 0xE0102E);
    }

    @Override
    public void onProjectileHit(SpellProjectileEntity projectile, LivingEntity target, @Nullable ServerPlayer caster) {
        ItemStack held = target.getItemInHand(InteractionHand.MAIN_HAND);
        if (!held.isEmpty()) {
            ItemStack copy = held.copy();
            target.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            MagicData data = caster == null ? null : MagicData.get(caster);
            if (caster != null && data != null && data.hasMaxMastery(this) && caster.getInventory().add(copy)) {
                copy = ItemStack.EMPTY;
            }
            if (!copy.isEmpty()) {
                ItemEntity drop = new ItemEntity(target.level(), target.getX(), target.getEyeY(), target.getZ(), copy);
                drop.setDeltaMovement(projectile.getDeltaMovement().normalize().scale(0.4).add(0, 0.3, 0));
                drop.setPickUpDelay(40);
                target.level().addFreshEntity(drop);
            }
        }
        target.knockback(0.8, -projectile.getDeltaMovement().x, -projectile.getDeltaMovement().z);
        if (caster != null) {
            MagicData data = MagicData.get(caster);
            if (data != null) {
                data.addMastery(this, 1, getXpPerUse());
            }
        }
    }
}
