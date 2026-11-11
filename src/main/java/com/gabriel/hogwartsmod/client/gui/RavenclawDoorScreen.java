package com.gabriel.hogwartsmod.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

import static com.gabriel.hogwartsmod.client.gui.MissionScreen.*;

public class RavenclawDoorScreen extends HouseDoorScreen {
    private static final int W = 220;
    private static final int H = 190;
    private static final ResourceLocation PAPER = MissionScreen.tex("carta_papel");
    private static final ResourceLocation EAGLE = MissionScreen.tex("aguia_aldrava");
    private final int riddle;
    private final String question;
    private EditBox field;

    public RavenclawDoorScreen(BlockPos pos, int riddle, String question) {
        super(pos, Component.translatable("screen.hogwartsmod.door.corvinal"));
        this.riddle = riddle;
        this.question = question;
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
        field = new EditBox(font, left + 40, top + H - 50, W - 80, 12, Component.empty());
        field.setBordered(false);
        field.setMaxLength(64);
        field.setTextColor(INK);
        field.setValue(value);
        field.visible = riddle >= 0;
        addRenderableWidget(field);
        setInitialFocus(field);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        HiRes.image(g, PAPER, left, top, W, H);
        float cx = left + W / 2f;
        HiRes.image(g, EAGLE, left + (W - 44) / 2, top + 12, 44, 44);
        HiRes.centered(g, tr("screen.hogwartsmod.door.corvinal.ask"), HiRes.Style.ITALIC, 7.5f, cx, top + 60, MUTED);
        String text = riddle >= 0 ? question : tr("screen.hogwartsmod.door.corvinal.none");
        List<String> lines = HiRes.wrap(text, HiRes.Style.BOLD, (W - 50) * HiRes.TEXT_SIZE / 8.5f);
        int y = top + 76;
        for (String line : lines) {
            HiRes.centered(g, line, HiRes.Style.BOLD, 8.5f, cx, y, TITLE);
            y += 12;
        }
        if (riddle >= 0) {
            g.fill(left + 36, top + H - 37, left + W - 36, top + H - 36, LINE);
            if (field.getValue().isEmpty() && !field.isFocused()) {
                HiRes.centered(g, tr("screen.hogwartsmod.door.answer_hint"), HiRes.Style.ITALIC, 7f, cx, top + H - 49, MUTED);
            }
            HiRes.centered(g, tr("screen.hogwartsmod.door.enter_hint"), HiRes.Style.ITALIC, 6.5f, cx, top + H - 30, MUTED);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    public void type(String text) {
        field.setValue(text);
    }

    public void submit() {
        if (riddle >= 0 && !field.getValue().isBlank()) {
            send(field.getValue(), new int[0], riddle);
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
