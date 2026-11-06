package com.gabriel.hogwartsmod.client.gui;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.client.zone.ClientZones;
import com.gabriel.hogwartsmod.network.ModNetwork;
import com.gabriel.hogwartsmod.network.ZoneEditPacket;
import com.gabriel.hogwartsmod.zone.Zone;
import com.gabriel.hogwartsmod.zone.ZoneRule;
import com.gabriel.hogwartsmod.zone.Zones;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class ZoneScreen extends Screen {
    private static final int W = 320;
    private static final int H = 210;
    private static final int ROW = 14;
    private static final int ROWS = 7;
    private static final int RULE_ROW = 17;

    private static final int INK = 0xFF38261A;
    private static final int NAME = 0xFF7A2618;
    private static final int TITLE = 0xFF6E281E;
    private static final int MUTED = 0xFF82684C;
    private static final int HIGHLIGHT = 0xFFDCC696;
    private static final int GOLD = 0xFFBE8C28;
    private static final int WINE = 0xFF8E2A2A;
    private static final int LINE = 0xFFC4AA7C;
    private static final int COVER = 0xFF5C3A22;
    private static final int NOTE = 0xFFD4C49E;
    private static final int NOTE_ON = 0xFFF0E4C4;
    private static final int NOTE_OFF = 0xFFD6C298;
    private static final int NOTE_EDGE = 0xFF967E5A;

    private static final ResourceLocation SHEET = tex("pulpit_sheet");
    private static final ResourceLocation CHECK = tex("check");

    @Nullable
    private static String lastSelected;

    private EditBox nameBox;
    @Nullable
    private String selected;
    private int scroll;
    private boolean confirmRemove;
    private int left;
    private int top;

    public ZoneScreen() {
        super(Component.translatable("screen.hogwartsmod.zones"));
        selected = lastSelected;
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new ZoneScreen());
    }

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "textures/gui/" + name + ".png");
    }

    @Nullable
    public String selectedName() {
        return selected;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        nameBox = new EditBox(font, left + 21, top + 167, 122, 10, Component.empty());
        nameBox.setBordered(false);
        nameBox.setMaxLength(ZoneEditPacket.MAX_NAME);
        nameBox.setTextColor(NAME);
        nameBox.setHint(Component.translatable("screen.hogwartsmod.zones.name_hint").withStyle(s -> s.withColor(0xA08A6E)));
        addWidget(nameBox);
    }

    @Override
    public void removed() {
        lastSelected = selected;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private List<Zone> zones() {
        return Zones.clientZones;
    }

    @Nullable
    private Zone current() {
        if (selected == null) {
            return null;
        }
        for (Zone zone : zones()) {
            if (zone.name().equals(selected)) {
                return zone;
            }
        }
        return null;
    }

    @Nullable
    private Zone touching() {
        if (!hasSelection() || minecraft == null || minecraft.level == null) {
            return null;
        }
        String dim = minecraft.level.dimension().location().toString();
        Zone.Box area = Zone.Box.of(ClientZones.first, ClientZones.second);
        for (Zone zone : zones()) {
            boolean hit = ClientZones.cut ? zone.touches(dim, area)
                    : zone.contains(dim, ClientZones.first) || zone.contains(dim, ClientZones.second);
            if (hit) {
                return zone;
            }
        }
        return null;
    }

    private boolean cutMode() {
        return ClientZones.cut && hasSelection();
    }

    private boolean hasSelection() {
        return ClientZones.first != null && ClientZones.second != null;
    }

    private static String tr(String key, Object... args) {
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
        drawNew(g, mouseX, mouseY);
        drawDetails(g, mouseX, mouseY);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawList(GuiGraphics g, int mouseX, int mouseY) {
        int x = left + 18;
        int w = 128;
        HiRes.text(g, tr("screen.hogwartsmod.zones.list"), HiRes.Style.BOLD, x, top + 31, INK);
        List<Zone> zones = zones();
        if (zones.isEmpty()) {
            HiRes.text(g, tr("screen.hogwartsmod.zones.empty"), HiRes.Style.ITALIC, x, top + 46, MUTED);
        }
        String dim = minecraft != null && minecraft.level != null ? minecraft.level.dimension().location().toString() : "";
        for (int i = 0; i < ROWS && i + scroll < zones.size(); i++) {
            Zone zone = zones.get(i + scroll);
            int y = top + 44 + i * ROW;
            boolean hover = mouseX >= x - 2 && mouseX < x + w + 2 && mouseY >= y - 1 && mouseY < y + ROW - 1;
            if (zone.name().equals(selected) || hover) {
                g.fill(x - 2, y - 1, x + w + 2, y + ROW - 1, HIGHLIGHT);
            }
            int rules = Integer.bitCount(zone.rules());
            String count = rules == 0 ? tr("screen.hogwartsmod.zones.no_rules") : tr("screen.hogwartsmod.zones.rule_count", rules);
            float countW = HiRes.width(count, HiRes.Style.ITALIC);
            boolean here = zone.dimension().equals(dim);
            HiRes.text(g, HiRes.ellipsize(zone.name(), HiRes.Style.REGULAR, w - countW - 6), HiRes.Style.REGULAR, x, y + 1, here ? INK : MUTED);
            HiRes.right(g, count, HiRes.Style.ITALIC, x + w, y + 1, MUTED);
        }
        if (zones.size() > ROWS) {
            String page = (scroll + 1) + "–" + Math.min(zones.size(), scroll + ROWS) + " / " + zones.size();
            HiRes.right(g, page, HiRes.Style.ITALIC, x + w, top + 31, MUTED);
        }
        g.fill(x, top + 143, x + w, top + 144, LINE);
    }

    private void drawNew(GuiGraphics g, int mouseX, int mouseY) {
        int x = left + 18;
        int w = 128;
        Zone touching = touching();
        if (cutMode()) {
            nameBox.visible = false;
            nameBox.setFocused(false);
            HiRes.text(g, tr("screen.hogwartsmod.zones.cut_title"), HiRes.Style.BOLD, x, top + 148, INK);
            HiRes.right(g, selectionStatus(), HiRes.Style.ITALIC, x + w, top + 148, WINE);
            String info = touching == null ? tr("screen.hogwartsmod.zones.cut_none") : tr("screen.hogwartsmod.zones.cut_inside", touching.name());
            HiRes.text(g, HiRes.ellipsize(info, HiRes.Style.ITALIC, w), HiRes.Style.ITALIC, x, top + 166, MUTED);
            button(g, x, top + 181, w, touching == null ? tr("screen.hogwartsmod.zones.cut_button_none") : tr("screen.hogwartsmod.zones.cut", touching.name()),
                    touching != null, WINE, mouseX, mouseY);
            return;
        }
        nameBox.visible = touching == null;
        if (touching != null && nameBox.isFocused()) {
            nameBox.setFocused(false);
        }
        HiRes.text(g, tr(touching == null ? "screen.hogwartsmod.zones.new" : "screen.hogwartsmod.zones.expand_title"), HiRes.Style.BOLD, x, top + 148, INK);
        String status = selectionStatus();
        HiRes.right(g, status, HiRes.Style.ITALIC, x + w, top + 148, hasSelection() ? GOLD : MUTED);
        if (touching != null) {
            HiRes.text(g, HiRes.ellipsize(tr("screen.hogwartsmod.zones.inside", touching.name()), HiRes.Style.ITALIC, w), HiRes.Style.ITALIC, x, top + 166, MUTED);
            button(g, x, top + 181, w, tr("screen.hogwartsmod.zones.expand", touching.name()), true, TITLE, mouseX, mouseY);
            return;
        }
        g.fill(x, top + 164, x + w, top + 177, NOTE_EDGE);
        g.fill(x + 1, top + 165, x + w - 1, top + 176, nameBox.isFocused() ? NOTE_ON : NOTE);
        drawName(g, x + 4, top + 166, w - 8);
        boolean ready = canCreate();
        button(g, x, top + 181, w, tr("screen.hogwartsmod.zones.create"), ready, TITLE, mouseX, mouseY);
    }

    private void drawName(GuiGraphics g, int x, int y, int w) {
        String value = nameBox.getValue();
        if (value.isEmpty() && !nameBox.isFocused()) {
            HiRes.text(g, tr("screen.hogwartsmod.zones.name_hint"), HiRes.Style.ITALIC, x, y, MUTED);
            return;
        }
        int cursor = Math.min(nameBox.getCursorPosition(), value.length());
        String shown = value;
        int offset = 0;
        while (offset < cursor && HiRes.width(value.substring(offset, cursor), HiRes.Style.REGULAR) > w - 3) {
            offset++;
        }
        shown = HiRes.ellipsize(value.substring(offset), HiRes.Style.REGULAR, w);
        HiRes.text(g, shown, HiRes.Style.REGULAR, x, y, NAME);
        if (nameBox.isFocused() && (System.currentTimeMillis() / 500) % 2 == 0) {
            float cx = x + HiRes.width(value.substring(offset, cursor), HiRes.Style.REGULAR) + 0.5f;
            g.fill((int) cx, y - 1, (int) cx + 1, y + 9, NAME);
        }
    }

    private String selectionStatus() {
        BlockPos a = ClientZones.first;
        BlockPos b = ClientZones.second;
        if (a != null && b != null) {
            return tr("screen.hogwartsmod.zones.selection", Math.abs(a.getX() - b.getX()) + 1, Math.abs(a.getY() - b.getY()) + 1,
                    Math.abs(a.getZ() - b.getZ()) + 1);
        }
        return tr("screen.hogwartsmod.zones.no_selection");
    }

    private boolean canCreate() {
        String name = nameBox == null ? "" : nameBox.getValue().strip();
        if (!hasSelection() || name.isEmpty() || cutMode() || touching() != null) {
            return false;
        }
        for (Zone zone : zones()) {
            if (zone.name().equalsIgnoreCase(name)) {
                return false;
            }
        }
        return true;
    }

    private void drawDetails(GuiGraphics g, int mouseX, int mouseY) {
        int x = left + 176;
        int w = 126;
        float cx = x + w / 2f;
        Zone zone = current();
        if (zone == null) {
            HiRes.centered(g, tr("screen.hogwartsmod.zones.pick"), HiRes.Style.ITALIC, cx, top + 90, MUTED);
            HiRes.centered(g, tr("screen.hogwartsmod.zones.help1"), HiRes.Style.ITALIC, cx, top + 120, MUTED);
            HiRes.centered(g, tr("screen.hogwartsmod.zones.help2"), HiRes.Style.ITALIC, cx, top + 131, MUTED);
            HiRes.centered(g, tr("screen.hogwartsmod.zones.help3"), HiRes.Style.ITALIC, cx, top + 146, 0xFF8E2A2A);
            return;
        }
        HiRes.centered(g, HiRes.ellipsize(zone.name(), HiRes.Style.BOLD, w), HiRes.Style.BOLD, 10f, cx, top + 30, TITLE);
        if (zone.boxes().size() == 1) {
            Zone.Box box = zone.boxes().get(0);
            BlockPos min = box.min();
            BlockPos max = box.max();
            HiRes.centered(g, tr("screen.hogwartsmod.zones.size", max.getX() - min.getX() + 1, max.getY() - min.getY() + 1, max.getZ() - min.getZ() + 1),
                    HiRes.Style.ITALIC, cx, top + 44, MUTED);
            String coords = min.getX() + " " + min.getY() + " " + min.getZ() + "  →  " + max.getX() + " " + max.getY() + " " + max.getZ();
            HiRes.centered(g, HiRes.ellipsize(coords, HiRes.Style.ITALIC, w), HiRes.Style.ITALIC, cx, top + 54, MUTED);
        } else {
            int cuts = zone.cuts();
            String volume = String.format("%,d", zone.volume()).replace(',', '.');
            HiRes.centered(g, cuts == 0 ? tr("screen.hogwartsmod.zones.parts", zone.boxes().size(), volume)
                            : cuts == 1 ? tr("screen.hogwartsmod.zones.parts_cut", zone.boxes().size() - cuts, volume)
                            : tr("screen.hogwartsmod.zones.parts_cuts", zone.boxes().size() - cuts, cuts, volume),
                    HiRes.Style.ITALIC, cx, top + 44, MUTED);
            String undo = tr("screen.hogwartsmod.zones.undo");
            float uw = HiRes.width(undo, HiRes.Style.BOLD);
            boolean hover = mouseX >= cx - uw / 2 - 2 && mouseX < cx + uw / 2 + 2 && mouseY >= top + 53 && mouseY < top + 64;
            if (hover) {
                g.fill((int) (cx - uw / 2 - 2), top + 53, (int) (cx + uw / 2 + 2), top + 64, HIGHLIGHT);
            }
            HiRes.centered(g, undo, HiRes.Style.BOLD, cx, top + 54, WINE);
        }

        HiRes.text(g, tr("screen.hogwartsmod.zones.rules"), HiRes.Style.BOLD, x, top + 68, INK);
        ZoneRule[] rules = ZoneRule.values();
        for (int i = 0; i < rules.length; i++) {
            ZoneRule rule = rules[i];
            int y = top + 80 + i * RULE_ROW;
            boolean hover = mouseX >= x - 2 && mouseX < x + w + 2 && mouseY >= y - 2 && mouseY < y + RULE_ROW - 3;
            if (hover) {
                g.fill(x - 2, y - 2, x + w + 2, y + RULE_ROW - 3, HIGHLIGHT);
            }
            g.fill(x, y + 2, x + 9, y + 11, NOTE_EDGE);
            g.fill(x + 1, y + 3, x + 8, y + 10, NOTE_ON);
            if (zone.has(rule)) {
                HiRes.image(g, CHECK, x, y + 1, 10, 10);
            }
            HiRes.image(g, tex("zone_" + rule.getId()), x + 13, y, 13, 13);
            HiRes.text(g, tr("screen.hogwartsmod.zones.rule." + rule.getId()), HiRes.Style.REGULAR, x + 30, y + 3, zone.has(rule) ? INK : MUTED);
        }

        button(g, x, top + 152, w, tr("screen.hogwartsmod.zones.redefine"), hasSelection() && !ClientZones.cut, TITLE, mouseX, mouseY);
        button(g, x, top + 172, w, confirmRemove ? tr("screen.hogwartsmod.zones.confirm_remove") : tr("screen.hogwartsmod.zones.remove"),
                true, WINE, mouseX, mouseY);
    }

    private void button(GuiGraphics g, int x, int y, int w, String label, boolean enabled, int color, int mouseX, int mouseY) {
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

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void send(ZoneEditPacket.Action action, String name, String rule, boolean value) {
        ModNetwork.CHANNEL.sendToServer(new ZoneEditPacket(action, name, rule, value));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) {
            return super.mouseClicked(mx, my, button);
        }
        Zone touching = touching();
        if (cutMode()) {
            if (touching != null && inside(mx, my, left + 18, top + 181, 128, 15)) {
                send(ZoneEditPacket.Action.CUT, touching.name(), "", false);
                selected = touching.name();
                click();
                return true;
            }
            touching = null;
        } else if (touching != null && inside(mx, my, left + 18, top + 181, 128, 15)) {
            send(ZoneEditPacket.Action.EXPAND, touching.name(), "", false);
            selected = touching.name();
            click();
            return true;
        }
        if (touching == null && !cutMode() && inside(mx, my, left + 18, top + 164, 128, 13)) {
            nameBox.setFocused(true);
            setFocused(nameBox);
            return super.mouseClicked(mx, my, button);
        }
        nameBox.setFocused(false);
        int x = left + 18;
        List<Zone> zones = zones();
        if (inside(mx, my, x - 2, top + 43, 132, ROWS * ROW)) {
            int row = (int) ((my - (top + 43)) / ROW) + scroll;
            if (row < zones.size()) {
                selected = zones.get(row).name();
                confirmRemove = false;
                click();
                return true;
            }
        }
        if (touching == null && !cutMode() && inside(mx, my, x, top + 181, 128, 15) && canCreate()) {
            String name = nameBox.getValue().strip();
            send(ZoneEditPacket.Action.CREATE, name, "", false);
            selected = name;
            nameBox.setValue("");
            click();
            return true;
        }
        Zone zone = current();
        if (zone != null && zone.boxes().size() > 1 && inside(mx, my, left + 176, top + 53, 126, 11)) {
            send(ZoneEditPacket.Action.UNDO_PART, zone.name(), "", false);
            click();
            return true;
        }
        if (zone != null) {
            int dx = left + 176;
            ZoneRule[] rules = ZoneRule.values();
            for (int i = 0; i < rules.length; i++) {
                int y = top + 80 + i * RULE_ROW;
                if (inside(mx, my, dx - 2, y - 2, 130, RULE_ROW - 1)) {
                    send(ZoneEditPacket.Action.RULE, zone.name(), rules[i].getId(), !zone.has(rules[i]));
                    click();
                    return true;
                }
            }
            if (inside(mx, my, dx, top + 152, 126, 15) && hasSelection() && !ClientZones.cut) {
                send(ZoneEditPacket.Action.REDEFINE, zone.name(), "", false);
                click();
                return true;
            }
            if (inside(mx, my, dx, top + 172, 126, 15)) {
                if (confirmRemove) {
                    send(ZoneEditPacket.Action.REMOVE, zone.name(), "", false);
                    selected = null;
                    confirmRemove = false;
                } else {
                    confirmRemove = true;
                }
                click();
                return true;
            }
        }
        confirmRemove = false;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int max = Math.max(0, zones().size() - ROWS);
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(delta)));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (nameBox.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                nameBox.setFocused(false);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                if (canCreate()) {
                    String name = nameBox.getValue().strip();
                    send(ZoneEditPacket.Action.CREATE, name, "", false);
                    selected = name;
                    nameBox.setValue("");
                    click();
                }
                return true;
            }
            return nameBox.keyPressed(keyCode, scanCode, modifiers) || nameBox.canConsumeInput();
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
