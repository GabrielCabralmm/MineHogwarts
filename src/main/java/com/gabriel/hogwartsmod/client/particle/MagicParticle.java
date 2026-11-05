package com.gabriel.hogwartsmod.client.particle;

import com.gabriel.hogwartsmod.particle.MagicParticleOptions;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class MagicParticle extends TextureSheetParticle {

    public enum Shape { GLOW, SPARK, EMBER, SMOKE, FIRE, RING, STAR, ORB }

    private static final int FULL_BRIGHT = 0xF000F0;

    private final Shape shape;
    private final SpriteSet sprites;
    private final float baseSize;
    private final float baseR;
    private final float baseG;
    private final float baseB;
    private final float spin;

    protected MagicParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz,
                            MagicParticleOptions options, SpriteSet sprites, Shape shape) {
        super(level, x, y, z);
        this.shape = shape;
        this.sprites = sprites;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.baseSize = options.scale();
        this.quadSize = options.scale();
        this.lifetime = Math.max(1, options.lifetime());
        this.baseR = options.red();
        this.baseG = options.green();
        this.baseB = options.blue();
        this.rCol = baseR;
        this.gCol = baseG;
        this.bCol = baseB;
        this.hasPhysics = false;
        this.spin = (random.nextFloat() - 0.5f) * 0.12f;
        this.roll = random.nextFloat() * Mth.TWO_PI;
        this.oRoll = roll;
        if (shape == Shape.EMBER) {
            pickSprite(sprites);
        } else {
            setSpriteFromAge(sprites);
        }
        switch (shape) {
            case GLOW -> { friction = 0.86f; gravity = 0f; }
            case SPARK -> { friction = 0.9f; gravity = 0.5f; }
            case EMBER -> { friction = 0.94f; gravity = -0.04f; }
            case SMOKE -> { friction = 0.9f; gravity = -0.015f; alpha = 0.35f; }
            case FIRE -> { friction = 0.93f; gravity = -0.02f; }
            case RING -> { friction = 1f; gravity = 0f; }
            case STAR -> { friction = 0.8f; gravity = 0f; }
            case ORB -> { friction = 1f; gravity = 0f; }
        }
        updateLook(0f);
    }

    @Override
    public void tick() {
        super.tick();
        if (shape != Shape.EMBER && isAlive()) {
            setSpriteFromAge(sprites);
        }
        oRoll = shape == Shape.FIRE || shape == Shape.SMOKE ? roll : 0f;
        roll = shape == Shape.FIRE || shape == Shape.SMOKE ? roll + spin : 0f;
        updateLook(age / (float) lifetime);
    }

    private void updateLook(float t) {
        switch (shape) {
            case GLOW, ORB -> {
                alpha = 1f - t * t;
                quadSize = baseSize;
            }
            case STAR -> {
                alpha = 1f;
                quadSize = baseSize * (1f + 0.3f * t);
            }
            case SPARK -> {
                alpha = 1f;
                quadSize = baseSize;
            }
            case EMBER -> {
                alpha = (0.65f + 0.35f * Mth.sin(age * 1.7f)) * (1f - t);
                quadSize = baseSize * (1f - 0.5f * t);
            }
            case SMOKE -> {
                alpha = 0.6f * (1f - t * 0.5f);
                quadSize = baseSize * (1f + 0.8f * t);
            }
            case FIRE -> {
                float hot = Math.max(0f, 1f - t * 1.8f);
                rCol = Mth.lerp(hot, baseR, 1f);
                gCol = Mth.lerp(hot, baseG, 0.95f);
                bCol = Mth.lerp(hot, baseB, 0.7f);
                alpha = 1f;
                quadSize = baseSize * (0.7f + 0.9f * t);
            }
            case RING -> {
                float ease = 1f - (1f - t) * (1f - t) * (1f - t);
                alpha = 1f;
                quadSize = baseSize * (0.1f + 0.9f * ease);
            }
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return shape == Shape.SMOKE ? ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT : MagicRenderTypes.ADDITIVE;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return shape == Shape.SMOKE ? super.getLightColor(partialTick) : FULL_BRIGHT;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        if (shape != Shape.RING) {
            super.render(buffer, camera, partialTick);
            return;
        }
        Vec3 cam = camera.getPosition();
        float x = (float) (Mth.lerp(partialTick, xo, this.x) - cam.x());
        float y = (float) (Mth.lerp(partialTick, yo, this.y) - cam.y());
        float z = (float) (Mth.lerp(partialTick, zo, this.z) - cam.z());
        float s = quadSize;
        float u0 = getU0();
        float u1 = getU1();
        float v0 = getV0();
        float v1 = getV1();
        int light = FULL_BRIGHT;
        vertex(buffer, x - s, y, z - s, u0, v0, light);
        vertex(buffer, x - s, y, z + s, u0, v1, light);
        vertex(buffer, x + s, y, z + s, u1, v1, light);
        vertex(buffer, x + s, y, z - s, u1, v0, light);
    }

    private void vertex(VertexConsumer buffer, float x, float y, float z, float u, float v, int light) {
        buffer.vertex(x, y, z).uv(u, v).color(rCol, gCol, bCol, alpha).uv2(light).endVertex();
    }

    public static class Provider implements ParticleProvider<MagicParticleOptions> {
        private final SpriteSet sprites;
        private final Shape shape;

        public Provider(SpriteSet sprites, Shape shape) {
            this.sprites = sprites;
            this.shape = shape;
        }

        @Nullable
        @Override
        public Particle createParticle(MagicParticleOptions options, ClientLevel level, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new MagicParticle(level, x, y, z, vx, vy, vz, options, sprites, shape);
        }
    }
}
