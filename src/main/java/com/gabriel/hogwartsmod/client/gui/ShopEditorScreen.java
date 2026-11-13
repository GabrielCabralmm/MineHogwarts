package com.gabriel.hogwartsmod.client.gui;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.network.ModNetwork;
import com.gabriel.hogwartsmod.network.OpenShopEditorPacket;
import com.gabriel.hogwartsmod.network.RemoveNpcPacket;
import com.gabriel.hogwartsmod.network.SaveShopNpcPacket;
import com.gabriel.hogwartsmod.shop.ShopNpcEntity;
import com.gabriel.hogwartsmod.shop.ShopOffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Consumer;

import static com.gabriel.hogwartsmod.client.gui.MissionScreen.*;

public class ShopEditorScreen extends Screen {
    private static final int W = 340;
    private static final int H = 226;
    private static final int ROW = 13;
    private static final int NAME_COLOR = 0xFF7A2618;
    private static final ResourceLocation SHEET = MissionScreen.tex("pulpit_sheet");

    private final int entity;
    private final List<ShopOffer> offers;
    private final List<String> models;
    private String name;
    private String model;
    private int selected;
    private boolean confirmRemove;
    private int page;
    private boolean sellTab;
    private int left;
    private int top;

    private EditBox nameBox;
    private EditBox searchBox;
    private List<Item> results = List.of();
    private final List<EditBox> boxes = new ArrayList<>();

    public ShopEditorScreen(OpenShopEditorPacket packet) {
        super(Component.translatable("screen.hogwartsmod.shop_editor"));
        this.entity = packet.entity();
        this.name = packet.name();
        this.model = packet.model();
        this.offers = new ArrayList<>(packet.offers());
        this.models = availableModels();
        this.selected = offers.isEmpty() ? -1 : 0;
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
        searchBox = box(left + 179, top + 150, 94, 40, "", this::search);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private ShopOffer current() {
        return selected >= 0 && selected < offers.size() && offers.get(selected).sell() == sellTab ? offers.get(selected) : null;
    }

    private List<Integer> visible() {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < offers.size(); i++) {
            if (offers.get(i).sell() == sellTab) {
                out.add(i);
            }
        }
        return out;
    }

    private int resultRows() {
        return 3;
    }

    private void search(String query) {
        String q = query.strip().toLowerCase();
        if (q.isEmpty()) {
            results = List.of();
            return;
        }
        List<Item> found = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR || (sellTab && page == 0 && !ShopOffer.sellable(item))) {
                continue;
            }
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            String label = item.getDescription().getString().toLowerCase();
            if (label.contains(q) || id.contains(q)) {
                found.add(item);
            }
        }
        found.sort(java.util.Comparator
                .comparingInt((Item i) -> i.getDescription().getString().toLowerCase().startsWith(q) ? 0 : 1)
                .thenComparingInt(i -> i.getDescription().getString().length()));
        results = found.subList(0, Math.min(5, found.size()));
    }

    private void pick(Item item, int count) {
        ShopOffer o = current();
        if (o == null || item == Items.AIR) {
            return;
        }
        if (page == 0) {
            if (!o.sell() || ShopOffer.sellable(item)) {
                offers.set(selected, o.withProduct(ShopOffer.Goods.of(item, count)));
            }
            return;
        }
        if (o.sell() || page != 1) {
            return;
        }
        List<ShopOffer.Goods> list = new ArrayList<>(o.costs());
        String id = BuiltInRegistries.ITEM.getKey(item).toString();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id().equals(id)) {
                list.set(i, list.get(i).withCount(list.get(i).count() + count));
                offers.set(selected, o.withCosts(list));
                return;
            }
        }
        if (list.size() < ShopOffer.MAX_COSTS) {
            list.add(new ShopOffer.Goods(id, count));
            offers.set(selected, o.withCosts(list));
        }
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
        String remove = tr(confirmRemove ? "screen.hogwartsmod.mission_editor.remove_confirm" : "screen.hogwartsmod.mission_editor.remove");
        float rw = HiRes.width(remove, HiRes.Style.BOLD, 7.5f);
        if (inside(mouseX, mouseY, (int) (x + w - rw - 3), top + 28, (int) rw + 6, 12) || confirmRemove) {
            HiRes.highlight(g, (int) (x + w - rw - 3), top + 28, x + w + 3, top + 40, HiRes.Mark.HOVER);
        }
        HiRes.text(g, remove, HiRes.Style.BOLD, 7.5f, x + w - rw, top + 30.5f, WINE);
        field(g, nameBox, x, top + 40, w);
        HiRes.text(g, tr("screen.hogwartsmod.mission_editor.model"), HiRes.Style.BOLD, x, top + 60, INK);
        stepper(g, x, top + 72, w, model, mouseX, mouseY);
        List<Integer> vis = visible();
        subTab(g, x - 2, top + 88, 56, tr("screen.hogwartsmod.shop_editor.tab_sells"), !sellTab, mouseX, mouseY);
        subTab(g, x + 56, top + 88, 56, tr("screen.hogwartsmod.shop_editor.tab_buys"), sellTab, mouseX, mouseY);
        HiRes.right(g, vis.size() + "/" + ShopNpcEntity.MAX_OFFERS, HiRes.Style.ITALIC, x + w, top + 91, MUTED);
        for (int k = 0; k < vis.size(); k++) {
            int i = vis.get(k);
            int y = top + 104 + k * ROW;
            boolean hover = inside(mouseX, mouseY, x - 2, y - 1, w + 4, ROW - 1);
            if (i == selected || hover) {
                HiRes.highlight(g, x - 2, y - 1, x + w + 2, y + ROW - 2, i == selected ? HiRes.Mark.SELECTED : HiRes.Mark.HOVER);
            }
            ShopOffer o = offers.get(i);
            MissionScreen.item(g, o.product().stack(), x + 2, y, 10);
            HiRes.text(g, HiRes.ellipsize(ShopScreen.goodsName(o.product()), HiRes.Style.REGULAR, (w - 28) * HiRes.TEXT_SIZE / 7.5f),
                    HiRes.Style.REGULAR, 7.5f, x + 15, y + 1.5f, INK);
            if (hover) {
                HiRes.right(g, "×", HiRes.Style.BOLD, x + w - 1, y + 0.5f, WINE);
            }
        }
        if (vis.size() < ShopNpcEntity.MAX_OFFERS) {
            int y = top + 104 + vis.size() * ROW;
            boolean hover = inside(mouseX, mouseY, x - 2, y - 1, w + 4, ROW - 1);
            if (hover) {
                HiRes.highlight(g, x - 2, y - 1, x + w + 2, y + ROW - 2, HiRes.Mark.HOVER);
            }
            HiRes.text(g, tr(sellTab ? "screen.hogwartsmod.shop_editor.add_buy" : "screen.hogwartsmod.shop_editor.add"), HiRes.Style.BOLD, 7.5f, x + 3, y + 1.5f, GREEN);
        }
    }

    private void drawRight(GuiGraphics g, int mouseX, int mouseY) {
        int x = left + 176;
        int w = 148;
        ShopOffer o = current();
        if (o == null) {
            HiRes.centered(g, tr("screen.hogwartsmod.shop_editor.pick"), HiRes.Style.ITALIC, x + w / 2f, top + 100, MUTED);
        } else {
            String[] pages = {"page_product", "page_price", "page_stock"};
            for (int i = 0; i < 3; i++) {
                subTab(g, x + i * 50, top + 28, 48, tr("screen.hogwartsmod.shop_editor." + pages[i]), page == i, mouseX, mouseY);
            }
            if (page == 1) {
                drawPricePage(g, x, w, o, mouseX, mouseY);
            } else if (page == 2) {
                drawStockPage(g, x, w, o, mouseX, mouseY);
            } else {
                drawProductPage(g, x, w, o, mouseX, mouseY);
            }
            if (page == 0 || (page == 1 && !o.sell())) {
                drawSearch(g, x, w, mouseX, mouseY);
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

    private void drawProductPage(GuiGraphics g, int x, int w, ShopOffer o, int mouseX, int mouseY) {
        HiRes.text(g, tr("screen.hogwartsmod.shop_editor.product"), HiRes.Style.BOLD, x, top + 46, INK);
        HiRes.highlight(g, x, top + 58, x + 22, top + 80, HiRes.Mark.SELECTED);
        MissionScreen.item(g, o.product().stack(), x + 3, top + 61, 16);
        HiRes.text(g, HiRes.ellipsize(o.product().item().getDescription().getString(), HiRes.Style.BOLD, w - 28), HiRes.Style.BOLD, x + 27, top + 59, INK);
        HiRes.text(g, tr("screen.hogwartsmod.mission_editor.amount"), HiRes.Style.ITALIC, 7.5f, x + 27, top + 71, MUTED);
        stepper(g, x + 80, top + 70, w - 80, String.valueOf(o.product().count()), mouseX, mouseY);
        HiRes.text(g, tr(o.sell() ? "screen.hogwartsmod.shop.pays" : "screen.hogwartsmod.shop.price"), HiRes.Style.BOLD, x, top + 94, INK);
        float px = x;
        int py = top + 107;
        int[] amounts = {o.galeoes(), o.sicles(), o.nuques()};
        String[] ids = {"galeao", "sicle", "nuque"};
        for (int i = 0; i < 3; i++) {
            if (amounts[i] <= 0) {
                continue;
            }
            HiRes.image(g, MissionScreen.tex("moeda_" + ids[i]), Math.round(px), py - 1, 10, 11);
            String n = String.valueOf(amounts[i]);
            HiRes.text(g, n, HiRes.Style.BOLD, px + 12, py, INK);
            px += 12 + HiRes.width(n, HiRes.Style.BOLD) + 7;
        }
        for (ShopOffer.Goods c : o.costs()) {
            MissionScreen.item(g, c.stack(), Math.round(px), py - 1, 10);
            String n = "×" + c.count();
            HiRes.text(g, n, HiRes.Style.BOLD, px + 12, py, INK);
            px += 12 + HiRes.width(n, HiRes.Style.BOLD) + 7;
        }
        if (o.free()) {
            HiRes.text(g, tr("screen.hogwartsmod.shop.free"), HiRes.Style.ITALIC, x, py, GREEN);
        }
        HiRes.text(g, tr(o.sell() ? "screen.hogwartsmod.shop_editor.change_bought" : "screen.hogwartsmod.shop_editor.change_product"), HiRes.Style.BOLD, x, top + 140, INK);
    }

    private void drawStockPage(GuiGraphics g, int x, int w, ShopOffer o, int mouseX, int mouseY) {
        HiRes.text(g, tr(o.sell() ? "screen.hogwartsmod.shop_editor.limit_buy" : "screen.hogwartsmod.shop_editor.limit"), HiRes.Style.BOLD, x, top + 46, INK);
        stepper(g, x, top + 59, w, o.limited() ? String.valueOf(o.max()) : tr("screen.hogwartsmod.shop_editor.unlimited"), mouseX, mouseY);
        if (!o.limited()) {
            HiRes.text(g, tr("screen.hogwartsmod.shop_editor.unlimited_hint"), HiRes.Style.ITALIC, 7f, x, top + 74, MUTED);
            return;
        }
        HiRes.text(g, tr("screen.hogwartsmod.shop_editor.now", o.stock(), o.max()), HiRes.Style.REGULAR, x, top + 78, INK);
        button(g, x + w - 62, top + 75, 62, tr("screen.hogwartsmod.shop_editor.refill"), o.stock() < o.max(), GREEN, mouseX, mouseY);
        boolean auto = o.restockDays() > 0;
        boolean hover = inside(mouseX, mouseY, x, top + 98, w, 12);
        g.fill(x, top + 99, x + 10, top + 109, NOTE_EDGE);
        g.fill(x + 1, top + 100, x + 9, top + 108, hover ? NOTE_ON : NOTE);
        if (auto) {
            HiRes.image(g, MissionScreen.tex("check"), x + 1, top + 100, 8, 8);
        }
        HiRes.text(g, tr("screen.hogwartsmod.shop_editor.auto"), HiRes.Style.BOLD, x + 15, top + 100, INK);
        if (auto) {
            HiRes.text(g, tr("screen.hogwartsmod.shop_editor.every"), HiRes.Style.ITALIC, 7.5f, x, top + 118, MUTED);
            stepper(g, x + 52, top + 117, w - 52, tr("screen.hogwartsmod.shop_editor.days", o.restockDays()), mouseX, mouseY);
            HiRes.text(g, tr("screen.hogwartsmod.shop_editor.auto_hint"), HiRes.Style.ITALIC, 7f, x, top + 134, MUTED);
        } else {
            HiRes.text(g, tr("screen.hogwartsmod.shop_editor.manual_hint"), HiRes.Style.ITALIC, 7f, x, top + 118, MUTED);
        }
    }

    private void drawPricePage(GuiGraphics g, int x, int w, ShopOffer o, int mouseX, int mouseY) {
        HiRes.text(g, tr(o.sell() ? "screen.hogwartsmod.shop.pays" : "screen.hogwartsmod.mission_editor.coins"), HiRes.Style.BOLD, x, top + 45, INK);
        int[] amounts = {o.galeoes(), o.sicles(), o.nuques()};
        String[] ids = {"galeao", "sicle", "nuque"};
        for (int i = 0; i < 3; i++) {
            int gx = x + i * 50;
            HiRes.image(g, MissionScreen.tex("moeda_" + ids[i]), gx, top + 56, 10, 11);
            stepper(g, gx + 11, top + 56, 37, String.valueOf(amounts[i]), mouseX, mouseY);
        }
        if (o.sell()) {
            HiRes.text(g, tr("screen.hogwartsmod.shop_editor.sell_hint"), HiRes.Style.ITALIC, 7f, x, top + 76, MUTED);
            return;
        }
        HiRes.text(g, tr("screen.hogwartsmod.shop_editor.costs"), HiRes.Style.BOLD, x, top + 74, INK);
        HiRes.right(g, o.costs().size() + "/" + ShopOffer.MAX_COSTS, HiRes.Style.ITALIC, x + w, top + 74, MUTED);
        if (o.costs().isEmpty()) {
            HiRes.text(g, tr("screen.hogwartsmod.shop_editor.no_costs"), HiRes.Style.ITALIC, 7.5f, x, top + 87, MUTED);
        }
        for (int i = 0; i < o.costs().size(); i++) {
            ShopOffer.Goods c = o.costs().get(i);
            int y = top + 85 + i * 13;
            MissionScreen.item(g, c.stack(), x, y, 11);
            HiRes.text(g, HiRes.ellipsize(c.item().getDescription().getString(), HiRes.Style.REGULAR, 66), HiRes.Style.REGULAR, x + 14, y + 1.5f, INK);
            stepper(g, x + 82, y + 1, 50, String.valueOf(c.count()), mouseX, mouseY);
            boolean hoverX = inside(mouseX, mouseY, x + w - 10, y, 10, 12);
            HiRes.text(g, "×", HiRes.Style.BOLD, x + w - 8, y + 1.5f, hoverX ? WINE : MUTED);
        }
        HiRes.text(g, tr("screen.hogwartsmod.shop_editor.add_cost"), HiRes.Style.BOLD, x, top + 140, INK);
    }

    private void drawSearch(GuiGraphics g, int x, int w, int mouseX, int mouseY) {
        field(g, searchBox, x, top + 150, 98);
        button(g, x + 101, top + 149, 47, tr("screen.hogwartsmod.mission_editor.from_hand"), true, TITLE, mouseX, mouseY);
        if (!searchBox.getValue().isBlank() && results.isEmpty()) {
            HiRes.text(g, tr("screen.hogwartsmod.mission_editor.no_results"), HiRes.Style.ITALIC, 7.5f, x, top + 167, MUTED);
        }
        if (searchBox.getValue().isBlank()) {
            HiRes.text(g, tr("screen.hogwartsmod.shop_editor.search_hint"), HiRes.Style.ITALIC, 7f, x, top + 167, MUTED);
        }
        for (int i = 0; i < Math.min(resultRows(), results.size()); i++) {
            Item item = results.get(i);
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
        String remove = tr(confirmRemove ? "screen.hogwartsmod.mission_editor.remove_confirm" : "screen.hogwartsmod.mission_editor.remove");
        float removeW = HiRes.width(remove, HiRes.Style.BOLD, 7.5f);
        if (inside(mx, my, (int) (lx + 134 - removeW - 3), top + 28, (int) removeW + 6, 12)) {
            if (confirmRemove) {
                ModNetwork.CHANNEL.sendToServer(new RemoveNpcPacket(entity));
                onClose();
            } else {
                confirmRemove = true;
            }
            click();
            return true;
        }
        confirmRemove = false;
        if (inside(mx, my, lx, top + 40, 134, 13)) {
            return focus(nameBox);
        }
        int dir = stepperDir(mx, my, lx, top + 72, 134);
        if (dir != 0 && !models.isEmpty()) {
            int i = models.indexOf(model);
            model = models.get(Math.floorMod((i < 0 ? 0 : i) + dir, models.size()));
            click();
            return true;
        }
        if (my >= top + 88 && my < top + 101 && mx >= lx - 2 && mx < lx + 110) {
            boolean want = mx >= lx + 56;
            if (want != sellTab) {
                sellTab = want;
                List<Integer> vis = visible();
                selected = vis.isEmpty() ? -1 : vis.get(0);
                page = 0;
                searchBox.setValue("");
                click();
            }
            return true;
        }
        List<Integer> vis = visible();
        for (int k = 0; k < vis.size(); k++) {
            int i = vis.get(k);
            int y = top + 104 + k * ROW;
            if (inside(mx, my, lx - 2, y - 1, 138, ROW - 1)) {
                if (mx >= lx + 124) {
                    offers.remove(i);
                    List<Integer> left2 = visible();
                    selected = left2.isEmpty() ? -1 : left2.get(Math.min(k, left2.size() - 1));
                } else {
                    selected = i;
                }
                click();
                return true;
            }
        }
        if (vis.size() < ShopNpcEntity.MAX_OFFERS && inside(mx, my, lx - 2, top + 104 + vis.size() * ROW - 1, 138, ROW - 1)) {
            offers.add(sellTab ? ShopOffer.blankSell() : ShopOffer.blank());
            selected = offers.size() - 1;
            page = 0;
            searchBox.setValue("");
            focus(searchBox);
            click();
            return true;
        }
        int x = left + 176;
        int w = 148;
        if (inside(mx, my, x, top + H - 26, w, 15)) {
            ModNetwork.CHANNEL.sendToServer(new SaveShopNpcPacket(entity, name, model, offers));
            click();
            onClose();
            return true;
        }
        ShopOffer o = current();
        if (o == null) {
            return super.mouseClicked(mx, my, button);
        }
        if (inside(mx, my, x, top + 28, 148, 13)) {
            page = Math.min(2, (int) ((mx - x) / 50));
            searchBox.setValue("");
            click();
            return true;
        }
        int step0 = hasShiftDown() ? 10 : 1;
        if (page == 2) {
            int d = stepperDir(mx, my, x, top + 59, w);
            if (d != 0) {
                offers.set(selected, o.withLimit(o.max() + d * step0, o.restockDays()));
                click();
                return true;
            }
            if (!o.limited()) {
                return super.mouseClicked(mx, my, button);
            }
            if (inside(mx, my, x + w - 62, top + 75, 62, 15)) {
                offers.set(selected, o.withStock(o.max()));
                click();
                return true;
            }
            if (inside(mx, my, x, top + 98, w, 12)) {
                offers.set(selected, o.withLimit(o.max(), o.restockDays() > 0 ? 0 : 1));
                click();
                return true;
            }
            if (o.restockDays() > 0) {
                int dd = stepperDir(mx, my, x + 52, top + 117, w - 52);
                if (dd != 0) {
                    offers.set(selected, o.withLimit(o.max(), Math.max(1, o.restockDays() + dd)));
                    click();
                    return true;
                }
            }
            return super.mouseClicked(mx, my, button);
        }
        if (page == 1 && o.sell()) {
            int[] am = {o.galeoes(), o.sicles(), o.nuques()};
            for (int i = 0; i < 3; i++) {
                int d = stepperDir(mx, my, x + i * 50 + 11, top + 56, 37);
                if (d != 0) {
                    am[i] = Math.max(0, Math.min(9999, am[i] + d * step0));
                    offers.set(selected, o.withPrice(am[0], am[1], am[2]));
                    click();
                    return true;
                }
            }
            return super.mouseClicked(mx, my, button);
        }
        if (inside(mx, my, x, top + 150, 98, 13)) {
            return focus(searchBox);
        }
        if (inside(mx, my, x + 101, top + 149, 47, 15) && minecraft != null && minecraft.player != null) {
            ItemStack hand = minecraft.player.getMainHandItem();
            if (!hand.isEmpty()) {
                pick(hand.getItem(), hand.getCount());
                click();
            }
            return true;
        }
        for (int i = 0; i < Math.min(resultRows(), results.size()); i++) {
            if (inside(mx, my, x - 2, top + 165 + i * 11 - 1, w + 4, 11)) {
                pick(results.get(i), 1);
                click();
                return true;
            }
        }
        int step = hasShiftDown() ? 10 : 1;
        if (page == 0) {
            int d = stepperDir(mx, my, x + 80, top + 70, w - 80);
            if (d != 0) {
                ShopOffer.Goods p = o.product();
                offers.set(selected, o.withProduct(p.withCount(Math.max(1, Math.min(p.item().getMaxStackSize() * 4, p.count() + d * step)))));
                click();
                return true;
            }
            return super.mouseClicked(mx, my, button);
        }
        int[] amounts = {o.galeoes(), o.sicles(), o.nuques()};
        for (int i = 0; i < 3; i++) {
            int d = stepperDir(mx, my, x + i * 50 + 11, top + 56, 37);
            if (d != 0) {
                amounts[i] = Math.max(0, Math.min(9999, amounts[i] + d * step));
                offers.set(selected, o.withPrice(amounts[0], amounts[1], amounts[2]));
                click();
                return true;
            }
        }
        for (int i = 0; i < o.costs().size(); i++) {
            int y = top + 85 + i * 13;
            List<ShopOffer.Goods> list = new ArrayList<>(o.costs());
            if (inside(mx, my, x + w - 10, y, 10, 12)) {
                list.remove(i);
                offers.set(selected, o.withCosts(list));
                click();
                return true;
            }
            int d = stepperDir(mx, my, x + 82, y + 1, 50);
            if (d != 0) {
                ShopOffer.Goods c = list.get(i);
                list.set(i, c.withCount(Math.max(1, Math.min(c.item().getMaxStackSize() * 4, c.count() + d * step))));
                offers.set(selected, o.withCosts(list));
                click();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private boolean focus(EditBox box) {
        box.setFocused(true);
        setFocused(box);
        return true;
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
