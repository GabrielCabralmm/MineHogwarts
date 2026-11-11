package com.gabriel.hogwartsmod.client.gui;

import com.gabriel.hogwartsmod.network.CraftWandPacket;
import com.gabriel.hogwartsmod.network.ModNetwork;
import com.gabriel.hogwartsmod.wand.BranchItem;
import com.gabriel.hogwartsmod.wand.WandBenchMenu;
import com.gabriel.hogwartsmod.wand.WandCore;
import com.gabriel.hogwartsmod.wand.WandHandle;
import com.gabriel.hogwartsmod.wand.WandStats;
import com.gabriel.hogwartsmod.wand.Wands;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static com.gabriel.hogwartsmod.client.gui.MissionScreen.*;

public class WandBenchScreen extends AbstractContainerScreen<WandBenchMenu> {
    private static final ResourceLocation PAPER = MissionScreen.tex("carta_papel");
    private static final int BTN_W = 54;
    private static final int BTN_H = 12;
    private static final int ARROW = 9;
    private static final long HOLD_MS = 2000;
    private static final int ROW_Y = 92;
    private static final int LEN_X = 53;
    private static final int FIELD_W = 70;

    private int quarters = 44;
    private long holdStart = -1;

    public WandBenchScreen(WandBenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 224;
    }

    private ItemStack preview() {
        ItemStack branch = menu.branch();
        WandCore core = WandCore.byItem(menu.core().getItem());
        if (!(branch.getItem() instanceof BranchItem item) || core == null) {
            return ItemStack.EMPTY;
        }
        return Wands.create(item.getWood(), core, quarters, WandHandle.NENHUM, null);
    }

    private int craftX() {
        return leftPos + (imageWidth - BTN_W) / 2;
    }

    private int craftY() {
        return topPos + 121;
    }

    private boolean overCraft(double mx, double my) {
        return mx >= craftX() && mx < craftX() + BTN_W && my >= craftY() && my < craftY() + BTN_H - 2;
    }

    private static boolean over(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // -- Desenho

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        drawCraft(g, mouseX, mouseY);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        HiRes.image(g, PAPER, leftPos - 10, topPos - 8, imageWidth + 20, imageHeight + 16);
        float cx = leftPos + imageWidth / 2f;
        HiRes.centered(g, title.getString(), HiRes.Style.BOLD, 10f, cx, topPos + 6, TITLE);
        HiRes.centered(g, tr("screen.hogwartsmod.bench.subtitle"), HiRes.Style.ITALIC, 6.5f, cx, topPos + 18, MUTED);
        g.fill(leftPos + 30, topPos + 29, leftPos + imageWidth - 30, topPos + 30, LINE);
        partSlot(g, WandBenchMenu.BRANCH_X, WandBenchMenu.BRANCH_Y, "screen.hogwartsmod.bench.branch", menu.branch());
        partSlot(g, WandBenchMenu.CORE_X, WandBenchMenu.CORE_Y, "screen.hogwartsmod.bench.core", menu.core());
        drawPreview(g);
        drawSelector(g, mouseX, mouseY, LEN_X, "screen.hogwartsmod.bench.length", Wands.formatLength(quarters) + "\"");
        HiRes.text(g, tr("screen.hogwartsmod.package.inventory"), HiRes.Style.BOLD, 7.5f, leftPos + 8, topPos + WandBenchMenu.INV_Y - 11, INK);
        for (Slot slot : menu.slots) {
            if (slot.index >= 2) {
                slot(g, leftPos + slot.x, topPos + slot.y);
            }
        }
    }

    private void partSlot(GuiGraphics g, int x, int y, String key, ItemStack stack) {
        int sx = leftPos + x;
        int sy = topPos + y;
        g.fill(sx - 3, sy - 3, sx + 19, sy + 19, 0xFF9C8058);
        g.fill(sx - 2, sy - 2, sx + 19, sy + 19, 0xFFF4E6C6);
        g.fill(sx - 2, sy - 2, sx + 18, sy + 18, 0xFFC8AE80);
        slot(g, sx, sy);
        HiRes.text(g, tr(key), HiRes.Style.BOLD, 7f, sx + 24, sy + 1, INK);
        String value = stack.isEmpty() ? tr(key + ".empty") : stack.getHoverName().getString();
        HiRes.text(g, HiRes.ellipsize(value, HiRes.Style.ITALIC, 52 * HiRes.TEXT_SIZE / 6.5f), HiRes.Style.ITALIC, 6.5f, sx + 24, sy + 10, stack.isEmpty() ? MUTED : GREEN);
    }

    private void drawPreview(GuiGraphics g) {
        int px = leftPos + 110;
        int py = topPos + 33;
        g.fill(px - 1, py - 1, px + 51, py + 45, 0xFFA48A62);
        g.fill(px, py, px + 50, py + 44, 0xFFE6D4AE);
        ItemStack wand = preview();
        if (wand.isEmpty()) {
            List<String> lines = HiRes.wrap(tr("screen.hogwartsmod.bench.preview_empty"), HiRes.Style.ITALIC, 44 * HiRes.TEXT_SIZE / 6f);
            int y = py + 23 - lines.size() * 4;
            for (String line : lines) {
                HiRes.centered(g, line, HiRes.Style.ITALIC, 6f, px + 25, y, MUTED);
                y += 8;
            }
            return;
        }
        g.pose().pushPose();
        g.pose().translate(px + 9, py + 3, 0);
        g.pose().scale(2f, 2f, 1f);
        g.renderItem(wand, 0, 0);
        g.pose().popPose();
        com.gabriel.hogwartsmod.wand.WandWood wood = Wands.wood(wand);
        HiRes.centered(g, wood == null ? "" : wood.getDisplayName().getString(), HiRes.Style.BOLD, 6.5f, px + 25, py + 35, TITLE);
        drawStats(g, wand);
    }

    private void drawStats(GuiGraphics g, ItemStack wand) {
        WandStats s = Wands.stats(wand);
        List<String> labels = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        stat(labels, colors, "damage", s.damage(), true);
        stat(labels, colors, "range", s.range(), true);
        stat(labels, colors, "cooldown", s.cooldown(), false);
        stat(labels, colors, "cost", s.cost(), false);
        stat(labels, colors, "shield", s.shield(), true);
        stat(labels, colors, "mastery", s.mastery(), true);
        int x = leftPos + 14;
        int y = topPos + 108;
        for (int i = 0; i < labels.size(); i++) {
            HiRes.text(g, labels.get(i), HiRes.Style.REGULAR, 6f, x + (i % 3) * 52, y + (i / 3) * 7, colors.get(i));
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

    private void drawSelector(GuiGraphics g, int mouseX, int mouseY, int x, String key, String value) {
        int sx = leftPos + x;
        int sy = topPos + ROW_Y;
        HiRes.centered(g, tr(key), HiRes.Style.BOLD, 7f, sx + FIELD_W / 2f, sy - 9, INK);
        arrow(g, sx, sy, "‹", over(mouseX, mouseY, sx, sy, ARROW, ARROW + 2));
        arrow(g, sx + FIELD_W - ARROW, sy, "›", over(mouseX, mouseY, sx + FIELD_W - ARROW, sy, ARROW, ARROW + 2));
        g.fill(sx + ARROW + 2, sy, sx + FIELD_W - ARROW - 2, sy + ARROW + 2, 0xFFE6D4AE);
        g.fill(sx + ARROW + 2, sy + ARROW + 1, sx + FIELD_W - ARROW - 2, sy + ARROW + 2, 0xFFA48A62);
        HiRes.centered(g, value, HiRes.Style.BOLD, 7f, sx + FIELD_W / 2f, sy + 2, TITLE);
    }

    private static void arrow(GuiGraphics g, int x, int y, String glyph, boolean hover) {
        g.fill(x, y, x + ARROW, y + ARROW + 2, 0xFF6E4A2C);
        g.fill(x, y, x + ARROW, y + ARROW + 1, hover ? 0xFFB07A48 : 0xFF8E5E36);
        HiRes.centered(g, glyph, HiRes.Style.BOLD, 9f, x + ARROW / 2f, y - 1, 0xFFF6E6C8);
    }

    private static void slot(GuiGraphics g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, 0xFFA48A62);
        g.fill(x, y, x + 17, y + 17, 0xFFF6EAD0);
        g.fill(x, y, x + 16, y + 16, 0xFFD7C296);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
    }

    private void drawCraft(GuiGraphics g, int mouseX, int mouseY) {
        int bx = craftX();
        int by = craftY();
        boolean can = !preview().isEmpty();
        boolean hover = can && overCraft(mouseX, mouseY);
        float progress = 0f;
        if (holdStart >= 0) {
            if (!hover) {
                holdStart = -1;
            } else {
                progress = Math.min(1f, (System.currentTimeMillis() - holdStart) / (float) HOLD_MS);
            }
        }
        HiRes.image(g, MissionScreen.tex(!can ? "botao_assinar_d" : hover ? "botao_assinar_h" : "botao_assinar_n"), bx, by, BTN_W, BTN_H);
        if (progress > 0f) {
            g.enableScissor(bx, by, bx + Math.max(1, Math.round(BTN_W * progress)), by + BTN_H);
            HiRes.image(g, MissionScreen.tex("botao_assinar_fill"), bx, by, BTN_W, BTN_H);
            g.disableScissor();
        }
        String label = tr(progress > 0f ? "screen.hogwartsmod.bench.crafting" : "screen.hogwartsmod.bench.craft");
        HiRes.centered(g, label, HiRes.Style.BOLD, 6.5f, bx + BTN_W / 2f, by + 2, can ? 0xFFF6E6C8 : 0xFFB8A88C);
        if (progress >= 1f) {
            holdStart = -1;
            craft();
        }
    }

    // -- Interação

    public void craft() {
        ModNetwork.CHANNEL.sendToServer(new CraftWandPacket(quarters));
    }

    public void choose(int quarters) {
        this.quarters = Wands.clampLength(quarters);
    }

    private void click() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int y = topPos + ROW_Y;
            int step = Screen.hasShiftDown() ? 4 : 1;
            if (over(mx, my, leftPos + LEN_X, y, ARROW, ARROW + 2)) {
                quarters = Wands.clampLength(quarters - step);
                click();
                return true;
            }
            if (over(mx, my, leftPos + LEN_X + FIELD_W - ARROW, y, ARROW, ARROW + 2)) {
                quarters = Wands.clampLength(quarters + step);
                click();
                return true;
            }
            if (overCraft(mx, my) && !preview().isEmpty()) {
                holdStart = System.currentTimeMillis();
                if (minecraft != null) {
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
                }
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        holdStart = -1;
        return super.mouseReleased(mx, my, button);
    }
}
