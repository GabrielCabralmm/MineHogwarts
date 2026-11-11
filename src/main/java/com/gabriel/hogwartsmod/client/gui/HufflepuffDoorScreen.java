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
    private static final int W = 280;
    private static final int H = 218;
    private static final int LID = 32;
    private static final int BTN_W = 62;
    private static final int BTN_H = 14;
    private static final float[] TILT = {14f, 0f, -14f};
    private static final ResourceLocation BACKGROUND = MissionScreen.tex("lufa_fundo");
    private static final ResourceLocation CREST = MissionScreen.tex("casa_lufa_lufa");
    private static final ResourceLocation SHADOW = MissionScreen.tex("sombra_barril");
    private static final ResourceLocation COMB_FULL = MissionScreen.tex("favo_cheio");
    private static final ResourceLocation COMB_EMPTY = MissionScreen.tex("favo_vazio");
    private static final int GOLD = 0xFFF2C64A;
    private static final int CREAM = 0xFFE8D6B0;
    private static final ResourceLocation BARREL = MissionScreen.tex("barril");
    private static final ResourceLocation BARREL_LIT = MissionScreen.tex("barril_aceso");
    private static final ResourceLocation BARREL_LEFT = MissionScreen.tex("barril_esq");
    private static final ResourceLocation BARREL_LIT_LEFT = MissionScreen.tex("barril_aceso_esq");

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
        return left + (index < 3 ? 96 : W - 96);
    }

    private int barrelY(int index) {
        return top + 90 + (index % 3) * 38;
    }

    private float tilt(int index) {
        float t = TILT[index % 3];
        return index < 3 ? t : -t;
    }

    // -- Desenho

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        HiRes.image(g, BACKGROUND, left, top, W, H);
        float cx = left + W / 2f;
        HiRes.image(g, CREST, (int) cx - 13, top + 10, 26, 30);
        HiRes.centered(g, title.getString(), HiRes.Style.BOLD, 10f, cx + 0.7f, top + 44.7f, 0xC0000000);
        HiRes.centered(g, title.getString(), HiRes.Style.BOLD, 10f, cx, top + 44, GOLD);
        HiRes.centered(g, tr("screen.hogwartsmod.door.lufa_lufa.hint"), HiRes.Style.ITALIC, 6.5f, cx, top + 57, CREAM);
        long now = System.currentTimeMillis();
        boolean rejecting = rejectedAt >= 0 && now - rejectedAt < 450;
        float shake = rejecting ? (float) Math.sin(now / 25.0) * 2.5f : 0f;
        for (int i = 0; i < 6; i++) {
            int x = barrelX(i);
            int y = barrelY(i);
            int dx = i < 3 ? -34 : -6;
            HiRes.image(g, SHADOW, x + dx, y + 12, 36, 12);
        }
        for (int i = 0; i < 6; i++) {
            drawBarrel(g, i, mouseX, mouseY, shake, rejecting);
        }
        for (int k = 0; k < length; k++) {
            int dx = (int) cx - length * 7 + k * 14;
            HiRes.image(g, k < hits.size() ? COMB_FULL : COMB_EMPTY, dx, top + 122, 14, 14);
        }
        int bx = left + (W - BTN_W) / 2;
        int by = top + H - 30;
        boolean hover = over(mouseX, mouseY, bx, by, BTN_W, BTN_H - 2);
        HiRes.image(g, MissionScreen.tex("botao_lufa_" + (hover ? "h" : "n")), bx, by, BTN_W, BTN_H);
        HiRes.centered(g, tr("screen.hogwartsmod.door.lufa_lufa.reset"), HiRes.Style.BOLD, 7.5f, bx + BTN_W / 2f, by + 3, GOLD);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawBarrel(GuiGraphics g, int i, int mouseX, int mouseY, float shake, boolean rejecting) {
        int x = barrelX(i);
        int y = barrelY(i);
        int lit = hits.indexOf(i + 1);
        boolean hover = !rejecting && Math.pow((mouseX - x) / 13.0, 2) + Math.pow((mouseY - y) / 20.0, 2) <= 1;
        boolean left = i < 3;
        ResourceLocation image = lit >= 0 ? (left ? BARREL_LIT : BARREL_LIT_LEFT) : (left ? BARREL : BARREL_LEFT);
        int bw = 77;
        int bh = 48;
        int lidOffset = Math.round(bw * 22f / 96f);
        g.pose().pushPose();
        g.pose().translate(x + shake, y, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(tilt(i)));
        HiRes.image(g, image, left ? -bw + lidOffset : -lidOffset, -bh / 2, bw, bh);
        if (hover) {
            g.fill(-9, -15, 9, 15, 0x22FFFFFF);
        }
        if (rejecting) {
            g.fill(left ? -bw + lidOffset : -lidOffset, -bh / 2 + 6, left ? lidOffset : bw - lidOffset, bh / 2 - 6, 0x40B02020);
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
                double ex = (mx - x) / 13.0;
                double ey = (my - y) / 20.0;
                if (ex * ex + ey * ey <= 1) {
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
