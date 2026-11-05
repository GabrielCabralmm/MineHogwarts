package com.gabriel.hogwartsmod.fx;

import com.gabriel.hogwartsmod.network.ModNetwork;
import com.gabriel.hogwartsmod.network.PlayerFxPacket;
import com.gabriel.hogwartsmod.particle.MagicParticleType;
import com.gabriel.hogwartsmod.particle.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

public final class SpellFx {
    public static final int WHITE = 0xFFFFFF;

    private SpellFx() {
    }

    public static void particle(ServerLevel level, MagicParticleType type, int color, float scale, int lifetime, Vec3 pos, Vec3 velocity) {
        level.sendParticles(type.with(color, scale, lifetime), pos.x, pos.y, pos.z, 0, velocity.x, velocity.y, velocity.z, 1.0);
    }

    public static void burst(ServerLevel level, MagicParticleType type, int color, float scale, int lifetime, Vec3 pos, int count, double speed) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < count; i++) {
            Vec3 dir = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize();
            particle(level, type, color, scale, lifetime, pos, dir.scale(speed * (0.4 + random.nextDouble() * 0.6)));
        }
    }

    public static void flash(ServerLevel level, Vec3 pos, int color, float scale) {
        particle(level, ModParticles.GLOW.get(), color, scale, 6, pos, Vec3.ZERO);
        particle(level, ModParticles.GLOW.get(), WHITE, scale * 0.45f, 4, pos, Vec3.ZERO);
    }

    public static void castFlash(ServerLevel level, Vec3 tip, int color) {
        flash(level, tip, color, 0.7f);
        burst(level, ModParticles.SPARK.get(), color, 0.12f, 8, tip, 6, 0.12);
    }

    public static void impact(ServerLevel level, Vec3 pos, int color) {
        flash(level, pos, color, 1.6f);
        particle(level, ModParticles.GLOW.get(), color, 2.4f, 10, pos, Vec3.ZERO);
        burst(level, ModParticles.SPARK.get(), color, 0.16f, 14, pos, 16, 0.32);
        burst(level, ModParticles.SPARK.get(), WHITE, 0.1f, 10, pos, 6, 0.25);
    }

    public static void sendState(ServerPlayer player, PlayerFxPacket.Kind kind, boolean value) {
        ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new PlayerFxPacket(player.getId(), kind, value));
    }
}
