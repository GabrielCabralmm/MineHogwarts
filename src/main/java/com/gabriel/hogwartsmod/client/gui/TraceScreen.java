package com.gabriel.hogwartsmod.client.gui;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.client.trace.TracePatterns;
import com.gabriel.hogwartsmod.network.ModNetwork;
import com.gabriel.hogwartsmod.network.TraceResultPacket;
import com.gabriel.hogwartsmod.spell.Spell;
import com.gabriel.hogwartsmod.teach.TraceResult;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class TraceScreen extends Screen {
    private static final int SHEET_W = 240;
    private static final int SHEET_H = 220;
    private static final int CANVAS = 140;
    private static final float MIN_ACCURACY = 1.0f;
    private static final float SHADOW_DELAY = 1.9f;
    private static final int ORB = 16;

    private static final int INK = 0xFF38261A;
    private static final int TITLE = 0xFF6E281E;
    private static final int MUTED = 0xFF82684C;
    private static final int GOLD = 0xFFBE8C28;
    private static final int GREEN = 0xFF3F6B2A;
    private static final int WINE = 0xFF8E2A2A;
    private static final int PAGE_LINE = 0xFFC4AA7C;

    private static final ResourceLocation LIGHT_ORB =
            ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "textures/gui/trace_light.png");
    private static final ResourceLocation SHADOW_ORB =
            ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "textures/gui/trace_shadow.png");
    private static final ResourceLocation SHEET =
            ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "textures/gui/trace_sheet.png");

    private enum State { READY, DRAWING, MISSED, FAILED, LEARNED }

    private static final float MARKER_WINDOW = 10f;
    private static final float[] MARKER_AT = {0f, 0.36f, 0.7f};
    private static final float LIGHT_SPEED = 16f;
    private static final float BOOST_SPEED = 5.5f;
    private static final int[] KEYS = {
            org.lwjgl.glfw.GLFW.GLFW_KEY_W, org.lwjgl.glfw.GLFW.GLFW_KEY_D,
            org.lwjgl.glfw.GLFW.GLFW_KEY_S, org.lwjgl.glfw.GLFW.GLFW_KEY_A};

    private final Spell spell;
    private final String teacher;
    private final int maxAttempts;
    private final float[][] pattern;
    private final float[] cum;
    private final float total;
    private final float shadowSpeed;
    private long drawStart;
    private long lastFrame;
    private long lastMove;
    private float pos;
    private float chaser;
    private float boost;
    private float flash;
    private final float[] markers;
    private final int[] markerDir;
    private final int[] markerState;
    private final List<float[]> sparks = new ArrayList<>();
    private static final java.util.Random RANDOM = new java.util.Random();
    private final HiRes.Canvas canvas = new HiRes.Canvas();
    private boolean cursorHidden;

    private int attemptsLeft;
    private State state = State.READY;
    private int stateTicks;
    private int ticks;
    private String feedback = "";
    private int feedbackColor = MUTED;
    private boolean dirty = true;
    private boolean finished;
    private int left;
    private int top;

    public TraceScreen(Spell spell, String teacher, int attempts) {
        super(Component.translatable("screen.hogwartsmod.trace"));
        this.spell = spell;
        this.teacher = teacher;
        this.maxAttempts = Math.max(1, attempts);
        this.attemptsLeft = this.maxAttempts;
        this.pattern = TracePatterns.get(spell.getId());
        this.cum = new float[pattern.length];
        for (int i = 1; i < pattern.length; i++) {
            cum[i] = cum[i - 1] + (float) Math.hypot(pattern[i][0] - pattern[i - 1][0], pattern[i][1] - pattern[i - 1][1]) * CANVAS;
        }
        this.total = cum[pattern.length - 1];
        this.shadowSpeed = 62f + 5f * spell.getRequiredLevel();
        int count = MARKER_AT.length;
        this.markers = new float[count];
        for (int i = 0; i < count; i++) {
            markers[i] = 24f + MARKER_AT[i] * (total - 24f);
        }
        this.markerDir = new int[count];
        this.markerState = new int[count];
        shuffleArrows();
    }

    @Override
    protected void init() {
        left = (width - SHEET_W) / 2;
        top = (height - SHEET_H) / 2;
        dirty = true;
        if (state == State.READY) {
            feedback = tr("screen.hogwartsmod.trace.ready");
            feedbackColor = MUTED;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int canvasX() {
        return left + (SHEET_W - CANVAS) / 2;
    }

    private int canvasY() {
        return top + 44;
    }

    private float guideX(int i) {
        return pattern[i][0] * CANVAS;
    }

    private float guideY(int i) {
        return pattern[i][1] * CANVAS;
    }

    // -- Desenho

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        HiRes.image(g, SHEET, left, top, SHEET_W, SHEET_H);
        float cx = left + SHEET_W / 2f;
        HiRes.centered(g, Component.translatable("screen.hogwartsmod.trace.title", spell.getDisplayName()).getString(),
                HiRes.Style.BOLD, 11f, cx, top + 15, TITLE);
        HiRes.centered(g, Component.translatable("screen.hogwartsmod.trace.teacher", teacher).getString(),
                HiRes.Style.ITALIC, cx, top + 30, MUTED);
        step();
        if (dirty) {
            redraw();
            dirty = false;
        }
        canvas.draw(g, canvasX(), canvasY(), CANVAS, CANVAS);
        drawOrbs(g);
        if (state == State.READY && (ticks / 10) % 2 == 0) {
            float sx = canvasX() + guideX(0);
            float sy = canvasY() + guideY(0);
            g.fill((int) sx - 1, (int) sy - 9, (int) sx + 1, (int) sy - 6, 0xFF1E7BFF);
        }
        HiRes.centered(g, feedback, state == State.LEARNED ? HiRes.Style.BOLD : HiRes.Style.ITALIC, cx, top + 190, feedbackColor);
        if (state == State.DRAWING) {
            HiRes.centered(g, tr("screen.hogwartsmod.trace.boost_hint"), HiRes.Style.ITALIC, 7f, cx, top + 180, MUTED);
        }
        drawAttempts(g, cx, top + 204);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawAttempts(GuiGraphics g, float cx, int y) {
        String label = tr("screen.hogwartsmod.trace.attempts");
        float labelW = HiRes.width(label, HiRes.Style.REGULAR);
        float total = labelW + 4 + maxAttempts * 8;
        float x = cx - total / 2f;
        HiRes.text(g, label, HiRes.Style.REGULAR, x, y - 1.5f, MUTED);
        for (int i = 0; i < maxAttempts; i++) {
            diamond(g, Math.round(x + labelW + 4 + i * 8), y, i < attemptsLeft ? GOLD : PAGE_LINE);
        }
    }

    private void diamond(GuiGraphics g, int x, int y, int color) {
        g.fill(x + 2, y, x + 4, y + 1, color);
        g.fill(x + 1, y + 1, x + 5, y + 2, color);
        g.fill(x, y + 2, x + 6, y + 4, color);
        g.fill(x + 1, y + 4, x + 5, y + 5, color);
        g.fill(x + 2, y + 5, x + 4, y + 6, color);
    }

    private float[] at(float d) {
        d = Math.max(0, Math.min(total, d));
        int i = 1;
        while (i < pattern.length - 1 && cum[i] < d) {
            i++;
        }
        float seg = cum[i] - cum[i - 1];
        float f = seg <= 0 ? 0 : (d - cum[i - 1]) / seg;
        return new float[]{guideX(i - 1) + (guideX(i) - guideX(i - 1)) * f, guideY(i - 1) + (guideY(i) - guideY(i - 1)) * f};
    }

    private float[] tangent(float d) {
        float[] a = at(d - 2f);
        float[] b = at(d + 2f);
        float len = (float) Math.hypot(b[0] - a[0], b[1] - a[1]);
        return len == 0 ? new float[]{1, 0} : new float[]{(b[0] - a[0]) / len, (b[1] - a[1]) / len};
    }

    private List<float[]> span(float from, float to) {
        List<float[]> out = new ArrayList<>();
        for (float d = from; d < to; d += 1.5f) {
            out.add(at(d));
        }
        out.add(at(to));
        return out;
    }

    private Path2D polyline(List<float[]> pts, float s) {
        Path2D path = new Path2D.Float();
        path.moveTo(pts.get(0)[0] * s, pts.get(0)[1] * s);
        for (int i = 1; i < pts.size(); i++) {
            path.lineTo(pts.get(i)[0] * s, pts.get(i)[1] * s);
        }
        return path;
    }

    private void redraw() {
        BufferedImage[] holder = new BufferedImage[1];
        Graphics2D g = canvas.begin(holder, CANVAS, CANVAS);
        float s = (float) canvas.scale();
        int n = pattern.length;
        boolean learned = state == State.LEARNED;
        Color spellColor = new Color(spell.getColor());

        Path2D guide = path(0, n - 1, s);
        g.setColor(new Color(0x82, 0x68, 0x4C, 110));
        g.setStroke(new BasicStroke(2.2f * s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{4f * s, 3.5f * s}, 0f));
        g.draw(guide);

        if (pos > 1f || learned) {
            drawLight(g, s, spellColor, span(0, learned ? total : pos));
        }

        if (state == State.DRAWING && chaser > 0) {
            Path2D dark = polyline(span(0, Math.min(pos, chaser)), s);
            g.setStroke(new BasicStroke(8f * s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(0xB0, 0x14, 0x1E, 60));
            g.draw(dark);
            g.setStroke(new BasicStroke(3.2f * s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(0x40, 0x0A, 0x10, 220));
            g.draw(dark);
        }

        for (int m = 0; m < markers.length; m++) {
            float[] p = at(markers[m]);
            boolean near = state == State.DRAWING && markerState[m] == 0 && Math.abs(pos - markers[m]) <= MARKER_WINDOW;
            float[] t = tangent(markers[m]);
            float nx = -t[1], ny = t[0];
            if (nx * (p[0] - CANVAS / 2f) + ny * (p[1] - CANVAS / 2f) < 0) {
                nx = -nx;
                ny = -ny;
            }
            float x = (p[0] + nx * 11f) * s, y = (p[1] + ny * 11f) * s, r = (near ? 6.2f : 5.2f) * s;
            g.setStroke(new BasicStroke(1.2f * s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(0xBE, 0x8C, 0x28, 200));
            g.draw(new java.awt.geom.Line2D.Float(p[0] * s, p[1] * s, x, y));
            dot(g, p[0] * s, p[1] * s, 2.2f * s, new Color(0xBE, 0x8C, 0x28, 230), true);
            if (near) {
                dot(g, x, y, r + 3.6f * s, withAlpha(spellColor, 60), true);
                dot(g, x, y, r + 1.8f * s, withAlpha(spellColor, 110), true);
            }
            dot(g, x + 0.6f * s, y + 0.9f * s, r, new Color(0x38, 0x26, 0x1A, 70), true);
            Color rim = markerState[m] == 2 ? new Color(0x8E, 0x2A, 0x2A) : new Color(0xBE, 0x8C, 0x28);
            Color face = markerState[m] == 1 ? new Color(0xC8, 0x8E, 0x22)
                    : markerState[m] == 2 ? new Color(0xD8, 0xC2, 0xA0) : new Color(0xF6, 0xEA, 0xCC);
            Color ink = markerState[m] == 2 ? new Color(0x8E, 0x2A, 0x2A, 170)
                    : markerState[m] == 1 ? new Color(0xFF, 0xF6, 0xDC) : new Color(0x38, 0x26, 0x1A);
            dot(g, x, y, r, rim, true);
            dot(g, x, y, r * 0.8f, face, true);
            letter(g, x, y, r * 1.15f, "WDSA".charAt(markerDir[m]), ink);
        }

        dot(g, guideX(n - 1) * s, guideY(n - 1) * s, 3.2f * s, new Color(0x82, 0x68, 0x4C, 170), false);
        if (state == State.READY) {
            dot(g, guideX(0) * s, guideY(0) * s, 9f * s, new Color(0x3A, 0xA8, 0xFF, 80), true);
            dot(g, guideX(0) * s, guideY(0) * s, 4.5f * s, new Color(0x1E, 0x7B, 0xFF, 255), true);
            dot(g, guideX(0) * s, guideY(0) * s, 1.8f * s, new Color(0xE6, 0xF8, 0xFF, 255), true);
        }
        g.dispose();
        canvas.end(holder[0]);
    }

    private void drawLight(Graphics2D g, float s, Color spellColor, List<float[]> smooth) {
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        boolean bad = state == State.MISSED || state == State.FAILED;
        Color glow = bad ? new Color(0x8E2A2A) : spellColor;
        Color core = bad ? new Color(0xF2C8C0) : BLUE_CORE;
        Color deep = bad ? glow : BLUE;
        int count = smooth.size();
        if (count < 2) {
            return;
        }
        int chunks = Math.min(16, count - 1);
        for (int layer = 0; layer < 4; layer++) {
            for (int c = 0; c < chunks; c++) {
                int from = c * (count - 1) / chunks;
                int to = (c + 1) * (count - 1) / chunks;
                float age = 1f - (c + 1f) / chunks;
                float fade = 0.45f + 0.55f * (1f - age) * (1f - age);
                float taper = 0.55f + 0.45f * (1f - age);
                Path2D part = new Path2D.Float();
                part.moveTo(smooth.get(from)[0] * s, smooth.get(from)[1] * s);
                for (int i = from + 1; i <= to; i++) {
                    part.lineTo(smooth.get(i)[0] * s, smooth.get(i)[1] * s);
                }
                float width;
                Color color;
                switch (layer) {
                    case 0 -> {
                        width = 5.5f;
                        color = new Color(0x2A, 0x1A, 0x10, (int) (28 * fade));
                    }
                    case 1 -> {
                        width = 7.5f * taper;
                        color = withAlpha(bad ? glow : AURA, (int) (95 * fade));
                    }
                    case 2 -> {
                        width = 3.2f * taper;
                        color = withAlpha(deep, (int) (230 * fade));
                    }
                    default -> {
                        width = 1.3f * taper;
                        color = withAlpha(core, (int) (255 * fade));
                    }
                }
                g.setStroke(new BasicStroke(width * s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(color);
                g.draw(part);
            }
        }
    }

    private static final Color AURA = new Color(0x3AA8FF);
    private static final Color BLUE = new Color(0x1E7BFF);
    private static final Color BLUE_CORE = new Color(0x9EE6FF);


    private static Color mix(Color a, Color b, float k) {
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * k), (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * k),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * k), (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * k));
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
    }


    private Path2D path(int from, int to, float s) {
        Path2D path = new Path2D.Float();
        path.moveTo(guideX(from) * s, guideY(from) * s);
        for (int i = from + 1; i <= to; i++) {
            path.lineTo(guideX(i) * s, guideY(i) * s);
        }
        return path;
    }

    private static void letter(Graphics2D g, float x, float y, float size, char c, Color color) {
        g.setComposite(AlphaComposite.SrcOver);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new java.awt.Font(java.awt.Font.SERIF, java.awt.Font.BOLD, 1).deriveFont(size));
        java.awt.font.GlyphVector gv = g.getFont().createGlyphVector(g.getFontRenderContext(), String.valueOf(c));
        java.awt.geom.Rectangle2D b = gv.getVisualBounds();
        g.setColor(color);
        g.drawGlyphVector(gv, (float) (x - b.getCenterX()), (float) (y - b.getCenterY()));
    }

    private void shuffleArrows() {
        for (int i = 0; i < markerDir.length; i++) {
            markerDir[i] = RANDOM.nextInt(4);
            markerState[i] = 0;
        }
    }

    private static void dot(Graphics2D g, float x, float y, float r, Color color, boolean fill) {
        g.setComposite(AlphaComposite.SrcOver);
        g.setColor(color);
        Ellipse2D circle = new Ellipse2D.Float(x - r, y - r, r * 2, r * 2);
        if (fill) {
            g.fill(circle);
        } else {
            g.setStroke(new BasicStroke(r * 0.5f));
            g.draw(circle);
        }
    }



    private void drawOrbs(GuiGraphics g) {
        float time = (System.nanoTime() % 4_000_000_000L) / 1_000_000_000f;
        if (state != State.DRAWING) {
            return;
        }
        float[] pen = at(pos);
        if (sparks.size() < 60 && RANDOM.nextFloat() < (boost > 0 ? 1f : 0.6f)) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            float speed = 6f + RANDOM.nextFloat() * (boost > 0 ? 26f : 14f);
            sparks.add(new float[]{pen[0], pen[1], (float) Math.cos(angle) * speed, (float) Math.sin(angle) * speed, 0f,
                    0.35f + RANDOM.nextFloat() * 0.3f});
        }
        drawSparks(g);
        float pulse = 1f + 0.08f * (float) Math.sin(time * 12) + (boost > 0 ? 0.35f : 0f) + flash * 0.6f;
        orb(g, LIGHT_ORB, canvasX() + pen[0], canvasY() + pen[1], ORB * pulse, 1f);
        float appear = Math.min(1f, elapsed() / SHADOW_DELAY);
        if (appear > 0) {
            float[] c = at(Math.max(0, chaser));
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 0.25f, 0.25f, appear);
            orbTinted(g, LIGHT_ORB, canvasX() + c[0], canvasY() + c[1], ORB * 0.9f * (1f + 0.12f * (float) Math.sin(time * 9)));
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }
    }

    private void drawSparks(GuiGraphics g) {
        long now = System.nanoTime();
        float dt = lastFrame == 0 ? 0 : Math.min(0.1f, (now - lastFrame) / 1_000_000_000f);
        lastFrame = now;
        sparks.removeIf(p -> p[4] >= p[5]);
        for (float[] p : sparks) {
            p[4] += dt;
            p[0] += p[2] * dt;
            p[1] += p[3] * dt;
            p[2] *= 0.92f;
            p[3] *= 0.92f;
            float life = 1f - p[4] / p[5];
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, life);
            orbTinted(g, LIGHT_ORB, canvasX() + p[0], canvasY() + p[1], 2.5f + 3f * life);
        }
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private void orbTinted(GuiGraphics g, ResourceLocation texture, float x, float y, float size) {
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(size / 64f, size / 64f, 1f);
        g.blit(texture, -32, -32, 0, 0, 64, 64, 64, 64);
        g.pose().popPose();
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    private void orb(GuiGraphics g, ResourceLocation texture, float x, float y, float size, float alpha) {
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(size / 64f, size / 64f, 1f);
        g.blit(texture, -32, -32, 0, 0, 64, 64, 64, 64);
        g.pose().popPose();
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    // -- Lógica do traçado

    private float elapsed() {
        return (System.nanoTime() - drawStart) / 1_000_000_000f;
    }

    private void step() {
        long now = System.nanoTime();
        float dt = lastMove == 0 ? 0 : Math.min(0.05f, (now - lastMove) / 1_000_000_000f);
        lastMove = now;
        flash = Math.max(0, flash - dt * 3f);
        if (state != State.DRAWING) {
            return;
        }
        boost = Math.max(0, boost - dt);
        float speed = LIGHT_SPEED * (boost > 0 ? BOOST_SPEED : 1f);
        float move = speed * dt;
        float before = pos;
        pos = Math.min(total, pos + move);
        for (int m = 0; m < markers.length; m++) {
            if (markerState[m] == 0 && boost > 0 && before < markers[m] - MARKER_WINDOW * 0.5f && pos >= markers[m] - MARKER_WINDOW * 0.5f) {
                boost = 0;
            }
            if (markerState[m] == 0 && pos > markers[m] + MARKER_WINDOW) {
                markerState[m] = 2;
            }
        }
        if (elapsed() > SHADOW_DELAY) {
            chaser += shadowSpeed * dt;
        }
        dirty = true;
        if (pos >= total - 0.5f) {
            learned();
        } else if (elapsed() > SHADOW_DELAY && chaser >= pos) {
            miss(tr("screen.hogwartsmod.trace.caught"));
        }
    }

    @Override
    public void tick() {
        ticks++;
        if (state == State.READY && ++stateTicks >= 25) {
            begin();
        }
        if (state == State.MISSED || state == State.FAILED || state == State.LEARNED) {
            stateTicks++;
            if (state == State.MISSED && stateTicks >= 30) {
                reset();
            } else if (state == State.FAILED && stateTicks >= 50) {
                finish(TraceResult.FAILED);
            } else if (state == State.LEARNED && stateTicks >= 45) {
                finished = true;
                onClose();
            }
        }
    }

    private void reset() {
        state = State.READY;
        pos = 0;
        chaser = 0;
        boost = 0;
        stateTicks = 0;
        shuffleArrows();
        feedback = tr("screen.hogwartsmod.trace.ready");
        feedbackColor = MUTED;
        dirty = true;
    }

    private void begin() {
        state = State.DRAWING;
        drawStart = System.nanoTime();
        lastMove = 0;
        sparks.clear();
        pos = 0;
        chaser = -shadowSpeed * 0.15f;
        hideCursor(true);
        feedback = tr("screen.hogwartsmod.trace.guide");
        feedbackColor = INK;
        dirty = true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (state == State.DRAWING) {
            for (int d = 0; d < 4; d++) {
                if (key == KEYS[d]) {
                    tryArrow(d);
                    return true;
                }
            }
        }
        return super.keyPressed(key, scan, modifiers);
    }

    private void tryArrow(int dir) {
        for (int m = 0; m < markers.length; m++) {
            if (markerState[m] == 0 && Math.abs(pos - markers[m]) <= MARKER_WINDOW) {
                dirty = true;
                if (markerDir[m] == dir) {
                    markerState[m] = 1;
                    boost = Float.MAX_VALUE;
                    flash = 1f;
                    chaser = Math.max(0, chaser - shadowSpeed * 0.2f);
                    playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.4f + Math.min(m, 6) * 0.1f);
                } else {
                    markerState[m] = 2;
                    chaser += shadowSpeed * 0.3f;
                    playSound(SoundEvents.NOTE_BLOCK_BASS.get(), 0.5f);
                }
                return;
            }
        }
        playSound(SoundEvents.NOTE_BLOCK_HAT.get(), 0.6f);
    }

    private void miss(String reason) {
        hideCursor(false);
        attemptsLeft--;
        stateTicks = 0;
        dirty = true;
        feedbackColor = WINE;
        if (attemptsLeft <= 0) {
            state = State.FAILED;
            feedback = tr("screen.hogwartsmod.trace.failed");
            playSound(SoundEvents.NOTE_BLOCK_BASS.get(), 0.6f);
        } else {
            state = State.MISSED;
            feedback = reason;
            playSound(SoundEvents.NOTE_BLOCK_BASS.get(), 0.9f);
        }
    }

    private void learned() {
        hideCursor(false);
        state = State.LEARNED;
        stateTicks = 0;
        dirty = true;
        feedback = Component.translatable("screen.hogwartsmod.trace.learned", spell.getDisplayName()).getString();
        feedbackColor = GREEN;
        playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f);
        send(TraceResult.SUCCESS);
    }

    private void playSound(SoundEvent sound, float pitch) {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch));
        }
    }

    private void send(TraceResult result) {
        if (!finished) {
            finished = true;
            ModNetwork.CHANNEL.sendToServer(new TraceResultPacket(spell.getId(), result));
        }
    }

    private void finish(TraceResult result) {
        send(result);
        onClose();
    }

    private void hideCursor(boolean hide) {
        if (minecraft == null || hide == cursorHidden) {
            return;
        }
        cursorHidden = hide;
        long window = minecraft.getWindow().getWindow();
        double mx = minecraft.mouseHandler.xpos();
        double my = minecraft.mouseHandler.ypos();
        org.lwjgl.glfw.GLFW.glfwSetInputMode(window, org.lwjgl.glfw.GLFW.GLFW_CURSOR,
                hide ? org.lwjgl.glfw.GLFW.GLFW_CURSOR_HIDDEN : org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL);
        org.lwjgl.glfw.GLFW.glfwSetCursorPos(window, mx, my);
    }

    @Override
    public void onClose() {
        hideCursor(false);
        send(TraceResult.GAVE_UP);
        canvas.close();
        super.onClose();
    }

    @Override
    public void removed() {
        hideCursor(false);
        send(TraceResult.GAVE_UP);
        canvas.close();
    }

    private static String tr(String key) {
        return Component.translatable(key).getString();
    }
}
