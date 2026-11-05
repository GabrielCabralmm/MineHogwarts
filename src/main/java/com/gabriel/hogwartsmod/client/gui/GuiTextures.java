package com.gabriel.hogwartsmod.client.gui;

import com.gabriel.hogwartsmod.HogwartsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;

public final class GuiTextures {
    public static final ResourceLocation SERIF = ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "serif");
    public static final ResourceLocation SERIF_BOLD = ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "serif_bold");
    public static final ResourceLocation SERIF_ITALIC = ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "serif_italic");
    private static final Set<ResourceLocation> SMOOTHED = new HashSet<>();

    private GuiTextures() {
    }

    public static MutableComponent serif(Component text) {
        return text.copy().withStyle(style -> style.withFont(SERIF));
    }

    public static MutableComponent bold(Component text) {
        return text.copy().withStyle(style -> style.withFont(SERIF_BOLD));
    }

    public static MutableComponent italic(Component text) {
        return text.copy().withStyle(style -> style.withFont(SERIF_ITALIC));
    }

    public static void blitSmooth(GuiGraphics g, ResourceLocation texture, int x, int y, int w, int h, int texW, int texH) {
        if (SMOOTHED.add(texture)) {
            Minecraft.getInstance().getTextureManager().getTexture(texture).setFilter(true, false);
        }
        g.blit(texture, x, y, w, h, 0, 0, texW, texH, texW, texH);
    }
}
