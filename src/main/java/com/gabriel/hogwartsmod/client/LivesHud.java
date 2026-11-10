package com.gabriel.hogwartsmod.client;

import com.gabriel.hogwartsmod.client.gui.HiRes;
import com.gabriel.hogwartsmod.magic.MagicData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public final class LivesHud implements IGuiOverlay {
    public static final LivesHud INSTANCE = new LivesHud();
    public static boolean enabled;
    private static final net.minecraft.resources.ResourceLocation ICONS = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/icons.png");

    private LivesHud() {
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (!enabled || mc.player == null || mc.options.hideGui || mc.player.isSpectator()) {
            return;
        }
        MagicData data = MagicData.get(mc.player);
        if (data == null) {
            return;
        }
        String text = "x" + data.getLives();
        float tw = HiRes.width(text, HiRes.Style.BOLD, 8f);
        int x = Math.round(width - 8 - tw - 11);
        int y = height - 15;
        int v = data.getLives() <= 1 ? 45 : 0;
        g.blit(ICONS, x, y, 16, v, 9, 9);
        g.blit(ICONS, x, y, 52, v, 9, 9);
        HiRes.text(g, text, HiRes.Style.BOLD, 8f, x + 11, y + 1, 0xFFF3E4C4);
    }
}
