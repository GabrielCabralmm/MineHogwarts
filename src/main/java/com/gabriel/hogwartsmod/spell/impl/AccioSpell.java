package com.gabriel.hogwartsmod.spell.impl;

import com.gabriel.hogwartsmod.magic.MagicData;
import com.gabriel.hogwartsmod.spell.Spell;
import com.gabriel.hogwartsmod.spell.SpellCategory;
import com.gabriel.hogwartsmod.spell.SpellDelivery;
import net.minecraft.core.particles.DustParticleOptions;
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
        super("accio", SpellCategory.FORCA, SpellDelivery.ALVO_DIRETO, 4, 12f, 100, 2, 15, 0x9060E0);
    }

    public double getRange(MagicData data) {
        return 16 + 6 * data.getMastery(this);
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
            DustParticleOptions dust = new DustParticleOptions(getColorVector(), 0.8f);
            Vec3 to = hit != null ? hit.getLocation() : end;
            Vec3 step = to.subtract(start).scale(1.0 / 16);
            for (int i = 1; i <= 16; i++) {
                Vec3 p = start.add(step.scale(i));
                server.sendParticles(dust, p.x, p.y - 0.2, p.z, 1, 0, 0, 0, 0);
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
