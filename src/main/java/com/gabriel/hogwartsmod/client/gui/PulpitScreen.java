package com.gabriel.hogwartsmod.client.gui;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.block.PulpitBlockEntity;
import com.gabriel.hogwartsmod.block.PulpitBlockEntity.Side;
import com.gabriel.hogwartsmod.client.trace.ClientTeachState;
import com.gabriel.hogwartsmod.magic.MagicData;
import com.gabriel.hogwartsmod.network.ModNetwork;
import com.gabriel.hogwartsmod.network.PulpitAreaPacket;
import com.gabriel.hogwartsmod.network.PulpitLessonPacket;
import com.gabriel.hogwartsmod.network.StudentsPacket;
import com.gabriel.hogwartsmod.spell.Spell;
import com.gabriel.hogwartsmod.spell.Spells;
import com.gabriel.hogwartsmod.teach.StudentStatus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PulpitScreen extends Screen {
    private static final int W = 320;
    private static final int H = 210;
    private static final int ROW = 14;
    private static final int SPELL_ROWS = 6;
    private static final int STUDENT_ROWS = 4;
    private static final int PLAN_W = 84;
    private static final int PLAN_H = 60;

    private static final int INK = 0xFF38261A;
    private static final int TITLE = 0xFF6E281E;
    private static final int MUTED = 0xFF82684C;
    private static final int HIGHLIGHT = 0xFFDCC696;
    private static final int GOLD = 0xFFBE8C28;
    private static final int GREEN = 0xFF3F6B2A;
    private static final int WINE = 0xFF8E2A2A;
    private static final int LINE = 0xFFC4AA7C;
    private static final int FLOOR = 0xFFE2D0A8;
    private static final int COVER = 0xFF5C3A22;
    private static final int NOTE = 0xFFD4C49E;
    private static final int NOTE_ON = 0xFFF0E4C4;
    private static final int NOTE_EDGE = 0xFF967E5A;

    private static final ResourceLocation SHEET = tex("pulpit_sheet");
    private static final ResourceLocation CHECK = tex("check");

    @Nullable
    private static PulpitScreen open;

    private final BlockPos pos;
    private final boolean canEdit;
    private final int[] extents;
    private boolean showArea;
    @Nullable
    private Spell spell;
    private int scroll;
    private int ticks;
    private int left;
    private int top;

    public PulpitScreen(BlockPos pos, boolean canEdit, PulpitBlockEntity pulpit) {
        super(Component.translatable("block.hogwartsmod.pulpito_professor"));
        this.pos = pos;
        this.canEdit = canEdit;
        this.extents = pulpit.getExtents();
        this.showArea = pulpit.isShowArea();
    }

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "textures/gui/" + name + ".png");
    }

    @Nullable
    public static int[] liveExtents(BlockPos pos) {
        return open != null && open.pos.equals(pos) ? open.extents : null;
    }

    @Override
    protected void init() {
        open = this;
        left = (width - W) / 2;
        top = (height - H) / 2;
    }

    @Override
    public void removed() {
        if (open == this) {
            open = null;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Nullable
    private MagicData data() {
        return minecraft != null && minecraft.player != null ? MagicData.get(minecraft.player) : null;
    }

    private List<Spell> known() {
        List<Spell> list = new ArrayList<>();
        MagicData data = data();
        if (data != null) {
            for (String id : data.getLearned()) {
                Spell s = Spells.get(id);
                if (s != null) {
                    list.add(s);
                }
            }
        }
        return list;
    }

    private long gameTime() {
        return minecraft != null && minecraft.level != null ? minecraft.level.getGameTime() : 0;
    }

    private List<StudentsPacket.Entry> students() {
        return spell != null && spell.getId().equals(ClientTeachState.spell) ? ClientTeachState.students : List.of();
    }

    private int cooldown() {
        MagicData data = data();
        return spell == null || data == null ? 0 : data.getTeachCooldownRemaining(spell, gameTime());
    }

    // -- Desenho

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        HiRes.image(g, SHEET, left, top, W, H);
        HiRes.centered(g, title.getString(), HiRes.Style.BOLD, 11f, left + W / 2f, top + 14, TITLE);
        g.fill(left + 160, top + 30, left + 161, top + H - 14, LINE);
        drawSpells(g, mouseX, mouseY);
        drawStudents(g);
        drawArea(g, mouseX, mouseY);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawSpells(GuiGraphics g, int mouseX, int mouseY) {
        int x = left + 18;
        int w = 128;
        HiRes.text(g, tr("screen.hogwartsmod.pulpit.spell"), HiRes.Style.BOLD, x, top + 31, INK);
        List<Spell> spells = known();
        MagicData data = data();
        long now = gameTime();
        for (int i = 0; i < SPELL_ROWS && i + scroll < spells.size(); i++) {
            Spell s = spells.get(i + scroll);
            int y = top + 44 + i * ROW;
            boolean hover = mouseX >= x - 2 && mouseX < x + w + 2 && mouseY >= y - 1 && mouseY < y + ROW - 1;
            if (s == spell || hover) {
                g.fill(x - 2, y - 1, x + w + 2, y + ROW - 1, HIGHLIGHT);
            }
            SpellIcons.draw(g, s, x, y, 11);
            int remaining = data == null ? 0 : data.getTeachCooldownRemaining(s, now);
            String status = remaining > 0 ? formatTime(remaining) : tr("screen.hogwartsmod.grimoire.ready");
            float statusW = HiRes.width(status, HiRes.Style.REGULAR);
            HiRes.text(g, HiRes.ellipsize(s.getDisplayName().getString(), HiRes.Style.REGULAR, w - 15 - statusW - 4), HiRes.Style.REGULAR, x + 15, y + 1, INK);
            HiRes.right(g, status, remaining > 0 ? HiRes.Style.REGULAR : HiRes.Style.BOLD, x + w, y + 1, remaining > 0 ? MUTED : GREEN);
        }
        if (spells.size() > SPELL_ROWS) {
            String page = (scroll + 1) + "–" + Math.min(spells.size(), scroll + SPELL_ROWS) + " / " + spells.size();
            HiRes.right(g, page, HiRes.Style.ITALIC, x + w, top + 31, MUTED);
        }
        g.fill(x, top + 132, x + w, top + 133, LINE);
    }

    private void drawStudents(GuiGraphics g) {
        int x = left + 18;
        int w = 128;
        HiRes.text(g, tr("screen.hogwartsmod.pulpit.students"), HiRes.Style.BOLD, x, top + 137, INK);
        if (spell == null) {
            HiRes.text(g, tr("screen.hogwartsmod.pulpit.pick_spell"), HiRes.Style.ITALIC, x, top + 151, MUTED);
            return;
        }
        List<StudentsPacket.Entry> students = students();
        if (students.isEmpty()) {
            HiRes.text(g, tr("screen.hogwartsmod.pulpit.empty"), HiRes.Style.ITALIC, x, top + 151, MUTED);
            return;
        }
        int can = 0;
        for (StudentsPacket.Entry e : students) {
            if (e.status() == StudentStatus.CAN_LEARN) {
                can++;
            }
        }
        for (int i = 0; i < Math.min(STUDENT_ROWS, students.size()); i++) {
            StudentsPacket.Entry e = students.get(i);
            int y = top + 150 + i * 11;
            String status = tr(e.status().key());
            float statusW = HiRes.width(status, HiRes.Style.ITALIC);
            HiRes.text(g, HiRes.ellipsize(e.name(), HiRes.Style.REGULAR, w - statusW - 6), HiRes.Style.REGULAR, x, y, e.status() == StudentStatus.CAN_LEARN ? INK : MUTED);
            HiRes.right(g, status, HiRes.Style.ITALIC, x + w, y, statusColor(e.status()));
        }
        String more = students.size() > STUDENT_ROWS ? " (+" + (students.size() - STUDENT_ROWS) + ")" : "";
        HiRes.text(g, Component.translatable("screen.hogwartsmod.pulpit.count", can, students.size()).getString() + more,
                HiRes.Style.ITALIC, x, top + H - 22, MUTED);
    }

    private void drawArea(GuiGraphics g, int mouseX, int mouseY) {
        float cx = left + 240;
        HiRes.centered(g, tr("screen.hogwartsmod.pulpit.area"), HiRes.Style.BOLD, cx, top + 31, INK);
        int cols = get(Side.LEFT) + get(Side.RIGHT) + 1;
        int rows = get(Side.FRONT) + get(Side.BACK) + 1;
        float cell = Math.min(PLAN_W / (float) cols, PLAN_H / (float) rows);
        int pw = Math.max(4, Math.round(cols * cell));
        int ph = Math.max(4, Math.round(rows * cell));
        int px = Math.round(cx - pw / 2f);
        int py = top + 54 + (PLAN_H - ph) / 2;
        g.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, GOLD);
        g.fill(px, py, px + pw, py + ph, FLOOR);
        if (cell >= 3) {
            for (int c = 1; c < cols; c++) {
                int lx = px + Math.round(c * cell);
                g.fill(lx, py, lx + 1, py + ph, 0x40A08060);
            }
            for (int r = 1; r < rows; r++) {
                int ly = py + Math.round(r * cell);
                g.fill(px, ly, px + pw, ly + 1, 0x40A08060);
            }
        }
        int bx = px + Math.round(get(Side.LEFT) * cell);
        int by = py + Math.round(get(Side.FRONT) * cell);
        int bs = Math.max(3, Math.round(cell));
        g.fill(bx - 1, by - 1, bx + bs + 1, by + bs + 1, GOLD);
        g.fill(bx, by, bx + bs, by + bs, COVER);

        int midY = py + ph / 2;
        arrowControl(g, Side.FRONT, Math.round(cx) - 4, py - 13, mouseX, mouseY);
        arrowControl(g, Side.BACK, Math.round(cx) - 4, py + ph + 5, mouseX, mouseY);
        arrowControl(g, Side.LEFT, px - 13, midY - 4, mouseX, mouseY);
        arrowControl(g, Side.RIGHT, px + pw + 5, midY - 4, mouseX, mouseY);
        numberFor(g, Side.FRONT, cx + 8, py - 12.5f, false);
        numberFor(g, Side.BACK, cx + 8, py + ph + 5.5f, false);
        numberFor(g, Side.LEFT, px - 15, midY - 3.5f, true);
        numberFor(g, Side.RIGHT, px + pw + 15, midY - 3.5f, false);

        int hy = top + 140;
        HiRes.text(g, tr("screen.hogwartsmod.pulpit.height"), HiRes.Style.REGULAR, left + 176, hy, INK);
        arrowControl(g, Side.UP, left + 230, hy, mouseX, mouseY);
        HiRes.text(g, String.valueOf(get(Side.UP)), HiRes.Style.BOLD, left + 241, hy, INK);
        arrowControl(g, Side.DOWN, left + 262, hy, mouseX, mouseY);
        HiRes.text(g, String.valueOf(get(Side.DOWN)), HiRes.Style.BOLD, left + 273, hy, INK);

        int height = get(Side.UP) + get(Side.DOWN) + 1;
        HiRes.centered(g, Component.translatable("screen.hogwartsmod.pulpit.size", cols, rows, height).getString(),
                HiRes.Style.ITALIC, cx, top + 153, MUTED);

        int chx = left + 180;
        int chy = top + 166;
        g.fill(chx, chy, chx + 8, chy + 8, NOTE_EDGE);
        g.fill(chx + 1, chy + 1, chx + 7, chy + 7, NOTE_ON);
        if (showArea) {
            HiRes.image(g, CHECK, chx, chy - 1, 9, 9);
        }
        HiRes.text(g, tr("screen.hogwartsmod.pulpit.show"), HiRes.Style.REGULAR, chx + 12, chy, canEdit ? INK : MUTED);

        boolean ready = spell != null && cooldown() <= 0 && isProfessor() && countCanLearn() > 0;
        int bx2 = left + 180;
        int by2 = top + H - 30;
        int bw = 120;
        boolean hover = ready && mouseX >= bx2 && mouseX < bx2 + bw && mouseY >= by2 && mouseY < by2 + 15;
        g.fill(bx2, by2, bx2 + bw, by2 + 15, ready ? COVER : NOTE_EDGE);
        g.fill(bx2 + 1, by2 + 1, bx2 + bw - 1, by2 + 14, hover ? NOTE_ON : ready ? NOTE : 0xFFD6C298);
        String label = !isProfessor() ? tr("screen.hogwartsmod.pulpit.need_professor")
                : spell != null && cooldown() > 0 ? formatTime(cooldown()) : tr("screen.hogwartsmod.pulpit.start");
        HiRes.centered(g, label, HiRes.Style.BOLD, bx2 + bw / 2f, by2 + 3, ready ? TITLE : MUTED);
    }

    private void arrowControl(GuiGraphics g, Side side, int x, int y, int mouseX, int mouseY) {
        boolean shrink = hasShiftDown();
        String dir = switch (side) {
            case FRONT, UP -> shrink ? "down" : "up";
            case BACK, DOWN -> shrink ? "up" : "down";
            case LEFT -> shrink ? "right" : "left";
            case RIGHT -> shrink ? "left" : "right";
        };
        boolean hover = canEdit && mouseX >= x - 1 && mouseX < x + 9 && mouseY >= y - 1 && mouseY < y + 9;
        String color = !canEdit ? "muted" : shrink ? "wine" : "ink";
        if (hover) {
            g.fill(x - 1, y - 1, x + 9, y + 9, HIGHLIGHT);
        }
        HiRes.image(g, tex("arrow_" + dir + "_" + color), x, y, 8, 8);
    }

    private void numberFor(GuiGraphics g, Side side, float x, float y, boolean alignRight) {
        String text = String.valueOf(get(side));
        if (alignRight) {
            HiRes.right(g, text, HiRes.Style.BOLD, x, y, INK);
        } else {
            HiRes.text(g, text, HiRes.Style.BOLD, x, y, INK);
        }
    }

    private int get(Side side) {
        return extents[side.ordinal()];
    }

    private boolean isProfessor() {
        MagicData data = data();
        return data != null && data.isProfessor();
    }

    private int countCanLearn() {
        int n = 0;
        for (StudentsPacket.Entry e : students()) {
            if (e.status() == StudentStatus.CAN_LEARN) {
                n++;
            }
        }
        return n;
    }

    private static int statusColor(StudentStatus status) {
        return switch (status) {
            case CAN_LEARN -> GREEN;
            case LOW_LEVEL -> WINE;
            case INVITED -> GOLD;
            default -> MUTED;
        };
    }

    private static String formatTime(int ticks) {
        int seconds = ticks / 20;
        if (seconds >= 3600) {
            return String.format("%dh %02dm", seconds / 3600, seconds / 60 % 60);
        }
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    // -- Interação

    @Override
    public void tick() {
        ticks++;
        if (ticks % 20 == 0) {
            requestStudents();
        }
    }

    private void requestStudents() {
        if (spell != null) {
            ModNetwork.CHANNEL.sendToServer(new PulpitLessonPacket(pos, spell.getId(), false));
        }
    }

    private void sendArea() {
        ModNetwork.CHANNEL.sendToServer(new PulpitAreaPacket(pos, extents.clone(), showArea));
        requestStudents();
    }

    private void click() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) {
            return super.mouseClicked(mx, my, button);
        }
        int x = left + 18;
        List<Spell> spells = known();
        if (mx >= x - 2 && mx < x + 130 && my >= top + 43 && my < top + 43 + SPELL_ROWS * ROW) {
            int row = (int) ((my - (top + 43)) / ROW) + scroll;
            if (row < spells.size()) {
                spell = spells.get(row);
                requestStudents();
                click();
                return true;
            }
        }
        if (canEdit && clickArrows(mx, my)) {
            return true;
        }
        if (canEdit && mx >= left + 180 && mx < left + 300 && my >= top + 165 && my < top + 175) {
            showArea = !showArea;
            sendArea();
            click();
            return true;
        }
        int bx = left + 180;
        int by = top + H - 30;
        if (mx >= bx && mx < bx + 120 && my >= by && my < by + 15 && spell != null && isProfessor() && cooldown() <= 0 && countCanLearn() > 0) {
            ModNetwork.CHANNEL.sendToServer(new PulpitLessonPacket(pos, spell.getId(), true));
            if (minecraft != null) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
            }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    private boolean clickArrows(double mx, double my) {
        float cx = left + 240;
        int cols = get(Side.LEFT) + get(Side.RIGHT) + 1;
        int rows = get(Side.FRONT) + get(Side.BACK) + 1;
        float cell = Math.min(PLAN_W / (float) cols, PLAN_H / (float) rows);
        int pw = Math.max(4, Math.round(cols * cell));
        int ph = Math.max(4, Math.round(rows * cell));
        int px = Math.round(cx - pw / 2f);
        int py = top + 54 + (PLAN_H - ph) / 2;
        int midY = py + ph / 2;
        int hy = top + 140;
        int[][] boxes = {
                {Side.FRONT.ordinal(), Math.round(cx) - 4, py - 13},
                {Side.BACK.ordinal(), Math.round(cx) - 4, py + ph + 5},
                {Side.LEFT.ordinal(), px - 13, midY - 4},
                {Side.RIGHT.ordinal(), px + pw + 5, midY - 4},
                {Side.UP.ordinal(), left + 230, hy},
                {Side.DOWN.ordinal(), left + 262, hy}};
        for (int[] b : boxes) {
            if (mx >= b[1] - 1 && mx < b[1] + 9 && my >= b[2] - 1 && my < b[2] + 9) {
                int delta = hasShiftDown() ? -1 : 1;
                int value = Math.max(0, Math.min(PulpitBlockEntity.MAX_EXTENT, extents[b[0]] + delta));
                if (value != extents[b[0]]) {
                    extents[b[0]] = value;
                    sendArea();
                    click();
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int max = Math.max(0, known().size() - SPELL_ROWS);
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(delta)));
        return true;
    }

    private static String tr(String key) {
        return Component.translatable(key).getString();
    }
}
