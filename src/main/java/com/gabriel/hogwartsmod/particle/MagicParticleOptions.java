package com.gabriel.hogwartsmod.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Locale;

public record MagicParticleOptions(ParticleType<MagicParticleOptions> type, int color, float scale, int lifetime)
        implements ParticleOptions {

    @SuppressWarnings("deprecation")
    public static final Deserializer<MagicParticleOptions> DESERIALIZER = new Deserializer<>() {
        @Override
        public MagicParticleOptions fromCommand(ParticleType<MagicParticleOptions> type, StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            int color = reader.readInt();
            reader.expect(' ');
            float scale = reader.readFloat();
            reader.expect(' ');
            int lifetime = reader.readInt();
            return new MagicParticleOptions(type, color, scale, lifetime);
        }

        @Override
        public MagicParticleOptions fromNetwork(ParticleType<MagicParticleOptions> type, FriendlyByteBuf buf) {
            return new MagicParticleOptions(type, buf.readInt(), buf.readFloat(), buf.readVarInt());
        }
    };

    public static Codec<MagicParticleOptions> codec(ParticleType<MagicParticleOptions> type) {
        return RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("color").forGetter(MagicParticleOptions::color),
                Codec.FLOAT.fieldOf("scale").forGetter(MagicParticleOptions::scale),
                Codec.INT.fieldOf("lifetime").forGetter(MagicParticleOptions::lifetime)
        ).apply(instance, (color, scale, lifetime) -> new MagicParticleOptions(type, color, scale, lifetime)));
    }

    @Override
    public ParticleType<?> getType() {
        return type;
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeInt(color);
        buf.writeFloat(scale);
        buf.writeVarInt(lifetime);
    }

    @Override
    public String writeToString() {
        return String.format(Locale.ROOT, "%s %d %.2f %d", BuiltInRegistries.PARTICLE_TYPE.getKey(type), color, scale, lifetime);
    }

    public float red() {
        return ((color >> 16) & 0xFF) / 255f;
    }

    public float green() {
        return ((color >> 8) & 0xFF) / 255f;
    }

    public float blue() {
        return (color & 0xFF) / 255f;
    }
}
