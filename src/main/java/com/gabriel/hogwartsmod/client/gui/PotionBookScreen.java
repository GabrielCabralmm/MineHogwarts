package com.gabriel.hogwartsmod.client.gui;

import com.gabriel.hogwartsmod.potion.Brew;
import com.gabriel.hogwartsmod.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

import static com.gabriel.hogwartsmod.client.gui.MissionScreen.tr;

public class PotionBookScreen extends Screen {
    private static final int BOOK_W = 300;
    private static final int BOOK_H = 190;
    private static final int INK = 0xFF38261A;
    private static final int TITLE = 0xFF6E281E;
    private static final int MUTED = 0xFF82684C;
    private static final int GOLD = 0xFFBE8C28;
    private static final int PAGE_LINE = 0xFFC4AA7C;
    private static final ResourceLocation BOOK = MissionScreen.tex("grimoire");

    private final int edition;
    private final List<Brew> brews;
    private final List<Row> rows = new ArrayList<>();
    private int selected;
    private int left;
    private int top;

    private record Row(Brew.Action header, Brew brew) {
    }

    public PotionBookScreen(int edition) {
        super(Component.translatable("item.hogwartsmod.livro_pocoes_" + edition));
        this.edition = edition;
        this.brews = Brew.ofDifficulty(edition);
        Brew.Action last = null;
        for (Brew brew : brews) {
            if (brew.getAction() != last) {
                rows.add(new Row(brew.getAction(), null));
                last = brew.getAction();
            }
            rows.add(new Row(null, brew));
        }
    }

    public static void open(int edition) {
        Minecraft.getInstance().setScreen(new PotionBookScreen(edition));
    }

    @Override
    protected void init() {
        left = (width - BOOK_W) / 2;
        top = (height - BOOK_H) / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int rowY(int i) {
        int y = top + 46;
        for (int r = 0; r < i; r++) {
            y += rows.get(r).header() != null ? 13 : 17;
        }
        return y;
    }

    // -- Desenho

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        HiRes.image(g, BOOK, left, top, BOOK_W, BOOK_H);
        int lx = left + 16;
        int pageW = BOOK_W / 2 - 26;
        float lc = lx + pageW / 2f;
        HiRes.centered(g, tr("screen.hogwartsmod.potion_book.title"), HiRes.Style.BOLD, 10f, lc, top + 15, TITLE);
        HiRes.centered(g, tr("screen.hogwartsmod.potion_book.volume", roman(edition)), HiRes.Style.ITALIC, 7.5f, lc, top + 28, MUTED);
        g.fill(lx + 20, top + 39, lx + pageW - 20, top + 40, PAGE_LINE);
        if (brews.isEmpty()) {
            int y = top + 70;
            for (String line : HiRes.wrap(tr("screen.hogwartsmod.potion_book.empty"), HiRes.Style.ITALIC, pageW - 10)) {
                HiRes.centered(g, line, HiRes.Style.ITALIC, lc, y, MUTED);
                y += 10;
            }
        }
        int index = 0;
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            int y = rowY(i);
            if (row.header() != null) {
                HiRes.text(g, row.header().getDisplayName().getString().toUpperCase(), HiRes.Style.BOLD, 6.5f, lx + 2, y + 2, GOLD);
                continue;
            }
            int brewIndex = index++;
            boolean hover = mouseX >= lx - 2 && mouseX < lx + pageW + 2 && mouseY >= y - 2 && mouseY < y + 14;
            if (brewIndex == selected || hover) {
                HiRes.highlight(g, lx - 2, y - 2, lx + pageW + 2, y + 14, brewIndex == selected ? HiRes.Mark.SELECTED : HiRes.Mark.HOVER);
            }
            MissionScreen.item(g, new ItemStack(ModItems.potion(row.brew())), lx + 3, y, 11);
            HiRes.text(g, row.brew().getDisplayName().getString(), HiRes.Style.BOLD, 7.5f, lx + 18, y + 2, INK);
        }
        if (!brews.isEmpty()) {
            drawRecipe(g, brews.get(Math.min(selected, brews.size() - 1)));
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawRecipe(GuiGraphics g, Brew brew) {
        int rx = left + BOOK_W / 2 + 10;
        int rw = BOOK_W / 2 - 26;
        float cx = rx + rw / 2f;
        HiRes.centered(g, brew.getDisplayName().getString(), HiRes.Style.BOLD, 10f, cx, top + 15, TITLE);
        String action = brew.getAction().getDisplayName().getString();
        HiRes.centered(g, action + " · " + tr("screen.hogwartsmod.potion_book.level", brew.getDifficulty()), HiRes.Style.ITALIC, 7f, cx, top + 28, GOLD);
        int y = top + 39;
        for (String line : HiRes.wrap(brew.getEffectText().getString(), HiRes.Style.ITALIC, rw * HiRes.TEXT_SIZE / 7f)) {
            HiRes.centered(g, line, HiRes.Style.ITALIC, 7f, cx, y, MUTED);
            y += 8;
        }
        y += 4;
        HiRes.text(g, tr("screen.hogwartsmod.potion_book.prepare"), HiRes.Style.BOLD, 8f, rx, y, INK);
        y += 11;
        MissionScreen.item(g, new ItemStack(Items.WATER_BUCKET), rx + 8, y - 1, 10);
        HiRes.text(g, HiRes.ellipsize(tr("screen.hogwartsmod.potion_book.base"), HiRes.Style.REGULAR, (rw - 22) * HiRes.TEXT_SIZE / 7f),
                HiRes.Style.REGULAR, 7f, rx + 21, y, INK);
        y += 11;
        int n = 1;
        for (Brew.Step step : brew.getSteps()) {
            String text;
            ItemStack icon;
            if (step.isStir()) {
                icon = ItemStack.EMPTY;
                text = tr("screen.hogwartsmod.potion_book.stir", step.count());
            } else if (step.isWand()) {
                icon = new ItemStack(ModItems.WAND.get());
                text = tr("screen.hogwartsmod.potion_book.wand", step.count());
            } else {
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(step.item()));
                icon = new ItemStack(item);
                text = step.count() + "× " + item.getDescription().getString();
            }
            HiRes.text(g, n + ".", HiRes.Style.BOLD, 7f, rx, y, GOLD);
            if (icon.isEmpty()) {
                HiRes.image(g, MissionScreen.tex("colher"), rx + 8, y - 1, 10, 10);
            } else {
                MissionScreen.item(g, icon, rx + 8, y - 1, 10);
            }
            HiRes.text(g, HiRes.ellipsize(text, HiRes.Style.REGULAR, (rw - 22) * HiRes.TEXT_SIZE / 7f), HiRes.Style.REGULAR, 7f, rx + 21, y, INK);
            y += 11;
            n++;
        }
        HiRes.text(g, n + ".", HiRes.Style.BOLD, 7f, rx, y, GOLD);
        MissionScreen.item(g, new ItemStack(Items.CAMPFIRE), rx + 8, y - 1, 10);
        HiRes.text(g, tr("screen.hogwartsmod.potion_book.boil", brew.getSeconds()), HiRes.Style.REGULAR, 7f, rx + 21, y, INK);
        HiRes.centered(g, tr("screen.hogwartsmod.potion_book.doses", brew.getDoses()), HiRes.Style.ITALIC, 6.5f, cx, top + BOOK_H - 20, MUTED);
    }

    private static String roman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            default -> "V";
        };
    }

    // -- Interação

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int lx = left + 16;
        int pageW = BOOK_W / 2 - 26;
        int index = 0;
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).header() != null) {
                continue;
            }
            int brewIndex = index++;
            int y = rowY(i);
            if (button == 0 && mx >= lx - 2 && mx < lx + pageW + 2 && my >= y - 2 && my < y + 14) {
                selected = brewIndex;
                if (minecraft != null) {
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
                }
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }
}
