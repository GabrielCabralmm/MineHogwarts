package com.gabriel.hogwartsmod.client.gui;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.spell.Spell;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class SpellIcons {

    private SpellIcons() {
    }

    public static ResourceLocation texture(Spell spell) {
        return ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "textures/gui/spells/" + spell.getId() + ".png");
    }

    public static void draw(GuiGraphics g, Spell spell, int x, int y, int size) {
        HiRes.image(g, texture(spell), x, y, size, size);
    }
}
