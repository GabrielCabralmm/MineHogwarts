package com.gabriel.hogwartsmod.creature;

import com.gabriel.hogwartsmod.registry.ModEntities;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public enum Species {
    HIPOGRIFO("hipogrifo", () -> ModEntities.HIPPOGRIFF.get()),
    TESTRALIO("testralio", () -> ModEntities.THESTRAL.get()),
    CORUJA("coruja", () -> ModEntities.OWL.get());

    private final String id;
    private final Supplier<EntityType<? extends Entity>> type;

    Species(String id, Supplier<EntityType<? extends Entity>> type) {
        this.id = id;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public EntityType<? extends Entity> getType() {
        return type.get();
    }

    public Component getDisplayName() {
        return Component.translatable("creature.hogwartsmod." + id);
    }

    @Nullable
    public static Species byId(String id) {
        for (Species s : values()) {
            if (s.id.equals(id)) {
                return s;
            }
        }
        return null;
    }

    @Nullable
    public static Species of(Entity entity) {
        for (Species s : values()) {
            if (entity.getType() == s.getType()) {
                return s;
            }
        }
        return null;
    }
}
