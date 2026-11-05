package com.gabriel.hogwartsmod.magic;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public enum Profession {
    NONE("nenhuma"),
    PROFESSOR("professor"),
    WANDMAKER("fabricante_varinhas"),
    BROOMMAKER("fabricante_vassouras");

    private final String id;

    Profession(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public Component getDisplayName() {
        return Component.translatable("profession.hogwartsmod." + id);
    }

    @Nullable
    public static Profession byId(String id) {
        for (Profession profession : values()) {
            if (profession.id.equals(id)) {
                return profession;
            }
        }
        return null;
    }
}
