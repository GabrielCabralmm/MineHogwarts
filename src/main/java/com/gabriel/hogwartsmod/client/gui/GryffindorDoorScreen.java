package com.gabriel.hogwartsmod.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import static com.gabriel.hogwartsmod.client.gui.MissionScreen.*;

public class GryffindorDoorScreen extends HouseDoorScreen {
    private static final int W = 190;
    private static final int H = 230;
    private static final ResourceLocation PAPER = MissionScreen.tex("carta_papel");
    private static final ResourceLocation LADY = MissionScreen.tex("porta_dama");
    private EditBox field;

    public GryffindorDoorScreen(BlockPos pos) {
        super(pos, Component.translatable("screen.hogwartsmod.door.grifinoria"));
    }

    @Override
    protected int screenWidth() {
        return W;
    }

    @Override
    protected int screenHeight() {
        return H;
    }

    @Override
    protected void init() {
        super.init();
        String value = field == null ? "" : field.getValue();
        field = new EditBox(font, left + 35, top + H - 50, W - 70, 12, Component.empty());
        field.setBordered(false);
        field.setMaxLength(48);
        field.setTextColor(INK);
        field.setValue(value);
        addRenderableWidget(field);
        setInitialFocus(field);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        HiRes.image(g, PAPER, left, top, W, H);
        float cx = left + W / 2f;
        HiRes.image(g, LADY, left + (W - 96) / 2, top + 16, 96, 128);
        float bob = (float) Math.sin(System.currentTimeMillis() / 260.0) * 1.5f;
        HiRes.centered(g, tr("screen.hogwartsmod.door.grifinoria.ask"), HiRes.Style.ITALIC, 9f, cx, top + 152 + bob, TITLE);
        g.fill(left + 32, top + H - 37, left + W - 32, top + H - 36, LINE);
        if (field.getValue().isEmpty() && !field.isFocused()) {
            HiRes.centered(g, tr("screen.hogwartsmod.door.password_hint"), HiRes.Style.ITALIC, 7f, cx, top + H - 49, MUTED);
        }
        HiRes.centered(g, tr("screen.hogwartsmod.door.enter_hint"), HiRes.Style.ITALIC, 6.5f, cx, top + H - 30, MUTED);
        super.render(g, mouseX, mouseY, partialTick);
    }

    public void type(String text) {
        field.setValue(text);
    }

    public void submit() {
        if (!field.getValue().isBlank()) {
            send(field.getValue(), new int[0], 0);
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == 257 || key == 335) {
            submit();
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }
}
