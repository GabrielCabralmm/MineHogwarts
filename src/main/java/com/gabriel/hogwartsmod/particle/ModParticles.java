package com.gabriel.hogwartsmod.particle;

import com.gabriel.hogwartsmod.HogwartsMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, HogwartsMod.MOD_ID);

    public static final RegistryObject<MagicParticleType> GLOW = PARTICLES.register("glow", MagicParticleType::new);
    public static final RegistryObject<MagicParticleType> SPARK = PARTICLES.register("spark", MagicParticleType::new);
    public static final RegistryObject<MagicParticleType> EMBER = PARTICLES.register("ember", MagicParticleType::new);
    public static final RegistryObject<MagicParticleType> SMOKE = PARTICLES.register("smoke", MagicParticleType::new);
    public static final RegistryObject<MagicParticleType> FIRE = PARTICLES.register("fire", MagicParticleType::new);
    public static final RegistryObject<MagicParticleType> RING = PARTICLES.register("ring", MagicParticleType::new);
    public static final RegistryObject<MagicParticleType> STAR = PARTICLES.register("star", MagicParticleType::new);
    public static final RegistryObject<MagicParticleType> ORB = PARTICLES.register("orb", MagicParticleType::new);

    private ModParticles() {
    }
}
