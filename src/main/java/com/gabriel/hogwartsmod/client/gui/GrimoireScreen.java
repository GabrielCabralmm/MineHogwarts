package com.gabriel.hogwartsmod.client.gui;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.magic.MagicData;
import com.gabriel.hogwartsmod.client.trace.ClientTeachState;
import com.gabriel.hogwartsmod.network.InviteStudentPacket;
import com.gabriel.hogwartsmod.network.PlaceEditPacket;
import net.minecraft.client.gui.components.EditBox;
import com.gabriel.hogwartsmod.network.ModNetwork;
import com.gabriel.hogwartsmod.network.RequestStudentsPacket;
import com.gabriel.hogwartsmod.network.StudentsPacket;
import com.gabriel.hogwartsmod.teach.StudentStatus;
import com.gabriel.hogwartsmod.teach.TeachRules;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
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
import java.util.UUID;

public class GrimoireScreen extends Screen {
    private static final int BOOK_W = 300;
    private static final int BOOK_H = 190;
    private static final int PER_PAGE = 9;
    private static final int TEACH_ROWS = 7;
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
    private static final int GREEN = 0xFF3F6B2A;
    private static final int WINE = 0xFF8E2A2A;

    private static final ResourceLocation BOOK =
            ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "textures/gui/grimoire.png");

    private enum Tab { SPELLS, TEACH, APPARITION, HOUSES, MISSIONS }

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
    @Nullable
    private EditBox nameBox;
    private int renaming = -1;
    private int publicPage;
    @Nullable
    private Spell teachSpell;
    @Nullable
    private UUID teachStudent;
    private int ticks;
    private int missionIndex;
    @org.jetbrains.annotations.Nullable
    private String cancelArmed;

    public GrimoireScreen() {
        super(Component.translatable("screen.hogwartsmod.grimoire"));
    }

    @Override
    protected void init() {
        left = (width - BOOK_W) / 2;
        top = (height - BOOK_H) / 2;
        nameBox = new EditBox(font, left + 19, top + BOOK_H - 41, BOOK_W / 2 - 32, 10, Component.empty());
        nameBox.setBordered(false);
        nameBox.setMaxLength(PlaceEditPacket.MAX_NAME);
        nameBox.setTextColor(0xFF38261A);
        nameBox.setHint(Component.translatable("screen.hogwartsmod.grimoire.place_hint").withStyle(s -> s.withColor(0xA08A6E)));
        nameBox.visible = tab == Tab.APPARITION;
        addRenderableWidget(nameBox);
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
            case TEACH -> drawTeachTab(g, mouseX, mouseY);
            case APPARITION -> drawApparitionTab(g, mouseX, mouseY);
            case HOUSES -> drawHousesTab(g);
            case MISSIONS -> drawMissionsTab(g, mouseX, mouseY);
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

    private boolean isLocked(Tab t) {
        MagicData data = data();
        return t == Tab.APPARITION && (data == null || !data.knows(Spells.APARATACAO));
    }

    private String tabLabel(Tab t) {
        return Component.translatable("screen.hogwartsmod.grimoire.tab." + t.name().toLowerCase()).getString();
    }

    private int tabHeight(Tab t) {
        return Math.round(HiRes.width(tabLabel(t), HiRes.Style.BOLD, TAB_TEXT_SIZE)) + TAB_PAD + (isLocked(t) ? LOCK_SPACE : 0);
    }

    private static boolean onLeft(Tab t) {
        return t == Tab.HOUSES || t == Tab.MISSIONS;
    }

    private int tabTop(Tab t) {
        int y = top + 10;
        for (Tab other : Tab.values()) {
            if (onLeft(other) != onLeft(t)) {
                continue;
            }
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
            int y0 = tabTop(t);
            int h = tabHeight(t);
            if (onLeft(t)) {
                int lx0 = left - (on ? 14 : 11);
                int lx1 = left + 10;
                g.fill(lx0, y0, lx1, y0 + h, NOTE_EDGE);
                g.fill(lx0 + 1, y0 + 1, lx1, y0 + h - 1, on ? NOTE_ON : NOTE);
                g.fill(lx0 + 1, y0 + h - 4, lx0 + 4, y0 + h - 1, NOTE_EDGE);
                g.pose().pushPose();
                g.pose().translate(lx0 + 1.5f, y0 + h - TAB_PAD / 2f, 0);
                g.pose().mulPose(Axis.ZP.rotationDegrees(-90));
                HiRes.text(g, tabLabel(t), HiRes.Style.BOLD, TAB_TEXT_SIZE, 0, 0, INK);
                g.pose().popPose();
                continue;
            }
            int x0 = left + BOOK_W - 10;
            int x1 = left + BOOK_W + (on ? 14 : 11);
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

    // -- Casas

    private static ResourceLocation crest(com.gabriel.hogwartsmod.house.House house) {
        return ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "textures/gui/casa_" + house.getId() + ".png");
    }

    private static int inkOf(com.gabriel.hogwartsmod.house.House house) {
        int rgb = house.getColor();
        int r = (rgb >> 16 & 255) * 3 / 4;
        int gr = (rgb >> 8 & 255) * 3 / 4;
        int b = (rgb & 255) * 3 / 4;
        return 0xFF000000 | r << 16 | gr << 8 | b;
    }

    private static int placeOf(int[] points, int value) {
        int place = 1;
        for (int p : points) {
            if (p > value) {
                place++;
            }
        }
        return place;
    }

    // -- Missões

    private List<com.gabriel.hogwartsmod.mission.ActiveMission> activeMissions() {
        MagicData data = data();
        return data == null ? List.of() : data.getMissions().active();
    }

    private int missionProgress(com.gabriel.hogwartsmod.mission.ActiveMission active) {
        com.gabriel.hogwartsmod.mission.Mission m = active.mission();
        if (m.type() == com.gabriel.hogwartsmod.mission.ObjectiveType.ENTREGAR && minecraft != null && minecraft.player != null) {
            int n = 0;
            for (net.minecraft.world.item.ItemStack stack : minecraft.player.getInventory().items) {
                if (stack.is(m.item())) {
                    n += stack.getCount();
                }
            }
            return Math.min(m.amount(), n);
        }
        return Math.min(m.amount(), active.progress());
    }

    private void drawMissionsTab(GuiGraphics g, int mouseX, int mouseY) {
        int lx = left + 16;
        int pageW = BOOK_W / 2 - 26;
        HiRes.centered(g, tr("screen.hogwartsmod.grimoire.missions"), HiRes.Style.BOLD, lx + pageW / 2f, top + 17, TITLE);
        List<com.gabriel.hogwartsmod.mission.ActiveMission> list = activeMissions();
        if (list.isEmpty()) {
            HiRes.centered(g, tr("screen.hogwartsmod.grimoire.no_missions1"), HiRes.Style.ITALIC, lx + pageW / 2f, top + 60, MUTED);
            HiRes.centered(g, tr("screen.hogwartsmod.grimoire.no_missions2"), HiRes.Style.ITALIC, lx + pageW / 2f, top + 71, MUTED);
            return;
        }
        missionIndex = Math.min(missionIndex, list.size() - 1);
        for (int i = 0; i < list.size(); i++) {
            com.gabriel.hogwartsmod.mission.ActiveMission a = list.get(i);
            int y = top + 31 + i * 27;
            boolean hover = mouseX >= lx - 2 && mouseX < lx + pageW + 2 && mouseY >= y - 2 && mouseY < y + 24;
            if (i == missionIndex || hover) {
                HiRes.highlight(g, lx - 2, y - 2, lx + pageW + 2, y + 24, i == missionIndex ? HiRes.Mark.SELECTED : HiRes.Mark.HOVER);
            }
            int progress = missionProgress(a);
            boolean ready = progress >= a.mission().amount();
            boolean hoverBox = mouseX >= lx + 1 && mouseX < lx + 12 && mouseY >= y - 1 && mouseY < y + 10;
            g.fill(lx + 3, y, lx + 11, y + 8, hoverBox ? GOLD : NOTE_EDGE);
            g.fill(lx + 4, y + 1, lx + 10, y + 7, NOTE_ON);
            if (a.tracked()) {
                HiRes.image(g, ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "textures/gui/check.png"), lx + 3, y - 1, 9, 9);
            }
            float scale = HiRes.TEXT_SIZE / 7f;
            HiRes.text(g, HiRes.ellipsize(a.mission().title().isBlank() ? "?" : a.mission().title(), HiRes.Style.BOLD, (pageW - 40) * scale),
                    HiRes.Style.BOLD, 7f, lx + 15, y + 1, a.tracked() ? INK : MUTED);
            if (ready) {
                HiRes.image(g, ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "textures/gui/check.png"), lx + pageW - 8, y, 8, 8);
            } else {
                String count = progress + "/" + a.mission().amount();
                HiRes.text(g, count, HiRes.Style.BOLD, 7f, lx + pageW - HiRes.width(count, HiRes.Style.BOLD, 7f), y + 1, GOLD);
            }
            HiRes.text(g, HiRes.ellipsize(a.npcName(), HiRes.Style.ITALIC, (pageW - 18) * HiRes.TEXT_SIZE / 6.5f), HiRes.Style.ITALIC, 6.5f, lx + 15, y + 10, MUTED);
            int bx = lx + 3;
            int bw = pageW - 6;
            HiRes.bar(g, bx, y + 19, bw, 3, progress / (float) a.mission().amount(), PAGE_LINE, ready ? GREEN : GOLD);
        }

        HiRes.centered(g, tr("screen.hogwartsmod.grimoire.track_hint"), HiRes.Style.ITALIC, 6.5f, lx + pageW / 2f, top + BOOK_H - 20, MUTED);

        com.gabriel.hogwartsmod.mission.ActiveMission a = list.get(missionIndex);
        com.gabriel.hogwartsmod.mission.Mission m = a.mission();
        int rx = left + BOOK_W / 2 + 10;
        int rw = BOOK_W / 2 - 26;
        float cx = rx + rw / 2f;
        HiRes.centered(g, HiRes.ellipsize(m.title().isBlank() ? "?" : m.title(), HiRes.Style.BOLD, rw), HiRes.Style.BOLD, 10f, cx, top + 17, TITLE);
        HiRes.centered(g, Component.translatable("screen.hogwartsmod.grimoire.mission_from", a.npcName()).getString(), HiRes.Style.ITALIC, cx, top + 31, MUTED);
        int y = top + 46;
        for (String line : HiRes.wrap(m.description(), HiRes.Style.ITALIC, rw)) {
            if (y > top + 86) {
                break;
            }
            HiRes.centered(g, line, HiRes.Style.ITALIC, cx, y, INK);
            y += 10;
        }
        y += 6;
        HiRes.text(g, tr("screen.hogwartsmod.missions.objective"), HiRes.Style.BOLD, rx, y, INK);
        y += 11;
        for (String line : HiRes.wrap(m.objectiveText().getString(), HiRes.Style.REGULAR, rw)) {
            HiRes.text(g, line, HiRes.Style.REGULAR, rx, y, INK);
            y += 10;
        }
        y += 3;
        HiRes.text(g, tr("screen.hogwartsmod.missions.reward"), HiRes.Style.BOLD, rx, y, INK);
        int rewardEnd = y + 11 + MissionScreen.drawReward(g, rx, y + 11, m);
        boolean ready = missionProgress(a) >= m.amount();
        String footer = ready ? Component.translatable("hud.hogwartsmod.mission_ready", a.npcName()).getString()
                : tr("screen.hogwartsmod.grimoire.mission_going");
        int footerY = top + BOOK_H - 32;
        if (rewardEnd <= footerY - 2) {
            HiRes.centered(g, HiRes.ellipsize(footer, HiRes.Style.ITALIC, rw * HiRes.TEXT_SIZE / 7f), HiRes.Style.ITALIC, 7f, cx, footerY, ready ? GREEN : MUTED);
        }
        boolean armed = a.key().equals(cancelArmed);
        String cancel = tr(armed ? "screen.hogwartsmod.grimoire.cancel_confirm" : "screen.hogwartsmod.grimoire.cancel");
        float cw = HiRes.width(cancel, HiRes.Style.BOLD, 7.5f);
        int cy = top + BOOK_H - 20;
        boolean hoverCancel = mouseX >= cx - cw / 2 - 3 && mouseX < cx + cw / 2 + 3 && mouseY >= cy - 2 && mouseY < cy + 9;
        if (hoverCancel || armed) {
            HiRes.highlight(g, Math.round(cx - cw / 2 - 6), cy - 3, Math.round(cx + cw / 2 + 6), cy + 10, HiRes.Mark.HOVER);
        }
        HiRes.centered(g, cancel, HiRes.Style.BOLD, 7.5f, cx, cy, armed || hoverCancel ? WINE : TITLE);
    }

    private boolean missionsClicked(double mx, double my) {
        int lx = left + 16;
        int pageW = BOOK_W / 2 - 26;
        List<com.gabriel.hogwartsmod.mission.ActiveMission> list = activeMissions();
        for (int i = 0; i < list.size(); i++) {
            int y = top + 31 + i * 27;
            com.gabriel.hogwartsmod.mission.ActiveMission a = list.get(i);
            if (mx >= lx + 1 && mx < lx + 12 && my >= y - 1 && my < y + 10) {
                journal(a, a.tracked() ? com.gabriel.hogwartsmod.mission.MissionManager.JournalAction.HIDE
                        : com.gabriel.hogwartsmod.mission.MissionManager.JournalAction.SHOW);
                return true;
            }
            if (mx >= lx - 2 && mx < lx + pageW + 2 && my >= y - 2 && my < y + 24) {
                if (missionIndex != i) {
                    cancelArmed = null;
                }
                missionIndex = i;
                return true;
            }
        }
        if (list.isEmpty()) {
            return false;
        }
        com.gabriel.hogwartsmod.mission.ActiveMission a = list.get(Math.min(missionIndex, list.size() - 1));
        float cx = left + BOOK_W / 2 + 10 + (BOOK_W / 2 - 26) / 2f;
        boolean armed = a.key().equals(cancelArmed);
        float cw = HiRes.width(tr(armed ? "screen.hogwartsmod.grimoire.cancel_confirm" : "screen.hogwartsmod.grimoire.cancel"), HiRes.Style.BOLD, 7.5f);
        int cy = top + BOOK_H - 20;
        if (mx >= cx - cw / 2 - 3 && mx < cx + cw / 2 + 3 && my >= cy - 2 && my < cy + 9) {
            if (armed) {
                cancelArmed = null;
                journal(a, com.gabriel.hogwartsmod.mission.MissionManager.JournalAction.ABANDON);
            } else {
                cancelArmed = a.key();
                click();
            }
            return true;
        }
        cancelArmed = null;
        return false;
    }

    private void click() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.get(), 1.4f, 0.35f));
        }
    }

    private void journal(com.gabriel.hogwartsmod.mission.ActiveMission a, com.gabriel.hogwartsmod.mission.MissionManager.JournalAction action) {
        com.gabriel.hogwartsmod.network.ModNetwork.CHANNEL.sendToServer(new com.gabriel.hogwartsmod.network.MissionJournalPacket(a.npc(), a.mission().id(), action));
        click();
    }

    private void drawHousesTab(GuiGraphics g) {
        int lx = left + 16;
        int pageW = BOOK_W / 2 - 26;
        HiRes.centered(g, tr("screen.hogwartsmod.grimoire.cup"), HiRes.Style.BOLD, lx + pageW / 2f, top + 17, TITLE);
        int[] points = com.gabriel.hogwartsmod.house.HousePoints.clientPoints;
        List<com.gabriel.hogwartsmod.house.House> order = new ArrayList<>(List.of(com.gabriel.hogwartsmod.house.House.values()));
        order.sort((a, b) -> points[b.ordinal()] - points[a.ordinal()]);
        int max = 1;
        for (int p : points) {
            max = Math.max(max, p);
        }
        boolean leader = points[order.get(0).ordinal()] > 0 && points[order.get(0).ordinal()] > points[order.get(1).ordinal()];
        for (int i = 0; i < order.size(); i++) {
            com.gabriel.hogwartsmod.house.House house = order.get(i);
            int y = top + 32 + i * 34;
            int value = points[house.ordinal()];
            boolean first = i == 0 && leader;
            if (first) {
                HiRes.highlight(g, lx - 3, y - 3, lx + pageW + 3, y + 30, HiRes.Mark.SELECTED);
            }
            HiRes.image(g, crest(house), lx, y, 23, 27);
            String place = value > 0 ? placeOf(points, value) + "º" : "–";
            HiRes.text(g, place, HiRes.Style.BOLD, lx + 28, y + 2, first ? GOLD : MUTED);
            HiRes.text(g, house.getDisplayName().getString(), HiRes.Style.BOLD, lx + 42, y + 2, inkOf(house));
            HiRes.right(g, String.valueOf(value), HiRes.Style.BOLD, lx + pageW, y + 2, INK);
            int barX = lx + 28;
            int barW = pageW - 28;
            int barY = y + 16;
            g.fill(barX, barY, barX + barW, barY + 5, NOTE_EDGE);
            g.fill(barX + 1, barY + 1, barX + barW - 1, barY + 4, SLOT_BG);
            int filled = Math.round((barW - 2) * (value / (float) max));
            if (value > 0) {
                g.fill(barX + 1, barY + 1, barX + 1 + filled, barY + 4, 0xFF000000 | house.getColor());
            }
        }

        int rx = left + BOOK_W / 2 + 10;
        int rw = BOOK_W / 2 - 26;
        float cx = rx + rw / 2f;
        HiRes.centered(g, tr("screen.hogwartsmod.grimoire.your_house"), HiRes.Style.BOLD, cx, top + 17, TITLE);
        com.gabriel.hogwartsmod.house.House mine = minecraft != null && minecraft.player != null
                ? com.gabriel.hogwartsmod.house.HouseManager.of(minecraft.player) : null;
        if (mine == null) {
            HiRes.centered(g, tr("screen.hogwartsmod.grimoire.no_house1"), HiRes.Style.ITALIC, cx, top + 60, MUTED);
            HiRes.centered(g, tr("screen.hogwartsmod.grimoire.no_house2"), HiRes.Style.ITALIC, cx, top + 71, MUTED);
        } else {
            HiRes.image(g, crest(mine), Math.round(cx - 28), top + 30, 56, 66);
            HiRes.centered(g, mine.getDisplayName().getString(), HiRes.Style.BOLD, 10f, cx, top + 101, inkOf(mine));
            int own = points[mine.ordinal()];
            String line = own > 0 ? Component.translatable("screen.hogwartsmod.grimoire.house_points", own, placeOf(points, own)).getString()
                    : Component.translatable("screen.hogwartsmod.grimoire.house_points_zero").getString();
            HiRes.centered(g, line, HiRes.Style.ITALIC, cx, top + 115, MUTED);
        }
        g.fill(rx, top + 130, rx + rw, top + 131, PAGE_LINE);
        com.gabriel.hogwartsmod.house.House last = com.gabriel.hogwartsmod.house.HousePoints.clientLastWinner;
        HiRes.centered(g, tr("screen.hogwartsmod.grimoire.last_cup"), HiRes.Style.BOLD, cx, top + 136, INK);
        if (last == null) {
            HiRes.centered(g, tr("screen.hogwartsmod.grimoire.no_cup"), HiRes.Style.ITALIC, cx, top + 149, MUTED);
        } else {
            String label = last.getDisplayName().getString();
            float w = HiRes.width(label, HiRes.Style.BOLD) + 14;
            HiRes.image(g, crest(last), Math.round(cx - w / 2), top + 147, 10, 12);
            HiRes.text(g, label, HiRes.Style.BOLD, cx - w / 2 + 14, top + 149, inkOf(last));
            int cups = com.gabriel.hogwartsmod.house.HousePoints.clientCups[last.ordinal()];
            HiRes.centered(g, Component.translatable("screen.hogwartsmod.grimoire.cups", cups).getString(), HiRes.Style.ITALIC, cx, top + 162, MUTED);
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
                HiRes.highlight(g, lx - 2, y - 1, lx + pageW + 2, y + ROW_H - 1, spell == armed ? HiRes.Mark.ARMED : spell == selected ? HiRes.Mark.SELECTED : HiRes.Mark.HOVER);
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

    private List<Spell> teachable() {
        List<Spell> list = new ArrayList<>();
        MagicData data = data();
        if (data != null) {
            for (Spell spell : learned()) {
                if (TeachRules.isListed(data, spell, ClientTeachState.limits)) {
                    list.add(spell);
                }
            }
        }
        return list;
    }

    private long gameTime() {
        return minecraft != null && minecraft.level != null ? minecraft.level.getGameTime() : 0;
    }

    private List<StudentsPacket.Entry> students() {
        return teachSpell != null && teachSpell.getId().equals(ClientTeachState.spell) ? ClientTeachState.students : List.of();
    }

    private void drawTeachTab(GuiGraphics g, int mouseX, int mouseY) {
        MagicData data = data();
        if (data == null) {
            return;
        }
        int lx = left + 16;
        int pageW = BOOK_W / 2 - 26;
        HiRes.centered(g, tr("screen.hogwartsmod.grimoire.teachable"), HiRes.Style.BOLD, lx + pageW / 2f, top + 17, TITLE);
        List<Spell> spells = teachable();
        if (teachSpell != null && !spells.contains(teachSpell)) {
            teachSpell = null;
        }
        long now = gameTime();
        for (int i = 0; i < Math.min(TEACH_ROWS, spells.size()); i++) {
            Spell spell = spells.get(i);
            int y = top + 30 + i * ROW_H;
            boolean hover = mouseX >= lx && mouseX < lx + pageW && mouseY >= y && mouseY < y + ROW_H;
            if (spell == teachSpell || hover) {
                HiRes.highlight(g, lx - 2, y - 1, lx + pageW + 2, y + ROW_H - 1, spell == teachSpell ? HiRes.Mark.SELECTED : HiRes.Mark.HOVER);
            }
            SpellIcons.draw(g, spell, lx, y, 12);
            int remaining = data.getTeachCooldownRemaining(spell, now);
            String status = remaining > 0 ? formatTime(remaining) : tr("screen.hogwartsmod.grimoire.ready");
            float statusW = HiRes.width(status, HiRes.Style.REGULAR);
            String name = HiRes.ellipsize(spell.getDisplayName().getString(), HiRes.Style.REGULAR, pageW - 16 - statusW - 4);
            HiRes.text(g, name, HiRes.Style.REGULAR, lx + 16, y + 1.5f, INK);
            HiRes.right(g, status, remaining > 0 ? HiRes.Style.REGULAR : HiRes.Style.BOLD, lx + pageW, y + 1.5f, remaining > 0 ? MUTED : GREEN);
        }
        if (spells.isEmpty()) {
            String key = !data.isProfessor() && data.getLevel() < ClientTeachState.limits.minLevel()
                    ? Component.translatable("screen.hogwartsmod.grimoire.teach_level", ClientTeachState.limits.minLevel()).getString()
                    : tr("screen.hogwartsmod.grimoire.teach_none");
            wrapped(g, key, HiRes.Style.ITALIC, lx + pageW / 2f, top + 60, pageW, MUTED);
        }
        String rule = data.isProfessor() ? tr("screen.hogwartsmod.grimoire.teach_rule_professor")
                : Component.translatable("screen.hogwartsmod.grimoire.teach_rule", ClientTeachState.limits.maxSpellLevel()).getString();
        List<String> ruleLines = HiRes.wrap(rule, HiRes.Style.ITALIC, pageW);
        for (int i = 0; i < ruleLines.size(); i++) {
            HiRes.centered(g, ruleLines.get(i), HiRes.Style.ITALIC, lx + pageW / 2f, top + BOOK_H - 34 + i * 10 - (ruleLines.size() - 1) * 5, MUTED);
        }

        int rx = left + BOOK_W / 2 + 10;
        int rw = BOOK_W / 2 - 26;
        HiRes.centered(g, tr("screen.hogwartsmod.grimoire.students"), HiRes.Style.BOLD, rx + rw / 2f, top + 17, TITLE);
        if (teachSpell == null) {
            wrapped(g, tr("screen.hogwartsmod.grimoire.pick_spell"), HiRes.Style.ITALIC, rx + rw / 2f, top + 60, rw, MUTED);
            return;
        }
        List<StudentsPacket.Entry> students = students();
        if (students.isEmpty()) {
            wrapped(g, Component.translatable("screen.hogwartsmod.grimoire.no_students", ClientTeachState.range).getString(),
                    HiRes.Style.ITALIC, rx + rw / 2f, top + 60, rw, MUTED);
        }
        for (int i = 0; i < Math.min(PER_PAGE - 1, students.size()); i++) {
            StudentsPacket.Entry entry = students.get(i);
            int y = top + 30 + i * ROW_H;
            boolean hover = mouseX >= rx && mouseX < rx + rw && mouseY >= y && mouseY < y + ROW_H;
            boolean pickable = entry.status() == StudentStatus.CAN_LEARN;
            if (entry.id().equals(teachStudent) || hover && pickable) {
                HiRes.highlight(g, rx - 2, y - 1, rx + rw + 2, y + ROW_H - 1, entry.id().equals(teachStudent) ? HiRes.Mark.SELECTED : HiRes.Mark.HOVER);
            }
            String status = tr(entry.status().key());
            float statusW = HiRes.width(status, HiRes.Style.ITALIC);
            String name = HiRes.ellipsize(entry.name(), HiRes.Style.REGULAR, rw - statusW - 6);
            HiRes.text(g, name, HiRes.Style.REGULAR, rx, y + 1.5f, pickable ? INK : MUTED);
            HiRes.right(g, status, HiRes.Style.ITALIC, rx + rw, y + 1.5f, statusColor(entry.status()));
        }
        boolean onCooldown = data.getTeachCooldownRemaining(teachSpell, now) > 0;
        boolean ready = !onCooldown && selectedStudent() != null;
        int by = buttonY();
        boolean hover = ready && mouseX >= rx && mouseX < rx + rw && mouseY >= by && mouseY < by + 15;
        g.fill(rx, by, rx + rw, by + 15, ready ? COVER : NOTE_EDGE);
        g.fill(rx + 1, by + 1, rx + rw - 1, by + 14, hover ? NOTE_ON : ready ? NOTE : SLOT_BG);
        String label = onCooldown ? tr("screen.hogwartsmod.grimoire.on_cooldown") : tr("screen.hogwartsmod.grimoire.invite");
        HiRes.centered(g, label, HiRes.Style.BOLD, rx + rw / 2f, by + 3, ready ? TITLE : MUTED);
    }

    private int buttonY() {
        return top + BOOK_H - 38;
    }

    @Nullable
    private StudentsPacket.Entry selectedStudent() {
        for (StudentsPacket.Entry entry : students()) {
            if (entry.id().equals(teachStudent) && entry.status() == StudentStatus.CAN_LEARN) {
                return entry;
            }
        }
        return null;
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

    private void requestStudents() {
        if (teachSpell != null) {
            ModNetwork.CHANNEL.sendToServer(new RequestStudentsPacket(teachSpell.getId()));
        }
    }

    @Override
    public void tick() {
        ticks++;
        if (tab == Tab.TEACH && ticks % 20 == 0) {
            requestStudents();
        }
    }

    private boolean teachClicked(double mx, double my) {
        int lx = left + 16;
        int pageW = BOOK_W / 2 - 26;
        List<Spell> spells = teachable();
        if (mx >= lx - 2 && mx < lx + pageW + 2 && my >= top + 30) {
            int row = (int) ((my - (top + 30)) / ROW_H);
            if (row < Math.min(TEACH_ROWS, spells.size())) {
                teachSpell = spells.get(row);
                teachStudent = null;
                requestStudents();
                return true;
            }
        }
        int rx = left + BOOK_W / 2 + 10;
        int rw = BOOK_W / 2 - 26;
        if (teachSpell == null || mx < rx - 2 || mx >= rx + rw + 2) {
            return false;
        }
        int by = buttonY();
        if (my >= by && my < by + 15) {
            StudentsPacket.Entry student = selectedStudent();
            MagicData data = data();
            if (student != null && data != null && data.getTeachCooldownRemaining(teachSpell, gameTime()) <= 0) {
                ModNetwork.CHANNEL.sendToServer(new InviteStudentPacket(teachSpell.getId(), student.id()));
                teachStudent = null;
                if (minecraft != null) {
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
                }
            }
            return true;
        }
        if (my >= top + 30) {
            int row = (int) ((my - (top + 30)) / ROW_H);
            List<StudentsPacket.Entry> students = students();
            if (row < Math.min(PER_PAGE - 1, students.size()) && students.get(row).status() == StudentStatus.CAN_LEARN) {
                teachStudent = students.get(row).id();
                return true;
            }
        }
        return false;
    }

    // -- Aba Aparatação

    private static final int PLACE_ROW = 21;
    private static final int PUBLIC_ROWS = 6;

    private String placeDetail(com.gabriel.hogwartsmod.apparition.ApparitionPoint point) {
        if (minecraft == null || minecraft.player == null || minecraft.level == null) {
            return "";
        }
        if (!point.dimension().equals(minecraft.level.dimension().location().toString())) {
            return tr("screen.hogwartsmod.wheel.other_dimension");
        }
        double distance = minecraft.player.position().distanceTo(point.position());
        return Component.translatable("screen.hogwartsmod.grimoire.place_detail", Math.round(distance),
                Math.round(com.gabriel.hogwartsmod.spell.impl.AparatacaoSpell.costFor(distance))).getString();
    }

    private void drawApparitionTab(GuiGraphics g, int mouseX, int mouseY) {
        MagicData data = data();
        if (data == null) {
            return;
        }
        int lx = left + 16;
        int pageW = BOOK_W / 2 - 26;
        List<com.gabriel.hogwartsmod.apparition.ApparitionPoint> places = data.getPlaces();
        int limit = Spells.APARATACAO.getPlaceLimit(data);
        HiRes.centered(g, Component.translatable("screen.hogwartsmod.grimoire.my_places", places.size(), limit).getString(),
                HiRes.Style.BOLD, lx + pageW / 2f, top + 17, TITLE);
        for (int i = 0; i < places.size(); i++) {
            var point = places.get(i);
            int y = top + 30 + i * PLACE_ROW;
            if (i == renaming) {
                HiRes.highlight(g, lx - 2, y - 1, lx + pageW + 2, y + PLACE_ROW - 2, HiRes.Mark.SELECTED);
            }
            String rename = tr("screen.hogwartsmod.grimoire.rename");
            String forget = tr("screen.hogwartsmod.grimoire.forget");
            float fw = HiRes.width(forget, HiRes.Style.BOLD, 7f);
            float rw = HiRes.width(rename, HiRes.Style.BOLD, 7f);
            boolean hoverForget = mouseY >= y - 1 && mouseY < y + 8 && mouseX >= lx + pageW - fw && mouseX < lx + pageW;
            boolean hoverRename = mouseY >= y - 1 && mouseY < y + 8 && mouseX >= lx + pageW - fw - 6 - rw && mouseX < lx + pageW - fw - 6;
            HiRes.text(g, HiRes.ellipsize(point.name(), HiRes.Style.REGULAR, pageW - fw - rw - 10), HiRes.Style.REGULAR, lx, y, INK);
            HiRes.text(g, placeDetail(point), HiRes.Style.ITALIC, 7f, lx, y + 9.5f, MUTED);
            HiRes.text(g, forget, HiRes.Style.BOLD, 7f, lx + pageW - fw, y + 1, hoverForget ? WINE : TITLE);
            HiRes.text(g, rename, HiRes.Style.BOLD, 7f, lx + pageW - fw - 6 - rw, y + 1, hoverRename ? GOLD : TITLE);
        }
        if (places.isEmpty()) {
            wrapped(g, tr("screen.hogwartsmod.grimoire.no_places"), HiRes.Style.ITALIC, lx + pageW / 2f, top + 60, pageW, MUTED);
        }
        int boxY = top + BOOK_H - 44;
        g.fill(lx, boxY, lx + pageW, boxY + 13, NOTE_EDGE);
        g.fill(lx + 1, boxY + 1, lx + pageW - 1, boxY + 12, nameBox != null && nameBox.isFocused() ? NOTE_ON : SLOT_BG);
        int by = top + BOOK_H - 28;
        boolean full = renaming < 0 && places.size() >= limit;
        boolean hover = !full && mouseX >= lx && mouseX < lx + pageW && mouseY >= by && mouseY < by + 14;
        g.fill(lx, by, lx + pageW, by + 14, full ? NOTE_EDGE : COVER);
        g.fill(lx + 1, by + 1, lx + pageW - 1, by + 13, hover ? NOTE_ON : full ? SLOT_BG : NOTE);
        String label = renaming >= 0 ? tr("screen.hogwartsmod.grimoire.save_name")
                : full ? tr("screen.hogwartsmod.grimoire.places_full") : tr("screen.hogwartsmod.grimoire.memorize");
        HiRes.centered(g, label, HiRes.Style.BOLD, lx + pageW / 2f, by + 3, full ? MUTED : TITLE);

        int rx = left + BOOK_W / 2 + 10;
        int rw = BOOK_W / 2 - 26;
        HiRes.centered(g, tr("screen.hogwartsmod.grimoire.public_places"), HiRes.Style.BOLD, rx + rw / 2f, top + 17, TITLE);
        List<com.gabriel.hogwartsmod.apparition.ApparitionPoint> publics = com.gabriel.hogwartsmod.client.apparition.ClientApparitionState.publicPoints;
        int pages = Math.max(1, (publics.size() + PUBLIC_ROWS - 1) / PUBLIC_ROWS);
        publicPage = Math.min(publicPage, pages - 1);
        for (int i = 0; i < PUBLIC_ROWS; i++) {
            int index = publicPage * PUBLIC_ROWS + i;
            if (index >= publics.size()) {
                break;
            }
            var point = publics.get(index);
            int y = top + 30 + i * (PLACE_ROW - 1);
            HiRes.text(g, HiRes.ellipsize(point.name(), HiRes.Style.REGULAR, rw), HiRes.Style.REGULAR, rx, y, INK);
            HiRes.text(g, placeDetail(point), HiRes.Style.ITALIC, 7f, rx, y + 9.5f, MUTED);
        }
        if (publics.isEmpty()) {
            wrapped(g, tr("screen.hogwartsmod.grimoire.no_public"), HiRes.Style.ITALIC, rx + rw / 2f, top + 60, rw, MUTED);
        }
        if (pages > 1) {
            HiRes.centered(g, "‹  " + (publicPage + 1) + " / " + pages + "  ›", HiRes.Style.BOLD, rx + rw / 2f, top + BOOK_H - 24, TITLE);
        }
    }

    private void submitPlace() {
        if (nameBox == null) {
            return;
        }
        String name = nameBox.getValue().strip();
        if (name.isEmpty()) {
            nameBox.setFocused(true);
            return;
        }
        if (renaming >= 0) {
            ModNetwork.CHANNEL.sendToServer(new PlaceEditPacket(PlaceEditPacket.Action.RENAME, renaming, name));
            renaming = -1;
        } else {
            ModNetwork.CHANNEL.sendToServer(new PlaceEditPacket(PlaceEditPacket.Action.MEMORIZE, -1, name));
        }
        nameBox.setValue("");
        nameBox.setFocused(false);
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
        }
    }

    private boolean apparitionClicked(double mx, double my) {
        MagicData data = data();
        if (data == null || nameBox == null) {
            return false;
        }
        int lx = left + 16;
        int pageW = BOOK_W / 2 - 26;
        int boxY = top + BOOK_H - 44;
        if (mx >= lx && mx < lx + pageW && my >= boxY && my < boxY + 13) {
            nameBox.setFocused(true);
            setFocused(nameBox);
            return true;
        }
        nameBox.setFocused(false);
        int by = top + BOOK_H - 28;
        if (mx >= lx && mx < lx + pageW && my >= by && my < by + 14) {
            if (renaming >= 0 || data.getPlaces().size() < Spells.APARATACAO.getPlaceLimit(data)) {
                submitPlace();
            }
            return true;
        }
        List<com.gabriel.hogwartsmod.apparition.ApparitionPoint> places = data.getPlaces();
        for (int i = 0; i < places.size(); i++) {
            int y = top + 30 + i * PLACE_ROW;
            if (my < y - 1 || my >= y + 8) {
                continue;
            }
            float fw = HiRes.width(tr("screen.hogwartsmod.grimoire.forget"), HiRes.Style.BOLD, 7f);
            float rw = HiRes.width(tr("screen.hogwartsmod.grimoire.rename"), HiRes.Style.BOLD, 7f);
            if (mx >= lx + pageW - fw && mx < lx + pageW) {
                ModNetwork.CHANNEL.sendToServer(new PlaceEditPacket(PlaceEditPacket.Action.FORGET, i, ""));
                renaming = -1;
                return true;
            }
            if (mx >= lx + pageW - fw - 6 - rw && mx < lx + pageW - fw - 6) {
                renaming = i;
                nameBox.setValue(places.get(i).name());
                nameBox.setFocused(true);
                setFocused(nameBox);
                return true;
            }
        }
        int rx = left + BOOK_W / 2 + 10;
        int rw = BOOK_W / 2 - 26;
        int pages = Math.max(1, (com.gabriel.hogwartsmod.client.apparition.ClientApparitionState.publicPoints.size() + PUBLIC_ROWS - 1) / PUBLIC_ROWS);
        if (pages > 1 && my >= top + BOOK_H - 26 && my < top + BOOK_H - 14 && mx >= rx && mx < rx + rw) {
            publicPage = mx < rx + rw / 2f ? Math.max(0, publicPage - 1) : Math.min(pages - 1, publicPage + 1);
            return true;
        }
        return false;
    }

    private void drawPagination(GuiGraphics g, int cx, int y) {
        int count = pageCount();
        int startX = cx - (count - 1) * 7;
        for (int i = 0; i < count; i++) {
            int x = startX + i * 14;
            if (i == page) {
                HiRes.highlight(g, x - 5, y - 2, x + 6, y + 9, HiRes.Mark.SELECTED);
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
            boolean inX = onLeft(t) ? mx >= left - 14 && mx < left + 10 : mx >= left + BOOK_W - 10 && mx < left + BOOK_W + 14;
            if (inX && my >= y0 && my < y0 + tabHeight(t)) {
                if (!isLocked(t)) {
                    tab = t;
                    armed = null;
                    if (t == Tab.TEACH) {
                        requestStudents();
                    }
                    if (nameBox != null) {
                        nameBox.visible = t == Tab.APPARITION;
                        nameBox.setFocused(false);
                    }
                }
                return true;
            }
        }
        if (tab == Tab.TEACH && button == 0 && teachClicked(mx, my)) {
            return true;
        }
        if (tab == Tab.APPARITION && button == 0 && apparitionClicked(mx, my)) {
            return true;
        }
        if (tab == Tab.MISSIONS && button == 0 && missionsClicked(mx, my)) {
            return true;
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
        if ((pressSpell != null && !pressSpell.isAbility() || pressSlot >= 0) && !dragging && Math.abs(mx - pressX) + Math.abs(my - pressY) > 3) {
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
                armed = armed == pressSpell || pressSpell.isAbility() ? null : pressSpell;
            }
            pressSpell = null;
            pressSlot = -1;
            dragging = false;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (nameBox != null && nameBox.isFocused() && nameBox.visible) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
                submitPlace();
                return true;
            }
            if (keyCode != org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                return nameBox.keyPressed(keyCode, scanCode, modifiers) || nameBox.canConsumeInput();
            }
        }
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
