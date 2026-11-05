package com.gabriel.hogwartsmod.spell.impl;

import com.gabriel.hogwartsmod.fx.SpellFx;
import com.gabriel.hogwartsmod.magic.MagicData;
import com.gabriel.hogwartsmod.network.PlayerFxPacket;
import com.gabriel.hogwartsmod.particle.ModParticles;
import com.gabriel.hogwartsmod.spell.Spell;
import com.gabriel.hogwartsmod.spell.SpellCategory;
import com.gabriel.hogwartsmod.spell.SpellDelivery;
import com.gabriel.hogwartsmod.spell.Spells;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class ProtegoSpell extends Spell {
    public static final int PERFECT_BLOCK_TICKS = 5;
    private static final float DRAIN_PER_TICK = 2f / 20f;

    public enum BlockResult { NONE, BLOCKED, PERFECT, REFLECTED, BROKEN }

    public ProtegoSpell() {
        super("protego", SpellCategory.PROTECAO, SpellDelivery.PROTECAO, 2, 10f, 60, 4, 40, 0x9CC8FF);
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
        SpellFx.sendState(caster, PlayerFxPacket.Kind.SHIELD, true);
        if (caster.level() instanceof ServerLevel server) {
            Vec3 front = caster.getEyePosition().add(caster.getLookAngle().scale(1.1));
            SpellFx.flash(server, front, getColor(), 1.8f);
            SpellFx.burst(server, ModParticles.SPARK.get(), getColor(), 0.12f, 14, front, 16, 0.25);
        }
        return true;
    }

    @Override
    public boolean tickHold(ServerPlayer caster, MagicData data, int ticksHeld) {
        if (!data.isShielding() || !data.spend(DRAIN_PER_TICK)) {
            return false;
        }
        return true;
    }

    @Override
    public void stopHold(ServerPlayer caster, MagicData data) {
        data.stopShield();
        SpellFx.sendState(caster, PlayerFxPacket.Kind.SHIELD, false);
    }

    private static void hitFx(Player defender, Vec3 from, boolean perfect) {
        if (defender instanceof ServerPlayer sp && defender.level() instanceof ServerLevel server) {
            SpellFx.sendState(sp, PlayerFxPacket.Kind.SHIELD_HIT, perfect);
            Vec3 point = defender.getEyePosition().add(from.subtract(defender.getEyePosition()).normalize().scale(1.1));
            int color = Spells.PROTEGO.getColor();
            SpellFx.flash(server, point, perfect ? SpellFx.WHITE : color, perfect ? 2.0f : 1.2f);
            SpellFx.particle(server, ModParticles.STAR.get(), color, perfect ? 1.6f : 0.9f, 8, point, Vec3.ZERO);
            SpellFx.burst(server, ModParticles.SPARK.get(), color, 0.16f, 14, point, perfect ? 30 : 14, 0.35);
        }
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
            hitFx(defender, from, true);
            data.addMastery(protego, 10, 3);
            if (server != null) {
                server.playSound(null, defender.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 1.6f);
            }
            return data.hasMaxMastery(protego) && category != SpellCategory.PROTECAO ? BlockResult.REFLECTED : BlockResult.PERFECT;
        }
        hitFx(defender, from, false);
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
