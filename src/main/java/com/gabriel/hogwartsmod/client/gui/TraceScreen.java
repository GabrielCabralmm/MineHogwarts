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
    private static final float SHADOW_DELAY = 1.0f;
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

    private final Spell spell;
    private final String teacher;
    private final int maxAttempts;
    private final float[][] pattern;
    private final float tolerance;
    private final float shadowSpeed;
    private final float spacing;
    private long drawStart;
    private long lastFrame;
    private float penX;
    private float penY;
    private final List<float[]> sparks = new ArrayList<>();
    private static final java.util.Random RANDOM = new java.util.Random();
    private int drawnShadow = -1;
    private final HiRes.Canvas canvas = new HiRes.Canvas();
    private final List<float[]> stroke = new ArrayList<>();

    private int attemptsLeft;
    private State state = State.READY;
    private int stateTicks;
    private int ticks;
    private int progress;
    private int offPoints;
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
        this.tolerance = 4f - Math.min(1.5f, spell.getRequiredLevel() * 0.15f);
        this.shadowSpeed = 55f + 4f * spell.getRequiredLevel();
        float length = 0;
        for (int i = 1; i < pattern.length; i++) {
            length += (float) Math.hypot(pattern[i][0] - pattern[i - 1][0], pattern[i][1] - pattern[i - 1][1]) * CANVAS;
        }
        this.spacing = length / (pattern.length - 1);
    }

    @Override
    protected void init() {
        left = (width - SHEET_W) / 2;
        top = (height - SHEET_H) / 2;
        dirty = true;
        if (state == State.READY) {
            feedback = tr("screen.hogwartsmod.trace.hint");
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
        updateShadow();
        if (dirty) {
            redraw();
            dirty = false;
        }
        canvas.draw(g, canvasX(), canvasY(), CANVAS, CANVAS);
        drawOrbs(g);
        if (state == State.READY && (ticks / 10) % 2 == 0) {
            float sx = canvasX() + guideX(0);
            float sy = canvasY() + guideY(0);
            g.fill((int) sx - 1, (int) sy - 9, (int) sx + 1, (int) sy - 6, GOLD);
        }
        HiRes.centered(g, feedback, state == State.LEARNED ? HiRes.Style.BOLD : HiRes.Style.ITALIC, cx, top + 190, feedbackColor);
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
        for (float f : new float[]{0.22f, 0.5f, 0.78f}) {
            arrow(g, (int) (f * (n - 1)), s);
        }

        if (progress > 0 || learned) {
            Path2D done = path(0, learned ? n - 1 : progress, s);
            g.setStroke(new BasicStroke(7f * s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(0x3A, 0xA8, 0xFF, 80));
            g.draw(done);
            g.setStroke(new BasicStroke(3f * s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(0x1E, 0x7B, 0xFF, 230));
            g.draw(done);
        }

        if (stroke.size() > 1 && !learned) {
            drawLight(g, s, spellColor);
        }

        int shadow = (int) shadowIndex();
        drawnShadow = shadow;
        if (state == State.DRAWING && shadow > 0) {
            Path2D dark = path(0, Math.min(n - 1, shadow), s);
            g.setStroke(new BasicStroke(8f * s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(0x2A, 0x14, 0x30, 70));
            g.draw(dark);
            g.setStroke(new BasicStroke(3.2f * s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(0x16, 0x0C, 0x1A, 220));
            g.draw(dark);
        }

        dot(g, guideX(n - 1) * s, guideY(n - 1) * s, 3.2f * s, new Color(0x82, 0x68, 0x4C, 170), false);
        dot(g, guideX(0) * s, guideY(0) * s, 9f * s, new Color(0xF0, 0xC8, 0x5A, 70), true);
        dot(g, guideX(0) * s, guideY(0) * s, 4.5f * s, new Color(0xBE, 0x8C, 0x28, 255), true);
        dot(g, guideX(0) * s, guideY(0) * s, 1.8f * s, new Color(0xFF, 0xF2, 0xC8, 255), true);
        g.dispose();
        canvas.end(holder[0]);
    }

    private void drawLight(Graphics2D g, float s, Color spellColor) {
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        List<float[]> smooth = smooth(stroke);
        boolean bad = state == State.MISSED || state == State.FAILED;
        Color glow = bad ? new Color(0x8E2A2A) : spellColor;
        Color core = bad ? new Color(0xF2C8C0) : BLUE_CORE;
        Color deep = bad ? glow : BLUE;
        int count = smooth.size();
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

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    private static List<float[]> smooth(List<float[]> points) {
        List<float[]> out = new ArrayList<>(points);
        for (int pass = 0; pass < 2 && out.size() > 2; pass++) {
            List<float[]> next = new ArrayList<>(out.size() * 2);
            next.add(out.get(0));
            for (int i = 0; i < out.size() - 1; i++) {
                float[] a = out.get(i);
                float[] b = out.get(i + 1);
                next.add(new float[]{a[0] * 0.75f + b[0] * 0.25f, a[1] * 0.75f + b[1] * 0.25f});
                next.add(new float[]{a[0] * 0.25f + b[0] * 0.75f, a[1] * 0.25f + b[1] * 0.75f});
            }
            next.add(out.get(out.size() - 1));
            out = next;
        }
        return out;
    }

    private Path2D path(int from, int to, float s) {
        Path2D path = new Path2D.Float();
        path.moveTo(guideX(from) * s, guideY(from) * s);
        for (int i = from + 1; i <= to; i++) {
            path.lineTo(guideX(i) * s, guideY(i) * s);
        }
        return path;
    }

    private void arrow(Graphics2D g, int i, float s) {
        int j = Math.min(pattern.length - 1, i + 2);
        double angle = Math.atan2(guideY(j) - guideY(i), guideX(j) - guideX(i));
        double x = guideX(i) * s;
        double y = guideY(i) * s;
        double len = 5 * s;
        Path2D head = new Path2D.Double();
        head.moveTo(x + Math.cos(angle + 2.5) * len, y + Math.sin(angle + 2.5) * len);
        head.lineTo(x, y);
        head.lineTo(x + Math.cos(angle - 2.5) * len, y + Math.sin(angle - 2.5) * len);
        g.setStroke(new BasicStroke(1.6f * s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(0x6E, 0x28, 0x1E, 150));
        g.draw(head);
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

    private float elapsed() {
        return (System.nanoTime() - drawStart) / 1_000_000_000f;
    }

    private float shadowIndex() {
        if (state != State.DRAWING) {
            return -1;
        }
        float t = elapsed() - SHADOW_DELAY;
        return t < 0 ? -1 : t * shadowSpeed / spacing;
    }

    private void updateShadow() {
        if (state != State.DRAWING) {
            return;
        }
        float shadow = shadowIndex();
        if (shadow >= 0 && shadow >= progress) {
            miss(tr("screen.hogwartsmod.trace.caught"));
            return;
        }
        if ((int) shadow != drawnShadow) {
            dirty = true;
        }
    }

    private void drawOrbs(GuiGraphics g) {
        float time = (System.nanoTime() % 4_000_000_000L) / 1_000_000_000f;
        if (state == State.DRAWING && !stroke.isEmpty()) {
            float[] pen = stroke.get(stroke.size() - 1);
            penX += (pen[0] - penX) * 0.6f;
            penY += (pen[1] - penY) * 0.6f;
            if (sparks.size() < 60 && RANDOM.nextFloat() < 0.8f) {
                double angle = RANDOM.nextDouble() * Math.PI * 2;
                float speed = 6f + RANDOM.nextFloat() * 14f;
                sparks.add(new float[]{penX, penY, (float) Math.cos(angle) * speed, (float) Math.sin(angle) * speed, 0f,
                        0.35f + RANDOM.nextFloat() * 0.3f});
            }
            drawSparks(g);
            orb(g, LIGHT_ORB, canvasX() + penX, canvasY() + penY, ORB * (1f + 0.08f * (float) Math.sin(time * 12)), 1f);
            float shadow = shadowIndex();
            float appear = Math.min(1f, elapsed() / SHADOW_DELAY);
            float fi = Math.max(0, Math.min(pattern.length - 1, shadow));
            int i = (int) fi;
            int j = Math.min(pattern.length - 1, i + 1);
            float f = fi - i;
            float x = guideX(i) + (guideX(j) - guideX(i)) * f;
            float y = guideY(i) + (guideY(j) - guideY(i)) * f;
            orb(g, SHADOW_ORB, canvasX() + x, canvasY() + y, ORB * 1.15f * (0.6f + 0.4f * appear) * (1f + 0.1f * (float) Math.sin(time * 9)), appear);
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

    @Override
    public void tick() {
        ticks++;
        updateShadow();
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
        stroke.clear();
        progress = 0;
        offPoints = 0;
        feedback = tr("screen.hogwartsmod.trace.hint");
        feedbackColor = MUTED;
        dirty = true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && state == State.READY) {
            float x = (float) (mx - canvasX());
            float y = (float) (my - canvasY());
            if (Math.hypot(x - guideX(0), y - guideY(0)) <= tolerance * 2f) {
                state = State.DRAWING;
                drawStart = System.nanoTime();
                penX = x;
                penY = y;
                sparks.clear();
                drawnShadow = -1;
                stroke.clear();
                stroke.add(new float[]{x, y});
                progress = 0;
                offPoints = 0;
                feedback = accuracyText();
                feedbackColor = INK;
                dirty = true;
            } else {
                feedback = tr("screen.hogwartsmod.trace.start_here");
                feedbackColor = WINE;
            }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button != 0 || state != State.DRAWING) {
            return super.mouseDragged(mx, my, button, dx, dy);
        }
        float x = (float) (mx - canvasX());
        float y = (float) (my - canvasY());
        float[] last = stroke.get(stroke.size() - 1);
        double step = Math.hypot(x - last[0], y - last[1]);
        if (step < 1.0) {
            return true;
        }
        int pieces = (int) Math.ceil(step / 2.0);
        for (int p = 1; p <= pieces; p++) {
            float t = p / (float) pieces;
            if (!addPoint(last[0] + (x - last[0]) * t, last[1] + (y - last[1]) * t)) {
                break;
            }
        }
        dirty = true;
        return true;
    }

    private boolean addPoint(float x, float y) {
        updateShadow();
        if (state != State.DRAWING) {
            return false;
        }
        stroke.add(new float[]{x, y});
        float nearest = Float.MAX_VALUE;
        for (int i = 0; i < pattern.length; i++) {
            nearest = Math.min(nearest, (float) Math.hypot(x - guideX(i), y - guideY(i)));
        }
        if (nearest > tolerance * 1.3f) {
            offPoints++;
        }
        if (accuracy() < MIN_ACCURACY) {
            miss(tr("screen.hogwartsmod.trace.left_path"));
            return false;
        }
        int limit = Math.min(pattern.length - 1, progress + 8);
        for (int j = progress; j <= limit; j++) {
            if (Math.hypot(x - guideX(j), y - guideY(j)) <= tolerance) {
                progress = j;
            }
        }
        feedback = accuracyText();
        if (progress >= pattern.length - 2) {
            if (accuracy() >= MIN_ACCURACY) {
                learned();
            } else {
                miss(tr("screen.hogwartsmod.trace.shaky"));
            }
            return false;
        }
        return true;
    }

    private float accuracy() {
        return stroke.isEmpty() ? 1f : 1f - offPoints / (float) stroke.size();
    }

    private String accuracyText() {
        return Component.translatable("screen.hogwartsmod.trace.accuracy", Math.round(accuracy() * 100)).getString();
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0 && state == State.DRAWING) {
            miss(tr("screen.hogwartsmod.trace.incomplete"));
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    private void miss(String reason) {
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

    @Override
    public void onClose() {
        send(TraceResult.GAVE_UP);
        canvas.close();
        super.onClose();
    }

    @Override
    public void removed() {
        send(TraceResult.GAVE_UP);
        canvas.close();
    }

    private static String tr(String key) {
        return Component.translatable(key).getString();
    }
}
