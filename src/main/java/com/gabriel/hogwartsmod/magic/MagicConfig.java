package com.gabriel.hogwartsmod.magic;

import net.minecraftforge.common.ForgeConfigSpec;

public final class MagicConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue RESERVE_BASE;
    public static final ForgeConfigSpec.DoubleValue RESERVE_PER_LEVEL;
    public static final ForgeConfigSpec.DoubleValue RESERVE_REGEN_PER_SECOND;
    public static final ForgeConfigSpec.IntValue MAX_LEVEL;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("reserva");
        RESERVE_BASE = builder.comment("Reserva Mágica máxima no nível 1").defineInRange("base", 100.0, 1.0, 10000.0);
        RESERVE_PER_LEVEL = builder.comment("Reserva Mágica máxima ganha a cada nível").defineInRange("porNivel", 2.0, 0.0, 1000.0);
        RESERVE_REGEN_PER_SECOND = builder.comment("Regeneração da Reserva Mágica por segundo").defineInRange("regeneracaoPorSegundo", 2.0, 0.0, 1000.0);
        builder.pop();
        builder.push("nivel");
        MAX_LEVEL = builder.comment("Nível Mágico máximo").defineInRange("maximo", 50, 1, 1000);
        builder.pop();
        SPEC = builder.build();
    }

    private MagicConfig() {
    }
}
