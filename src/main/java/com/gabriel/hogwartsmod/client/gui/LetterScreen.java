package com.gabriel.hogwartsmod.client.gui;

import com.gabriel.hogwartsmod.mail.LetterItem;
import com.gabriel.hogwartsmod.network.EditLetterPacket;
import com.gabriel.hogwartsmod.network.ModNetwork;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import static com.gabriel.hogwartsmod.client.gui.MissionScreen.*;

public class LetterScreen extends Screen {
    private static final int W = 200;
    private static final int H = 236;
    private static final int BODY_W = 160;
    private static final int LINE_H = 10;
    private static final int MAX_LINES = 15;
    private static final int SELECTION = 0x5A8E6A3A;
    private static final ResourceLocation PAPER = MissionScreen.tex("carta_papel");
    private static final ResourceLocation SEAL = MissionScreen.tex("lacre");
    private static final int BTN_W = 80;
    private static final int BTN_H = 18;
    private static final long HOLD_MS = 1500;

    private final InteractionHand hand;
    private boolean sealed;
    private String author;
    private final String initialTo;
    private final String initialText;
    private EditBox toBox;
    private Body body;
    private boolean bodyFocused = true;
    private long holdStart = -1;
    private boolean sent;
    private int left;
    private int top;

    public LetterScreen(InteractionHand hand, ItemStack stack) {
        super(Component.translatable("item.hogwartsmod.envelope"));
        this.hand = hand;
        this.sealed = LetterItem.isSealed(stack);
        this.author = LetterItem.author(stack);
        this.initialTo = LetterItem.to(stack);
        this.initialText = LetterItem.text(stack);
    }

    public static void open(InteractionHand hand, ItemStack stack) {
        Minecraft.getInstance().setScreen(new LetterScreen(hand, stack));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        String to = toBox == null ? initialTo : toBox.getValue();
        String text = body == null ? initialText : body.value();
        toBox = new EditBox(font, left + 52, top + 26, 128, 10, Component.empty());
        toBox.setBordered(false);
        toBox.setMaxLength(LetterItem.MAX_TO);
        toBox.setValue(to);
        body = new Body(font, BODY_W);
        body.setCharacterLimit(LetterItem.MAX_TEXT);
        body.setValue(text);
        if (!sealed) {
            addWidget(toBox);
            if (to.isEmpty()) {
                focusTo();
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int bodyX() {
        return left + 20;
    }

    private int bodyY() {
        return top + 48;
    }

    private void focusTo() {
        bodyFocused = false;
        setFocused(toBox);
        toBox.setFocused(true);
    }

    private void focusBody() {
        bodyFocused = true;
        toBox.setFocused(false);
        setFocused(null);
    }

    // -- Desenho

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        HiRes.image(g, PAPER, left, top, W, H);
        int x = bodyX();
        HiRes.text(g, tr("screen.hogwartsmod.letter.to"), HiRes.Style.BOLD, 9f, x, top + 25, TITLE);
        g.fill(left + 50, top + 37, left + W - 20, top + 38, LINE);
        if (sealed) {
            HiRes.text(g, HiRes.ellipsize(toBox.getValue(), HiRes.Style.ITALIC, 128 * HiRes.TEXT_SIZE / 9f), HiRes.Style.ITALIC, 9f, left + 52, top + 25, INK);
        } else {
            drawToField(g);
        }
        for (int i = 1; i <= MAX_LINES; i++) {
            g.fill(x, bodyY() + i * LINE_H - 1, x + BODY_W, bodyY() + i * LINE_H, 0x40B49A6C);
        }
        drawBody(g);
        if (sealed) {
            drawSignature(g);
        } else {
            drawFooter(g, mouseX, mouseY);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawToField(GuiGraphics g) {
        String value = toBox.getValue();
        int x = left + 52;
        int y = top + 25;
        if (value.isEmpty() && !toBox.isFocused()) {
            HiRes.text(g, tr("screen.hogwartsmod.letter.to_hint"), HiRes.Style.ITALIC, 8f, x, y + 1, MUTED);
        }
        HiRes.text(g, value, HiRes.Style.ITALIC, 9f, x, y, INK);
        if (toBox.isFocused() && blink()) {
            int cursor = Math.min(toBox.getCursorPosition(), value.length());
            float cx = x + HiRes.width(value.substring(0, cursor), HiRes.Style.ITALIC, 9f) + 0.5f;
            g.fill((int) cx, y, (int) cx + 1, y + 10, INK);
        }
    }

    private boolean blink() {
        return (System.currentTimeMillis() / 500) % 2 == 0;
    }

    private void drawBody(GuiGraphics g) {
        int x = bodyX();
        int y = bodyY();
        String value = body.value();
        boolean selection = !sealed && bodyFocused && body.hasSelection();
        int[] selected = body.selected();
        int line = 0;
        for (int[] view : body.lines()) {
            int ly = y + line * LINE_H + 1;
            String text = value.substring(view[0], view[1]);
            if (selection) {
                int from = Math.max(selected[0], view[0]);
                int to = Math.min(selected[1], view[1]);
                if (from < to) {
                    int sx = x + font.width(value.substring(view[0], from));
                    int ex = x + font.width(value.substring(view[0], to));
                    g.fill(sx, ly - 1, ex, ly + 9, SELECTION);
                }
            }
            g.drawString(font, text, x, ly, INK, false);
            line++;
        }
        if (!sealed && bodyFocused && blink()) {
            int at = body.getLineAtCursor();
            int[] view = body.line(at);
            int cursor = Math.max(view[0], Math.min(body.cursor(), view[1]));
            int cx = x + font.width(value.substring(view[0], cursor));
            int ly = y + at * LINE_H + 1;
            g.fill(cx, ly - 1, cx + 1, ly + 9, INK);
        }
        if (!sealed && value.isEmpty() && !bodyFocused) {
            HiRes.text(g, tr("screen.hogwartsmod.letter.body_hint"), HiRes.Style.ITALIC, 8f, x, y + 1, MUTED);
        }
    }

    private void drawFooter(GuiGraphics g, int mouseX, int mouseY) {
        int y = top + H - 32;
        String count = body.value().length() + "/" + LetterItem.MAX_TEXT;
        HiRes.text(g, count, HiRes.Style.ITALIC, 7f, bodyX(), y + 1, MUTED);
        HiRes.text(g, tr("screen.hogwartsmod.letter.draft_hint"), HiRes.Style.ITALIC, 7f, bodyX(), y + 9, MUTED);
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
        String label = tr(progress > 0f ? "screen.hogwartsmod.letter.signing" : "screen.hogwartsmod.letter.sign");
        HiRes.centered(g, label, HiRes.Style.BOLD, 8f, bx + BTN_W / 2f, by + 4, can ? 0xFFF6E6C8 : 0xFFB8A88C);
        if (can) {
            HiRes.centered(g, tr("screen.hogwartsmod.letter.hold_hint"), HiRes.Style.ITALIC, 6.5f, bx + BTN_W / 2f, by - 9, MUTED);
        }
        if (progress >= 1f) {
            sign();
        }
    }

    private void sign() {
        holdStart = -1;
        send(true);
        sealed = true;
        author = minecraft.player == null ? "" : minecraft.player.getGameProfile().getName();
        toBox.setFocused(false);
        bodyFocused = false;
        removeWidget(toBox);
    }

    private int signX() {
        return left + W - 20 - BTN_W;
    }

    private int signY() {
        return top + H - 34;
    }

    private boolean overSign(double mx, double my) {
        return mx >= signX() && mx < signX() + BTN_W && my >= signY() && my < signY() + BTN_H - 3;
    }

    private void drawSignature(GuiGraphics g) {
        int sy = top + H - 46;
        HiRes.image(g, SEAL, left + W - 52, sy, 30, 30);
        HiRes.right(g, tr("screen.hogwartsmod.letter.sealed_by"), HiRes.Style.ITALIC, left + W - 56, sy + 6, MUTED);
        HiRes.right(g, author, HiRes.Style.BOLD, left + W - 56, sy + 16, TITLE);
    }

    private boolean canSeal() {
        return !toBox.getValue().isBlank() && !body.value().isBlank();
    }

    // -- Interação

    private void click() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (sealed || button != 0) {
            return super.mouseClicked(mx, my, button);
        }
        if (overSign(mx, my) && canSeal()) {
            holdStart = System.currentTimeMillis();
            if (minecraft != null) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
            }
            return true;
        }
        if (mx >= left + 48 && mx < left + W - 18 && my >= top + 22 && my < top + 40) {
            focusTo();
            toBox.mouseClicked(mx, my, button);
            return true;
        }
        if (mx >= bodyX() - 2 && mx < bodyX() + BODY_W + 2 && my >= bodyY() && my < bodyY() + MAX_LINES * LINE_H) {
            focusBody();
            body.setSelecting(hasShiftDown());
            body.seekCursorToPoint(mx - bodyX(), (my - bodyY()) * 9.0 / LINE_H);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        holdStart = -1;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (!sealed && bodyFocused && button == 0 && mx >= bodyX() - 2 && mx < bodyX() + BODY_W + 2) {
            body.setSelecting(true);
            body.seekCursorToPoint(mx - bodyX(), (my - bodyY()) * 9.0 / LINE_H);
            body.setSelecting(hasShiftDown());
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == 256) {
            onClose();
            return true;
        }
        if (sealed) {
            return super.keyPressed(key, scan, modifiers);
        }
        if (!bodyFocused) {
            if (key == 257 || key == 335 || key == 258) {
                focusBody();
                return true;
            }
            return toBox.keyPressed(key, scan, modifiers);
        }
        String before = body.value();
        boolean handled = body.keyPressed(key);
        limitLines(before);
        return handled || super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (sealed) {
            return false;
        }
        if (!bodyFocused) {
            return toBox.charTyped(c, modifiers);
        }
        if (!SharedConstants.isAllowedChatCharacter(c)) {
            return false;
        }
        String before = body.value();
        body.insertText(Character.toString(c));
        limitLines(before);
        return true;
    }

    private void limitLines(String before) {
        if (body.getLineCount() > MAX_LINES) {
            body.setValue(before);
        }
    }

    private void send(boolean seal) {
        if (sent) {
            return;
        }
        sent = true;
        ModNetwork.CHANNEL.sendToServer(new EditLetterPacket(hand, toBox.getValue(), body.value(), seal));
    }

    @Override
    public void removed() {
        if (!sealed && (!toBox.getValue().equals(initialTo) || !body.value().equals(initialText))) {
            send(false);
        }
        super.removed();
    }

    private static final class Body extends MultilineTextField {

        Body(net.minecraft.client.gui.Font font, int width) {
            super(font, width);
        }

        java.util.List<int[]> lines() {
            java.util.List<int[]> out = new java.util.ArrayList<>();
            for (StringView view : iterateLines()) {
                out.add(new int[]{view.beginIndex(), view.endIndex()});
            }
            return out;
        }

        int[] line(int index) {
            StringView view = getLineView(index);
            return new int[]{view.beginIndex(), view.endIndex()};
        }

        int[] selected() {
            StringView view = getSelected();
            return new int[]{view.beginIndex(), view.endIndex()};
        }
    }
}
