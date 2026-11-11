package com.gabriel.hogwartsmod.client.gui;

import com.gabriel.hogwartsmod.house.door.HouseDoors;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;

import static com.gabriel.hogwartsmod.client.gui.MissionScreen.*;

public class HufflepuffDoorScreen extends HouseDoorScreen {
    private static final int W = 260;
    private static final int H = 200;
    private static final int LID = 38;
    private static final int BTN_W = 62;
    private static final int BTN_H = 14;
    private static final float[] TILT = {22f, 0f, -22f};
    private static final ResourceLocation PAPER = MissionScreen.tex("carta_papel");
    private static final ResourceLocation BARREL = MissionScreen.tex("barril");
    private static final ResourceLocation BARREL_LIT = MissionScreen.tex("barril_aceso");
    private static final ResourceLocation BARREL_SIDE = MissionScreen.tex("barril_lado");

    private final int length;
    private final List<Integer> hits = new ArrayList<>();
    private long rejectedAt = -1;

    public HufflepuffDoorScreen(BlockPos pos, int length) {
        super(pos, Component.translatable("screen.hogwartsmod.door.lufa_lufa"));
        this.length = length;
    }

    @Override
    protected int screenWidth() {
        return W;
    }

    @Override
    protected int screenHeight() {
        return H;
    }

    private int barrelX(int index) {
        return left + (index < 3 ? 78 : W - 78);
    }

    private int barrelY(int index) {
        return top + 66 + (index % 3) * 42;
    }

    private float tilt(int index) {
        float t = TILT[index % 3];
        return index < 3 ? t : -t;
    }

    // -- Desenho

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        HiRes.image(g, PAPER, left, top, W, H);
        float cx = left + W / 2f;
        HiRes.centered(g, title.getString(), HiRes.Style.BOLD, 10f, cx, top + 14, TITLE);
        HiRes.centered(g, tr("screen.hogwartsmod.door.lufa_lufa.hint"), HiRes.Style.ITALIC, 6.5f, cx, top + 27, MUTED);
        long now = System.currentTimeMillis();
        boolean rejecting = rejectedAt >= 0 && now - rejectedAt < 450;
        float shake = rejecting ? (float) Math.sin(now / 25.0) * 2.5f : 0f;
        for (int i = 0; i < 6; i++) {
            drawBarrel(g, i, mouseX, mouseY, shake, rejecting);
        }
        for (int k = 0; k < length; k++) {
            int dx = (int) cx - length * 5 + k * 10 + 1;
            int c = k < hits.size() ? 0xFFE8B923 : 0xFFC4AA7C;
            g.fill(dx, top + 108, dx + 7, top + 115, 0xFF6E4A2C);
            g.fill(dx + 1, top + 109, dx + 6, top + 114, c);
        }
        int bx = left + (W - BTN_W) / 2;
        int by = top + H - 30;
        boolean hover = over(mouseX, mouseY, bx, by, BTN_W, BTN_H - 2);
        HiRes.image(g, MissionScreen.tex("botao_couro_largo_" + (hover ? "h" : "n")), bx, by, BTN_W, BTN_H);
        HiRes.centered(g, tr("screen.hogwartsmod.door.lufa_lufa.reset"), HiRes.Style.BOLD, 7.5f, bx + BTN_W / 2f, by + 3, 0xFFF6E6C8);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawBarrel(GuiGraphics g, int i, int mouseX, int mouseY, float shake, boolean rejecting) {
        int x = barrelX(i);
        int y = barrelY(i);
        int lit = hits.indexOf(i + 1);
        boolean hover = !rejecting && over(mouseX, mouseY, x - LID / 2, y - LID / 2, LID, LID);
        g.pose().pushPose();
        g.pose().translate(x + shake, y, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(tilt(i)));
        int side = i < 3 ? -1 : 1;
        int bodyW = 52;
        HiRes.image(g, BARREL_SIDE, side < 0 ? -bodyW - 2 : 2, -LID / 2 + 2, bodyW, LID - 4);
        g.pose().scale(0.72f, 1f, 1f);
        HiRes.image(g, lit >= 0 ? BARREL_LIT : BARREL, -LID / 2, -LID / 2, LID, LID);
        if (hover) {
            g.fill(-LID / 2 + 4, -LID / 2 + 4, LID / 2 - 4, LID / 2 - 4, 0x30FFFFFF);
        }
        if (rejecting) {
            g.fill(-LID / 2 + 4, -LID / 2 + 4, LID / 2 - 4, LID / 2 - 4, 0x55B02020);
        }
        g.pose().popPose();
        if (lit >= 0) {
            HiRes.centered(g, String.valueOf(lit + 1), HiRes.Style.BOLD, 12f, x + shake + 0.6f, y - 6 + 0.6f, 0x80000000);
            HiRes.centered(g, String.valueOf(lit + 1), HiRes.Style.BOLD, 12f, x + shake, y - 6, 0xFF4A2A0C);
        }
    }

    // -- Interação

    public void hit(int barrel) {
        if (hits.size() >= length || isWaiting()) {
            return;
        }
        hits.add(barrel);
        sound(SoundEvents.BARREL_OPEN, 1.4f + 0.08f * hits.size());
        send("", hits.stream().mapToInt(Integer::intValue).toArray(), 0);
    }

    public void reset() {
        hits.clear();
    }

    @Override
    public void onResult(int result) {
        if (result == HouseDoors.RESULT_WRONG) {
            hits.clear();
            rejectedAt = System.currentTimeMillis();
            sound(SoundEvents.BARREL_CLOSE, 0.6f);
        }
        super.onResult(result == HouseDoors.RESULT_WRONG ? HouseDoors.RESULT_STEP : result);
    }

    @Override
    protected void submit() {
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            for (int i = 0; i < 6; i++) {
                int x = barrelX(i);
                int y = barrelY(i);
                if ((mx - x) * (mx - x) + (my - y) * (my - y) <= (LID / 2.0) * (LID / 2.0)) {
                    hit(i + 1);
                    return true;
                }
            }
            int bx = left + (W - BTN_W) / 2;
            if (over(mx, my, bx, top + H - 30, BTN_W, BTN_H - 2)) {
                click();
                reset();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }
}
