package com.gabriel.hogwartsmod.particle;

import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleType;

public class MagicParticleType extends ParticleType<MagicParticleOptions> {

    @SuppressWarnings("deprecation")
    public MagicParticleType() {
        super(false, MagicParticleOptions.DESERIALIZER);
    }

    @Override
    public Codec<MagicParticleOptions> codec() {
        return MagicParticleOptions.codec(this);
    }

    public MagicParticleOptions with(int color, float scale, int lifetime) {
        return new MagicParticleOptions(this, color, scale, lifetime);
    }
}
