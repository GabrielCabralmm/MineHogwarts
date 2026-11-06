package com.gabriel.hogwartsmod.client.gui;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.house.House;
import com.gabriel.hogwartsmod.network.ModNetwork;
import com.gabriel.hogwartsmod.network.SortingChoosePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class SortingScreen extends Screen {
    private static final int W = 320;
    private static final int H = 210;
    private static final int CREST_W = 68;
    private static final int CREST_H = 89;
    private static final int GAP = 6;

    private static final int TITLE = 0xFF6E281E;
    private static final int MUTED = 0xFF82684C;
    private static final int GOLD = 0xFFBE8C28;
    private static final int HIGHLIGHT = 0xFFDCC696;

    private static final ResourceLocation SHEET = tex("pulpit_sheet");

    private final UUID target;
    private final String name;
    @Nullable
    private final House current;
    private boolean sent;
    private int left;
    private int top;
    private int ticks;

    public SortingScreen(UUID target, String name, @Nullable House current) {
        super(Component.translatable("screen.hogwartsmod.sorting"));
        this.target = target;
        this.name = name;
        this.current = current;
    }

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "textures/gui/" + name + ".png");
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

    @Override
    public void tick() {
        ticks++;
    }

    @Override
    public void removed() {
        if (!sent) {
            sent = true;
            ModNetwork.CHANNEL.sendToServer(new SortingChoosePacket(target, ""));
        }
    }

    private int crestX(int i) {
        int total = House.values().length * CREST_W + (House.values().length - 1) * GAP;
        return left + (W - total) / 2 + i * (CREST_W + GAP);
    }

    private int crestY() {
        return top + 66;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        HiRes.image(g, SHEET, left, top, W, H);
        HiRes.centered(g, title.getString(), HiRes.Style.BOLD, 13f, left + W / 2f, top + 18, TITLE);
        HiRes.centered(g, Component.translatable("screen.hogwartsmod.sorting.question", name).getString(), HiRes.Style.ITALIC, 9.5f,
                left + W / 2f, top + 38, MUTED);
        if (current != null) {
            HiRes.centered(g, Component.translatable("screen.hogwartsmod.sorting.current", current.getDisplayName().getString()).getString(),
                    HiRes.Style.ITALIC, left + W / 2f, top + 51, MUTED);
        }
        House[] houses = House.values();
        for (int i = 0; i < houses.length; i++) {
            House house = houses[i];
            int x = crestX(i);
            int y = crestY();
            boolean hover = mouseX >= x - 3 && mouseX < x + CREST_W + 3 && mouseY >= y - 4 && mouseY < y + CREST_H + 6;
            float lift = hover ? 3f + (float) Math.sin((ticks + partialTick) * 0.25) * 0.8f : 0f;
            if (hover) {
                g.fill(x - 3, y - 6, x + CREST_W + 3, y + CREST_H + 6, HIGHLIGHT);
            }
            if (house == current) {
                g.fill(x + 8, y + CREST_H + 4, x + CREST_W - 8, y + CREST_H + 6, GOLD);
            }
            g.pose().pushPose();
            g.pose().translate(0, -lift, 0);
            HiRes.image(g, tex("casa_" + house.getId()), x, y, CREST_W, CREST_H);
            g.pose().popPose();
        }
        HiRes.centered(g, Component.translatable("screen.hogwartsmod.sorting.hint").getString(), HiRes.Style.ITALIC, left + W / 2f, top + H - 26, MUTED);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            House[] houses = House.values();
            for (int i = 0; i < houses.length; i++) {
                int x = crestX(i);
                int y = crestY();
                if (mx >= x - 3 && mx < x + CREST_W + 3 && my >= y - 4 && my < y + CREST_H + 6) {
                    sent = true;
                    ModNetwork.CHANNEL.sendToServer(new SortingChoosePacket(target, houses[i].getId()));
                    if (minecraft != null) {
                        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
                    }
                    onClose();
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }
}
