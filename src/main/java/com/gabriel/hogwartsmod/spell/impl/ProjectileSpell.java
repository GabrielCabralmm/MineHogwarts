package com.gabriel.hogwartsmod.spell.impl;

import com.gabriel.hogwartsmod.entity.SpellProjectileEntity;
import com.gabriel.hogwartsmod.magic.MagicData;
import com.gabriel.hogwartsmod.spell.Spell;
import com.gabriel.hogwartsmod.spell.SpellCategory;
import com.gabriel.hogwartsmod.spell.SpellDelivery;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public abstract class ProjectileSpell extends Spell {

    protected ProjectileSpell(String id, SpellCategory category, int requiredLevel, float cost, int cooldownTicks,
                              int maxMastery, int masteryBase, int color) {
        super(id, category, SpellDelivery.PROJETIL, requiredLevel, cost, cooldownTicks, maxMastery, masteryBase, color);
    }

    @Override
    public void cast(ServerPlayer caster, MagicData data) {
        SpellProjectileEntity projectile = new SpellProjectileEntity(caster.level(), caster, this);
        Vec3 tip = wandTip(caster);
        projectile.setPos(tip.x, tip.y, tip.z);
        projectile.shootFromRotation(caster, caster.getXRot(), caster.getYRot(), 0.0f, 2.2f, 0.0f);
        caster.level().addFreshEntity(projectile);
        caster.level().playSound(null, caster.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.8f, 1.4f);
    }
}
