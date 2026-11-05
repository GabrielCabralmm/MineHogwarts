package com.gabriel.hogwartsmod.spell.impl;

import com.gabriel.hogwartsmod.magic.MagicData;
import com.gabriel.hogwartsmod.spell.Spell;
import com.gabriel.hogwartsmod.spell.SpellCategory;
import com.gabriel.hogwartsmod.spell.SpellDelivery;
import com.gabriel.hogwartsmod.spell.Spells;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class ProtegoSpell extends Spell {
    public static final int PERFECT_BLOCK_TICKS = 5;
    private static final float DRAIN_PER_TICK = 2f / 20f;

    public enum BlockResult { NONE, BLOCKED, PERFECT, REFLECTED, BROKEN }

    public ProtegoSpell() {
        super("protego", SpellCategory.PROTECAO, SpellDelivery.PROTECAO, 2, 10f, 60, 4, 40, 0x7FB8F0);
    }

    @Override
    public int getXpPerUse() {
        return 1;
    }

    public float getShieldMaxHp(MagicData data) {
        return 40f + 10f * data.getMastery(this);
    }

    @Override
    public boolean startHold(ServerPlayer caster, MagicData data) {
        data.startShield(getShieldMaxHp(data), caster.level().getGameTime());
        caster.level().playSound(null, caster.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6f, 1.8f);
        return true;
    }

    @Override
    public boolean tickHold(ServerPlayer caster, MagicData data, int ticksHeld) {
        if (!data.isShielding() || !data.spend(DRAIN_PER_TICK)) {
            return false;
        }
        if (ticksHeld % 3 == 0 && caster.level() instanceof ServerLevel server) {
            DustParticleOptions dust = new DustParticleOptions(new Vector3f(0.55f, 0.75f, 1f), 1.0f);
            Vec3 look = caster.getLookAngle();
            Vec3 center = caster.getEyePosition().add(look.scale(1.2));
            Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
            Vec3 up = right.cross(look).normalize();
            for (int i = 0; i < 16; i++) {
                double a = i * Math.PI * 2 / 16;
                Vec3 p = center.add(right.scale(Math.cos(a) * 0.9)).add(up.scale(Math.sin(a) * 0.9));
                server.sendParticles(dust, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
        }
        return true;
    }

    @Override
    public void stopHold(ServerPlayer caster, MagicData data) {
        data.stopShield();
    }

    public static boolean isFacing(Player player, Vec3 from) {
        Vec3 look = player.getViewVector(1.0f);
        Vec3 dir = from.subtract(player.getEyePosition());
        Vec3 flatLook = new Vec3(look.x, 0, look.z);
        Vec3 flatDir = new Vec3(dir.x, 0, dir.z);
        if (flatLook.lengthSqr() < 1.0E-4 || flatDir.lengthSqr() < 1.0E-4) {
            return true;
        }
        return flatLook.normalize().dot(flatDir.normalize()) > 0.3;
    }

    public static BlockResult tryBlock(Player defender, Vec3 from, SpellCategory category, float damage) {
        MagicData data = MagicData.get(defender);
        if (data == null || !data.isShielding() || !defender.isUsingItem() || !isFacing(defender, from)) {
            return BlockResult.NONE;
        }
        ProtegoSpell protego = Spells.PROTEGO;
        long now = defender.level().getGameTime();
        boolean perfect = now - data.getShieldStart() <= PERFECT_BLOCK_TICKS;
        ServerLevel server = defender.level() instanceof ServerLevel s ? s : null;
        if (perfect) {
            data.addMastery(protego, 10, 3);
            if (server != null) {
                server.playSound(null, defender.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 1.6f);
            }
            return data.hasMaxMastery(protego) && category != SpellCategory.PROTECAO ? BlockResult.REFLECTED : BlockResult.PERFECT;
        }
        if (category == SpellCategory.FORCA) {
            return BlockResult.BLOCKED;
        }
        if (category == SpellCategory.DANO && damage > 0) {
            data.addMastery(protego, Math.max(1, Math.round(damage)), Math.max(1, Math.round(damage / 2)));
            data.setShieldHp(data.getShieldHp() - damage);
            if (data.getShieldHp() <= 0) {
                data.stopShield();
                defender.stopUsingItem();
                if (server != null) {
                    server.playSound(null, defender.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);
                }
                return BlockResult.BROKEN;
            }
        }
        if (server != null) {
            server.playSound(null, defender.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.8f, 1.3f);
        }
        return BlockResult.BLOCKED;
    }
}
