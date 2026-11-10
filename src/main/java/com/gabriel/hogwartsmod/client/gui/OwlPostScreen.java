package com.gabriel.hogwartsmod.client.gui;

import com.gabriel.hogwartsmod.network.ModNetwork;
import com.gabriel.hogwartsmod.network.SendOwlPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static com.gabriel.hogwartsmod.client.gui.MissionScreen.*;

public class OwlPostScreen extends Screen {
    private static final int W = 170;
    private static final int H = 200;
    private static final int ROW = 15;
    private static final int VISIBLE = 8;
    private static final int BTN_W = 62;
    private static final int BTN_H = 14;
    private static final ResourceLocation PAPER = MissionScreen.tex("carta_papel");

    private final int owl;
    private final InteractionHand hand;
    private final List<PlayerInfo> players = new ArrayList<>();
    private int selected = -1;
    private int scroll;
    private int left;
    private int top;

    public OwlPostScreen(int owl, InteractionHand hand) {
        super(Component.translatable("screen.hogwartsmod.owl_post"));
        this.owl = owl;
        this.hand = hand;
    }

    public static void open(int owl, InteractionHand hand) {
        Minecraft.getInstance().setScreen(new OwlPostScreen(owl, hand));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        players.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null && mc.player != null) {
            UUID self = mc.player.getUUID();
            for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
                if (!info.getProfile().getId().equals(self)) {
                    players.add(info);
                }
            }
        }
        players.sort(Comparator.comparing(p -> p.getProfile().getName().toLowerCase()));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int listX() {
        return left + 22;
    }

    private int listY() {
        return top + 44;
    }

    private int listW() {
        return W - 44;
    }

    // -- Desenho

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        HiRes.image(g, PAPER, left, top, W, H);
        float cx = left + W / 2f;
        HiRes.centered(g, title.getString(), HiRes.Style.BOLD, 10f, cx, top + 15, TITLE);
        HiRes.centered(g, tr("screen.hogwartsmod.owl_post.subtitle"), HiRes.Style.ITALIC, 7f, cx, top + 28, MUTED);
        g.fill(left + 40, top + 38, left + W - 40, top + 39, LINE);
        if (players.isEmpty()) {
            int y = top + 70;
            for (String line : HiRes.wrap(tr("screen.hogwartsmod.owl_post.empty"), HiRes.Style.ITALIC, listW() * HiRes.TEXT_SIZE / 7.5f)) {
                HiRes.centered(g, line, HiRes.Style.ITALIC, 7.5f, cx, y, MUTED);
                y += 10;
            }
        }
        int x = listX();
        for (int i = 0; i < Math.min(VISIBLE, players.size() - scroll); i++) {
            int index = i + scroll;
            PlayerInfo info = players.get(index);
            int y = listY() + i * ROW;
            boolean hover = mouseX >= x - 2 && mouseX < x + listW() + 2 && mouseY >= y - 2 && mouseY < y + ROW - 3;
            if (index == selected || hover) {
                HiRes.highlight(g, x - 2, y - 2, x + listW() + 2, y + ROW - 3, index == selected ? HiRes.Mark.SELECTED : HiRes.Mark.HOVER);
            }
            PlayerFaceRenderer.draw(g, info.getSkinLocation(), x + 2, y, 10);
            String name = info.getProfile().getName();
            HiRes.text(g, HiRes.ellipsize(name, HiRes.Style.REGULAR, (listW() - 20) * HiRes.TEXT_SIZE / 8f), HiRes.Style.REGULAR, 8f, x + 17, y + 1, INK);
        }
        if (players.size() > VISIBLE) {
            int trackY = listY() - 2;
            int trackH = VISIBLE * ROW - 2;
            int barH = Math.max(12, trackH * VISIBLE / players.size());
            int barY = trackY + (trackH - barH) * scroll / Math.max(1, players.size() - VISIBLE);
            g.fill(x + listW() + 5, trackY, x + listW() + 6, trackY + trackH, LINE);
            g.fill(x + listW() + 4, barY, x + listW() + 7, barY + barH, NOTE_EDGE);
        }
        drawSend(g, mouseX, mouseY);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawSend(GuiGraphics g, int mouseX, int mouseY) {
        boolean can = selected >= 0;
        int bx = left + (W - BTN_W) / 2;
        int by = top + H - 30;
        boolean hover = can && overSend(mouseX, mouseY);
        HiRes.image(g, MissionScreen.tex("botao_couro_largo_" + (!can ? "d" : hover ? "h" : "n")), bx, by, BTN_W, BTN_H);
        HiRes.centered(g, tr("screen.hogwartsmod.owl_post.send"), HiRes.Style.BOLD, 7.5f, bx + BTN_W / 2f, by + 3, can ? 0xFFF6E6C8 : 0xFF9C8C74);
    }

    private boolean overSend(double mx, double my) {
        int bx = left + (W - BTN_W) / 2;
        int by = top + H - 30;
        return mx >= bx && mx < bx + BTN_W && my >= by && my < by + BTN_H;
    }

    // -- Interação

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
        int x = listX();
        for (int i = 0; i < Math.min(VISIBLE, players.size() - scroll); i++) {
            int y = listY() + i * ROW;
            if (mx >= x - 2 && mx < x + listW() + 2 && my >= y - 2 && my < y + ROW - 3) {
                selected = i + scroll;
                click();
                return true;
            }
        }
        if (selected >= 0 && overSend(mx, my)) {
            send();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    public void send() {
        if (selected < 0 || selected >= players.size()) {
            return;
        }
        click();
        ModNetwork.CHANNEL.sendToServer(new SendOwlPacket(owl, hand, players.get(selected).getProfile().getId()));
        onClose();
    }

    public void select(int index) {
        selected = index;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int max = Math.max(0, players.size() - VISIBLE);
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(delta)));
        return true;
    }
}
