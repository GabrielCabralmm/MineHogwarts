package com.gabriel.hogwartsmod.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

import static com.gabriel.hogwartsmod.client.gui.MissionScreen.*;

public class RavenclawDoorScreen extends HouseDoorScreen {
    private static final int W = 230;
    private static final int H = 226;
    private static final int BLUE = 0xFF2E4A86;
    private static final ResourceLocation BACKGROUND = MissionScreen.tex("corvinal_fundo");
    private static final int BRONZE = 0xFFE0A85A;
    private static final int SKY = 0xFFD8E4FF;
    private static final ResourceLocation EAGLE = MissionScreen.tex("aguia_aldrava");
    private static final ResourceLocation CREST = MissionScreen.tex("casa_corvinal");
    private final int riddle;
    private final String question;

    public RavenclawDoorScreen(BlockPos pos, int riddle, String question) {
        super(pos, Component.translatable("screen.hogwartsmod.door.corvinal"));
        this.riddle = riddle;
        this.question = question;
        this.maxTyped = 64;
        fieldInk = SKY;
        fieldMuted = 0xFF93A6D4;
        fieldLine = 0xFFC08A4A;
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
        return riddle >= 0;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        HiRes.image(g, BACKGROUND, left, top, W, H);
        float cx = left + W / 2f;
        HiRes.image(g, CREST, (int) cx - 13, top + 12, 26, 30);
        shadowed(g, title.getString(), HiRes.Style.BOLD, 9f, cx, top + 45, BRONZE);
        g.fill(left + 56, top + 57, left + W - 56, top + 58, 0xFFC08A4A);
        float swing = (float) Math.sin(System.currentTimeMillis() / 600.0) * 4f;
        g.pose().pushPose();
        g.pose().translate(cx, top + 62, 0);
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(swing * 0.4f));
        HiRes.image(g, EAGLE, -30, 0, 60, 48);
        g.pose().popPose();
        HiRes.centered(g, tr("screen.hogwartsmod.door.corvinal.ask"), HiRes.Style.ITALIC, 7.5f, cx, top + 113, 0xFF93A6D4);
        String text = riddle >= 0 ? "“" + question + "”" : tr("screen.hogwartsmod.door.corvinal.none");
        List<String> lines = HiRes.wrap(text, HiRes.Style.BOLD, (W - 50) * HiRes.TEXT_SIZE / 8.5f);
        int y = top + 126;
        for (String line : lines) {
            shadowed(g, line, HiRes.Style.BOLD, 8.5f, cx, y, SKY);
            y += 11;
        }
        if (riddle >= 0) {
            drawField(g, cx, top + H - 46, 72, tr("screen.hogwartsmod.door.answer_hint"));
            HiRes.centered(g, tr("screen.hogwartsmod.door.enter_hint"), HiRes.Style.ITALIC, 6.5f, cx, top + H - 26, 0xFF93A6D4);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void submit() {
        if (riddle >= 0 && !typed.toString().isBlank()) {
            send(typed.toString(), new int[0], riddle);
        }
    }
}
