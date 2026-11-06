package com.gabriel.hogwartsmod.spell.impl;

import com.gabriel.hogwartsmod.apparition.ApparitionPoint;
import com.gabriel.hogwartsmod.apparition.PublicLocations;
import com.gabriel.hogwartsmod.fx.SpellFx;
import com.gabriel.hogwartsmod.magic.MagicData;
import com.gabriel.hogwartsmod.network.PlayerFxPacket;
import com.gabriel.hogwartsmod.particle.ModParticles;
import com.gabriel.hogwartsmod.spell.Spell;
import com.gabriel.hogwartsmod.spell.SpellCategory;
import com.gabriel.hogwartsmod.spell.SpellDelivery;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AparatacaoSpell extends Spell {
    public static final int CAST_TICKS = 40;
    public static final int MAX_PLACES = 5;

    public enum Mode { VISUAL, PERSONAL, PUBLIC }

    public AparatacaoSpell() {
        super("aparatacao", SpellCategory.ESSENCIAL, SpellDelivery.EM_SI_MESMO, 25, 30f, 200, 5, 15, 0x5AA0FF);
    }

    @Override
    public boolean isAbility() {
        return true;
    }

    public int getVisualRange(MagicData data) {
        return 16 + 8 * data.getMastery(this);
    }

    public int getPlaceLimit(MagicData data) {
        return Math.min(MAX_PLACES, 1 + data.getMastery(this));
    }

    public static float costFor(double distance) {
        return 30f + (float) Math.min(70.0, distance * 0.05);
    }

    // -- Início da conjuração

    public void request(ServerPlayer player, Mode mode, int index, String name) {
        MagicData data = MagicData.get(player);
        if (data == null || !player.isAlive() || player.isSpectator() || player.isPassenger() || player.isSleeping()) {
            return;
        }
        long now = player.level().getGameTime();
        if (!data.knows(this) || data.isCasting() || data.isDodging(now)) {
            return;
        }
        if (data.getLevel() < getRequiredLevel()) {
            player.displayClientMessage(Component.translatable("message.hogwartsmod.level_required", getRequiredLevel()), true);
            return;
        }
        if (data.getCooldownRemaining(this, now) > 0) {
            return;
        }
        if (com.gabriel.hogwartsmod.zone.Zones.blocks(player, com.gabriel.hogwartsmod.zone.ZoneRule.APARATACAO)) {
            com.gabriel.hogwartsmod.zone.Zones.warn(player, com.gabriel.hogwartsmod.zone.ZoneRule.APARATACAO);
            return;
        }
        Vec3 target = switch (mode) {
            case VISUAL -> visualTarget(player, data);
            case PERSONAL -> pointTarget(player, index >= 0 && index < data.getPlaces().size() ? data.getPlaces().get(index) : null);
            case PUBLIC -> pointTarget(player, PublicLocations.get(player.server).find(name));
        };
        if (target == null) {
            return;
        }
        if (com.gabriel.hogwartsmod.zone.Zones.blocks(player.level(), target, com.gabriel.hogwartsmod.zone.ZoneRule.APARATACAO)) {
            player.displayClientMessage(Component.translatable("message.hogwartsmod.zone_blocked.aparatacao_destino").withStyle(net.minecraft.ChatFormatting.GOLD), true);
            return;
        }
        double distance = target.distanceTo(player.position());
        if (distance < 1.5) {
            return;
        }
        if (data.getReserve() < costFor(distance)) {
            player.displayClientMessage(Component.translatable("message.hogwartsmod.no_reserve"), true);
            return;
        }
        data.startCast(player.position(), target, player.getYRot(), now + CAST_TICKS);
        SpellFx.sendState(player, PlayerFxPacket.Kind.APPARATE, true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 0.8f, 1.4f);
    }

    @Nullable
    private Vec3 visualTarget(ServerPlayer player, MagicData data) {
        HitResult hit = player.pick(getVisualRange(data), 1f, false);
        if (!(hit instanceof BlockHitResult block) || hit.getType() != HitResult.Type.BLOCK) {
            player.displayClientMessage(Component.translatable("message.hogwartsmod.apparate_aim"), true);
            return null;
        }
        Vec3 spot = safeSpot(player, block.getBlockPos().relative(block.getDirection()));
        if (spot == null) {
            player.displayClientMessage(Component.translatable("message.hogwartsmod.apparate_blocked"), true);
        }
        return spot;
    }

    @Nullable
    private Vec3 pointTarget(ServerPlayer player, @Nullable ApparitionPoint point) {
        if (point == null) {
            return null;
        }
        if (!point.dimension().equals(player.level().dimension().location().toString())) {
            player.displayClientMessage(Component.translatable("message.hogwartsmod.apparate_dimension"), true);
            return null;
        }
        Vec3 spot = safeSpot(player, BlockPos.containing(point.position()));
        if (spot == null) {
            player.displayClientMessage(Component.translatable("message.hogwartsmod.apparate_blocked"), true);
        }
        return spot;
    }

    @Nullable
    private static Vec3 safeSpot(ServerPlayer player, BlockPos start) {
        ServerLevel level = player.serverLevel();
        for (int dy = 0; dy <= 3; dy++) {
            BlockPos pos = start.above(dy);
            Vec3 feet = new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            if (level.noCollision(player, player.getBoundingBox().move(feet.subtract(player.position())))) {
                for (int down = 0; down < 4; down++) {
                    BlockPos below = pos.below(down + 1);
                    if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()) {
                        return new Vec3(feet.x, pos.getY() - down, feet.z);
                    }
                }
                return feet;
            }
        }
        return null;
    }

    // -- Durante a conjuração

    public void tick(ServerPlayer player, MagicData data) {
        Vec3 from = data.getCastFrom();
        if (from != null && player.position().distanceToSqr(from) > 0.6 * 0.6) {
            interrupt(player, data, "message.hogwartsmod.apparate_moved");
            return;
        }
        if (player.level().getGameTime() >= data.getCastEnd()) {
            complete(player, data);
        }
    }

    public void interrupt(ServerPlayer player, MagicData data, String reason) {
        data.clearCast();
        SpellFx.sendState(player, PlayerFxPacket.Kind.APPARATE, false);
        data.setCooldown(this, player.level().getGameTime(), 40);
        player.displayClientMessage(Component.translatable(reason), true);
        if (player.level() instanceof ServerLevel server) {
            SpellFx.burst(server, ModParticles.SMOKE.get(), 0x2E2A36, 0.5f, 18, player.position().add(0, 1, 0), 10, 0.06);
            server.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.7f, 1.2f);
        }
    }

    private void complete(ServerPlayer player, MagicData data) {
        Vec3 target = data.getCastTarget();
        data.clearCast();
        if (target == null) {
            return;
        }
        float cost = costFor(target.distanceTo(player.position()));
        if (!data.spend(cost)) {
            SpellFx.sendState(player, PlayerFxPacket.Kind.APPARATE, false);
            player.displayClientMessage(Component.translatable("message.hogwartsmod.no_reserve"), true);
            return;
        }
        ServerLevel level = player.serverLevel();
        crack(level, player.position());
        SpellFx.sendState(player, PlayerFxPacket.Kind.APPARATE, false);
        player.teleportTo(level, target.x, target.y, target.z, player.getYRot(), player.getXRot());
        player.resetFallDistance();
        SpellFx.sendState(player, PlayerFxPacket.Kind.ARRIVE, true);
        crack(level, target);
        long now = level.getGameTime();
        data.setCooldown(this, now, Math.max(60, getCooldownTicks() - 20 * data.getMastery(this)));
        float nausea = Math.max(0f, 0.5f - 0.1f * data.getMastery(this));
        if (player.getRandom().nextFloat() < nausea) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0, false, false, true));
        }
        data.addMastery(this, 1, getXpPerUse());
    }

    private void crack(ServerLevel level, Vec3 feet) {
        Vec3 center = feet.add(0, 1.0, 0);
        SpellFx.particle(level, ModParticles.RING.get(), 0x9CCBFF, 2.2f, 6, center, Vec3.ZERO);
        SpellFx.particle(level, ModParticles.RING.get(), 0x5AA0FF, 1.6f, 8, feet.add(0, 0.05, 0), Vec3.ZERO);
        SpellFx.particle(level, ModParticles.STAR.get(), 0xE6F2FF, 0.7f, 4, center, Vec3.ZERO);
        SpellFx.particle(level, ModParticles.GLOW.get(), 0x3C7BD8, 1.2f, 4, center, Vec3.ZERO);
        SpellFx.burst(level, ModParticles.SMOKE.get(), 0x241F2C, 0.3f, 12, center, 10, 0.2);
        SpellFx.burst(level, ModParticles.BOLT.get(), 0x3C9BFF, 0.1f, 2, center, 8, 0.12);
        SpellFx.burst(level, ModParticles.BOLT.get(), 0xA8DCFF, 0.08f, 2, center, 5, 0.1);
        level.playSound(null, BlockPos.containing(feet), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1.2f, 0.55f);
        level.playSound(null, BlockPos.containing(feet), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.8f, 0.6f);
    }

    public static String dimensionOf(ServerPlayer player) {
        return player.level().dimension().location().toString();
    }

    public static List<ApparitionPoint> publicPoints(ServerPlayer player) {
        return PublicLocations.get(player.server).all();
    }
}
