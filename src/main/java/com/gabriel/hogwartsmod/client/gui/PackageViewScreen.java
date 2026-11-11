package com.gabriel.hogwartsmod.client.gui;

import com.gabriel.hogwartsmod.mail.PackageItem;
import com.gabriel.hogwartsmod.network.ModNetwork;
import com.gabriel.hogwartsmod.network.PackagePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import static com.gabriel.hogwartsmod.client.gui.MissionScreen.*;

public class PackageViewScreen extends Screen {
    private static final int W = 150;
    private static final int H = 150;
    private static final int BTN_W = 62;
    private static final int BTN_H = 14;
    private static final ResourceLocation PAPER = MissionScreen.tex("carta_papel");
    private static final ResourceLocation SEAL = MissionScreen.tex("lacre");

    private final InteractionHand hand;
    private int left;
    private int top;

    public PackageViewScreen(InteractionHand hand) {
        super(Component.translatable("item.hogwartsmod.pacote"));
        this.hand = hand;
    }

    public static void open(InteractionHand hand) {
        Minecraft.getInstance().setScreen(new PackageViewScreen(hand));
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

    private ItemStack box() {
        return minecraft == null || minecraft.player == null ? ItemStack.EMPTY : minecraft.player.getItemInHand(hand);
    }

    @Override
    public void tick() {
        if (!(box().getItem() instanceof PackageItem)) {
            onClose();
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        HiRes.image(g, PAPER, left, top, W, H);
        float cx = left + W / 2f;
        ItemStack box = box();
        HiRes.centered(g, box.getHoverName().getString(), HiRes.Style.BOLD, 10f, cx, top + 16, TITLE);
        HiRes.centered(g, tr("screen.hogwartsmod.package.sealed"), HiRes.Style.ITALIC, 7f, cx, top + 29, MUTED);
        MissionScreen.item(g, box, Math.round(cx) - 16, top + 46, 32);
        HiRes.image(g, SEAL, Math.round(cx) + 8, top + 66, 18, 18);
        int bx = left + (W - BTN_W) / 2;
        int by = top + H - 34;
        boolean hover = mouseX >= bx && mouseX < bx + BTN_W && mouseY >= by && mouseY < by + BTN_H;
        HiRes.image(g, MissionScreen.tex("botao_couro_largo_" + (hover ? "h" : "n")), bx, by, BTN_W, BTN_H);
        HiRes.centered(g, tr("screen.hogwartsmod.package.open"), HiRes.Style.BOLD, 7.5f, bx + BTN_W / 2f, by + 3, 0xFFF6E6C8);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int bx = left + (W - BTN_W) / 2;
        int by = top + H - 34;
        if (button == 0 && mx >= bx && mx < bx + BTN_W && my >= by && my < by + BTN_H) {
            if (minecraft != null) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            }
            ModNetwork.CHANNEL.sendToServer(new PackagePacket(hand, false));
            onClose();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }
}
