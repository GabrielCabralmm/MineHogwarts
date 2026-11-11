package com.gabriel.hogwartsmod.client.gui;

import com.gabriel.hogwartsmod.house.door.HouseDoors;
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
    private static final int RADIUS = 20;
    private static final int BTN_W = 62;
    private static final int BTN_H = 14;
    private static final ResourceLocation BACKGROUND = MissionScreen.tex("lufa_fundo");
    private static final ResourceLocation CREST = MissionScreen.tex("casa_lufa_lufa");
    private static final ResourceLocation COMB_FULL = MissionScreen.tex("favo_cheio");
    private static final ResourceLocation COMB_EMPTY = MissionScreen.tex("favo_vazio");
    private static final int GOLD = 0xFFF2C64A;
    private static final int CREAM = 0xFFE8D6B0;
    private static final ResourceLocation BARREL = MissionScreen.tex("barril");
    private static final ResourceLocation BARREL_LIT = MissionScreen.tex("barril_aceso");

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
        int inset = index % 3 == 1 ? 58 : 72;
        return left + (index < 3 ? inset : W - inset);
    }

    private int barrelY(int index) {
        return top + 102 + (index % 3) * 40;
    }

    // -- Desenho

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        HiRes.image(g, BACKGROUND, left, top, W, H);
        float cx = left + W / 2f;
        HiRes.image(g, CREST, (int) cx - 13, top + 20, 26, 30);
        HiRes.centered(g, title.getString(), HiRes.Style.BOLD, 10f, cx + 0.7f, top + 54.7f, 0xC0000000);
        HiRes.centered(g, title.getString(), HiRes.Style.BOLD, 10f, cx, top + 54, GOLD);
        HiRes.centered(g, tr("screen.hogwartsmod.door.lufa_lufa.hint"), HiRes.Style.ITALIC, 6.5f, cx, top + 67, CREAM);
        long now = System.currentTimeMillis();
        boolean rejecting = rejectedAt >= 0 && now - rejectedAt < 450;
        float shake = rejecting ? (float) Math.sin(now / 25.0) * 2.5f : 0f;
        for (int i = 0; i < 6; i++) {
            drawBarrel(g, i, mouseX, mouseY, shake, rejecting);
        }
        for (int k = 0; k < length; k++) {
            int dx = (int) cx - length * 7 + k * 14;
            HiRes.image(g, k < hits.size() ? COMB_FULL : COMB_EMPTY, dx, top + 135, 14, 14);
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
        boolean hover = !rejecting && Math.pow(mouseX - x, 2) + Math.pow(mouseY - y, 2) <= RADIUS * RADIUS;
        int size = RADIUS * 2 + 4;
        float lift = hover ? 1.5f : 0f;
        if (rejecting) {
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 0.5f, 0.45f, 1f);
        } else if (hover) {
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.15f, 1.12f, 1.05f, 1f);
        }
        HiRes.image(g, lit >= 0 ? BARREL_LIT : BARREL, Math.round(x + shake - size / 2f - lift), Math.round(y - size / 2f - lift), size, size);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        if (lit >= 0) {
            HiRes.centered(g, String.valueOf(lit + 1), HiRes.Style.BOLD, 13f, x + shake + 0.7f, y - 7 + 0.7f, 0x80000000);
            HiRes.centered(g, String.valueOf(lit + 1), HiRes.Style.BOLD, 13f, x + shake, y - 7, 0xFF4A2A0C);
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
                if ((mx - x) * (mx - x) + (my - y) * (my - y) <= RADIUS * RADIUS) {
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
