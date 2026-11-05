package com.gabriel.hogwartsmod.magic;

import net.minecraftforge.common.ForgeConfigSpec;

public final class MagicConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue RESERVE_BASE;
    public static final ForgeConfigSpec.DoubleValue RESERVE_PER_LEVEL;
    public static final ForgeConfigSpec.DoubleValue RESERVE_REGEN_PER_SECOND;
    public static final ForgeConfigSpec.IntValue MAX_LEVEL;
    public static final ForgeConfigSpec.IntValue PROFESSOR_COOLDOWN;
    public static final ForgeConfigSpec.IntValue INFORMAL_COOLDOWN;
    public static final ForgeConfigSpec.IntValue INFORMAL_MIN_LEVEL;
    public static final ForgeConfigSpec.IntValue INFORMAL_MAX_SPELL_LEVEL;
    public static final ForgeConfigSpec.IntValue INVITE_RANGE;
    public static final ForgeConfigSpec.IntValue INVITE_SECONDS;
    public static final ForgeConfigSpec.IntValue TRACE_ATTEMPTS;
    public static final ForgeConfigSpec.IntValue LEARN_XP_BASE;
    public static final ForgeConfigSpec.IntValue LEARN_XP_PER_LEVEL;
    public static final ForgeConfigSpec.IntValue TEACHER_XP_BASE;
    public static final ForgeConfigSpec.IntValue TEACHER_XP_PER_LEVEL;

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
        builder.push("ensino");
        PROFESSOR_COOLDOWN = builder.comment("Recarga de ensino de cada feitiço para professores, em ticks (24000 = 1 dia do Minecraft)").defineInRange("recargaProfessor", 24000, 0, 10_000_000);
        INFORMAL_COOLDOWN = builder.comment("Recarga de ensino de cada feitiço para quem não é professor, em ticks").defineInRange("recargaSemProfissao", 48000, 0, 10_000_000);
        INFORMAL_MIN_LEVEL = builder.comment("Nível Mágico mínimo para ensinar sem ser professor").defineInRange("nivelMinimoSemProfissao", 10, 1, 1000);
        INFORMAL_MAX_SPELL_LEVEL = builder.comment("Nível máximo dos feitiços que podem ser ensinados sem ser professor").defineInRange("nivelMaximoDoFeiticoSemProfissao", 10, 1, 1000);
        INVITE_RANGE = builder.comment("Distância máxima, em blocos, entre quem ensina e o aluno").defineInRange("distanciaConvite", 8, 1, 128);
        INVITE_SECONDS = builder.comment("Tempo, em segundos, para o aluno aceitar o convite").defineInRange("validadeConvite", 60, 5, 3600);
        TRACE_ATTEMPTS = builder.comment("Tentativas do aluno no traçado do feitiço").defineInRange("tentativasTracado", 3, 1, 20);
        LEARN_XP_BASE = builder.comment("Experiência ganha ao aprender um feitiço").defineInRange("xpAprenderBase", 20, 0, 100000);
        LEARN_XP_PER_LEVEL = builder.comment("Experiência extra ao aprender, por nível do feitiço").defineInRange("xpAprenderPorNivel", 10, 0, 100000);
        TEACHER_XP_BASE = builder.comment("Experiência do professor por aluno aprovado").defineInRange("xpProfessorBase", 15, 0, 100000);
        TEACHER_XP_PER_LEVEL = builder.comment("Experiência extra do professor por aluno aprovado, por nível do feitiço").defineInRange("xpProfessorPorNivel", 5, 0, 100000);
        builder.pop();
        SPEC = builder.build();
    }

    private MagicConfig() {
    }
}
