package com.gabriel.hogwartsmod.wand;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public enum WandHandle {
    LISO("liso"),
    ANELADO("anelado"),
    ESPIRAL("espiral"),
    NODOSO("nodoso"),
    NENHUM("nenhum");

    private final String id;

    WandHandle(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public Component getDisplayName() {
        return Component.translatable("handle.hogwartsmod." + id);
    }

    @Nullable
    public static WandHandle byId(String id) {
        for (WandHandle handle : values()) {
            if (handle.id.equals(id)) {
                return handle;
            }
        }
        return null;
    }
}
