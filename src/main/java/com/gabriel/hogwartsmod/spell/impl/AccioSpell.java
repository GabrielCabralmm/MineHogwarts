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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class AccioSpell extends Spell {

    public AccioSpell() {
        super("accio", SpellCategory.FORCA, SpellDelivery.ALVO_DIRETO, 4, 12f, 100, 2, 15, 0xC8D8FF);
    }

    public double getRange(MagicData data) {
        return (16 + 6 * data.getMastery(this)) * data.wandStats().range();
    }

    @Override
    public void cast(ServerPlayer caster, MagicData data) {
        double range = getRange(data);
        Vec3 start = caster.getEyePosition();
        Vec3 end = start.add(caster.getLookAngle().scale(range));
        HitResult blockHit = caster.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        if (blockHit.getType() != HitResult.Type.MISS) {
            end = blockHit.getLocation();
        }
        AABB box = caster.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(caster, start, end, box,
                e -> (e instanceof LivingEntity || e instanceof ItemEntity) && e != caster && !e.isSpectator(), range * range);
        if (caster.level() instanceof ServerLevel server) {
            Vec3 tip = wandTip(caster);
            Vec3 to = hit != null ? hit.getLocation() : end;
            Vec3 path = to.subtract(tip);
            int steps = Math.max(6, (int) (path.length() * 2));
            Vec3 back = path.normalize().scale(-0.18);
            SpellFx.flash(server, tip, getColor(), 0.6f);
            for (int i = 1; i <= steps; i++) {
                Vec3 p = tip.add(path.scale(i / (double) steps));
                SpellFx.particle(server, ModParticles.GLOW.get(), 0x46557A, 0.45f, 10, p, back);
                if (i % 2 == 0) {
                    SpellFx.particle(server, ModParticles.SPARK.get(), getColor(), 0.1f, 12, p, back.scale(1.5));
                }
            }
            if (hit != null) {
                SpellFx.particle(server, ModParticles.STAR.get(), getColor(), 1.0f, 8, hit.getLocation(), Vec3.ZERO);
                SpellFx.burst(server, ModParticles.SPARK.get(), getColor(), 0.12f, 14, hit.getLocation(), 16, 0.2);
            }
        }
        caster.level().playSound(null, caster.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 0.8f, 1.5f);
        if (hit == null) {
            return;
        }
        Entity target = hit.getEntity();
        Vec3 pull = caster.position().subtract(target.position());
        double distance = pull.length();
        Vec3 velocity = pull.normalize().scale(Math.min(2.5, 0.6 + distance * 0.12)).add(0, 0.35, 0);
        target.setDeltaMovement(velocity);
        target.hurtMarked = true;
        data.addMastery(this, 1, getXpPerUse());
    }
}
