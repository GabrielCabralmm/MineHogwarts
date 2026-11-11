package com.gabriel.hogwartsmod.client;

import com.gabriel.hogwartsmod.client.gui.HiRes;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public final class DuelCueHud implements IGuiOverlay {
    public static final DuelCueHud INSTANCE = new DuelCueHud();
    private static final int OUTLINE = 0xFF1C1008;

    private static String cue = "";
    private static int number;
    private static long start;

    private DuelCueHud() {
    }

    public static void show(String kind, int n) {
        cue = kind;
        number = n;
        start = System.currentTimeMillis();
    }

    private static long duration() {
        return switch (cue) {
            case "count" -> 950;
            case "go" -> 1500;
            default -> 2600;
        };
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        if (cue.isEmpty() || Minecraft.getInstance().options.hideGui) {
            return;
        }
        long t = System.currentTimeMillis() - start;
        long d = duration();
        if (t > d) {
            cue = "";
            return;
        }
        float alpha = Math.min(1f, t / 120f) * Math.min(1f, (d - t) / 300f);
        float pop = 1f + 0.45f * (float) Math.pow(Math.max(0f, 1f - t / 220f), 2);
        String title;
        String subtitle = "";
        int color;
        float size;
        switch (cue) {
            case "count" -> {
                title = String.valueOf(number);
                subtitle = tr("message.hogwartsmod.duel.salute");
                color = 0xFFF2C14E;
                size = 34f;
            }
            case "go" -> {
                title = tr("message.hogwartsmod.duel.go");
                color = 0xFFF4C542;
                size = 26f;
            }
            case "win" -> {
                title = tr("message.hogwartsmod.duel.title_win");
                subtitle = tr("message.hogwartsmod.duel.sub_win");
                color = 0xFFF4C542;
                size = 26f;
            }
            case "lose" -> {
                title = tr("message.hogwartsmod.duel.title_lose");
                subtitle = tr("message.hogwartsmod.duel.sub_lose");
                color = 0xFFC9C0B2;
                size = 26f;
            }
            default -> {
                title = tr("message.hogwartsmod.duel.title_draw");
                subtitle = tr("message.hogwartsmod.duel.sub_draw");
                color = 0xFFEADBB8;
                size = 26f;
            }
        }
        float cx = width / 2f;
        float cy = height * 0.32f;
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().scale(pop, pop, 1f);
        float h = HiRes.lineHeight(HiRes.Style.BOLD, size);
        float w = HiRes.width(title, HiRes.Style.BOLD, size);
        for (int[] o : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}, {1, 1}, {2, 2}}) {
            HiRes.text(g, title, HiRes.Style.BOLD, size, -w / 2f + o[0], -h / 2f + o[1], OUTLINE);
        }
        HiRes.text(g, title, HiRes.Style.BOLD, size, -w / 2f, -h / 2f, color);
        g.pose().popPose();
        float lineY = cy + h / 2f + 4;
        float half = Math.max(w / 2f + 26, 70);
        int gold = (color & 0x00FFFFFF) | 0xFF000000;
        ornament(g, cx, lineY, half, gold);
        if (!subtitle.isEmpty()) {
            float sw = HiRes.width(subtitle, HiRes.Style.ITALIC, 9f);
            HiRes.text(g, subtitle, HiRes.Style.ITALIC, 9f, cx - sw / 2f + 1, lineY + 7, OUTLINE);
            HiRes.text(g, subtitle, HiRes.Style.ITALIC, 9f, cx - sw / 2f, lineY + 6, 0xFFF3E6C8);
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    private static void ornament(GuiGraphics g, float cx, float y, float half, int color) {
        int x0 = Math.round(cx - half);
        int x1 = Math.round(cx + half);
        int yy = Math.round(y);
        int c = Math.round(cx);
        int fadeColor = (color & 0x00FFFFFF) | 0x66000000;
        g.fill(x0, yy, c - 5, yy + 1, fadeColor);
        g.fill(c + 5, yy, x1, yy + 1, fadeColor);
        g.fill(x0 + 10, yy, c - 5, yy + 1, color);
        g.fill(c + 5, yy, x1 - 10, yy + 1, color);
        g.fill(c - 1, yy - 2, c + 1, yy + 3, color);
        g.fill(c - 2, yy - 1, c + 2, yy + 2, color);
        g.fill(c - 3, yy, c + 3, yy + 1, color);
        g.fill(x0 - 2, yy - 1, x0, yy + 2, color);
        g.fill(x1, yy - 1, x1 + 2, yy + 2, color);
    }

    private static String tr(String key) {
        return Component.translatable(key).getString();
    }
}
