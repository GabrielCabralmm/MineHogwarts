package com.gabriel.hogwartsmod.client.gui;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.magic.MagicData;
import com.gabriel.hogwartsmod.network.ModNetwork;
import com.gabriel.hogwartsmod.network.UpdateSlotsPacket;
import com.gabriel.hogwartsmod.spell.Spell;
import com.gabriel.hogwartsmod.spell.Spells;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
    private static final float TAB_TEXT_SIZE = 7f;

    private static final int COVER = 0xFF5C3A22;
    private static final int PAGE_LINE = 0xFFC4AA7C;
    private static final int INK = 0xFF38261A;
    private static final int TITLE = 0xFF6E281E;
    private static final int MUTED = 0xFF82684C;
    private static final int HIGHLIGHT = 0xFFDCC696;
    private static final int SLOT_BG = 0xFFD6C298;
    private static final int GOLD = 0xFFBE8C28;
    private static final int NOTE = 0xFFD4C49E;
    private static final int NOTE_ON = 0xFFF0E4C4;
    private static final int NOTE_EDGE = 0xFF967E5A;

    private static final ResourceLocation BOOK =
            ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "textures/gui/grimoire.png");

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
        HiRes.image(g, BOOK, left, top, BOOK_W, BOOK_H);
    }

    private static boolean isLocked(Tab t) {
        return t == Tab.APPARITION;
    }

    private String tabLabel(Tab t) {
        return Component.translatable("screen.hogwartsmod.grimoire.tab." + t.name().toLowerCase()).getString();
    }

    private int tabHeight(Tab t) {
        return Math.round(HiRes.width(tabLabel(t), HiRes.Style.BOLD, TAB_TEXT_SIZE)) + TAB_PAD + (isLocked(t) ? LOCK_SPACE : 0);
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
            g.pose().translate(x1 - 1.5f, y0 + TAB_PAD / 2f, 0);
            g.pose().mulPose(Axis.ZP.rotationDegrees(90));
            HiRes.text(g, tabLabel(t), HiRes.Style.BOLD, TAB_TEXT_SIZE, 0, 0, INK);
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
        HiRes.centered(g, tr("screen.hogwartsmod.grimoire.learned"), HiRes.Style.BOLD, lx + pageW / 2f, top + 17, TITLE);
        List<Spell> spells = learned();
        page = Mth.clamp(page, 0, pageCount() - 1);
        if (spells.isEmpty()) {
            wrapped(g, tr("screen.hogwartsmod.grimoire.empty"), HiRes.Style.ITALIC, lx + pageW / 2f, top + 60, pageW, MUTED);
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
            float levelW = HiRes.width(level, HiRes.Style.REGULAR);
            String name = HiRes.ellipsize(spell.getDisplayName().getString(), HiRes.Style.REGULAR, pageW - 16 - levelW - 4);
            HiRes.text(g, name, HiRes.Style.REGULAR, lx + 16, y + 1.5f, INK);
            HiRes.right(g, level, HiRes.Style.REGULAR, lx + pageW, y + 1.5f, MUTED);
        }
        drawPagination(g, lx + pageW / 2, top + BOOK_H - 24);

        int rx = left + BOOK_W / 2 + 10;
        int rw = BOOK_W / 2 - 26;
        int rc = rx + rw / 2;
        if (selected != null) {
            HiRes.centered(g, selected.getDisplayName().getString(), HiRes.Style.BOLD, rc, top + 17, TITLE);
            SpellIcons.draw(g, selected, rc - 12, top + 28, 24);
            List<String> lines = HiRes.wrap(tr("spell.hogwartsmod." + selected.getId() + ".desc"), HiRes.Style.ITALIC, rw);
            for (int i = 0; i < Math.min(3, lines.size()); i++) {
                HiRes.centered(g, lines.get(i), HiRes.Style.ITALIC, rc, top + 56 + i * 10, INK);
            }
            int sy = top + 88;
            stat(g, rx, rw, sy, "category", tr("category.hogwartsmod." + selected.getCategory().name().toLowerCase()));
            stat(g, rx, rw, sy + 10, "cost", formatCost(selected));
            stat(g, rx, rw, sy + 20, "cooldown", String.format("%.1f s", selected.getCooldownTicks() / 20f));
            HiRes.text(g, tr("screen.hogwartsmod.grimoire.stat.mastery"), HiRes.Style.REGULAR, rx, sy + 30, MUTED);
            int mastery = data.getMastery(selected);
            for (int i = 0; i < selected.getMaxMastery(); i++) {
                int dx = rx + rw - (selected.getMaxMastery() - i) * 8;
                diamond(g, dx, sy + 32, i < mastery);
            }
        }
        HiRes.centered(g, tr("screen.hogwartsmod.grimoire.shortcuts"), HiRes.Style.BOLD, rc, top + 131, TITLE);
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
                HiRes.centered(g, "+", HiRes.Style.REGULAR, x + SLOT / 2f, y + 4, MUTED);
            }
            HiRes.centered(g, String.valueOf(i + 1), HiRes.Style.REGULAR, x + SLOT / 2f, y + SLOT + 1, MUTED);
        }
    }

    private void drawTeachTab(GuiGraphics g) {
        MagicData data = data();
        if (data == null) {
            return;
        }
        int lx = left + 16;
        int pageW = BOOK_W / 2 - 26;
        HiRes.centered(g, tr("screen.hogwartsmod.grimoire.teachable"), HiRes.Style.BOLD, lx + pageW / 2f, top + 17, TITLE);
        int row = 0;
        for (Spell spell : learned()) {
            if (!data.hasMaxMastery(spell)) {
                continue;
            }
            int y = top + 30 + row * ROW_H;
            SpellIcons.draw(g, spell, lx, y, 12);
            HiRes.text(g, spell.getDisplayName().getString(), HiRes.Style.REGULAR, lx + 16, y + 1.5f, INK);
            if (++row >= PER_PAGE) {
                break;
            }
        }
        if (row == 0) {
            wrapped(g, tr("screen.hogwartsmod.grimoire.teach_none"), HiRes.Style.ITALIC, lx + pageW / 2f, top + 60, pageW, MUTED);
        }
        int rx = left + BOOK_W / 2 + 10;
        int rw = BOOK_W / 2 - 26;
        HiRes.centered(g, tr("screen.hogwartsmod.grimoire.students"), HiRes.Style.BOLD, rx + rw / 2f, top + 17, TITLE);
        wrapped(g, tr("screen.hogwartsmod.grimoire.teach_soon"), HiRes.Style.ITALIC, rx + rw / 2f, top + 60, rw, MUTED);
    }

    private void drawPagination(GuiGraphics g, int cx, int y) {
        int count = pageCount();
        int startX = cx - (count - 1) * 7;
        for (int i = 0; i < count; i++) {
            int x = startX + i * 14;
            if (i == page) {
                g.fill(x - 5, y - 2, x + 6, y + 9, HIGHLIGHT);
            }
            HiRes.centered(g, String.valueOf(i + 1), i == page ? HiRes.Style.BOLD : HiRes.Style.REGULAR, x + 0.5f, y - 1, i == page ? TITLE : MUTED);
        }
    }

    private void stat(GuiGraphics g, int x, int w, int y, String key, String value) {
        HiRes.text(g, tr("screen.hogwartsmod.grimoire.stat." + key), HiRes.Style.REGULAR, x, y, MUTED);
        HiRes.right(g, value, HiRes.Style.REGULAR, x + w, y, INK);
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

    private static String tr(String key) {
        return Component.translatable(key).getString();
    }

    private void wrapped(GuiGraphics g, String text, HiRes.Style style, float cx, int y, int w, int color) {
        List<String> lines = HiRes.wrap(text, style, w);
        for (int i = 0; i < lines.size(); i++) {
            HiRes.centered(g, lines.get(i), style, cx, y + i * 10, color);
        }
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
