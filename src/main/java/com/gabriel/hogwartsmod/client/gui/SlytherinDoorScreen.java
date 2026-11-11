package com.gabriel.hogwartsmod.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

import static com.gabriel.hogwartsmod.client.gui.MissionScreen.*;

public class SlytherinDoorScreen extends HouseDoorScreen {
    private static final int W = 250;
    private static final int H = 190;
    private static final int RING_W = 44;
    private static final int GAP = 6;
    private static final int CYL_H = 70;
    private static final int STEP = 26;
    private static final int BTN_W = 62;
    private static final int BTN_H = 14;
    private static final ResourceLocation PAPER = MissionScreen.tex("carta_papel");
    private static final ResourceLocation UP = MissionScreen.tex("arrow_up_ink");
    private static final ResourceLocation DOWN = MissionScreen.tex("arrow_down_ink");

    private final int symbols;
    private final int[] value;
    private final float[] roll;
    private long lastFrame = System.currentTimeMillis();

    public SlytherinDoorScreen(BlockPos pos, int rings, int symbols) {
        super(pos, Component.translatable("screen.hogwartsmod.door.sonserina"));
        this.symbols = Math.max(2, symbols);
        this.value = new int[Math.max(1, rings)];
        this.roll = new float[value.length];
    }

    @Override
    protected int screenWidth() {
        return W;
    }

    @Override
    protected int screenHeight() {
        return H;
    }

    private int ringX(int i) {
        int total = value.length * RING_W + (value.length - 1) * GAP;
        return left + (W - total) / 2 + i * (RING_W + GAP);
    }

    private int cylY() {
        return top + 62;
    }

    private static ResourceLocation symbol(int index) {
        return MissionScreen.tex("serpente_" + index);
    }

    // -- Desenho

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        long now = System.currentTimeMillis();
        float dt = Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        for (int i = 0; i < roll.length; i++) {
            roll[i] *= (float) Math.pow(0.0005, dt);
            if (Math.abs(roll[i]) < 0.01f) {
                roll[i] = 0;
            }
        }
        renderBackground(g);
        HiRes.image(g, PAPER, left, top, W, H);
        float cx = left + W / 2f;
        HiRes.centered(g, title.getString(), HiRes.Style.BOLD, 10f, cx, top + 14, TITLE);
        HiRes.centered(g, tr("screen.hogwartsmod.door.sonserina.hint"), HiRes.Style.ITALIC, 6.5f, cx, top + 27, MUTED);
        int y = cylY();
        g.fill(ringX(0) - 6, y - 2, ringX(value.length - 1) + RING_W + 6, y + CYL_H + 2, 0xFF2A2E2C);
        for (int i = 0; i < value.length; i++) {
            drawRing(g, i, mouseX, mouseY);
        }
        int mid = y + CYL_H / 2;
        g.fill(ringX(0) - 6, mid - 15, ringX(value.length - 1) + RING_W + 6, mid - 14, 0xFF7FD8A0);
        g.fill(ringX(0) - 6, mid + 14, ringX(value.length - 1) + RING_W + 6, mid + 15, 0xFF7FD8A0);
        g.fill(ringX(0) - 6, mid - 14, ringX(value.length - 1) + RING_W + 6, mid + 14, 0x2232C070);
        int bx = left + (W - BTN_W) / 2;
        int by = top + H - 32;
        boolean hover = over(mouseX, mouseY, bx, by, BTN_W, BTN_H - 2);
        HiRes.image(g, MissionScreen.tex("botao_couro_largo_" + (hover ? "h" : "n")), bx, by, BTN_W, BTN_H);
        HiRes.centered(g, tr("screen.hogwartsmod.door.sonserina.open"), HiRes.Style.BOLD, 7.5f, bx + BTN_W / 2f, by + 3, 0xFFF6E6C8);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawRing(GuiGraphics g, int i, int mouseX, int mouseY) {
        int x = ringX(i);
        int y = cylY();
        for (int r = 0; r < CYL_H; r++) {
            double t = (r - CYL_H / 2.0) / (CYL_H / 2.0);
            float light = (float) Math.cos(t * Math.PI / 2);
            int c = (int) (40 + 70 * light);
            g.fill(x, y + r, x + RING_W, y + r + 1, 0xFF000000 | (c << 16) | ((c + 6) << 8) | (c + 2));
        }
        g.fill(x, y, x + 1, y + CYL_H, 0x55000000);
        g.fill(x + RING_W - 1, y, x + RING_W, y + CYL_H, 0x55000000);
        g.enableScissor(x, y, x + RING_W, y + CYL_H);
        int mid = y + CYL_H / 2;
        for (int k = -2; k <= 2; k++) {
            float offset = (k + roll[i]) * STEP;
            int index = Math.floorMod(value[i] + k, symbols);
            float dist = Math.abs(offset) / (STEP * 2f);
            int size = Math.round(26 * (1f - 0.35f * Math.min(1f, dist)));
            HiRes.image(g, symbol(index), x + (RING_W - size) / 2, Math.round(mid + offset - size / 2f), size, size);
        }
        g.disableScissor();
        boolean up = over(mouseX, mouseY, x + RING_W / 2 - 6, y - 16, 12, 12);
        boolean down = over(mouseX, mouseY, x + RING_W / 2 - 6, y + CYL_H + 4, 12, 12);
        HiRes.image(g, up ? MissionScreen.tex("arrow_up_wine") : UP, x + RING_W / 2 - 5, y - 15, 10, 10);
        HiRes.image(g, down ? MissionScreen.tex("arrow_down_wine") : DOWN, x + RING_W / 2 - 5, y + CYL_H + 5, 10, 10);
    }

    // -- Interação

    public void turn(int ring, int delta) {
        value[ring] = Math.floorMod(value[ring] + delta, symbols);
        roll[ring] += delta;
        sound(SoundEvents.STONE_BUTTON_CLICK_ON, 0.6f + 0.1f * ring);
    }

    public void submit() {
        int[] combo = new int[value.length];
        for (int i = 0; i < value.length; i++) {
            combo[i] = value[i] + 1;
        }
        send("", combo, 0);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            for (int i = 0; i < value.length; i++) {
                int x = ringX(i);
                if (over(mx, my, x + RING_W / 2 - 6, cylY() - 16, 12, 12)) {
                    turn(i, -1);
                    return true;
                }
                if (over(mx, my, x + RING_W / 2 - 6, cylY() + CYL_H + 4, 12, 12)) {
                    turn(i, 1);
                    return true;
                }
            }
            int bx = left + (W - BTN_W) / 2;
            if (over(mx, my, bx, top + H - 32, BTN_W, BTN_H - 2)) {
                click();
                submit();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        for (int i = 0; i < value.length; i++) {
            if (over(mx, my, ringX(i), cylY(), RING_W, CYL_H)) {
                turn(i, delta > 0 ? -1 : 1);
                return true;
            }
        }
        return super.mouseScrolled(mx, my, delta);
    }
}
