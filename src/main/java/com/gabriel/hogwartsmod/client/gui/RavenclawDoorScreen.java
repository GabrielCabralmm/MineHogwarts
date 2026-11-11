package com.gabriel.hogwartsmod.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

import static com.gabriel.hogwartsmod.client.gui.MissionScreen.*;

public class RavenclawDoorScreen extends HouseDoorScreen {
    private static final int W = 230;
    private static final int H = 200;
    private static final int BLUE = 0xFF2E4A86;
    private static final ResourceLocation PAPER = MissionScreen.tex("carta_papel");
    private static final ResourceLocation EAGLE = MissionScreen.tex("aguia_aldrava");
    private final int riddle;
    private final String question;

    public RavenclawDoorScreen(BlockPos pos, int riddle, String question) {
        super(pos, Component.translatable("screen.hogwartsmod.door.corvinal"));
        this.riddle = riddle;
        this.question = question;
        this.maxTyped = 64;
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
        HiRes.image(g, PAPER, left, top, W, H);
        float cx = left + W / 2f;
        HiRes.centered(g, title.getString(), HiRes.Style.BOLD, 8.5f, cx, top + 12, BLUE);
        g.fill(left + 56, top + 23, left + W - 56, top + 24, LINE);
        float swing = (float) Math.sin(System.currentTimeMillis() / 600.0) * 4f;
        g.pose().pushPose();
        g.pose().translate(cx, top + 30, 0);
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(swing * 0.4f));
        HiRes.image(g, EAGLE, -24, 0, 48, 48);
        g.pose().popPose();
        HiRes.centered(g, tr("screen.hogwartsmod.door.corvinal.ask"), HiRes.Style.ITALIC, 7.5f, cx, top + 82, MUTED);
        String text = riddle >= 0 ? "“" + question + "”" : tr("screen.hogwartsmod.door.corvinal.none");
        List<String> lines = HiRes.wrap(text, HiRes.Style.BOLD, (W - 50) * HiRes.TEXT_SIZE / 8.5f);
        int y = top + 96;
        for (String line : lines) {
            HiRes.centered(g, line, HiRes.Style.BOLD, 8.5f, cx, y, BLUE);
            y += 11;
        }
        if (riddle >= 0) {
            drawField(g, cx, top + H - 46, 72, tr("screen.hogwartsmod.door.answer_hint"));
            HiRes.centered(g, tr("screen.hogwartsmod.door.enter_hint"), HiRes.Style.ITALIC, 6.5f, cx, top + H - 26, MUTED);
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
