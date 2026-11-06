package com.gabriel.hogwartsmod.client.gui;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.magic.MagicData;
import com.gabriel.hogwartsmod.network.ModNetwork;
import com.gabriel.hogwartsmod.network.NpcLearnPacket;
import com.gabriel.hogwartsmod.spell.Spell;
import com.gabriel.hogwartsmod.spell.Spells;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class NpcScreen extends Screen {
    private static final int W = 240;
    private static final int H = 220;
    private static final int ROW = 18;
    private static final int ROWS = 7;

    private static final int INK = 0xFF38261A;
    private static final int TITLE = 0xFF6E281E;
    private static final int MUTED = 0xFF82684C;
    private static final int GREEN = 0xFF3F6B2A;
    private static final int WINE = 0xFF8E2A2A;
    private static final int LINE = 0xFFC4AA7C;

    private static final ResourceLocation SHEET =
            ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "textures/gui/trace_sheet.png");

    private enum Status { LEARN, KNOWS, LOW_LEVEL }

    private final int entity;
    private final String name;
    private final List<Spell> spells = new ArrayList<>();
    private int scroll;
    private int left;
    private int top;

    public NpcScreen(int entity, String name, List<String> spellIds) {
        super(Component.literal(name));
        this.entity = entity;
        this.name = name;
        for (String id : spellIds) {
            Spell spell = Spells.get(id);
            if (spell != null) {
                spells.add(spell);
            }
        }
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

    @Nullable
    private MagicData data() {
        return minecraft != null && minecraft.player != null ? MagicData.get(minecraft.player) : null;
    }

    private Status status(Spell spell) {
        MagicData data = data();
        if (data == null || data.knows(spell)) {
            return Status.KNOWS;
        }
        return data.getLevel() < spell.getRequiredLevel() ? Status.LOW_LEVEL : Status.LEARN;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        HiRes.image(g, SHEET, left, top, W, H);
        float cx = left + W / 2f;
        HiRes.centered(g, name, HiRes.Style.BOLD, 11f, cx, top + 15, TITLE);
        HiRes.centered(g, tr("screen.hogwartsmod.npc.subtitle"), HiRes.Style.ITALIC, cx, top + 30, MUTED);
        int x = left + 26;
        int w = W - 52;
        g.fill(x, top + 42, x + w, top + 43, LINE);
        if (spells.isEmpty()) {
            List<String> lines = HiRes.wrap(tr("screen.hogwartsmod.npc.empty"), HiRes.Style.ITALIC, w);
            for (int i = 0; i < lines.size(); i++) {
                HiRes.centered(g, lines.get(i), HiRes.Style.ITALIC, cx, top + 90 + i * 10, MUTED);
            }
        }
        for (int i = 0; i < ROWS && i + scroll < spells.size(); i++) {
            Spell spell = spells.get(i + scroll);
            Status status = status(spell);
            int y = top + 48 + i * ROW;
            boolean hover = status == Status.LEARN && mouseX >= x - 3 && mouseX < x + w + 3 && mouseY >= y - 1 && mouseY < y + ROW - 1;
            if (hover) {
                HiRes.highlight(g, x - 3, y - 1, x + w + 3, y + ROW - 1, HiRes.Mark.HOVER);
            }
            SpellIcons.draw(g, spell, x, y + 1, 14);
            HiRes.text(g, spell.getDisplayName().getString(), HiRes.Style.REGULAR, x + 19, y, status == Status.KNOWS ? MUTED : INK);
            String detail = Component.translatable("screen.hogwartsmod.npc.detail", spell.getRequiredLevel(),
                    Component.translatable("category.hogwartsmod." + spell.getCategory().name().toLowerCase())).getString();
            HiRes.text(g, detail, HiRes.Style.ITALIC, 7f, x + 19, y + 9, MUTED);
            String label = switch (status) {
                case LEARN -> tr("screen.hogwartsmod.npc.learn");
                case KNOWS -> tr("teach.hogwartsmod.status.knows");
                case LOW_LEVEL -> Component.translatable("screen.hogwartsmod.npc.level", spell.getRequiredLevel()).getString();
            };
            int color = switch (status) {
                case LEARN -> GREEN;
                case KNOWS -> MUTED;
                case LOW_LEVEL -> WINE;
            };
            HiRes.right(g, label, status == Status.LEARN ? HiRes.Style.BOLD : HiRes.Style.ITALIC, x + w, y + 4, color);
        }
        if (spells.size() > ROWS) {
            HiRes.right(g, (scroll + 1) + "–" + Math.min(spells.size(), scroll + ROWS) + " / " + spells.size(),
                    HiRes.Style.ITALIC, x + w, top + 178, MUTED);
        }
        HiRes.centered(g, tr("screen.hogwartsmod.npc.hint"), HiRes.Style.ITALIC, cx, top + 192, MUTED);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int x = left + 26;
        int w = W - 52;
        if (button == 0 && mx >= x - 3 && mx < x + w + 3 && my >= top + 47) {
            int row = (int) ((my - (top + 47)) / ROW);
            if (row < ROWS && row + scroll < spells.size()) {
                Spell spell = spells.get(row + scroll);
                if (status(spell) == Status.LEARN) {
                    ModNetwork.CHANNEL.sendToServer(new NpcLearnPacket(entity, spell.getId()));
                    if (minecraft != null) {
                        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
                    }
                }
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scroll = Math.max(0, Math.min(Math.max(0, spells.size() - ROWS), scroll - (int) Math.signum(delta)));
        return true;
    }

    private static String tr(String key) {
        return Component.translatable(key).getString();
    }
}
