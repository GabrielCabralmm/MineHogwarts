package com.gabriel.hogwartsmod.spell.impl;

import com.gabriel.hogwartsmod.fx.SpellFx;
import com.gabriel.hogwartsmod.magic.MagicData;
import com.gabriel.hogwartsmod.particle.ModParticles;
import com.gabriel.hogwartsmod.spell.Spell;
import com.gabriel.hogwartsmod.spell.SpellCategory;
import com.gabriel.hogwartsmod.spell.SpellDelivery;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;

public class ArrestoMomentumSpell extends Spell {

    public ArrestoMomentumSpell() {
        super("arresto_momentum", SpellCategory.CONTROLE, SpellDelivery.AREA, 8, 30f, 300, 3, 25, 0xFFD27A);
    }

    public double getRadius(MagicData data) {
        return 5 + data.getMastery(this);
    }

    public int getDuration(MagicData data) {
        return 80 + 20 * data.getMastery(this);
    }

    @Override
    public void cast(ServerPlayer caster, MagicData data) {
        if (!(caster.level() instanceof ServerLevel server)) {
            return;
        }
        double radius = getRadius(data);
        Vec3 feet = caster.position().add(0, 0.08, 0);
        SpellFx.particle(server, ModParticles.RING.get(), getColor(), (float) radius, 18, feet, Vec3.ZERO);
        SpellFx.particle(server, ModParticles.RING.get(), 0xFFF2CC, (float) radius * 0.7f, 14, feet.add(0, 0.6, 0), Vec3.ZERO);
        SpellFx.flash(server, wandTip(caster), getColor(), 1.2f);
        var random = server.getRandom();
        for (int i = 0; i < 40; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double d = random.nextDouble() * radius;
            Vec3 p = feet.add(Math.cos(a) * d, 0.1, Math.sin(a) * d);
            SpellFx.particle(server, ModParticles.EMBER.get(), getColor(), 0.08f, 30 + random.nextInt(20), p, new Vec3(0, 0.02 + random.nextDouble() * 0.03, 0));
        }
        server.playSound(null, caster.blockPosition(), SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.PLAYERS, 1.0f, 0.8f);
        int affected = 0;
        for (Entity entity : server.getEntities(caster, caster.getBoundingBox().inflate(radius))) {
            if (entity.distanceTo(caster) > radius) {
                continue;
            }
            if (entity instanceof Projectile projectile) {
                projectile.setDeltaMovement(projectile.getDeltaMovement().scale(0.1));
                projectile.hurtMarked = true;
                affected++;
            } else if (entity instanceof LivingEntity living) {
                if (living instanceof Player defender
                        && ProtegoSpell.tryBlock(defender, caster.position(), getCategory(), 0) != ProtegoSpell.BlockResult.NONE) {
                    continue;
                }
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, getDuration(data), 4, false, true));
                Vec3 center = living.position().add(0, living.getBbHeight() / 2, 0);
                SpellFx.particle(server, ModParticles.GLOW.get(), getColor(), living.getBbHeight() * 1.2f, 16, center, Vec3.ZERO);
                SpellFx.burst(server, ModParticles.EMBER.get(), getColor(), 0.08f, 30, center, 10, 0.05);
                affected++;
            }
        }
        if (affected > 0) {
            data.addMastery(this, 1, getXpPerUse());
        }
    }
}
