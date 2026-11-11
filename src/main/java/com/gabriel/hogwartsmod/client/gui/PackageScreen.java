package com.gabriel.hogwartsmod.client.gui;

import com.gabriel.hogwartsmod.mail.PackageMenu;
import com.gabriel.hogwartsmod.network.ModNetwork;
import com.gabriel.hogwartsmod.network.PackagePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import static com.gabriel.hogwartsmod.client.gui.MissionScreen.*;

public class PackageScreen extends AbstractContainerScreen<PackageMenu> {
    private static final ResourceLocation PAPER = MissionScreen.tex("carta_papel");
    private static final int BTN_W = 54;
    private static final int BTN_H = 12;
    private static final long HOLD_MS = 1500;

    private long holdStart = -1;
    private boolean sent;

    public PackageScreen(PackageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 190;
    }

    private int signX() {
        return leftPos + (imageWidth - BTN_W) / 2;
    }

    private int signY() {
        return topPos + PackageMenu.SLOT_Y + 26;
    }

    private boolean overSign(double mx, double my) {
        return mx >= signX() && mx < signX() + BTN_W && my >= signY() && my < signY() + BTN_H - 2;
    }

    private boolean canSeal() {
        return menu.getSlot(0).hasItem() && !sent;
    }

    // -- Desenho

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        drawSign(g, mouseX, mouseY);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        HiRes.image(g, PAPER, leftPos - 10, topPos - 8, imageWidth + 20, imageHeight + 16);
        float cx = leftPos + imageWidth / 2f;
        HiRes.centered(g, title.getString(), HiRes.Style.BOLD, 10f, cx, topPos + 6, TITLE);
        HiRes.centered(g, tr("screen.hogwartsmod.package.subtitle"), HiRes.Style.ITALIC, 6.5f, cx, topPos + 19, MUTED);
        int sx = leftPos + PackageMenu.SLOT_X;
        int sy = topPos + PackageMenu.SLOT_Y;
        g.fill(sx - 6, sy - 6, sx + 22, sy + 22, 0xFF9C8058);
        g.fill(sx - 5, sy - 5, sx + 22, sy + 22, 0xFFF4E6C6);
        g.fill(sx - 5, sy - 5, sx + 21, sy + 21, 0xFFC8AE80);
        g.fill(sx - 4, sy - 4, sx + 20, sy + 20, 0xFFE2CFA6);
        slot(g, sx, sy);
        HiRes.text(g, tr("screen.hogwartsmod.package.inventory"), HiRes.Style.BOLD, 7.5f, leftPos + 8, topPos + PackageMenu.INV_Y - 11, INK);
        for (Slot slot : menu.slots) {
            if (slot != menu.getSlot(0)) {
                slot(g, leftPos + slot.x, topPos + slot.y);
            }
        }
    }

    private static void slot(GuiGraphics g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, 0xFFA48A62);
        g.fill(x, y, x + 17, y + 17, 0xFFF6EAD0);
        g.fill(x, y, x + 16, y + 16, 0xFFD7C296);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
    }

    private void drawSign(GuiGraphics g, int mouseX, int mouseY) {
        int bx = signX();
        int by = signY();
        boolean can = canSeal();
        boolean hover = can && overSign(mouseX, mouseY);
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
        String label = tr(progress > 0f ? "screen.hogwartsmod.package.wrapping" : "screen.hogwartsmod.package.wrap");
        HiRes.centered(g, label, HiRes.Style.BOLD, 6.5f, bx + BTN_W / 2f, by + 2, can ? 0xFFF6E6C8 : 0xFFB8A88C);
        String hint = tr(can ? "screen.hogwartsmod.package.hold_hint" : "screen.hogwartsmod.package.empty");
        HiRes.centered(g, hint, HiRes.Style.ITALIC, 6f, bx + BTN_W / 2f, by + 14, MUTED);
        if (progress >= 1f) {
            holdStart = -1;
            sent = true;
            ModNetwork.CHANNEL.sendToServer(new PackagePacket(menu.hand(), true));
        }
    }

    // -- Interação

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && overSign(mx, my) && canSeal()) {
            holdStart = System.currentTimeMillis();
            if (minecraft != null) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
            }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        holdStart = -1;
        return super.mouseReleased(mx, my, button);
    }
}
