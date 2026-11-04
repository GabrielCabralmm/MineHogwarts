package com.gabriel.hogwartsmod.spell;

import net.minecraft.network.chat.Component;
import org.joml.Vector3f;

public enum Spell {
    LUMOS("lumos", 0xFFF6C2, 40, false),
    EXPELLIARMUS("expelliarmus", 0xE03030, 30, true),
    STUPEFY("stupefy", 0xD03070, 40, true);

    private final String id;
    private final int color;
    private final int cooldownTicks;
    private final boolean projectile;

    Spell(String id, int color, int cooldownTicks, boolean projectile) {
        this.id = id;
        this.color = color;
        this.cooldownTicks = cooldownTicks;
        this.projectile = projectile;
    }

    public String getId() {
        return id;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    public boolean isProjectile() {
        return projectile;
    }

    public Vector3f getColorVector() {
        return new Vector3f(((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f);
    }

    public Component getDisplayName() {
        return Component.translatable("spell.hogwartsmod." + id);
    }

    public Spell next() {
        Spell[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static Spell byId(String id) {
        for (Spell spell : values()) {
            if (spell.id.equals(id)) {
                return spell;
            }
        }
        return LUMOS;
    }
}
