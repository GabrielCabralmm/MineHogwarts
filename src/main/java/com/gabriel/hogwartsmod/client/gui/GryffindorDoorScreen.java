package com.gabriel.hogwartsmod.client.gui;

import com.gabriel.hogwartsmod.HogwartsMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import static com.gabriel.hogwartsmod.client.gui.MissionScreen.*;

public class GryffindorDoorScreen extends HouseDoorScreen {
    private static final int W = 200;
    private static final int H = 232;
    private static final ResourceLocation BACKGROUND = MissionScreen.tex("grifinoria_fundo");
    private static final int GOLD = 0xFFF2C64A;
    private static final int CREAM = 0xFFF0DDB8;
    private static final ResourceLocation LADY = ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "textures/block/porta/grifinoria_tela.png");
    private static final ResourceLocation FRAME = MissionScreen.tex("moldura_retrato");
    private static final ResourceLocation CREST = MissionScreen.tex("casa_grifinoria");

    public GryffindorDoorScreen(BlockPos pos) {
        super(pos, Component.translatable("screen.hogwartsmod.door.grifinoria"));
        fieldInk = CREAM;
        fieldMuted = 0xFFC9A98A;
        fieldLine = 0xFFD9A84A;
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
        HiRes.image(g, BACKGROUND, left, top, W, H);
        float cx = left + W / 2f;
        HiRes.image(g, CREST, (int) cx - 13, top + 10, 26, 30);
        shadowed(g, title.getString(), HiRes.Style.BOLD, 9f, cx, top + 43, GOLD);
        g.fill(left + 50, top + 55, left + W - 50, top + 56, 0xFFD9A84A);
        int fw = 78;
        int fh = 107;
        int fx = left + (W - fw) / 2;
        int fy = top + 61;
        int border = Math.round(fw * 8f / 128f);
        long ticks = minecraft != null && minecraft.level != null ? minecraft.level.getGameTime() : 0;
        int frame = (int) ((ticks + partialTick) / 2.5f) % 48;
        g.blit(LADY, fx + border - 1, fy + border - 1, fw - border * 2 + 2, fh - border * 2 + 2, 0, frame * 160, 112, 160, 112, 160 * 48);
        HiRes.image(g, FRAME, fx, fy, fw, fh);
        double time = System.currentTimeMillis() / 1000.0;
        float bob = (float) Math.sin(time * 4) * 1.5f;
        HiRes.centered(g, "♪", HiRes.Style.ITALIC, 9f, cx - 44 + (float) Math.sin(time * 3) * 2, top + 170 - bob, GOLD);
        HiRes.centered(g, "♫", HiRes.Style.ITALIC, 9f, cx + 44 + (float) Math.cos(time * 3) * 2, top + 170 + bob, GOLD);
        shadowed(g, tr("screen.hogwartsmod.door.grifinoria.ask"), HiRes.Style.ITALIC, 10f, cx, top + 171 + bob * 0.5f, CREAM);
        drawField(g, cx, top + H - 46, 64, tr("screen.hogwartsmod.door.password_hint"));
        HiRes.centered(g, tr("screen.hogwartsmod.door.enter_hint"), HiRes.Style.ITALIC, 6.5f, cx, top + H - 24, 0xFFC9A98A);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void submit() {
        if (!typed.toString().isBlank()) {
            send(typed.toString(), new int[0], 0);
        }
    }
}
