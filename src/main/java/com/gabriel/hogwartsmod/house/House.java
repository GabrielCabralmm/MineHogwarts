package com.gabriel.hogwartsmod.house;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.jetbrains.annotations.Nullable;

public enum House {
    GRIFINORIA("grifinoria", 0xD3302F),
    SONSERINA("sonserina", 0x2EA05E),
    CORVINAL("corvinal", 0x4A7BDA),
    LUFA_LUFA("lufa_lufa", 0xE8B923);

    private final String id;
    private final int color;

    House(String id, int color) {
        this.id = id;
        this.color = color;
    }

    public String getId() {
        return id;
    }

    public int getColor() {
        return color;
    }

    public Component getDisplayName() {
        return Component.translatable("house.hogwartsmod." + id).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)));
    }

    @Nullable
    public static House byId(@Nullable String id) {
        if (id == null) {
            return null;
        }
        for (House house : values()) {
            if (house.id.equalsIgnoreCase(id)) {
                return house;
            }
        }
        return null;
    }
}
