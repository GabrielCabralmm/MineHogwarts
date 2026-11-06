package com.gabriel.hogwartsmod.client.gui;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.mission.Mission;
import com.gabriel.hogwartsmod.mission.MissionNpcEntity;
import com.gabriel.hogwartsmod.mission.ObjectiveType;
import com.gabriel.hogwartsmod.network.ModNetwork;
import com.gabriel.hogwartsmod.network.OpenMissionEditorPacket;
import com.gabriel.hogwartsmod.network.SaveMissionNpcPacket;
import com.gabriel.hogwartsmod.zone.Zone;
import com.gabriel.hogwartsmod.zone.Zones;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Consumer;

import static com.gabriel.hogwartsmod.client.gui.MissionScreen.*;

public class MissionEditorScreen extends Screen {
    private static final int W = 340;
    private static final int H = 226;
    private static final int ROW = 14;
    private static final int NAME_COLOR = 0xFF7A2618;
    private static final ResourceLocation SHEET = MissionScreen.tex("pulpit_sheet");

    private final int entity;
    private final List<Mission> missions;
    private final List<String> models;
    private String name;
    private String model;
    private int selected;
    private int left;
    private int top;

    private EditBox nameBox;
    private EditBox titleBox;
    private EditBox descBox;
    private EditBox targetBox;
    private EditBox searchBox;
    private boolean rewardPage;
    private List<net.minecraft.world.item.Item> results = List.of();
    private final List<EditBox> boxes = new ArrayList<>();

    public MissionEditorScreen(OpenMissionEditorPacket packet) {
        super(Component.translatable("screen.hogwartsmod.mission_editor"));
        this.entity = packet.entity();
        this.name = packet.name();
        this.model = packet.model();
        this.missions = new ArrayList<>(packet.missions());
        this.models = availableModels();
        this.selected = missions.isEmpty() ? -1 : 0;
    }

    private static List<String> availableModels() {
        TreeSet<String> names = new TreeSet<>();
        var manager = Minecraft.getInstance().getResourceManager();
        manager.listResources("geo/npc", p -> p.getNamespace().equals(HogwartsMod.MOD_ID) && p.getPath().endsWith(".geo.json"))
                .keySet().forEach(p -> names.add(p.getPath().substring("geo/npc/".length(), p.getPath().length() - ".geo.json".length())));
        manager.listResources("textures/entity/npc", p -> p.getNamespace().equals(HogwartsMod.MOD_ID) && p.getPath().endsWith(".png"))
                .keySet().forEach(p -> names.add(p.getPath().substring("textures/entity/npc/".length(), p.getPath().length() - ".png".length())));
        return new ArrayList<>(names);
    }

    private EditBox box(int x, int y, int w, int max, String value, Consumer<String> responder) {
        EditBox box = new EditBox(font, x, y, w, 10, Component.empty());
        box.setBordered(false);
        box.setMaxLength(max);
        box.setValue(value);
        box.setResponder(responder);
        addWidget(box);
        boxes.add(box);
        return box;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        boxes.clear();
        nameBox = box(left + 21, top + 43, 128, 32, name, v -> name = v);
        int rx = left + 179;
        titleBox = box(rx, top + 42, 142, Mission.MAX_TITLE, "", v -> edit(m -> m.withTitle(v)));
        descBox = box(rx, top + 69, 142, Mission.MAX_DESCRIPTION, "", v -> edit(m -> m.withDescription(v)));
        targetBox = box(rx, top + 114, 94, Mission.MAX_TARGET, "", v -> edit(m -> m.withTarget(v)));
        searchBox = box(rx, top + 150, 94, 40, "", this::search);
        loadSelected();
    }

    private void loadSelected() {
        Mission m = current();
        titleBox.setValue(m == null ? "" : m.title());
        descBox.setValue(m == null ? "" : m.description());
        targetBox.setValue(m == null ? "" : m.target());
        titleBox.moveCursorToStart();
        descBox.moveCursorToStart();
        targetBox.moveCursorToStart();
        searchBox.setValue("");
    }

    private void search(String query) {
        String q = query.strip().toLowerCase();
        if (q.isEmpty()) {
            results = List.of();
            return;
        }
        List<net.minecraft.world.item.Item> found = new ArrayList<>();
        for (net.minecraft.world.item.Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) {
                continue;
            }
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            String name = item.getDescription().getString().toLowerCase();
            if (name.contains(q) || id.contains(q)) {
                found.add(item);
            }
        }
        found.sort(java.util.Comparator
                .comparingInt((net.minecraft.world.item.Item i) -> i.getDescription().getString().toLowerCase().startsWith(q) ? 0 : 1)
                .thenComparingInt(i -> i.getDescription().getString().length()));
        results = found.subList(0, Math.min(3, found.size()));
    }

    private void addReward(net.minecraft.world.item.Item item, int count) {
        Mission m = current();
        if (m == null || item == Items.AIR) {
            return;
        }
        String id = BuiltInRegistries.ITEM.getKey(item).toString();
        List<Mission.RewardItem> list = new ArrayList<>(m.items());
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id().equals(id)) {
                list.set(i, new Mission.RewardItem(id, list.get(i).count() + count));
                missions.set(selected, m.withItems(list));
                return;
            }
        }
        if (list.size() < Mission.MAX_ITEMS) {
            list.add(new Mission.RewardItem(id, count));
            missions.set(selected, m.withItems(list));
        }
    }

    private Mission current() {
        return selected >= 0 && selected < missions.size() ? missions.get(selected) : null;
    }

    private void edit(java.util.function.UnaryOperator<Mission> change) {
        Mission m = current();
        if (m != null) {
            missions.set(selected, change.apply(m));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // -- Desenho

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        HiRes.image(g, SHEET, left, top, W, H);
        HiRes.centered(g, title.getString(), HiRes.Style.BOLD, 11f, left + W / 2f, top + 14, TITLE);
        g.fill(left + 166, top + 30, left + 167, top + H - 14, LINE);
        drawLeft(g, mouseX, mouseY);
        drawRight(g, mouseX, mouseY);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void field(GuiGraphics g, EditBox box, int x, int y, int w) {
        g.fill(x, y, x + w, y + 13, NOTE_EDGE);
        g.fill(x + 1, y + 1, x + w - 1, y + 12, box.isFocused() ? NOTE_ON : NOTE);
        String value = box.getValue();
        int cursor = Math.min(box.getCursorPosition(), value.length());
        int offset = 0;
        while (offset < cursor && HiRes.width(value.substring(offset, cursor), HiRes.Style.REGULAR) > w - 10) {
            offset++;
        }
        HiRes.text(g, HiRes.ellipsize(value.substring(offset), HiRes.Style.REGULAR, w - 8), HiRes.Style.REGULAR, x + 4, y + 2, NAME_COLOR);
        if (box.isFocused() && (System.currentTimeMillis() / 500) % 2 == 0) {
            float cx = x + 4 + HiRes.width(value.substring(offset, cursor), HiRes.Style.REGULAR) + 0.5f;
            g.fill((int) cx, y + 2, (int) cx + 1, y + 11, NAME_COLOR);
        }
    }

    private void drawLeft(GuiGraphics g, int mouseX, int mouseY) {
        int x = left + 18;
        int w = 134;
        HiRes.text(g, tr("screen.hogwartsmod.mission_editor.name"), HiRes.Style.BOLD, x, top + 30, INK);
        field(g, nameBox, x, top + 40, w);
        HiRes.text(g, tr("screen.hogwartsmod.mission_editor.model"), HiRes.Style.BOLD, x, top + 60, INK);
        stepper(g, x, top + 72, w, model, mouseX, mouseY);
        HiRes.text(g, tr("screen.hogwartsmod.mission_editor.missions"), HiRes.Style.BOLD, x, top + 92, INK);
        HiRes.right(g, missions.size() + "/" + MissionNpcEntity.MAX_MISSIONS, HiRes.Style.ITALIC, x + w, top + 92, MUTED);
        for (int i = 0; i < missions.size(); i++) {
            int y = top + 104 + i * ROW;
            boolean hover = inside(mouseX, mouseY, x - 2, y - 1, w + 4, ROW - 1);
            if (i == selected || hover) {
                HiRes.highlight(g, x - 2, y - 1, x + w + 2, y + ROW - 2, i == selected ? HiRes.Mark.SELECTED : HiRes.Mark.HOVER);
            }
            Mission m = missions.get(i);
            String label = m.title().isBlank() ? tr("screen.hogwartsmod.mission_editor.untitled") : m.title();
            HiRes.text(g, HiRes.ellipsize(label, HiRes.Style.REGULAR, w - 18), HiRes.Style.REGULAR, x + 3, y + 1.5f, m.title().isBlank() ? MUTED : INK);
            if (hover) {
                HiRes.right(g, "×", HiRes.Style.BOLD, x + w - 1, y + 1.5f, WINE);
            }
        }
        if (missions.size() < MissionNpcEntity.MAX_MISSIONS) {
            int y = top + 104 + missions.size() * ROW;
            boolean hover = inside(mouseX, mouseY, x - 2, y - 1, w + 4, ROW - 1);
            if (hover) {
                HiRes.highlight(g, x - 2, y - 1, x + w + 2, y + ROW - 2, HiRes.Mark.HOVER);
            }
            HiRes.text(g, tr("screen.hogwartsmod.mission_editor.add"), HiRes.Style.BOLD, x + 3, y + 1.5f, GREEN);
        }
    }

    private void drawRight(GuiGraphics g, int mouseX, int mouseY) {
        int x = left + 176;
        int w = 148;
        Mission m = current();
        if (m == null) {
            HiRes.centered(g, tr("screen.hogwartsmod.mission_editor.pick"), HiRes.Style.ITALIC, x + w / 2f, top + 100, MUTED);
        } else {
            subTab(g, x, top + 28, 73, tr("screen.hogwartsmod.mission_editor.page_mission"), !rewardPage, mouseX, mouseY);
            subTab(g, x + 75, top + 28, 73, tr("screen.hogwartsmod.mission_editor.page_reward"), rewardPage, mouseX, mouseY);
            if (rewardPage) {
                drawRewardPage(g, x, w, m, mouseX, mouseY);
            } else {
                drawMissionPage(g, x, w, m, mouseX, mouseY);
            }
        }
        button(g, x, top + H - 26, w, tr("screen.hogwartsmod.mission_editor.save"), true, GREEN, mouseX, mouseY);
    }

    private void subTab(GuiGraphics g, int x, int y, int w, String label, boolean on, int mouseX, int mouseY) {
        if (on || inside(mouseX, mouseY, x, y, w, 13)) {
            HiRes.highlight(g, x, y, x + w, y + 13, on ? HiRes.Mark.SELECTED : HiRes.Mark.HOVER);
        }
        HiRes.centered(g, label, HiRes.Style.BOLD, x + w / 2f + (on ? 2 : 0), y + 2.5f, on ? TITLE : MUTED);
    }

    private void drawMissionPage(GuiGraphics g, int x, int w, Mission m, int mouseX, int mouseY) {
        HiRes.text(g, tr("screen.hogwartsmod.mission_editor.title"), HiRes.Style.BOLD, x, top + 45, INK);
        field(g, titleBox, x, top + 55, w);
        HiRes.text(g, tr("screen.hogwartsmod.mission_editor.description"), HiRes.Style.BOLD, x, top + 72, INK);
        field(g, descBox, x, top + 82, w);
        HiRes.text(g, tr("screen.hogwartsmod.mission_editor.type"), HiRes.Style.BOLD, x, top + 101, INK);
        stepper(g, x + 46, top + 100, w - 46, tr(m.type().key()), mouseX, mouseY);
        HiRes.text(g, tr("screen.hogwartsmod.mission_editor.target." + m.type().getId()), HiRes.Style.BOLD, x, top + 117, INK);
        field(g, targetBox, x, top + 127, 98);
        String helper = switch (m.type()) {
            case ENTREGAR -> tr("screen.hogwartsmod.mission_editor.from_hand");
            case DERROTAR -> tr("screen.hogwartsmod.mission_editor.next_mob");
            case IR -> tr("screen.hogwartsmod.mission_editor.next_zone");
        };
        button(g, x + 101, top + 126, 47, helper, true, TITLE, mouseX, mouseY);
        boolean valid = valid(m);
        HiRes.text(g, HiRes.ellipsize(valid ? m.targetName().getString() : tr("screen.hogwartsmod.mission_editor.invalid"),
                HiRes.Style.ITALIC, w), HiRes.Style.ITALIC, 7.5f, x, top + 143, valid ? GREEN : WINE);
        if (m.type() != ObjectiveType.IR) {
            HiRes.text(g, tr("screen.hogwartsmod.mission_editor.amount"), HiRes.Style.BOLD, x, top + 158, INK);
            stepper(g, x + 70, top + 157, w - 70, String.valueOf(m.amount()), mouseX, mouseY);
        }
        int ry = top + 178;
        g.fill(x, ry, x + 9, ry + 9, NOTE_EDGE);
        g.fill(x + 1, ry + 1, x + 8, ry + 8, NOTE_ON);
        if (m.repeatable()) {
            HiRes.image(g, MissionScreen.tex("check"), x, ry - 1, 10, 10);
        }
        HiRes.text(g, tr("screen.hogwartsmod.mission_editor.repeatable"), HiRes.Style.REGULAR, x + 13, ry + 1, m.repeatable() ? INK : MUTED);
        if (m.repeatable()) {
            stepper(g, x + 84, ry, w - 84, m.cooldownHours() + "h", mouseX, mouseY);
        }
    }

    private void drawRewardPage(GuiGraphics g, int x, int w, Mission m, int mouseX, int mouseY) {
        HiRes.text(g, tr("screen.hogwartsmod.mission_editor.coins"), HiRes.Style.BOLD, x, top + 45, INK);
        int[] amounts = {m.galeoes(), m.sicles(), m.nuques()};
        String[] ids = {"galeao", "sicle", "nuque"};
        for (int i = 0; i < 3; i++) {
            int gx = x + i * 50;
            HiRes.image(g, MissionScreen.tex("moeda_" + ids[i]), gx, top + 56, 10, 11);
            stepper(g, gx + 11, top + 56, 37, String.valueOf(amounts[i]), mouseX, mouseY);
        }
        HiRes.text(g, tr("screen.hogwartsmod.mission_editor.items"), HiRes.Style.BOLD, x, top + 74, INK);
        HiRes.right(g, m.items().size() + "/" + Mission.MAX_ITEMS, HiRes.Style.ITALIC, x + w, top + 74, MUTED);
        if (m.items().isEmpty()) {
            HiRes.text(g, tr("screen.hogwartsmod.mission_editor.no_items"), HiRes.Style.ITALIC, 7.5f, x, top + 87, MUTED);
        }
        for (int i = 0; i < m.items().size(); i++) {
            Mission.RewardItem r = m.items().get(i);
            int y = top + 85 + i * 13;
            MissionScreen.item(g, r.stack(), x, y, 11);
            HiRes.text(g, HiRes.ellipsize(r.item().getDescription().getString(), HiRes.Style.REGULAR, 66), HiRes.Style.REGULAR, x + 14, y + 1.5f, INK);
            stepper(g, x + 82, y + 1, 50, String.valueOf(r.count()), mouseX, mouseY);
            boolean hoverX = inside(mouseX, mouseY, x + w - 10, y, 10, 12);
            HiRes.text(g, "×", HiRes.Style.BOLD, x + w - 8, y + 1.5f, hoverX ? WINE : MUTED);
        }
        HiRes.text(g, tr("screen.hogwartsmod.mission_editor.search"), HiRes.Style.BOLD, x, top + 140, INK);
        field(g, searchBox, x, top + 150, 98);
        button(g, x + 101, top + 149, 47, tr("screen.hogwartsmod.mission_editor.from_hand"), m.items().size() < Mission.MAX_ITEMS, TITLE, mouseX, mouseY);
        if (!searchBox.getValue().isBlank() && results.isEmpty()) {
            HiRes.text(g, tr("screen.hogwartsmod.mission_editor.no_results"), HiRes.Style.ITALIC, 7.5f, x, top + 167, MUTED);
        }
        for (int i = 0; i < results.size(); i++) {
            net.minecraft.world.item.Item item = results.get(i);
            int y = top + 165 + i * 11;
            boolean hover = inside(mouseX, mouseY, x - 2, y - 1, w + 4, 11);
            if (hover) {
                HiRes.highlight(g, x - 2, y - 1, x + w + 2, y + 10, HiRes.Mark.HOVER);
            }
            MissionScreen.item(g, new ItemStack(item), x, y, 9);
            HiRes.text(g, HiRes.ellipsize(item.getDescription().getString(), HiRes.Style.REGULAR, w - 30), HiRes.Style.REGULAR, 7.5f, x + 12, y + 1, INK);
            HiRes.right(g, "+", HiRes.Style.BOLD, x + w, y, GREEN);
        }
    }

    private static boolean valid(Mission m) {
        return switch (m.type()) {
            case ENTREGAR -> m.item() != Items.AIR;
            case DERROTAR -> m.entity() != null;
            case IR -> Zones.clientZones.stream().anyMatch(z -> z.name().equalsIgnoreCase(m.target()));
        };
    }

    private void stepper(GuiGraphics g, int x, int y, int w, String value, int mouseX, int mouseY) {
        arrowAt(g, "left", x, y, mouseX, mouseY);
        arrowAt(g, "right", x + w - 9, y, mouseX, mouseY);
        HiRes.centered(g, HiRes.ellipsize(value, HiRes.Style.BOLD, w - 22), HiRes.Style.BOLD, x + w / 2f, y + 1, INK);
    }

    private void arrowAt(GuiGraphics g, String dir, int x, int y, int mouseX, int mouseY) {
        if (inside(mouseX, mouseY, x - 1, y - 1, 11, 11)) {
            HiRes.highlight(g, x - 1, y - 1, x + 10, y + 10, HiRes.Mark.HOVER);
        }
        HiRes.image(g, MissionScreen.tex("arrow_" + dir + "_ink"), x, y + 1, 8, 8);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // -- Interação

    private void click() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        }
    }

    private int stepperDir(double mx, double my, int x, int y, int w) {
        if (inside(mx, my, x - 1, y - 1, 11, 11)) {
            return -1;
        }
        if (inside(mx, my, x + w - 10, y - 1, 11, 11)) {
            return 1;
        }
        return 0;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) {
            return super.mouseClicked(mx, my, button);
        }
        for (EditBox box : boxes) {
            box.setFocused(false);
        }
        int lx = left + 18;
        if (inside(mx, my, lx, top + 40, 134, 13)) {
            return focus(nameBox, mx, my, button);
        }
        int dir = stepperDir(mx, my, lx, top + 72, 134);
        if (dir != 0 && !models.isEmpty()) {
            int i = models.indexOf(model);
            model = models.get(Math.floorMod((i < 0 ? 0 : i) + dir, models.size()));
            click();
            return true;
        }
        for (int i = 0; i < missions.size(); i++) {
            int y = top + 104 + i * ROW;
            if (inside(mx, my, lx - 2, y - 1, 138, ROW - 1)) {
                if (mx >= lx + 124) {
                    missions.remove(i);
                    selected = Math.min(selected, missions.size() - 1);
                } else {
                    selected = i;
                }
                loadSelected();
                click();
                return true;
            }
        }
        if (missions.size() < MissionNpcEntity.MAX_MISSIONS && inside(mx, my, lx - 2, top + 104 + missions.size() * ROW - 1, 138, ROW - 1)) {
            missions.add(Mission.blank());
            selected = missions.size() - 1;
            loadSelected();
            titleBox.setFocused(true);
            setFocused(titleBox);
            click();
            return true;
        }
        int x = left + 176;
        int w = 148;
        if (inside(mx, my, x, top + H - 26, w, 15)) {
            ModNetwork.CHANNEL.sendToServer(new SaveMissionNpcPacket(entity, name, model, missions));
            click();
            onClose();
            return true;
        }
        Mission m = current();
        if (m == null) {
            return super.mouseClicked(mx, my, button);
        }
        if (inside(mx, my, x, top + 28, 73, 13) || inside(mx, my, x + 75, top + 28, 73, 13)) {
            rewardPage = mx >= x + 75;
            click();
            return true;
        }
        int step = hasShiftDown() ? 10 : 1;
        if (rewardPage) {
            int[] amounts = {m.galeoes(), m.sicles(), m.nuques()};
            for (int i = 0; i < 3; i++) {
                int d = stepperDir(mx, my, x + i * 50 + 11, top + 56, 37);
                if (d != 0) {
                    amounts[i] = Math.max(0, Math.min(9999, amounts[i] + d * step));
                    missions.set(selected, m.withReward(amounts[0], amounts[1], amounts[2]));
                    click();
                    return true;
                }
            }
            for (int i = 0; i < m.items().size(); i++) {
                int y = top + 85 + i * 13;
                List<Mission.RewardItem> list = new ArrayList<>(m.items());
                if (inside(mx, my, x + w - 10, y, 10, 12)) {
                    list.remove(i);
                    missions.set(selected, m.withItems(list));
                    click();
                    return true;
                }
                int d = stepperDir(mx, my, x + 82, y + 1, 50);
                if (d != 0) {
                    Mission.RewardItem r = list.get(i);
                    list.set(i, new Mission.RewardItem(r.id(), Math.max(1, Math.min(r.item().getMaxStackSize() * 4, r.count() + d * step))));
                    missions.set(selected, m.withItems(list));
                    click();
                    return true;
                }
            }
            if (inside(mx, my, x, top + 150, 98, 13)) {
                return focus(searchBox, mx, my, button);
            }
            if (inside(mx, my, x + 101, top + 149, 47, 15) && minecraft != null && minecraft.player != null) {
                ItemStack hand = minecraft.player.getMainHandItem();
                if (!hand.isEmpty()) {
                    addReward(hand.getItem(), hand.getCount());
                    click();
                }
                return true;
            }
            for (int i = 0; i < results.size(); i++) {
                if (inside(mx, my, x - 2, top + 165 + i * 11 - 1, w + 4, 11)) {
                    addReward(results.get(i), 1);
                    click();
                    return true;
                }
            }
            return super.mouseClicked(mx, my, button);
        }
        if (inside(mx, my, x, top + 55, w, 13)) {
            return focus(titleBox, mx, my, button);
        }
        if (inside(mx, my, x, top + 82, w, 13)) {
            return focus(descBox, mx, my, button);
        }
        if (inside(mx, my, x, top + 127, 98, 13)) {
            return focus(targetBox, mx, my, button);
        }
        if (stepperDir(mx, my, x + 46, top + 100, w - 46) != 0) {
            Mission next = m.withType(m.type().next());
            next = next.withTarget(switch (next.type()) {
                case ENTREGAR -> "minecraft:feather";
                case DERROTAR -> "minecraft:spider";
                case IR -> Zones.clientZones.isEmpty() ? "" : Zones.clientZones.get(0).name();
            });
            missions.set(selected, next);
            loadSelected();
            click();
            return true;
        }
        if (inside(mx, my, x + 101, top + 126, 47, 15)) {
            helper(m);
            click();
            return true;
        }
        if (m.type() != ObjectiveType.IR) {
            int d = stepperDir(mx, my, x + 70, top + 157, w - 70);
            if (d != 0) {
                missions.set(selected, m.withAmount(Math.max(1, Math.min(999, m.amount() + d * step))));
                click();
                return true;
            }
        }
        int ry = top + 178;
        if (inside(mx, my, x, ry - 1, 80, 11)) {
            missions.set(selected, m.withRepeatable(!m.repeatable(), m.cooldownHours()));
            click();
            return true;
        }
        if (m.repeatable()) {
            int d = stepperDir(mx, my, x + 84, ry, w - 84);
            if (d != 0) {
                missions.set(selected, m.withRepeatable(true, Math.max(0, Math.min(720, m.cooldownHours() + d * step))));
                click();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private boolean focus(EditBox box, double mx, double my, int button) {
        box.setFocused(true);
        setFocused(box);
        return true;
    }

    private static final String[] COMMON_MOBS = {"minecraft:spider", "minecraft:zombie", "minecraft:skeleton", "minecraft:creeper",
            "minecraft:cave_spider", "minecraft:slime", "minecraft:witch", "minecraft:enderman", "minecraft:phantom", "minecraft:wolf"};

    private void helper(Mission m) {
        switch (m.type()) {
            case ENTREGAR -> {
                ItemStack hand = minecraft == null || minecraft.player == null ? ItemStack.EMPTY : minecraft.player.getMainHandItem();
                if (!hand.isEmpty()) {
                    targetBox.setValue(BuiltInRegistries.ITEM.getKey(hand.getItem()).toString());
                }
            }
            case DERROTAR -> {
                int i = java.util.Arrays.asList(COMMON_MOBS).indexOf(m.target());
                targetBox.setValue(COMMON_MOBS[(i + 1) % COMMON_MOBS.length]);
            }
            case IR -> {
                List<Zone> zones = Zones.clientZones;
                if (!zones.isEmpty()) {
                    int i = 0;
                    for (int k = 0; k < zones.size(); k++) {
                        if (zones.get(k).name().equalsIgnoreCase(m.target())) {
                            i = k + 1;
                        }
                    }
                    targetBox.setValue(zones.get(i % zones.size()).name());
                }
            }
        }
        targetBox.moveCursorToStart();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (EditBox box : boxes) {
            if (box.isFocused()) {
                if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                    box.setFocused(false);
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_TAB) {
                    int i = boxes.indexOf(box);
                    box.setFocused(false);
                    EditBox next = boxes.get((i + 1) % boxes.size());
                    next.setFocused(true);
                    setFocused(next);
                    return true;
                }
                return box.keyPressed(keyCode, scanCode, modifiers) || box.canConsumeInput();
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
