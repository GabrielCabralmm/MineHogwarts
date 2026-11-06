package com.gabriel.hogwartsmod.client.gui;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.mission.Mission;
import com.gabriel.hogwartsmod.mission.MissionManager;
import com.gabriel.hogwartsmod.mission.MissionProgress;
import com.gabriel.hogwartsmod.mission.MissionView;
import com.gabriel.hogwartsmod.network.MissionActionPacket;
import com.gabriel.hogwartsmod.network.ModNetwork;
import com.gabriel.hogwartsmod.network.OpenMissionsPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MissionScreen extends Screen {
    private static final int W = 320;
    private static final int H = 210;
    private static final int ROW = 16;

    static final int INK = 0xFF38261A;
    static final int TITLE = 0xFF6E281E;
    static final int MUTED = 0xFF82684C;
    static final int GOLD = 0xFFBE8C28;
    static final int GREEN = 0xFF3F6B2A;
    static final int WINE = 0xFF8E2A2A;
    static final int LINE = 0xFFC4AA7C;
    static final int COVER = 0xFF5C3A22;
    static final int NOTE = 0xFFD4C49E;
    static final int NOTE_ON = 0xFFF0E4C4;
    static final int NOTE_OFF = 0xFFD6C298;
    static final int NOTE_EDGE = 0xFF967E5A;

    private static final ResourceLocation SHEET = tex("pulpit_sheet");

    private OpenMissionsPacket data;
    @Nullable
    private String selected;
    private int left;
    private int top;

    public MissionScreen(OpenMissionsPacket data) {
        super(Component.literal(data.name()));
        this.data = data;
        if (!data.missions().isEmpty()) {
            selected = data.missions().get(0).mission().id();
        }
    }

    static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "textures/gui/" + name + ".png");
    }

    public int entity() {
        return data.entity();
    }

    public void update(OpenMissionsPacket packet) {
        this.data = packet;
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
    private MissionView current() {
        for (MissionView view : data.missions()) {
            if (view.mission().id().equals(selected)) {
                return view;
            }
        }
        return null;
    }

    static String tr(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    // -- Desenho

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        HiRes.image(g, SHEET, left, top, W, H);
        HiRes.centered(g, title.getString(), HiRes.Style.BOLD, 11f, left + W / 2f, top + 14, TITLE);
        g.fill(left + 160, top + 30, left + 161, top + H - 14, LINE);
        drawList(g, mouseX, mouseY);
        drawDetails(g, mouseX, mouseY);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawList(GuiGraphics g, int mouseX, int mouseY) {
        int x = left + 18;
        int w = 128;
        HiRes.text(g, tr("screen.hogwartsmod.missions.list"), HiRes.Style.BOLD, x, top + 31, INK);
        List<MissionView> views = data.missions();
        if (views.isEmpty()) {
            HiRes.text(g, tr("screen.hogwartsmod.missions.none"), HiRes.Style.ITALIC, x, top + 47, MUTED);
        }
        for (int i = 0; i < views.size(); i++) {
            MissionView view = views.get(i);
            int y = top + 44 + i * ROW;
            boolean chosen = view.mission().id().equals(selected);
            boolean hover = mouseX >= x - 2 && mouseX < x + w + 2 && mouseY >= y - 1 && mouseY < y + ROW - 2;
            if (chosen || hover) {
                HiRes.highlight(g, x - 2, y - 1, x + w + 2, y + ROW - 2, chosen ? HiRes.Mark.SELECTED : HiRes.Mark.HOVER);
            }
            String status = stateLabel(view);
            float sw = HiRes.width(status, HiRes.Style.ITALIC, 7f);
            String name = view.mission().title().isBlank() ? "?" : view.mission().title();
            boolean faded = view.state() == MissionView.State.DONE || view.state() == MissionView.State.COOLDOWN;
            HiRes.text(g, HiRes.ellipsize(name, HiRes.Style.REGULAR, w - sw - 8), HiRes.Style.REGULAR, x + 3, y + 2, faded ? MUTED : INK);
            HiRes.text(g, status, HiRes.Style.ITALIC, 7f, x + w - sw, y + 3, stateColor(view.state()));
        }
        HiRes.text(g, tr("screen.hogwartsmod.missions.active", data.activeCount(), MissionProgress.MAX_ACTIVE), HiRes.Style.ITALIC,
                x, top + H - 24, MUTED);
    }

    static String stateLabel(MissionView view) {
        return switch (view.state()) {
            case AVAILABLE -> tr("screen.hogwartsmod.missions.state.available");
            case ACTIVE -> view.progress() + "/" + view.mission().amount();
            case READY -> tr("screen.hogwartsmod.missions.state.ready");
            case COOLDOWN -> formatWait(view.cooldown());
            case DONE -> tr("screen.hogwartsmod.missions.state.done");
        };
    }

    static int stateColor(MissionView.State state) {
        return switch (state) {
            case AVAILABLE -> GREEN;
            case ACTIVE -> GOLD;
            case READY -> WINE;
            default -> MUTED;
        };
    }

    static String formatWait(long millis) {
        long minutes = Math.max(1, millis / 60000);
        if (minutes >= 60) {
            return (minutes / 60) + "h " + (minutes % 60) + "m";
        }
        return minutes + "m";
    }

    private void drawDetails(GuiGraphics g, int mouseX, int mouseY) {
        MissionView view = current();
        int x = left + 174;
        int w = 128;
        float cx = x + w / 2f;
        if (view == null) {
            HiRes.centered(g, tr("screen.hogwartsmod.missions.pick"), HiRes.Style.ITALIC, cx, top + 95, MUTED);
            return;
        }
        Mission m = view.mission();
        HiRes.centered(g, HiRes.ellipsize(m.title().isBlank() ? "?" : m.title(), HiRes.Style.BOLD, w), HiRes.Style.BOLD, 9.5f, cx, top + 30, TITLE);
        int y = top + 45;
        if (!m.description().isBlank()) {
            List<String> lines = HiRes.wrap(m.description(), HiRes.Style.ITALIC, w);
            for (int i = 0; i < Math.min(4, lines.size()); i++) {
                HiRes.centered(g, lines.get(i), HiRes.Style.ITALIC, cx, y, MUTED);
                y += 10;
            }
        }
        y += 4;
        HiRes.text(g, tr("screen.hogwartsmod.missions.objective"), HiRes.Style.BOLD, x, y, INK);
        y += 11;
        String objective = m.objectiveText().getString();
        for (String line : HiRes.wrap(objective, HiRes.Style.REGULAR, w)) {
            HiRes.text(g, line, HiRes.Style.REGULAR, x, y, INK);
            y += 10;
        }
        if (view.state() == MissionView.State.ACTIVE || view.state() == MissionView.State.READY) {
            int bw = w;
            g.fill(x, y + 1, x + bw, y + 6, NOTE_EDGE);
            g.fill(x + 1, y + 2, x + bw - 1, y + 5, NOTE_OFF);
            int filled = Math.round((bw - 2) * Math.min(1f, view.progress() / (float) m.amount()));
            g.fill(x + 1, y + 2, x + 1 + filled, y + 5, view.state() == MissionView.State.READY ? 0xFF6E9A3A : GOLD);
            y += 10;
        }
        y += 2;
        HiRes.text(g, tr("screen.hogwartsmod.missions.reward"), HiRes.Style.BOLD, x, y, INK);
        y += 11;
        drawReward(g, x, y, m);
        y += 14;
        String repeat = m.repeatable() ? tr("screen.hogwartsmod.missions.repeat", m.cooldownHours()) : tr("screen.hogwartsmod.missions.once");
        HiRes.text(g, repeat, HiRes.Style.ITALIC, 7.5f, x, y, MUTED);

        int by = top + H - 30;
        switch (view.state()) {
            case AVAILABLE -> button(g, x, by, w, tr("screen.hogwartsmod.missions.accept"), data.activeCount() < MissionProgress.MAX_ACTIVE, TITLE, mouseX, mouseY);
            case READY -> button(g, x, by, w, tr("screen.hogwartsmod.missions.complete"), true, GREEN, mouseX, mouseY);
            case ACTIVE -> button(g, x, by, w, tr("screen.hogwartsmod.missions.abandon"), true, WINE, mouseX, mouseY);
            case COOLDOWN -> button(g, x, by, w, tr("screen.hogwartsmod.missions.wait", formatWait(view.cooldown())), false, MUTED, mouseX, mouseY);
            case DONE -> button(g, x, by, w, tr("screen.hogwartsmod.missions.state.done"), false, MUTED, mouseX, mouseY);
        }
    }

    static void drawReward(GuiGraphics g, int x, int y, Mission m) {
        int[] amounts = {m.galeoes(), m.sicles(), m.nuques()};
        String[] ids = {"galeao", "sicle", "nuque"};
        float cx = x;
        boolean any = false;
        for (int i = 0; i < 3; i++) {
            if (amounts[i] <= 0) {
                continue;
            }
            any = true;
            HiRes.image(g, tex("moeda_" + ids[i]), Math.round(cx), y - 1, 10, 11);
            String n = String.valueOf(amounts[i]);
            HiRes.text(g, n, HiRes.Style.BOLD, cx + 12, y, INK);
            cx += 12 + HiRes.width(n, HiRes.Style.BOLD) + 8;
        }
        if (!any) {
            HiRes.text(g, tr("screen.hogwartsmod.missions.no_reward"), HiRes.Style.ITALIC, x, y, MUTED);
        }
    }

    static void button(GuiGraphics g, int x, int y, int w, String label, boolean enabled, int color, int mouseX, int mouseY) {
        boolean hover = enabled && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + 15;
        g.fill(x, y, x + w, y + 15, enabled ? COVER : NOTE_EDGE);
        g.fill(x + 1, y + 1, x + w - 1, y + 14, hover ? NOTE_ON : enabled ? NOTE : NOTE_OFF);
        HiRes.centered(g, HiRes.ellipsize(label, HiRes.Style.BOLD, w - 6), HiRes.Style.BOLD, x + w / 2f, y + 3, enabled ? color : MUTED);
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
        int x = left + 18;
        List<MissionView> views = data.missions();
        for (int i = 0; i < views.size(); i++) {
            int y = top + 44 + i * ROW;
            if (mx >= x - 2 && mx < x + 130 && my >= y - 1 && my < y + ROW - 2) {
                selected = views.get(i).mission().id();
                click();
                return true;
            }
        }
        MissionView view = current();
        int bx = left + 174;
        int by = top + H - 30;
        if (view != null && mx >= bx && mx < bx + 128 && my >= by && my < by + 15) {
            MissionManager.Action action = switch (view.state()) {
                case AVAILABLE -> data.activeCount() < MissionProgress.MAX_ACTIVE ? MissionManager.Action.ACCEPT : null;
                case READY -> MissionManager.Action.COMPLETE;
                case ACTIVE -> MissionManager.Action.ABANDON;
                default -> null;
            };
            if (action != null) {
                ModNetwork.CHANNEL.sendToServer(new MissionActionPacket(data.entity(), view.mission().id(), action));
                click();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }
}
