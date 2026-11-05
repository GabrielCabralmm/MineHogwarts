package com.gabriel.hogwartsmod.spell.impl;

import com.gabriel.hogwartsmod.magic.MagicData;
import com.gabriel.hogwartsmod.spell.Spell;
import com.gabriel.hogwartsmod.spell.SpellCategory;
import com.gabriel.hogwartsmod.spell.SpellDelivery;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class IncendioSpell extends Spell {
    private static final double RANGE = 10.0;
    private static final float DRAIN_PER_TICK = 6f / 20f;

    public IncendioSpell() {
        super("incendio", SpellCategory.DANO, SpellDelivery.FEIXE, 5, 3f, 120, 4, 25, 0xF07020);
    }

    public float getDamage(MagicData data) {
        return 1.5f + 0.5f * data.getMastery(this);
    }

    @Override
    public float getShieldDamage() {
        return 1.5f;
    }

    @Override
    public boolean tickHold(ServerPlayer caster, MagicData data, int ticksHeld) {
        if (!data.spend(DRAIN_PER_TICK)) {
            return false;
        }
        if (ticksHeld % 2 != 0 || !(caster.level() instanceof ServerLevel server)) {
            return true;
        }
        Vec3 start = wandTip(caster);
        Vec3 end = start.add(caster.getLookAngle().scale(RANGE));
        HitResult blockHit = server.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        if (blockHit.getType() != HitResult.Type.MISS) {
            end = blockHit.getLocation();
        }
        Vec3 step = end.subtract(start).scale(1.0 / 12);
        for (int i = 1; i <= 12; i++) {
            Vec3 p = start.add(step.scale(i));
            server.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.01);
        }
        if (ticksHeld % 10 == 0) {
            server.playSound(null, caster.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.4f, 1.2f);
        }
        AABB box = caster.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(caster, start, end, box,
                e -> e instanceof LivingEntity && e != caster && !e.isSpectator(), RANGE * RANGE);
        if (hit == null || !(hit.getEntity() instanceof LivingEntity target)) {
            return true;
        }
        burn(caster, data, target, start, server);
        if (data.hasMaxMastery(this)) {
            for (LivingEntity near : server.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(2.0),
                    e -> e != target && e != caster)) {
                near.setSecondsOnFire(3);
            }
        }
        long now = server.getGameTime();
        if (now - data.getLastIncendioMastery() >= 20) {
            data.setLastIncendioMastery(now);
            data.addMastery(this, 1, getXpPerUse());
        }
        return true;
    }

    private void burn(ServerPlayer caster, MagicData data, LivingEntity target, Vec3 from, ServerLevel server) {
        if (target instanceof Player defender) {
            ProtegoSpell.BlockResult block = ProtegoSpell.tryBlock(defender, from, getCategory(), getShieldDamage());
            if (block != ProtegoSpell.BlockResult.NONE) {
                return;
            }
        }
        target.hurt(server.damageSources().indirectMagic(caster, caster), getDamage(data));
        target.setSecondsOnFire(3);
    }
}
