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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;

public class ArrestoMomentumSpell extends Spell {

    public ArrestoMomentumSpell() {
        super("arresto_momentum", SpellCategory.CONTROLE, SpellDelivery.AREA, 8, 30f, 300, 3, 25, 0xE8D070);
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
        DustParticleOptions dust = new DustParticleOptions(getColorVector(), 1.4f);
        for (int i = 0; i < 48; i++) {
            double a = i * Math.PI * 2 / 48;
            server.sendParticles(dust, caster.getX() + Math.cos(a) * radius, caster.getY() + 0.3, caster.getZ() + Math.sin(a) * radius, 1, 0, 0.2, 0, 0);
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
                affected++;
            }
        }
        if (affected > 0) {
            data.addMastery(this, 1, getXpPerUse());
        }
    }
}
