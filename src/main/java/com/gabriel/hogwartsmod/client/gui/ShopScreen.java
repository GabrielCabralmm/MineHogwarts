package com.gabriel.hogwartsmod.client.gui;

import com.gabriel.hogwartsmod.money.Coin;
import com.gabriel.hogwartsmod.money.Wallet;
import com.gabriel.hogwartsmod.network.ModNetwork;
import com.gabriel.hogwartsmod.network.OpenShopPacket;
import com.gabriel.hogwartsmod.network.ShopBuyPacket;
import com.gabriel.hogwartsmod.shop.ShopManager;
import com.gabriel.hogwartsmod.shop.ShopOffer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static com.gabriel.hogwartsmod.client.gui.MissionScreen.*;

public class ShopScreen extends Screen {
    private static final int W = 320;
    private static final int H = 210;
    private static final int ROW = 16;
    private static final ResourceLocation SHEET = MissionScreen.tex("pulpit_sheet");

    private final OpenShopPacket data;
    private int selected;
    private int left;
    private int top;

    public ShopScreen(OpenShopPacket data) {
        super(Component.literal(data.name()));
        this.data = data;
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

    private ShopOffer current() {
        List<ShopOffer> offers = data.offers();
        return offers.isEmpty() ? null : offers.get(Math.min(selected, offers.size() - 1));
    }

    private Player player() {
        return minecraft == null ? null : minecraft.player;
    }

    static String goodsName(ShopOffer.Goods goods) {
        String name = goods.item().getDescription().getString();
        return goods.count() > 1 ? goods.count() + "× " + name : name;
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
        HiRes.text(g, tr("screen.hogwartsmod.shop.list"), HiRes.Style.BOLD, x, top + 31, INK);
        List<ShopOffer> offers = data.offers();
        if (offers.isEmpty()) {
            HiRes.text(g, tr("screen.hogwartsmod.shop.empty"), HiRes.Style.ITALIC, x, top + 47, MUTED);
        }
        Player player = player();
        for (int i = 0; i < offers.size(); i++) {
            ShopOffer offer = offers.get(i);
            int y = top + 44 + i * ROW;
            boolean chosen = i == selected;
            boolean hover = mouseX >= x - 2 && mouseX < x + w + 2 && mouseY >= y - 1 && mouseY < y + ROW - 2;
            if (chosen || hover) {
                HiRes.highlight(g, x - 2, y - 1, x + w + 2, y + ROW - 2, chosen ? HiRes.Mark.SELECTED : HiRes.Mark.HOVER);
            }
            boolean afford = player != null && ShopManager.canBuy(player, offer);
            MissionScreen.item(g, offer.product().stack(), x + 2, y + 1, 11);
            HiRes.text(g, HiRes.ellipsize(goodsName(offer.product()), HiRes.Style.REGULAR, w - 18), HiRes.Style.REGULAR, x + 16, y + 2,
                    afford ? INK : MUTED);
        }
        drawWallet(g, x, top + H - 25);
    }

    private void drawWallet(GuiGraphics g, int x, int y) {
        Player player = player();
        if (player == null) {
            return;
        }
        String label = tr("screen.hogwartsmod.shop.wallet");
        HiRes.text(g, label, HiRes.Style.ITALIC, 7.5f, x, y + 1, MUTED);
        float cx = x + HiRes.width(label, HiRes.Style.ITALIC, 7.5f) + 5;
        for (Coin coin : Coin.values()) {
            HiRes.image(g, MissionScreen.tex("moeda_" + coin.getId()), Math.round(cx), y, 9, 10);
            String n = String.valueOf(Wallet.count(player, coin));
            HiRes.text(g, n, HiRes.Style.BOLD, 7.5f, cx + 11, y + 1, INK);
            cx += 11 + HiRes.width(n, HiRes.Style.BOLD, 7.5f) + 6;
        }
    }

    private void drawDetails(GuiGraphics g, int mouseX, int mouseY) {
        ShopOffer offer = current();
        int x = left + 174;
        int w = 128;
        float cx = x + w / 2f;
        if (offer == null) {
            return;
        }
        Player player = player();
        ItemStack stack = offer.product().stack();
        HiRes.highlight(g, Math.round(cx) - 16, top + 29, Math.round(cx) + 16, top + 61, HiRes.Mark.SELECTED);
        MissionScreen.item(g, stack, Math.round(cx) - 12, top + 33, 24);
        HiRes.centered(g, HiRes.ellipsize(goodsName(offer.product()), HiRes.Style.BOLD, w), HiRes.Style.BOLD, 9.5f, cx, top + 67, TITLE);
        int y = top + 86;
        HiRes.text(g, tr("screen.hogwartsmod.shop.price"), HiRes.Style.BOLD, x, y, INK);
        y += 12;
        boolean coinsOk = player != null && Wallet.value(player) >= offer.price();
        if (offer.free()) {
            HiRes.text(g, tr("screen.hogwartsmod.shop.free"), HiRes.Style.ITALIC, x, y, GREEN);
            y += 12;
        }
        if (offer.price() > 0) {
            int[] amounts = {offer.galeoes(), offer.sicles(), offer.nuques()};
            float px = x;
            Coin[] coins = Coin.values();
            for (int i = 0; i < coins.length; i++) {
                if (amounts[i] <= 0) {
                    continue;
                }
                HiRes.image(g, MissionScreen.tex("moeda_" + coins[i].getId()), Math.round(px), y - 1, 10, 11);
                String n = String.valueOf(amounts[i]);
                HiRes.text(g, n, HiRes.Style.BOLD, px + 12, y, coinsOk ? INK : WINE);
                px += 12 + HiRes.width(n, HiRes.Style.BOLD) + 8;
            }
            y += 13;
        }
        for (ShopOffer.Goods cost : offer.costs()) {
            int have = player == null ? 0 : ShopManager.count(player, cost.item());
            boolean ok = have >= cost.count();
            MissionScreen.item(g, cost.stack(), x, y - 1, 10);
            String label = cost.count() + "× " + cost.item().getDescription().getString();
            String mine = "(" + have + ")";
            float mw = HiRes.width(mine, HiRes.Style.ITALIC, 7f);
            HiRes.text(g, HiRes.ellipsize(label, HiRes.Style.REGULAR, w - mw - 18), HiRes.Style.REGULAR, x + 13, y, ok ? INK : WINE);
            HiRes.text(g, mine, HiRes.Style.ITALIC, 7f, x + w - mw, y + 1, ok ? GREEN : WINE);
            y += 12;
        }
        boolean afford = player != null && ShopManager.canBuy(player, offer);
        HiRes.centered(g, tr("screen.hogwartsmod.shop.bulk", ShopManager.BULK), HiRes.Style.ITALIC, 7f, cx, top + H - 42, MUTED);
        String label = afford ? tr("screen.hogwartsmod.shop.buy") : tr(coinsOk ? "screen.hogwartsmod.shop.missing_items" : "screen.hogwartsmod.shop.missing_coins");
        button(g, x, top + H - 30, w, label, afford, GREEN, mouseX, mouseY);
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
        List<ShopOffer> offers = data.offers();
        for (int i = 0; i < offers.size(); i++) {
            int y = top + 44 + i * ROW;
            if (mx >= x - 2 && mx < x + 130 && my >= y - 1 && my < y + ROW - 2) {
                selected = i;
                click();
                return true;
            }
        }
        ShopOffer offer = current();
        int bx = left + 174;
        int by = top + H - 30;
        if (offer != null && mx >= bx && mx < bx + 128 && my >= by && my < by + 15 && player() != null && ShopManager.canBuy(player(), offer)) {
            ModNetwork.CHANNEL.sendToServer(new ShopBuyPacket(data.entity(), offer.id(), hasShiftDown() ? ShopManager.BULK : 1));
            click();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }
}
