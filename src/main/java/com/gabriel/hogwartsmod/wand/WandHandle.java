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
    private net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> item;

    WandHandle(String id) {
        this.id = id;
    }

    @Nullable
    public net.minecraft.world.item.Item getItem() {
        return item == null ? null : item.get();
    }

    void bind(net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> item) {
        this.item = item;
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
