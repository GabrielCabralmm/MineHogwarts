package com.gabriel.hogwartsmod.potion;

import com.gabriel.hogwartsmod.magic.MagicData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public enum Brew {
    WIGGENWELD("wiggenweld", 1, Action.CURA, 0x5FCFA2, 15, List.of(
            Step.item("hogwartsmod:muco_vermecego", 2),
            Step.stir(2),
            Step.item("hogwartsmod:casca_wiggentree", 1),
            Step.item("minecraft:honey_bottle", 1),
            Step.stir(3))),
    REVIGORANTE("revigorante", 2, Action.MAGIA, 0x4F9BFF, 20, List.of(
            Step.item("hogwartsmod:po_pedra_da_lua", 2),
            Step.stir(1),
            Step.item("minecraft:glowstone_dust", 1),
            Step.item("minecraft:sugar", 2),
            Step.stir(3),
            Step.wand(1)));

    public static final String STIR = "mexer";
    public static final String WAND = "varinha";
    public static final int MAX_DIFFICULTY = 5;

    public enum Action {
        CURA, MAGIA, PROTECAO, MELHORIA, MENTE, TRANSFORMACAO;

        public Component getDisplayName() {
            return Component.translatable("potion.hogwartsmod.action." + name().toLowerCase());
        }
    }

    private final String id;
    private final int difficulty;
    private final Action action;
    private final int color;
    private final int seconds;
    private final List<Step> steps;
    private final List<String> tokens;

    Brew(String id, int difficulty, Action action, int color, int seconds, List<Step> steps) {
        this.id = id;
        this.difficulty = difficulty;
        this.action = action;
        this.color = color;
        this.seconds = seconds;
        this.steps = steps;
        List<String> list = new ArrayList<>();
        for (Step step : steps) {
            for (int i = 0; i < step.count(); i++) {
                list.add(step.isStir() ? STIR : step.item());
            }
        }
        this.tokens = List.copyOf(list);
    }

    public record Step(@Nullable String item, int count) {
        static Step item(String id, int count) {
            return new Step(id, count);
        }

        static Step stir(int count) {
            return new Step(null, count);
        }

        static Step wand(int count) {
            return new Step(WAND, count);
        }

        public boolean isStir() {
            return item == null;
        }

        public boolean isWand() {
            return WAND.equals(item);
        }
    }

    public String getId() {
        return id;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public Action getAction() {
        return action;
    }

    public int getDoses() {
        return difficulty <= 2 ? 3 : difficulty <= 4 ? 2 : 1;
    }

    public static List<Brew> ofDifficulty(int level) {
        List<Brew> list = new ArrayList<>();
        for (Action action : Action.values()) {
            for (Brew brew : values()) {
                if (brew.difficulty == level && brew.action == action) {
                    list.add(brew);
                }
            }
        }
        return list;
    }

    public int getColor() {
        return color;
    }

    public int getSeconds() {
        return seconds;
    }

    public List<Step> getSteps() {
        return steps;
    }

    public List<String> getTokens() {
        return tokens;
    }

    public Component getDisplayName() {
        return Component.translatable("potion.hogwartsmod." + id);
    }

    public Component getEffectText() {
        return Component.translatable("potion.hogwartsmod." + id + ".effect");
    }

    @Nullable
    public static Brew byId(String id) {
        for (Brew brew : values()) {
            if (brew.id.equals(id)) {
                return brew;
            }
        }
        return null;
    }

    public static boolean usesItem(String itemId) {
        for (Brew brew : values()) {
            if (brew.tokens.contains(itemId)) {
                return true;
            }
        }
        return false;
    }

    public void apply(LivingEntity entity) {
        switch (this) {
            case WIGGENWELD -> {
                entity.heal(10f);
                entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 0));
                entity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                entity.removeEffect(MobEffects.WEAKNESS);
            }
            case REVIGORANTE -> {
                if (entity instanceof Player player) {
                    MagicData data = MagicData.get(player);
                    if (data != null) {
                        data.setReserve(data.getReserve() + data.getMaxReserve() * 0.5f);
                    }
                }
                entity.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 600, 0));
            }
        }
    }
}
