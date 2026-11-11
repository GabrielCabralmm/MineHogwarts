package com.gabriel.hogwartsmod.client.gui;

import com.gabriel.hogwartsmod.broom.BroomBenchMenu;
import com.gabriel.hogwartsmod.broom.BroomHandleItem;
import com.gabriel.hogwartsmod.broom.BroomType;
import com.gabriel.hogwartsmod.broom.Brooms;
import com.gabriel.hogwartsmod.network.CraftBroomPacket;
import com.gabriel.hogwartsmod.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import static com.gabriel.hogwartsmod.client.gui.MissionScreen.*;

public class BroomBenchScreen extends AbstractContainerScreen<BroomBenchMenu> {
    private static final ResourceLocation PAPER = MissionScreen.tex("carta_papel");
    private static final BroomType[] TYPES = {BroomType.SPORT, BroomType.TRANSPORT, BroomType.CARGO};
    private static final int BTN_W = 54;
    private static final int BTN_H = 12;
    private static final int ARROW = 9;
    private static final long HOLD_MS = 2000;
    private static final int ROW_Y = 92;
    private static final int SEL_X = 48;
    private static final int FIELD_W = 80;

    private int type;
    private long holdStart = -1;

    public BroomBenchScreen(BroomBenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 226;
    }

    private BroomType selected() {
        return TYPES[type];
    }

    private ItemStack preview() {
        ItemStack handle = menu.handle();
        return Brooms.create(selected(), handle.getItem() instanceof BroomHandleItem item ? item.getWood() : null, null);
    }

    private int craftX() {
        return leftPos + (imageWidth - BTN_W) / 2;
    }

    private int craftY() {
        return topPos + 128;
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
        HiRes.centered(g, tr("screen.hogwartsmod.broom_bench.subtitle"), HiRes.Style.ITALIC, 6.5f, cx, topPos + 18, MUTED);
        g.fill(leftPos + 30, topPos + 28, leftPos + imageWidth - 30, topPos + 29, LINE);
        partSlot(g, BroomBenchMenu.HANDLE_Y, "screen.hogwartsmod.broom_bench.handle", menu.handle(), 1, true);
        partSlot(g, BroomBenchMenu.BRISTLE_Y, "screen.hogwartsmod.broom_bench.bristles", menu.bristles(), BroomBenchMenu.BRISTLES, true);
        partSlot(g, BroomBenchMenu.CHEST_Y, "screen.hogwartsmod.broom_bench.chest", menu.chest(), 1, selected() == BroomType.CARGO);
        drawPreview(g);
        drawSelector(g, mouseX, mouseY);
        drawStats(g);
        HiRes.text(g, tr("screen.hogwartsmod.package.inventory"), HiRes.Style.BOLD, 7.5f, leftPos + 8, topPos + BroomBenchMenu.INV_Y - 11, INK);
        for (Slot slot : menu.slots) {
            if (slot.index >= 3) {
                slot(g, leftPos + slot.x, topPos + slot.y);
            }
        }
    }

    private void partSlot(GuiGraphics g, int y, String key, ItemStack stack, int need, boolean required) {
        int sx = leftPos + BroomBenchMenu.PART_X;
        int sy = topPos + y;
        g.fill(sx - 3, sy - 3, sx + 19, sy + 19, required ? 0xFF9C8058 : 0xFFC4AE88);
        g.fill(sx - 2, sy - 2, sx + 19, sy + 19, 0xFFF4E6C6);
        g.fill(sx - 2, sy - 2, sx + 18, sy + 18, required ? 0xFFC8AE80 : 0xFFDCCBA6);
        slot(g, sx, sy);
        String label = tr(key) + (need > 1 ? " ×" + need : "");
        HiRes.text(g, label, HiRes.Style.BOLD, 7f, sx + 24, sy + 1, required ? INK : MUTED);
        boolean ok = !stack.isEmpty() && stack.getCount() >= need;
        String value = !required ? tr(key + ".optional") : stack.isEmpty() ? tr(key + ".empty")
                : stack.getItem() instanceof BroomHandleItem handle ? handle.getWood().getDisplayName().getString() : stack.getHoverName().getString();
        HiRes.text(g, HiRes.ellipsize(value, HiRes.Style.ITALIC, 60 * HiRes.TEXT_SIZE / 6.5f), HiRes.Style.ITALIC, 6.5f, sx + 24, sy + 10,
                !required ? MUTED : ok ? GREEN : stack.isEmpty() ? MUTED : WINE);
    }

    private void drawPreview(GuiGraphics g) {
        int px = leftPos + 112;
        int py = topPos + 33;
        g.fill(px - 1, py - 1, px + 51, py + 45, 0xFFA48A62);
        g.fill(px, py, px + 50, py + 44, 0xFFE6D4AE);
        ItemStack broom = preview();
        g.pose().pushPose();
        g.pose().translate(px + 9, py + 3, 0);
        g.pose().scale(2f, 2f, 1f);
        g.renderItem(broom, 0, 0);
        g.pose().popPose();
        String wood = Brooms.wood(broom) == null ? tr("screen.hogwartsmod.broom_bench.no_wood") : Brooms.wood(broom).getDisplayName().getString();
        HiRes.centered(g, wood, HiRes.Style.BOLD, 6.5f, px + 25, py + 35, TITLE);
    }

    private void drawSelector(GuiGraphics g, int mouseX, int mouseY) {
        int sx = leftPos + SEL_X;
        int sy = topPos + ROW_Y;
        arrow(g, sx, sy, "‹", over(mouseX, mouseY, sx, sy, ARROW, ARROW + 2));
        arrow(g, sx + FIELD_W - ARROW, sy, "›", over(mouseX, mouseY, sx + FIELD_W - ARROW, sy, ARROW, ARROW + 2));
        g.fill(sx + ARROW + 2, sy, sx + FIELD_W - ARROW - 2, sy + ARROW + 2, 0xFFE6D4AE);
        g.fill(sx + ARROW + 2, sy + ARROW + 1, sx + FIELD_W - ARROW - 2, sy + ARROW + 2, 0xFFA48A62);
        HiRes.centered(g, tr("screen.hogwartsmod.broom_bench.type." + selected().getId()), HiRes.Style.BOLD, 7f, sx + FIELD_W / 2f, sy + 2, TITLE);
    }

    private static void arrow(GuiGraphics g, int x, int y, String glyph, boolean hover) {
        g.fill(x, y, x + ARROW, y + ARROW + 2, 0xFF6E4A2C);
        g.fill(x, y, x + ARROW, y + ARROW + 1, hover ? 0xFFB07A48 : 0xFF8E5E36);
        HiRes.centered(g, glyph, HiRes.Style.BOLD, 9f, x + ARROW / 2f, y - 1, 0xFFF6E6C8);
    }

    private void drawStats(GuiGraphics g) {
        BroomType t = selected();
        stat(g, 0, 0, "speed", t.getMaxSpeed() / BroomType.SPORT.getMaxSpeed());
        stat(g, 1, 0, "acceleration", t.getAcceleration() / BroomType.SPORT.getAcceleration());
        stat(g, 0, 1, "handling", t.getHandling() / BroomType.SPORT.getHandling());
        stat(g, 1, 1, "stamina", t.getStamina() / BroomType.CARGO.getStamina());
        String capacity = t == BroomType.CARGO ? tr("tooltip.hogwartsmod.broom.cargo", BroomType.CARGO_SLOTS) : tr("tooltip.hogwartsmod.broom.seats", t.getSeats());
        HiRes.centered(g, capacity, HiRes.Style.ITALIC, 6f, leftPos + imageWidth / 2f, topPos + 120, MUTED);
    }

    private void stat(GuiGraphics g, int col, int row, String key, float fraction) {
        int x = leftPos + 12 + col * 82;
        int y = topPos + 105 + row * 7;
        HiRes.text(g, tr("screen.hogwartsmod.broom_bench.stat." + key), HiRes.Style.REGULAR, 6f, x, y, INK);
        HiRes.bar(g, x + 40, y + 2, 34, 4, Math.min(1f, fraction), 0xFFD7C296, 0xFF6E4A2C);
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
        boolean can = menu.hasParts(selected());
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
        ModNetwork.CHANNEL.sendToServer(new CraftBroomPacket(selected()));
    }

    public void choose(int index) {
        type = Math.floorMod(index, TYPES.length);
    }

    private void click() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int sx = leftPos + SEL_X;
            int sy = topPos + ROW_Y;
            if (over(mx, my, sx, sy, ARROW, ARROW + 2)) {
                choose(type - 1);
                click();
                return true;
            }
            if (over(mx, my, sx + FIELD_W - ARROW, sy, ARROW, ARROW + 2)) {
                choose(type + 1);
                click();
                return true;
            }
            if (overCraft(mx, my) && menu.hasParts(selected())) {
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
