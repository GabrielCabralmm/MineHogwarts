package com.gabriel.hogwartsmod.wand;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public enum WandWood {
    CARVALHO("carvalho", true, 0xA2824E),
    AZEVINHO("azevinho", false, 0xDCD0B4),
    TEIXO("teixo", false, 0xA05232),
    SABUGUEIRO("sabugueiro", false, 0xC4BAA4),
    EBANO("ebano", false, 0x2C2422),
    VIDEIRA("videira", false, 0x966E46),
    SALGUEIRO("salgueiro", false, 0xC8B48C),
    CEREJEIRA("cerejeira", true, 0xB06450),
    NOGUEIRA("nogueira", false, 0x64422A),
    ACACIA("acacia", true, 0xB46432),
    ABETO("abeto", true, 0x725430),
    FREIXO("freixo", false, 0xD2C4A0);

    private final String id;
    private final boolean vanilla;
    private final int color;

    WandWood(String id, boolean vanilla, int color) {
        this.id = id;
        this.vanilla = vanilla;
        this.color = color;
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
