package com.gabriel.hogwartsmod.wand;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public enum WandWood {
    CARVALHO("carvalho", true, true, 0xA2824E),
    AZEVINHO("azevinho", false, false, 0xDCD0B4),
    TEIXO("teixo", false, false, 0xA05232),
    SABUGUEIRO("sabugueiro", false, false, 0xC4BAA4),
    EBANO("ebano", false, false, 0x2C2422),
    VIDEIRA("videira", false, false, 0x966E46),
    SALGUEIRO("salgueiro", false, true, 0xC8B48C),
    CEREJEIRA("cerejeira", true, true, 0xB06450),
    NOGUEIRA("nogueira", false, true, 0x64422A),
    ACACIA("acacia", true, true, 0xB46432),
    ABETO("abeto", true, true, 0x725430),
    FREIXO("freixo", false, true, 0xD2C4A0);

    private final String id;
    private final boolean vanilla;
    private final boolean common;
    private final int color;

    WandWood(String id, boolean vanilla, boolean common, int color) {
        this.id = id;
        this.vanilla = vanilla;
        this.common = common;
        this.color = color;
    }

    public boolean isCommon() {
        return common;
    }

    public String getId() {
        return id;
    }

    public boolean isVanilla() {
        return vanilla;
    }

    public int getColor() {
        return color;
    }

    public Component getDisplayName() {
        return Component.translatable("wood.hogwartsmod." + id);
    }

    @Nullable
    public static WandWood byId(String id) {
        for (WandWood wood : values()) {
            if (wood.id.equals(id)) {
                return wood;
            }
        }
        return null;
    }
}
