package com.gabriel.hogwartsmod.client.gui;

import com.gabriel.hogwartsmod.magic.MagicData;
import com.gabriel.hogwartsmod.network.ModNetwork;
import com.gabriel.hogwartsmod.network.UpdateSlotsPacket;
import com.gabriel.hogwartsmod.spell.Spell;
import com.gabriel.hogwartsmod.spell.Spells;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GrimoireScreen extends Screen {
    private static final int BOOK_W = 300;
    private static final int BOOK_H = 190;
    private static final int PER_PAGE = 9;
    private static final int ROW_H = 15;
    private static final int SLOT = 18;
    private static final int SLOT_GAP = 3;
    private static final int TAB_PAD = 10;
    private static final int LOCK_SPACE = 11;
    private static final float TAB_TEXT_SCALE = 0.8f;

    private static final int COVER = 0xFF5C3A22;
    private static final int COVER_EDGE = 0xFF3C2414;
    private static final int PAGE = 0xFFECDCB6;
    private static final int PAGE_LINE = 0xFFC4AA7C;
    private static final int SPINE = 0xFF966E46;
    private static final int INK = 0xFF38261A;
    private static final int TITLE = 0xFF6E281E;
    private static final int MUTED = 0xFF82684C;
    private static final int HIGHLIGHT = 0xFFDCC696;
    private static final int SLOT_BG = 0xFFD6C298;
    private static final int GOLD = 0xFFBE8C28;
    private static final int NOTE = 0xFFD4C49E;
    private static final int NOTE_ON = 0xFFF0E4C4;
    private static final int NOTE_EDGE = 0xFF967E5A;

    private enum Tab { SPELLS, TEACH, APPARITION }

    private int left;
    private int top;
    private Tab tab = Tab.SPELLS;
    private int page;
    @Nullable
    private Spell selected;
    @Nullable
    private Spell armed;
    @Nullable
    private Spell pressSpell;
    private int pressSlot = -1;
    private double pressX;
    private double pressY;
    private boolean dragging;

    public GrimoireScreen() {
        super(Component.translatable("screen.hogwartsmod.grimoire"));
    }

    @Override
    protected void init() {
        left = (width - BOOK_W) / 2 - 8;
        top = (height - BOOK_H) / 2;
        MagicData data = data();
        if (data != null && selected == null) {
            selected = data.getActiveSpell() != null ? data.getActiveSpell() : learned().stream().findFirst().orElse(null);
        }
    }

    @Nullable
    private MagicData data() {
        return minecraft != null && minecraft.player != null ? MagicData.get(minecraft.player) : null;
    }

    private List<Spell> learned() {
        List<Spell> list = new ArrayList<>();
        MagicData data = data();
        if (data != null) {
            for (String id : data.getLearned()) {
                Spell spell = Spells.get(id);
                if (spell != null) {
                    list.add(spell);
                }
            }
        }
        return list;
    }

    private int pageCount() {
        return Math.max(1, (learned().size() + PER_PAGE - 1) / PER_PAGE);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // -- Desenho

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        drawTabs(g);
        drawBook(g);
        switch (tab) {
            case SPELLS -> drawSpellsTab(g, mouseX, mouseY);
            case TEACH -> drawTeachTab(g);
            case APPARITION -> {
            }
        }
        super.render(g, mouseX, mouseY, partialTick);
        if (dragging) {
            Spell drag = pressSpell != null ? pressSpell : slotSpell(pressSlot);
            if (drag != null) {
                g.pose().pushPose();
                g.pose().translate(0, 0, 300);
                SpellIcons.draw(g, drag, mouseX - 8, mouseY - 8, 16);
                g.pose().popPose();
            }
        } else if (tab == Tab.SPELLS) {
            int hover = slotAt(mouseX, mouseY);
            Spell spell = hover >= 0 ? slotSpell(hover) : null;
            if (spell != null) {
                g.renderTooltip(font, spell.getDisplayName(), mouseX, mouseY);
            }
        }
    }

    private void drawBook(GuiGraphics g) {
        g.fill(left, top, left + BOOK_W, top + BOOK_H, COVER_EDGE);
        g.fill(left + 2, top + 2, left + BOOK_W - 2, top + BOOK_H - 2, COVER);
        g.fill(left + 8, top + 8, left + BOOK_W / 2 - 2, top + BOOK_H - 8, PAGE);
        g.fill(left + BOOK_W / 2 + 2, top + 8, left + BOOK_W - 8, top + BOOK_H - 8, PAGE);
        g.fill(left + BOOK_W / 2 - 2, top + 6, left + BOOK_W / 2 + 2, top + BOOK_H - 6, SPINE);
        outline(g, left + 12, top + 12, left + BOOK_W / 2 - 6, top + BOOK_H - 12, PAGE_LINE);
        outline(g, left + BOOK_W / 2 + 6, top + 12, left + BOOK_W - 12, top + BOOK_H - 12, PAGE_LINE);
    }

    private static boolean isLocked(Tab t) {
        return t == Tab.APPARITION;
    }

    private Component tabLabel(Tab t) {
        return Component.translatable("screen.hogwartsmod.grimoire.tab." + t.name().toLowerCase());
    }

    private int tabHeight(Tab t) {
        return Math.round(font.width(tabLabel(t)) * TAB_TEXT_SCALE) + TAB_PAD + (isLocked(t) ? LOCK_SPACE : 0);
    }

    private int tabTop(Tab t) {
        int y = top + 10;
        for (Tab other : Tab.values()) {
            if (other == t) {
                return y;
            }
            y += tabHeight(other) + 3;
        }
        return y;
    }

    private void drawTabs(GuiGraphics g) {
        for (Tab t : Tab.values()) {
            boolean on = t == tab;
            int x0 = left + BOOK_W - 10;
            int x1 = left + BOOK_W + (on ? 14 : 11);
            int y0 = tabTop(t);
            int h = tabHeight(t);
            g.fill(x0, y0, x1, y0 + h, NOTE_EDGE);
            g.fill(x0, y0 + 1, x1 - 1, y0 + h - 1, on ? NOTE_ON : NOTE);
            g.fill(x1 - 4, y0 + h - 4, x1 - 1, y0 + h - 1, NOTE_EDGE);
            g.pose().pushPose();
            g.pose().translate(x1 - 2, y0 + TAB_PAD / 2f, 0);
            g.pose().mulPose(Axis.ZP.rotationDegrees(90));
            g.pose().scale(TAB_TEXT_SCALE, TAB_TEXT_SCALE, 1f);
            g.drawString(font, tabLabel(t), 0, 0, INK, false);
            g.pose().popPose();
            if (isLocked(t)) {
                drawLock(g, x1 - 9, y0 + h - LOCK_SPACE);
            }
        }
    }

    private void drawLock(GuiGraphics g, int x, int y) {
        g.fill(x + 1, y, x + 5, y + 1, INK);
        g.fill(x, y + 1, x + 1, y + 4, INK);
        g.fill(x + 5, y + 1, x + 6, y + 4, INK);
        g.fill(x - 1, y + 4, x + 7, y + 9, INK);
        g.fill(x, y + 5, x + 6, y + 8, GOLD);
        g.fill(x + 2, y + 6, x + 4, y + 7, INK);
    }

    private void drawSpellsTab(GuiGraphics g, int mouseX, int mouseY) {
        MagicData data = data();
        if (data == null) {
            return;
        }
        int lx = left + 16;
        int pageW = BOOK_W / 2 - 26;
        centered(g, Component.translatable("screen.hogwartsmod.grimoire.learned"), lx + pageW / 2, top + 18, TITLE);
        List<Spell> spells = learned();
        page = Mth.clamp(page, 0, pageCount() - 1);
        if (spells.isEmpty()) {
            wrapped(g, Component.translatable("screen.hogwartsmod.grimoire.empty"), lx + pageW / 2, top + 60, pageW, MUTED);
        }
        for (int i = 0; i < PER_PAGE; i++) {
            int index = page * PER_PAGE + i;
            if (index >= spells.size()) {
                break;
            }
            Spell spell = spells.get(index);
            int y = top + 30 + i * ROW_H;
            boolean hover = mouseX >= lx && mouseX < lx + pageW && mouseY >= y && mouseY < y + ROW_H;
            if (spell == selected || hover) {
                g.fill(lx - 2, y - 1, lx + pageW + 2, y + ROW_H - 1, spell == armed ? GOLD : HIGHLIGHT);
            }
            SpellIcons.draw(g, spell, lx, y, 12);
            String level = "Nv " + spell.getRequiredLevel();
            int maxName = pageW - 16 - font.width(level) - 4;
            String name = spell.getDisplayName().getString();
            if (font.width(name) > maxName) {
                name = font.plainSubstrByWidth(name, maxName - font.width("…")) + "…";
            }
            g.drawString(font, name, lx + 16, y + 2, INK, false);
            g.drawString(font, level, lx + pageW - font.width(level), y + 2, MUTED, false);
        }
        drawPagination(g, lx + pageW / 2, top + BOOK_H - 24);

        int rx = left + BOOK_W / 2 + 10;
        int rw = BOOK_W / 2 - 26;
        int rc = rx + rw / 2;
        if (selected != null) {
            centered(g, selected.getDisplayName(), rc, top + 18, TITLE);
            SpellIcons.draw(g, selected, rc - 12, top + 28, 24);
            List<FormattedCharSequence> lines = font.split(Component.translatable("spell.hogwartsmod." + selected.getId() + ".desc"), rw);
            for (int i = 0; i < Math.min(3, lines.size()); i++) {
                g.drawString(font, lines.get(i), rc - font.width(lines.get(i)) / 2, top + 56 + i * 9, INK, false);
            }
            int sy = top + 86;
            stat(g, rx, rw, sy, "category", Component.translatable("category.hogwartsmod." + selected.getCategory().name().toLowerCase()));
            stat(g, rx, rw, sy + 10, "cost", Component.literal(formatCost(selected)));
            stat(g, rx, rw, sy + 20, "cooldown", Component.literal(String.format("%.1f s", selected.getCooldownTicks() / 20f)));
            g.drawString(font, Component.translatable("screen.hogwartsmod.grimoire.stat.mastery"), rx, sy + 30, MUTED, false);
            int mastery = data.getMastery(selected);
            for (int i = 0; i < selected.getMaxMastery(); i++) {
                int dx = rx + rw - (selected.getMaxMastery() - i) * 8;
                diamond(g, dx, sy + 31, i < mastery);
            }
        }
        centered(g, Component.translatable("screen.hogwartsmod.grimoire.shortcuts"), rc, top + 132, TITLE);
        for (int i = 0; i < MagicData.SLOT_COUNT; i++) {
            int x = slotX(i);
            int y = slotY();
            boolean active = i == data.getActiveSlot();
            boolean hover = slotAt(mouseX, mouseY) == i;
            g.fill(x, y, x + SLOT, y + SLOT, active ? GOLD : COVER);
            g.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, hover && (dragging || armed != null) ? HIGHLIGHT : SLOT_BG);
            Spell spell = slotSpell(i);
            if (spell != null && !(dragging && pressSlot == i)) {
                SpellIcons.draw(g, spell, x + 1, y + 1, 16);
            } else if (spell == null) {
                centered(g, Component.literal("+"), x + SLOT / 2, y + 5, MUTED);
            }
            centered(g, Component.literal(String.valueOf(i + 1)), x + SLOT / 2, y + SLOT + 2, MUTED);
        }
    }

    private void drawTeachTab(GuiGraphics g) {
        MagicData data = data();
        if (data == null) {
            return;
        }
        int lx = left + 16;
        int pageW = BOOK_W / 2 - 26;
        centered(g, Component.translatable("screen.hogwartsmod.grimoire.teachable"), lx + pageW / 2, top + 18, TITLE);
        int row = 0;
        for (Spell spell : learned()) {
            if (!data.hasMaxMastery(spell)) {
                continue;
            }
            int y = top + 30 + row * ROW_H;
            SpellIcons.draw(g, spell, lx, y, 12);
            g.drawString(font, spell.getDisplayName(), lx + 16, y + 2, INK, false);
            if (++row >= PER_PAGE) {
                break;
            }
        }
        if (row == 0) {
            wrapped(g, Component.translatable("screen.hogwartsmod.grimoire.teach_none"), lx + pageW / 2, top + 60, pageW, MUTED);
        }
        int rx = left + BOOK_W / 2 + 10;
        int rw = BOOK_W / 2 - 26;
        centered(g, Component.translatable("screen.hogwartsmod.grimoire.students"), rx + rw / 2, top + 18, TITLE);
        wrapped(g, Component.translatable("screen.hogwartsmod.grimoire.teach_soon"), rx + rw / 2, top + 60, rw, MUTED);
    }

    private void drawPagination(GuiGraphics g, int cx, int y) {
        int count = pageCount();
        int startX = cx - (count - 1) * 7;
        for (int i = 0; i < count; i++) {
            int x = startX + i * 14;
            if (i == page) {
                g.fill(x - 5, y - 2, x + 6, y + 9, HIGHLIGHT);
            }
            centered(g, Component.literal(String.valueOf(i + 1)), x + 1, y, i == page ? TITLE : MUTED);
        }
    }

    private void stat(GuiGraphics g, int x, int w, int y, String key, Component value) {
        g.drawString(font, Component.translatable("screen.hogwartsmod.grimoire.stat." + key), x, y, MUTED, false);
        g.drawString(font, value, x + w - font.width(value), y, INK, false);
    }

    private void diamond(GuiGraphics g, int x, int y, boolean filled) {
        int color = filled ? GOLD : PAGE_LINE;
        g.fill(x + 2, y, x + 4, y + 1, color);
        g.fill(x + 1, y + 1, x + 5, y + 2, color);
        g.fill(x, y + 2, x + 6, y + 4, color);
        g.fill(x + 1, y + 4, x + 5, y + 5, color);
        g.fill(x + 2, y + 5, x + 4, y + 6, color);
    }

    private static String formatCost(Spell spell) {
        String base = spell.getCost() == Math.floor(spell.getCost()) ? String.valueOf((int) spell.getCost()) : String.valueOf(spell.getCost());
        return spell.isSustained() ? base + " +/s" : base;
    }

    private void centered(GuiGraphics g, Component text, int cx, int y, int color) {
        g.drawString(font, text, cx - font.width(text) / 2, y, color, false);
    }

    private void wrapped(GuiGraphics g, Component text, int cx, int y, int w, int color) {
        List<FormattedCharSequence> lines = font.split(text, w);
        for (int i = 0; i < lines.size(); i++) {
            g.drawString(font, lines.get(i), cx - font.width(lines.get(i)) / 2, y + i * 10, color, false);
        }
    }

    private static void outline(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        g.fill(x0, y0, x1, y0 + 1, color);
        g.fill(x0, y1 - 1, x1, y1, color);
        g.fill(x0, y0, x0 + 1, y1, color);
        g.fill(x1 - 1, y0, x1, y1, color);
    }

    // -- Posições

    private int slotX(int i) {
        int rx = left + BOOK_W / 2 + 10;
        int rw = BOOK_W / 2 - 26;
        int total = MagicData.SLOT_COUNT * SLOT + (MagicData.SLOT_COUNT - 1) * SLOT_GAP;
        return rx + (rw - total) / 2 + i * (SLOT + SLOT_GAP);
    }

    private int slotY() {
        return top + 144;
    }

    private int slotAt(double mx, double my) {
        for (int i = 0; i < MagicData.SLOT_COUNT; i++) {
            int x = slotX(i);
            if (mx >= x && mx < x + SLOT && my >= slotY() && my < slotY() + SLOT) {
                return i;
            }
        }
        return -1;
    }

    @Nullable
    private Spell rowAt(double mx, double my) {
        int lx = left + 16;
        int pageW = BOOK_W / 2 - 26;
        if (mx < lx - 2 || mx >= lx + pageW + 2 || my < top + 30) {
            return null;
        }
        int row = (int) ((my - (top + 30)) / ROW_H);
        int index = page * PER_PAGE + row;
        List<Spell> spells = learned();
        return row < PER_PAGE && index < spells.size() ? spells.get(index) : null;
    }

    @Nullable
    private Spell slotSpell(int i) {
        MagicData data = data();
        return data == null || i < 0 ? null : data.getSlot(i);
    }

    // -- Interação

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (Tab t : Tab.values()) {
            int y0 = tabTop(t);
            if (mx >= left + BOOK_W - 10 && mx < left + BOOK_W + 14 && my >= y0 && my < y0 + tabHeight(t)) {
                if (!isLocked(t)) {
                    tab = t;
                    armed = null;
                }
                return true;
            }
        }
        if (tab != Tab.SPELLS) {
            return super.mouseClicked(mx, my, button);
        }
        int slot = slotAt(mx, my);
        if (button == 1 && slot >= 0) {
            setSlot(slot, null);
            return true;
        }
        if (button != 0) {
            return super.mouseClicked(mx, my, button);
        }
        int count = pageCount();
        int cx = left + 16 + (BOOK_W / 2 - 26) / 2;
        int py = top + BOOK_H - 24;
        int startX = cx - (count - 1) * 7;
        for (int i = 0; i < count; i++) {
            int x = startX + i * 14;
            if (mx >= x - 5 && mx < x + 6 && my >= py - 2 && my < py + 9) {
                page = i;
                return true;
            }
        }
        if (slot >= 0) {
            if (armed != null) {
                setSlot(slot, armed);
                armed = null;
                return true;
            }
            if (slotSpell(slot) != null) {
                pressSlot = slot;
                pressX = mx;
                pressY = my;
                selected = slotSpell(slot);
            }
            return true;
        }
        Spell row = rowAt(mx, my);
        if (row != null) {
            pressSpell = row;
            pressX = mx;
            pressY = my;
            return true;
        }
        armed = null;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if ((pressSpell != null || pressSlot >= 0) && !dragging && Math.abs(mx - pressX) + Math.abs(my - pressY) > 3) {
            dragging = true;
            armed = null;
        }
        return dragging || super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0) {
            int target = slotAt(mx, my);
            if (dragging) {
                if (pressSpell != null && target >= 0) {
                    setSlot(target, pressSpell);
                } else if (pressSlot >= 0) {
                    if (target >= 0 && target != pressSlot) {
                        Spell from = slotSpell(pressSlot);
                        Spell to = slotSpell(target);
                        setSlots(pressSlot, to, target, from);
                    } else if (target < 0) {
                        setSlot(pressSlot, null);
                    }
                }
            } else if (pressSpell != null) {
                selected = pressSpell;
                armed = armed == pressSpell ? null : pressSpell;
            }
            pressSpell = null;
            pressSlot = -1;
            dragging = false;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (com.gabriel.hogwartsmod.client.ModKeys.OPEN_GRIMOIRE.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void setSlot(int index, @Nullable Spell spell) {
        setSlots(index, spell, -1, null);
    }

    private void setSlots(int a, @Nullable Spell spellA, int b, @Nullable Spell spellB) {
        MagicData data = data();
        if (data == null) {
            return;
        }
        data.setSlot(a, spellA);
        if (b >= 0) {
            data.setSlot(b, spellB);
        }
        String[] ids = new String[MagicData.SLOT_COUNT];
        for (int i = 0; i < MagicData.SLOT_COUNT; i++) {
            Spell spell = data.getSlot(i);
            ids[i] = spell == null ? null : spell.getId();
        }
        if (minecraft != null && minecraft.player != null) {
            ModNetwork.CHANNEL.sendToServer(new UpdateSlotsPacket(ids));
        }
    }
}
