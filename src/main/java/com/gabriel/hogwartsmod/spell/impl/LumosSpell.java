package com.gabriel.hogwartsmod.spell.impl;

import com.gabriel.hogwartsmod.fx.SpellFx;
import com.gabriel.hogwartsmod.magic.MagicData;
import com.gabriel.hogwartsmod.network.PlayerFxPacket;
import com.gabriel.hogwartsmod.particle.ModParticles;
import com.gabriel.hogwartsmod.spell.Spell;
import com.gabriel.hogwartsmod.spell.SpellCategory;
import com.gabriel.hogwartsmod.spell.SpellDelivery;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.phys.Vec3;

public class LumosSpell extends Spell {

    public LumosSpell() {
        super("lumos", SpellCategory.ESSENCIAL, SpellDelivery.EM_SI_MESMO, 1, 5f, 40, 2, 10, 0xDDEBFF);
    }

    public int getDurationTicks(MagicData data) {
        return 20 * (60 + 30 * data.getMastery(this));
    }

    public int getLightLevel(MagicData data) {
        return 12 + data.getMastery(this) + (data.hasMaxMastery(this) ? 1 : 0);
    }

    @Override
    public boolean isFreeCast(MagicData data, long gameTime) {
        return data.getLumosUntil() > gameTime;
    }

    @Override
    public void cast(ServerPlayer caster, MagicData data) {
        long now = caster.level().getGameTime();
        if (data.getLumosUntil() > now) {
            data.setLumosUntil(0);
            SpellFx.sendState(caster, PlayerFxPacket.Kind.LUMOS, false);
            caster.level().playSound(null, caster.blockPosition(), SoundEvents.CANDLE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 1.2f);
            return;
        }
        data.setLumosUntil(now + getDurationTicks(data));
        data.addMastery(this, 1, getXpPerUse());
        Vec3 tip = wandTip(caster);
        if (caster.level() instanceof ServerLevel server) {
            SpellFx.flash(server, tip, getColor(), 1.6f);
            SpellFx.burst(server, ModParticles.SPARK.get(), getColor(), 0.12f, 16, tip, 18, 0.15);
            SpellFx.burst(server, ModParticles.STAR.get(), 0xFFF6C8, 0.4f, 10, tip, 4, 0.1);
        }
        SpellFx.sendState(caster, PlayerFxPacket.Kind.LUMOS, true);
        caster.level().playSound(null, caster.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.6f);
    }

    public void tickLight(ServerPlayer player, MagicData data) {
        Level level = player.level();
        boolean active = data.getLumosUntil() > level.getGameTime();
        BlockPos old = data.getLumosLight();
        if (!active) {
            if (data.isLumosAnnounced()) {
                data.setLumosAnnounced(false);
                SpellFx.sendState(player, PlayerFxPacket.Kind.LUMOS, false);
            }
            if (old != null) {
                removeLight(level, old);
                data.setLumosLight(null);
            }
            return;
        }
        BlockPos target = BlockPos.containing(player.getEyePosition());
        if (!level.getBlockState(target).isAir() && !level.getBlockState(target).is(Blocks.LIGHT)) {
            target = player.blockPosition();
        }
        if (target.equals(old)) {
            return;
        }
        if (old != null) {
            removeLight(level, old);
        }
        if (level.getBlockState(target).isAir()) {
            level.setBlock(target, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, getLightLevel(data)), 3);
            data.setLumosLight(target);
        } else {
            data.setLumosLight(null);
        }
    }

    public static void removeLight(Level level, BlockPos pos) {
        if (level.getBlockState(pos).is(Blocks.LIGHT)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }
}
