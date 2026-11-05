package com.gabriel.hogwartsmod.teach;

import com.gabriel.hogwartsmod.magic.MagicConfig;
import com.gabriel.hogwartsmod.magic.MagicData;
import com.gabriel.hogwartsmod.spell.Spell;

public final class TeachRules {
    public enum Check {
        OK,
        NOT_KNOWN,
        TEACHER_LEVEL,
        TOO_ADVANCED,
        NO_MASTERY,
        COOLDOWN;

        public String key() {
            return "teach.hogwartsmod.check." + name().toLowerCase();
        }
    }

    public record Limits(int minLevel, int maxSpellLevel) {
        public static Limits fromConfig() {
            return new Limits(MagicConfig.INFORMAL_MIN_LEVEL.get(), MagicConfig.INFORMAL_MAX_SPELL_LEVEL.get());
        }
    }

    private TeachRules() {
    }

    public static Check check(MagicData teacher, Spell spell, Limits limits, long gameTime) {
        if (!teacher.knows(spell)) {
            return Check.NOT_KNOWN;
        }
        if (!teacher.isProfessor()) {
            if (teacher.getLevel() < limits.minLevel()) {
                return Check.TEACHER_LEVEL;
            }
            if (spell.getRequiredLevel() > limits.maxSpellLevel()) {
                return Check.TOO_ADVANCED;
            }
            if (!teacher.hasMaxMastery(spell)) {
                return Check.NO_MASTERY;
            }
        }
        if (teacher.getTeachCooldownRemaining(spell, gameTime) > 0) {
            return Check.COOLDOWN;
        }
        return Check.OK;
    }

    public static boolean isListed(MagicData teacher, Spell spell, Limits limits) {
        Check check = check(teacher, spell, limits, 0L);
        return check == Check.OK || check == Check.COOLDOWN;
    }

    public static int cooldownTicks(MagicData teacher) {
        return teacher.isProfessor() ? MagicConfig.PROFESSOR_COOLDOWN.get() : MagicConfig.INFORMAL_COOLDOWN.get();
    }
}
