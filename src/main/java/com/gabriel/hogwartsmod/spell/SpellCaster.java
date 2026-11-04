package com.gabriel.hogwartsmod.spell;

import com.gabriel.hogwartsmod.entity.SpellProjectileEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class SpellCaster {

    private SpellCaster() {
    }

    public static void cast(ServerLevel level, Player player, Spell spell) {
        switch (spell) {
            case LUMOS -> castLumos(level, player);
            case EXPELLIARMUS, STUPEFY -> castProjectile(level, player, spell);
        }
    }

    private static void castLumos(ServerLevel level, Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 60, 0, false, false, true));
        Vec3 tip = wandTip(player);
        level.sendParticles(ParticleTypes.END_ROD, tip.x, tip.y, tip.z, 20, 0.15, 0.15, 0.15, 0.02);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.6f);
    }

    private static void castProjectile(ServerLevel level, Player player, Spell spell) {
        SpellProjectileEntity projectile = new SpellProjectileEntity(level, player, spell);
        Vec3 tip = wandTip(player);
        projectile.setPos(tip.x, tip.y, tip.z);
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 2.2f, 0.0f);
        level.addFreshEntity(projectile);
        level.playSound(null, player.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.8f, 1.4f);
    }

    public static Vec3 wandTip(Player player) {
        return player.getEyePosition().add(player.getLookAngle().scale(0.8)).add(0, -0.2, 0);
    }
}
