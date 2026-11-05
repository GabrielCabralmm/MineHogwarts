package com.gabriel.hogwartsmod.spell.impl;

import com.gabriel.hogwartsmod.entity.SpellProjectileEntity;
import com.gabriel.hogwartsmod.magic.MagicData;
import com.gabriel.hogwartsmod.spell.SpellCategory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.Nullable;

public class StupefySpell extends ProjectileSpell {

    public StupefySpell() {
        super("stupefy", SpellCategory.CONTROLE, 3, 15f, 80, 3, 20, 0xFF3322);
    }

    @Override
    public void onProjectileHit(SpellProjectileEntity projectile, LivingEntity target, @Nullable ServerPlayer caster) {
        MagicData data = caster == null ? null : MagicData.get(caster);
        int mastery = data == null ? 0 : data.getMastery(this);
        int duration = 60 + 20 * mastery;
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 9, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 4, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration, 4, false, true));
        if (target instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
        if (data != null) {
            if (data.hasMaxMastery(this)) {
                target.knockback(1.2, -projectile.getDeltaMovement().x, -projectile.getDeltaMovement().z);
            }
            data.addMastery(this, 1, getXpPerUse());
        }
    }
}
