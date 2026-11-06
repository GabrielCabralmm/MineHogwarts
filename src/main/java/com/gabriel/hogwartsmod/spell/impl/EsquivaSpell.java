package com.gabriel.hogwartsmod.spell.impl;

import com.gabriel.hogwartsmod.fx.SpellFx;
import com.gabriel.hogwartsmod.magic.MagicData;
import com.gabriel.hogwartsmod.network.DodgeMovePacket;
import com.gabriel.hogwartsmod.network.ModNetwork;
import com.gabriel.hogwartsmod.network.PlayerFxPacket;
import com.gabriel.hogwartsmod.particle.ModParticles;
import com.gabriel.hogwartsmod.spell.Spell;
import com.gabriel.hogwartsmod.spell.SpellCategory;
import com.gabriel.hogwartsmod.spell.SpellDelivery;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

public class EsquivaSpell extends Spell {
    public static final int DURATION = 6;
    public static final int INVULNERABLE = 4;
    public static final int SMOKE = 0x2E2A36;

    public EsquivaSpell() {
        super("esquiva", SpellCategory.ESSENCIAL, SpellDelivery.EM_SI_MESMO, 10, 8f, 70, 3, 20, 0x8A7FA8);
    }

    @Override
    public boolean isAbility() {
        return true;
    }

    public float getDistance(MagicData data) {
        return 4f + data.getMastery(this) * (2f / 3f);
    }

    public int getCooldown(MagicData data) {
        return getCooldownTicks() - 10 * data.getMastery(this);
    }

    public void tryDodge(ServerPlayer player, float dx, float dz) {
        MagicData data = MagicData.get(player);
        if (data == null || !player.isAlive() || player.isSpectator() || player.isPassenger() || player.isSleeping()) {
            return;
        }
        long now = player.level().getGameTime();
        if (!data.knows(this) || data.isDodging(now)) {
            return;
        }
        if (data.getLevel() < getRequiredLevel()) {
            player.displayClientMessage(Component.translatable("message.hogwartsmod.level_required", getRequiredLevel()), true);
            return;
        }
        if (data.getCooldownRemaining(this, now) > 0) {
            return;
        }
        if (!data.spend(getCost())) {
            player.displayClientMessage(Component.translatable("message.hogwartsmod.no_reserve"), true);
            return;
        }
        Vec3 dir = new Vec3(dx, 0, dz);
        dir = dir.lengthSqr() < 1.0e-4 ? Vec3.directionFromRotation(0, player.getYRot()).scale(-1) : dir.normalize();
        float distance = getDistance(data);
        data.setCooldown(this, now, getCooldown(data));
        data.startDodge(now, DURATION, INVULNERABLE);
        data.addMastery(this, 1, getXpPerUse());
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new DodgeMovePacket((float) dir.x, (float) dir.z, distance, DURATION));
        SpellFx.sendState(player, PlayerFxPacket.Kind.DODGE, true);
        if (player.level() instanceof ServerLevel server) {
            puff(server, player.position());
            server.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.0f, 0.8f);
        }
    }

    public void finish(ServerPlayer player) {
        SpellFx.sendState(player, PlayerFxPacket.Kind.DODGE, false);
        if (player.level() instanceof ServerLevel server) {
            puff(server, player.position());
            server.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.7f, 1.3f);
        }
    }

    private void puff(ServerLevel server, Vec3 feet) {
        Vec3 center = feet.add(0, 0.9, 0);
        SpellFx.burst(server, ModParticles.SMOKE.get(), SMOKE, 0.65f, 22, center, 12, 0.1);
        SpellFx.burst(server, ModParticles.SMOKE.get(), 0x4A4458, 0.5f, 18, center.add(0, 0.5, 0), 8, 0.07);
        SpellFx.burst(server, ModParticles.SPARK.get(), getColor(), 0.07f, 12, center, 8, 0.18);
    }
}
