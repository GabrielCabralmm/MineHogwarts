package com.gabriel.hogwartsmod.client;

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
        String text = String.valueOf(data.getLives());
        int w = 12 + 2 + mc.font.width(text) + 4;
        int x = width - 4 - w;
        int y = height - 4 - 11;
        g.fill(x, y, x + w, y + 11, 0xC0281E14);
        int v = data.getLives() <= 1 ? 45 : 0;
        g.blit(ICONS, x + 2, y + 1, 16, v, 9, 9);
        g.blit(ICONS, x + 2, y + 1, 52, v, 9, 9);
        g.drawString(mc.font, text, x + 14, y + 2, 0xFFECDCB6, false);
    }
}
