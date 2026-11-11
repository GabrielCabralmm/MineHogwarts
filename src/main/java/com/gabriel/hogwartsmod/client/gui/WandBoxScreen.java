package com.gabriel.hogwartsmod.client.gui;

import com.gabriel.hogwartsmod.client.WandPreview;
import com.gabriel.hogwartsmod.network.ChooseWandPacket;
import com.gabriel.hogwartsmod.network.ModNetwork;
import com.gabriel.hogwartsmod.wand.WandCore;
import com.gabriel.hogwartsmod.wand.WandHandle;
import com.gabriel.hogwartsmod.wand.WandStats;
import com.gabriel.hogwartsmod.wand.WandWood;
import com.gabriel.hogwartsmod.wand.Wands;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static com.gabriel.hogwartsmod.client.gui.MissionScreen.*;

public class WandBoxScreen extends Screen {
    private static final int W = 270;
    private static final int H = 200;
    private static final int ROW = 11;
    private static final int ARROW = 9;
    private static final int FIELD_W = 70;
    private static final int BTN_W = 112;
    private static final int BTN_H = 15;
    private static final int SMALL_W = 62;
    private static final ResourceLocation PAPER = MissionScreen.tex("carta_papel");

    private final InteractionHand hand;
    private final List<WandWood> woods = new ArrayList<>();
    private final List<WandCore> cores = new ArrayList<>();
    private int wood;
    private int core;
    private int quarters = 44;
    private boolean confirming;
    private boolean sent;
    private int left;
    private int top;

    public WandBoxScreen(InteractionHand hand) {
        super(Component.translatable("screen.hogwartsmod.wand_box"));
        this.hand = hand;
        for (WandWood w : WandWood.values()) {
            if (w.isCommon()) {
                woods.add(w);
            }
        }
        for (WandCore c : WandCore.values()) {
            if (c.isCommon()) {
                cores.add(c);
            }
        }
    }

    public static void open(InteractionHand hand) {
        Minecraft.getInstance().setScreen(new WandBoxScreen(hand));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private ItemStack preview() {
        return Wands.create(woods.get(wood), cores.get(core), quarters, WandHandle.NENHUM, null);
    }

    private int listX() {
        return left + 18;
    }

    private int woodY() {
        return top + 46;
    }

    private int coreY() {
        return woodY() + woods.size() * ROW + 16;
    }

    private int rightX() {
        return left + 132;
    }

    private int rightW() {
        return W - 132 - 18;
    }

    private static boolean over(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // -- Desenho

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        HiRes.image(g, PAPER, left, top, W, H);
        float cx = left + W / 2f;
        HiRes.centered(g, title.getString(), HiRes.Style.BOLD, 10f, cx, top + 12, TITLE);
        HiRes.centered(g, tr("screen.hogwartsmod.wand_box.subtitle"), HiRes.Style.ITALIC, 6.5f, cx, top + 24, MUTED);
        g.fill(left + 40, top + 33, left + W - 40, top + 34, LINE);
        int mx = confirming ? -1 : mouseX;
        int my = confirming ? -1 : mouseY;
        HiRes.text(g, tr("screen.hogwartsmod.bench.branch"), HiRes.Style.BOLD, 7.5f, listX(), woodY() - 10, INK);
        for (int i = 0; i < woods.size(); i++) {
            WandWood w = woods.get(i);
            int y = woodY() + i * ROW;
            option(g, mx, my, y, i == wood, w.getDisplayName().getString(), 0xFF000000 | w.getColor());
        }
        HiRes.text(g, tr("screen.hogwartsmod.bench.core"), HiRes.Style.BOLD, 7.5f, listX(), coreY() - 10, INK);
        for (int i = 0; i < cores.size(); i++) {
            WandCore c = cores.get(i);
            int y = coreY() + i * ROW;
            option(g, mx, my, y, i == core, c.getDisplayName().getString(), 0xFF000000 | c.getColor());
        }
        drawBox(g);
        drawLength(g, mx, my);
        drawSummary(g);
        drawButton(g, mx, my, left + (W - BTN_W) / 2 + 66, top + H - 28, BTN_W, tr("screen.hogwartsmod.wand_box.choose"));
        if (confirming) {
            drawConfirm(g, mouseX, mouseY);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void option(GuiGraphics g, int mx, int my, int y, boolean selected, String name, int color) {
        int x = listX();
        int w = 104;
        boolean hover = over(mx, my, x - 2, y - 2, w + 4, ROW);
        if (selected || hover) {
            HiRes.highlight(g, x - 2, y - 2, x + w + 2, y + ROW - 2, selected ? HiRes.Mark.SELECTED : HiRes.Mark.HOVER);
        }
        g.fill(x + 1, y, x + 8, y + 7, 0xFF5A4030);
        g.fill(x + 2, y + 1, x + 7, y + 6, color);
        HiRes.Style style = selected ? HiRes.Style.BOLD : HiRes.Style.REGULAR;
        HiRes.text(g, HiRes.ellipsize(name, style, (w - 12) * HiRes.TEXT_SIZE / 7f), style, 7f, x + 12, y, INK);
    }

    private void drawBox(GuiGraphics g) {
        int x = rightX();
        int y = top + 40;
        int w = rightW();
        int h = 44;
        g.fill(x, y, x + w, y + h, 0xFF4A2E1C);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF8A5E3A);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xFF6E4A2C);
        g.fill(x + 5, y + 5, x + w - 5, y + h - 5, 0xFF2A1418);
        g.fill(x + 5, y + 5, x + w - 5, y + 7, 0xFF1A0C10);
        g.fill(x + 5, y + h / 2 + 4, x + w - 5, y + h / 2 + 6, 0xFF3A1E24);
        WandPreview.render(g, preview(), x + w / 2f, y + h / 2f, w - 16, 25f, -15f);
    }

    private void drawLength(GuiGraphics g, int mx, int my) {
        int sx = rightX() + (rightW() - FIELD_W) / 2;
        int sy = top + 102;
        HiRes.centered(g, tr("screen.hogwartsmod.bench.length"), HiRes.Style.BOLD, 7f, sx + FIELD_W / 2f, sy - 10, INK);
        arrow(g, sx, sy, "‹", over(mx, my, sx, sy, ARROW, ARROW + 2));
        arrow(g, sx + FIELD_W - ARROW, sy, "›", over(mx, my, sx + FIELD_W - ARROW, sy, ARROW, ARROW + 2));
        g.fill(sx + ARROW + 2, sy, sx + FIELD_W - ARROW - 2, sy + ARROW + 2, 0xFFE6D4AE);
        g.fill(sx + ARROW + 2, sy + ARROW + 1, sx + FIELD_W - ARROW - 2, sy + ARROW + 2, 0xFFA48A62);
        HiRes.centered(g, Wands.formatLength(quarters) + "\"", HiRes.Style.BOLD, 7f, sx + FIELD_W / 2f, sy + 2, TITLE);
    }

    private static void arrow(GuiGraphics g, int x, int y, String glyph, boolean hover) {
        g.fill(x, y, x + ARROW, y + ARROW + 2, 0xFF6E4A2C);
        g.fill(x, y, x + ARROW, y + ARROW + 1, hover ? 0xFFB07A48 : 0xFF8E5E36);
        HiRes.centered(g, glyph, HiRes.Style.BOLD, 9f, x + ARROW / 2f, y - 1, 0xFFF6E6C8);
    }

    private void drawSummary(GuiGraphics g) {
        float cx = rightX() + rightW() / 2f;
        int y = top + 120;
        String line = woods.get(wood).getDisplayName().getString() + " · " + cores.get(core).getDisplayName().getString();
        HiRes.centered(g, HiRes.ellipsize(line, HiRes.Style.ITALIC, rightW() * HiRes.TEXT_SIZE / 6.5f), HiRes.Style.ITALIC, 6.5f, cx, y, INK);
        WandStats s = WandStats.of(woods.get(wood), cores.get(core), quarters);
        List<String> labels = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        stat(labels, colors, "damage", s.damage(), true);
        stat(labels, colors, "range", s.range(), true);
        stat(labels, colors, "cooldown", s.cooldown(), false);
        stat(labels, colors, "cost", s.cost(), false);
        stat(labels, colors, "shield", s.shield(), true);
        stat(labels, colors, "mastery", s.mastery(), true);
        int x = rightX() + 4;
        for (int i = 0; i < labels.size(); i++) {
            HiRes.text(g, labels.get(i), HiRes.Style.REGULAR, 6f, x + (i % 2) * (rightW() / 2), y + 10 + (i / 2) * 7, colors.get(i));
        }
    }

    private static void stat(List<String> labels, List<Integer> colors, String key, float value, boolean higherIsBetter) {
        int pct = Math.round((value - 1f) * 100f);
        if (pct == 0) {
            return;
        }
        labels.add((pct > 0 ? "+" : "−") + Math.abs(pct) + "% " + tr("tooltip.hogwartsmod.wand.stat." + key + ".short"));
        colors.add((pct > 0) == higherIsBetter ? GREEN : WINE);
    }

    private void drawButton(GuiGraphics g, int mx, int my, int x, int y, int w, String label) {
        boolean hover = over(mx, my, x, y, w, BTN_H - 2);
        HiRes.image(g, MissionScreen.tex("botao_couro_largo_" + (hover ? "h" : "n")), x, y, w, BTN_H);
        HiRes.centered(g, label, HiRes.Style.BOLD, 7f, x + w / 2f, y + 3, 0xFFF6E6C8);
    }

    private int boxX() {
        return left + (W - 190) / 2;
    }

    private int boxY() {
        return top + (H - 74) / 2;
    }

    private void drawConfirm(GuiGraphics g, int mouseX, int mouseY) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        g.fill(left + 6, top + 6, left + W - 6, top + H - 6, 0x99201410);
        int x = boxX();
        int y = boxY();
        HiRes.image(g, PAPER, x, y, 190, 74);
        HiRes.centered(g, tr("screen.hogwartsmod.wand_box.confirm_title"), HiRes.Style.BOLD, 8.5f, x + 95, y + 12, TITLE);
        HiRes.centered(g, tr("screen.hogwartsmod.wand_box.confirm_text"), HiRes.Style.ITALIC, 6.5f, x + 95, y + 26, MUTED);
        drawButton(g, mouseX, mouseY, x + 22, y + 46, SMALL_W, tr("screen.hogwartsmod.wand_box.confirm"));
        drawButton(g, mouseX, mouseY, x + 190 - 22 - SMALL_W, y + 46, SMALL_W, tr("screen.hogwartsmod.wand_box.back"));
        g.pose().popPose();
    }

    // -- Interação

    private void click() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        }
    }

    public void select(int wood, int core, int quarters) {
        this.wood = Math.floorMod(wood, woods.size());
        this.core = Math.floorMod(core, cores.size());
        this.quarters = Wands.clampLength(quarters);
    }

    public void setConfirming(boolean value) {
        confirming = value;
    }

    public void confirm() {
        if (sent) {
            return;
        }
        sent = true;
        ModNetwork.CHANNEL.sendToServer(new ChooseWandPacket(hand, woods.get(wood), cores.get(core), quarters));
        onClose();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) {
            return super.mouseClicked(mx, my, button);
        }
        if (confirming) {
            if (over(mx, my, boxX() + 22, boxY() + 46, SMALL_W, BTN_H - 2)) {
                click();
                confirm();
            } else if (over(mx, my, boxX() + 190 - 22 - SMALL_W, boxY() + 46, SMALL_W, BTN_H - 2)) {
                click();
                confirming = false;
            }
            return true;
        }
        for (int i = 0; i < woods.size(); i++) {
            if (over(mx, my, listX() - 2, woodY() + i * ROW - 2, 108, ROW)) {
                wood = i;
                click();
                return true;
            }
        }
        for (int i = 0; i < cores.size(); i++) {
            if (over(mx, my, listX() - 2, coreY() + i * ROW - 2, 108, ROW)) {
                core = i;
                click();
                return true;
            }
        }
        int sx = rightX() + (rightW() - FIELD_W) / 2;
        int sy = top + 102;
        int step = hasShiftDown() ? 4 : 1;
        if (over(mx, my, sx, sy, ARROW, ARROW + 2)) {
            quarters = Wands.clampLength(quarters - step);
            click();
            return true;
        }
        if (over(mx, my, sx + FIELD_W - ARROW, sy, ARROW, ARROW + 2)) {
            quarters = Wands.clampLength(quarters + step);
            click();
            return true;
        }
        if (over(mx, my, left + (W - BTN_W) / 2 + 66, top + H - 28, BTN_W, BTN_H - 2)) {
            click();
            confirming = true;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (confirming && key == 256) {
            confirming = false;
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }
}
