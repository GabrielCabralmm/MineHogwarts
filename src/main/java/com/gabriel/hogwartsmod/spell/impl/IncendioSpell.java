package com.gabriel.hogwartsmod.spell.impl;

import com.gabriel.hogwartsmod.fx.SpellFx;
import com.gabriel.hogwartsmod.magic.MagicData;
import com.gabriel.hogwartsmod.particle.ModParticles;
import com.gabriel.hogwartsmod.spell.Spell;
import com.gabriel.hogwartsmod.spell.SpellCategory;
import com.gabriel.hogwartsmod.spell.SpellDelivery;
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
        super("incendio", SpellCategory.DANO, SpellDelivery.FEIXE, 5, 3f, 120, 4, 25, 0xFF6A1A);
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
        if (!(caster.level() instanceof ServerLevel server)) {
            return true;
        }
        Vec3 start = wandTip(caster);
        jet(server, start, caster.getLookAngle(), ticksHeld);
        if (ticksHeld % 2 != 0) {
            return true;
        }
        Vec3 end = start.add(caster.getLookAngle().scale(RANGE));
        HitResult blockHit = server.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        if (blockHit.getType() != HitResult.Type.MISS) {
            end = blockHit.getLocation();
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

    private void jet(ServerLevel server, Vec3 tip, Vec3 look, int ticksHeld) {
        var random = server.getRandom();
        for (int i = 0; i < 7; i++) {
            Vec3 spread = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).scale(0.06);
            Vec3 velocity = look.scale(0.55 + random.nextDouble() * 0.2).add(spread);
            int color = random.nextInt(3) == 0 ? 0xFFC03A : getColor();
            SpellFx.particle(server, ModParticles.FIRE.get(), color, 0.4f + random.nextFloat() * 0.2f, 12 + random.nextInt(6), tip, velocity);
        }
        for (int i = 0; i < 2; i++) {
            Vec3 emberVel = look.scale(0.5).add(new Vec3(random.nextGaussian(), random.nextGaussian() + 1, random.nextGaussian()).scale(0.08));
            SpellFx.particle(server, ModParticles.EMBER.get(), 0xFFA040, 0.07f, 22 + random.nextInt(10), tip, emberVel);
        }
        if (ticksHeld % 2 == 0) {
            Vec3 end = tip.add(look.scale(4 + random.nextDouble() * 3));
            SpellFx.particle(server, ModParticles.SMOKE.get(), 0x3A3634, 0.7f, 30, end, look.scale(0.08).add(0, 0.05, 0));
            SpellFx.particle(server, ModParticles.SPARK.get(), 0xFFD060, 0.12f, 12, tip,
                    look.scale(0.4).add(new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).scale(0.1)));
        }
        SpellFx.particle(server, ModParticles.GLOW.get(), 0xFF8A30, 0.6f, 2, tip, Vec3.ZERO);
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
