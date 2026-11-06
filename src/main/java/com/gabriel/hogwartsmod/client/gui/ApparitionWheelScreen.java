package com.gabriel.hogwartsmod.client.gui;

import com.gabriel.hogwartsmod.apparition.ApparitionPoint;
import com.gabriel.hogwartsmod.client.ModKeys;
import com.gabriel.hogwartsmod.client.apparition.ClientApparitionState;
import com.gabriel.hogwartsmod.magic.MagicData;
import com.gabriel.hogwartsmod.network.ApparatePacket;
import com.gabriel.hogwartsmod.network.ModNetwork;
import com.gabriel.hogwartsmod.spell.impl.AparatacaoSpell;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class ApparitionWheelScreen extends Screen {
    private int OUTER = 92;
    private int INNER = 34;
    private static final int PER_PAGE = 6;

    private static final int INK = 0xFF38261A;
    private static final int TITLE = 0xFF6E281E;
    private static final int MUTED = 0xFF82684C;
    private static final int PAPER = 0xFFECDCB6;

    private record Entry(ApparitionPoint point, boolean personal, int index, boolean available) {
    }

    private final HiRes.Canvas canvas = new HiRes.Canvas();
    private final List<List<Entry>> pages = new ArrayList<>();
    private int page;
    private int hovered = -1;
    private int drawnHover = -2;
    private int drawnPage = -1;
    private boolean keySeen;

    public ApparitionWheelScreen() {
        super(Component.translatable("screen.hogwartsmod.wheel"));
    }

    @Override
    protected void init() {
        OUTER = Math.max(60, Math.min(92, (height - 64) / 2));
        INNER = Math.round(OUTER * 0.42f);
        pages.clear();
        String dimension = minecraft != null && minecraft.level != null ? minecraft.level.dimension().location().toString() : "";
        MagicData data = minecraft != null && minecraft.player != null ? MagicData.get(minecraft.player) : null;
        List<Entry> mine = new ArrayList<>();
        if (data != null) {
            List<ApparitionPoint> places = data.getPlaces();
            for (int i = 0; i < places.size(); i++) {
                mine.add(new Entry(places.get(i), true, i, places.get(i).dimension().equals(dimension)));
            }
        }
        pages.add(mine);
        List<ApparitionPoint> publics = ClientApparitionState.publicPoints;
        for (int i = 0; i < publics.size(); i += PER_PAGE) {
            List<Entry> chunk = new ArrayList<>();
            for (int j = i; j < Math.min(publics.size(), i + PER_PAGE); j++) {
                chunk.add(new Entry(publics.get(j), false, j, publics.get(j).dimension().equals(dimension)));
            }
            pages.add(chunk);
        }
        drawnPage = -1;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private List<Entry> entries() {
        return pages.get(Math.min(page, pages.size() - 1));
    }

    private double distanceTo(ApparitionPoint point) {
        return minecraft == null || minecraft.player == null ? 0 : minecraft.player.position().distanceTo(point.position());
    }

    // -- Desenho

    @Override
    public void renderBackground(GuiGraphics g) {
        g.fillGradient(0, 0, width, height, 0x90000000, 0xA0000000);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        int cx = width / 2;
        int cy = height / 2 - 6;
        hovered = sliceAt(mouseX - cx, mouseY - cy);
        if (hovered != drawnHover || page != drawnPage) {
            redraw();
            drawnHover = hovered;
            drawnPage = page;
        }
        int size = OUTER * 2 + 8;
        canvas.draw(g, cx - size / 2, cy - size / 2, size, size);

        List<Entry> entries = entries();
        int n = Math.max(1, entries.size());
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            double mid = Math.toRadians(-90 + (i + 0.5) * 360.0 / n);
            float r = (OUTER + INNER) / 2f + 2;
            float tx = cx + (float) Math.cos(mid) * r;
            float ty = cy + (float) Math.sin(mid) * r;
            int color = e.available ? INK : MUTED;
            String name = HiRes.ellipsize(e.point.name(), HiRes.Style.BOLD, (OUTER - INNER) * 0.95f);
            HiRes.centered(g, name, HiRes.Style.BOLD, 7.5f, tx, ty - 7, i == hovered ? TITLE : color);
            String detail = e.available
                    ? Component.translatable("screen.hogwartsmod.wheel.blocks", Math.round(distanceTo(e.point))).getString()
                    : tr("screen.hogwartsmod.wheel.other_dimension");
            HiRes.centered(g, detail, HiRes.Style.ITALIC, 6.5f, tx, ty + 2, MUTED);
        }

        Entry current = hovered >= 0 && hovered < entries.size() ? entries.get(hovered) : null;
        if (current != null) {
            double distance = distanceTo(current.point);
            HiRes.centered(g, HiRes.ellipsize(current.point.name(), HiRes.Style.BOLD, INNER * 2 - 8), HiRes.Style.BOLD, 8f, cx, cy - 17, TITLE);
            if (current.available) {
                HiRes.centered(g, Component.translatable("screen.hogwartsmod.wheel.blocks", Math.round(distance)).getString(),
                        HiRes.Style.REGULAR, 7f, cx, cy - 7, INK);
                HiRes.centered(g, Component.translatable("screen.hogwartsmod.wheel.cost", Math.round(AparatacaoSpell.costFor(distance))).getString(),
                        HiRes.Style.REGULAR, 7f, cx, cy + 1, INK);
                List<String> hint = HiRes.wrap(tr("screen.hogwartsmod.wheel.release"), HiRes.Style.ITALIC, INNER * 2 - 16);
                for (int i = 0; i < Math.min(2, hint.size()); i++) {
                    HiRes.centered(g, hint.get(i), HiRes.Style.ITALIC, 6f, cx, cy + 10 + i * 7, MUTED);
                }
            } else {
                HiRes.centered(g, tr("screen.hogwartsmod.wheel.other_dimension"), HiRes.Style.ITALIC, 7f, cx, cy + 2, MUTED);
            }
        } else if (entries.isEmpty()) {
            List<String> lines = HiRes.wrap(tr(page == 0 ? "screen.hogwartsmod.wheel.empty_mine" : "screen.hogwartsmod.wheel.empty"),
                    HiRes.Style.ITALIC, INNER * 2 - 12);
            for (int i = 0; i < lines.size(); i++) {
                HiRes.centered(g, lines.get(i), HiRes.Style.ITALIC, 6.5f, cx, cy - lines.size() * 4 + i * 8, MUTED);
            }
        } else {
            List<String> lines = HiRes.wrap(tr("screen.hogwartsmod.wheel.cancel"), HiRes.Style.ITALIC, INNER * 2 - 12);
            for (int i = 0; i < lines.size(); i++) {
                HiRes.centered(g, lines.get(i), HiRes.Style.ITALIC, 6.5f, cx, cy - lines.size() * 4 + i * 8, MUTED);
            }
        }

        String pageName = tr(page == 0 ? "screen.hogwartsmod.wheel.mine" : "screen.hogwartsmod.wheel.public");
        int by = cy + OUTER + 12;
        HiRes.centered(g, pageName, HiRes.Style.BOLD, 8f, cx, by, PAPER);
        int dots = pages.size();
        int dx = cx - (dots * 8 - 3) / 2;
        for (int i = 0; i < dots; i++) {
            int color = i == page ? 0xFFF0C85A : 0x90ECDCB6;
            g.fill(dx + i * 8, by + 12, dx + i * 8 + 5, by + 17, color);
        }
        if (dots > 1) {
            HiRes.centered(g, tr("screen.hogwartsmod.wheel.scroll"), HiRes.Style.ITALIC, 6.5f, cx, by + 21, 0xB0ECDCB6);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    private int sliceAt(double dx, double dy) {
        double dist = Math.sqrt(dx * dx + dy * dy);
        List<Entry> entries = entries();
        if (dist < INNER || dist > OUTER + 30 || entries.isEmpty()) {
            return -1;
        }
        double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90;
        if (angle < 0) {
            angle += 360;
        }
        int index = (int) (angle / (360.0 / entries.size()));
        return Math.min(entries.size() - 1, index);
    }

    private void redraw() {
        int size = OUTER * 2 + 8;
        BufferedImage[] holder = new BufferedImage[1];
        Graphics2D g = canvas.begin(holder, size, size);
        float s = (float) canvas.scale();
        float c = size / 2f * s;
        float ro = OUTER * s;
        float ri = INNER * s;
        List<Entry> entries = entries();
        int n = Math.max(1, entries.size());
        Ellipse2D inner = new Ellipse2D.Float(c - ri, c - ri, ri * 2, ri * 2);
        g.setColor(new Color(0x20, 0x14, 0x0C, 120));
        g.fill(new Ellipse2D.Float(c - ro - 3 * s, c - ro - 3 * s, (ro + 3 * s) * 2, (ro + 3 * s) * 2));
        for (int i = 0; i < n; i++) {
            double start = 90 - i * 360.0 / n;
            Area slice = new Area(new Arc2D.Float(c - ro, c - ro, ro * 2, ro * 2, (float) start, (float) (-360.0 / n), Arc2D.PIE));
            slice.subtract(new Area(inner));
            boolean on = i == hovered;
            boolean available = i < entries.size() && entries.get(i).available;
            g.setColor(on ? new Color(0xFFF4DC) : available ? new Color(0xECDCB6) : new Color(0xC8B894));
            g.fill(slice);
            g.setStroke(new BasicStroke(1.2f * s));
            g.setColor(new Color(0x5C3A22));
            g.draw(slice);
            if (on) {
                g.setStroke(new BasicStroke(2.2f * s));
                g.setColor(new Color(0xBE8C28));
                g.draw(slice);
            }
        }
        g.setColor(new Color(0xF4E8C8));
        g.fill(inner);
        g.setStroke(new BasicStroke(1.6f * s));
        g.setColor(new Color(0xBE8C28));
        g.draw(inner);
        g.setColor(new Color(0x5C3A22));
        g.setStroke(new BasicStroke(1.6f * s));
        g.draw(new Ellipse2D.Float(c - ro, c - ro, ro * 2, ro * 2));
        g.dispose();
        canvas.end(holder[0]);
    }

    // -- Interação

    @Override
    public void tick() {
        if (ModKeys.isPhysicallyDown(ModKeys.APPARATE)) {
            keySeen = true;
        } else if (keySeen) {
            select();
        }
    }

    private void select() {
        List<Entry> entries = entries();
        Entry choice = hovered >= 0 && hovered < entries.size() ? entries.get(hovered) : null;
        if (choice != null && choice.available) {
            ModNetwork.CHANNEL.sendToServer(new ApparatePacket(choice.personal ? AparatacaoSpell.Mode.PERSONAL : AparatacaoSpell.Mode.PUBLIC,
                    choice.index, choice.point.name()));
        }
        onClose();
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (pages.size() > 1) {
            page = Math.floorMod(page - (int) Math.signum(delta), pages.size());
            if (minecraft != null) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.2f));
            }
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            select();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void removed() {
        canvas.close();
    }

    private static String tr(String key) {
        return Component.translatable(key).getString();
    }
}
