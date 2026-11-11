package com.gabriel.hogwartsmod.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import static com.gabriel.hogwartsmod.client.gui.MissionScreen.*;

public class GryffindorDoorScreen extends HouseDoorScreen {
    private static final int W = 200;
    private static final int H = 232;
    private static final ResourceLocation PAPER = MissionScreen.tex("carta_papel");
    private static final ResourceLocation LADY = MissionScreen.tex("porta_dama");
    private static final ResourceLocation FRAME = MissionScreen.tex("moldura_retrato");

    public GryffindorDoorScreen(BlockPos pos) {
        super(pos, Component.translatable("screen.hogwartsmod.door.grifinoria"));
    }

    @Override
    protected int screenWidth() {
        return W;
    }

    @Override
    protected int screenHeight() {
        return H;
    }

    @Override
    protected boolean acceptsText() {
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        HiRes.image(g, PAPER, left, top, W, H);
        float cx = left + W / 2f;
        HiRes.centered(g, title.getString(), HiRes.Style.BOLD, 8.5f, cx, top + 12, TITLE);
        g.fill(left + 50, top + 23, left + W - 50, top + 24, LINE);
        int fw = 102;
        int fh = 132;
        int fx = left + (W - fw) / 2;
        int fy = top + 28;
        int border = Math.round(fw * 20f / 136f);
        HiRes.image(g, LADY, fx + border - 1, fy + border - 1, fw - border * 2 + 2, fh - border * 2 + 2);
        HiRes.image(g, FRAME, fx, fy, fw, fh);
        double time = System.currentTimeMillis() / 1000.0;
        float bob = (float) Math.sin(time * 4) * 1.5f;
        HiRes.centered(g, "♪", HiRes.Style.ITALIC, 9f, cx - 44 + (float) Math.sin(time * 3) * 2, top + 164 - bob, WINE);
        HiRes.centered(g, "♫", HiRes.Style.ITALIC, 9f, cx + 44 + (float) Math.cos(time * 3) * 2, top + 164 + bob, WINE);
        HiRes.centered(g, tr("screen.hogwartsmod.door.grifinoria.ask"), HiRes.Style.ITALIC, 10f, cx, top + 166 + bob * 0.5f, TITLE);
        drawField(g, cx, top + H - 50, 64, tr("screen.hogwartsmod.door.password_hint"));
        HiRes.centered(g, tr("screen.hogwartsmod.door.enter_hint"), HiRes.Style.ITALIC, 6.5f, cx, top + H - 28, MUTED);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void submit() {
        if (!typed.toString().isBlank()) {
            send(typed.toString(), new int[0], 0);
        }
    }
}
