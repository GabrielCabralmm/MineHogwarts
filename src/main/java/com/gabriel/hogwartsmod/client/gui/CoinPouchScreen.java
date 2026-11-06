package com.gabriel.hogwartsmod.client.gui;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.money.Coin;
import com.gabriel.hogwartsmod.money.CoinPouchItem;
import com.gabriel.hogwartsmod.money.PouchActions;
import com.gabriel.hogwartsmod.network.ModNetwork;
import com.gabriel.hogwartsmod.network.PouchActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class CoinPouchScreen extends Screen {
    private static final int W = 300;
    private static final int H = 210;
    private static final int COL = 84;
    private static final int COIN = 40;

    private static final int INK = 0xFFF3E4C4;
    private static final int TITLE = 0xFFF0C860;
    private static final int MUTED = 0xFFC8AE86;
    private static final int HIGHLIGHT = 0x40F0C860;
    private static final int GOLD = 0xFFC9962E;
    private static final int GOLD_DIM = 0xFF6E5430;
    private static final int BUTTON = 0xFF3A2212;
    private static final int BUTTON_ON = 0xFF5A3618;
    private static final int BUTTON_OFF = 0xFF2E1C10;

    private static final ResourceLocation SHEET = tex("bolsa_fundo");

    private final InteractionHand hand;
    private int left;
    private int top;

    public CoinPouchScreen(InteractionHand hand) {
        super(Component.translatable("screen.hogwartsmod.pouch"));
        this.hand = hand;
    }

    public static void open(InteractionHand hand) {
        Minecraft.getInstance().setScreen(new CoinPouchScreen(hand));
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

    private ItemStack pouch() {
        return minecraft == null || minecraft.player == null ? ItemStack.EMPTY : minecraft.player.getItemInHand(hand);
    }

    private int inInventory(Coin coin) {
        int n = 0;
        if (minecraft != null && minecraft.player != null) {
            for (ItemStack stack : minecraft.player.getInventory().items) {
                if (Coin.of(stack) == coin) {
                    n += stack.getCount();
                }
            }
        }
        return n;
    }

    private int colX(int i) {
        return left + W / 2 - COL * 3 / 2 + i * COL;
    }

    private static String tr(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    @Override
    public void tick() {
        if (!(pouch().getItem() instanceof CoinPouchItem)) {
            onClose();
        }
    }

    // -- Desenho

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        HiRes.image(g, SHEET, left, top, W, H);
        HiRes.centered(g, title.getString(), HiRes.Style.BOLD, 12f, left + W / 2f, top + 10, TITLE);
        ItemStack pouch = pouch();
        Coin[] coins = Coin.values();
        for (int i = 0; i < coins.length; i++) {
            Coin coin = coins[i];
            int cx = colX(i) + COL / 2;
            int y = top + 53;
            HiRes.image(g, tex("moeda_" + coin.getId()), cx - COIN / 2, y, COIN, COIN + 2);
            HiRes.centered(g, String.valueOf(CoinPouchItem.get(pouch, coin)), HiRes.Style.BOLD, 13f, cx, y + COIN + 7, INK);
            HiRes.centered(g, tr("money.hogwartsmod." + coin.getId()), HiRes.Style.ITALIC, cx, y + COIN + 22, MUTED);
            int by = y + COIN + 35;
            button(g, cx - 39, by, 38, tr("screen.hogwartsmod.pouch.take"), CoinPouchItem.get(pouch, coin) > 0, mouseX, mouseY);
            button(g, cx + 1, by, 38, tr("screen.hogwartsmod.pouch.put"), inInventory(coin) > 0, mouseX, mouseY);
            if (i < coins.length - 1) {
                drawExchange(g, coin, coins[i + 1], colX(i + 1), y + 8, pouch, mouseX, mouseY);
            }
        }
        int ty = top + 156;
        g.fill(left + 30, ty - 5, left + W - 30, ty - 4, GOLD_DIM);
        HiRes.centered(g, totalLine(pouch), HiRes.Style.ITALIC, left + W / 2f, ty, MUTED);
        int anyCoins = inInventory(Coin.GALEAO) + inInventory(Coin.SICLE) + inInventory(Coin.NUQUE);
        button(g, left + W / 2 - 70, ty + 14, 140, tr("screen.hogwartsmod.pouch.put_all"), anyCoins > 0, mouseX, mouseY);
        HiRes.centered(g, tr("screen.hogwartsmod.pouch.hint"), HiRes.Style.ITALIC, 7f, left + W / 2f, top + H - 20, MUTED);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawExchange(GuiGraphics g, Coin high, Coin low, int x, int y, ItemStack pouch, int mouseX, int mouseY) {
        int rate = high.getValue() / low.getValue();
        boolean canBreak = CoinPouchItem.get(pouch, high) > 0;
        boolean canJoin = CoinPouchItem.get(pouch, low) >= rate;
        arrow(g, "right", x - 5, y, canBreak, mouseX, mouseY);
        arrow(g, "left", x - 5, y + 14, canJoin, mouseX, mouseY);
        HiRes.centered(g, "1 : " + rate, HiRes.Style.BOLD, 7f, x, y + 26, MUTED);
    }

    private void arrow(GuiGraphics g, String dir, int x, int y, boolean enabled, int mouseX, int mouseY) {
        boolean hover = enabled && mouseX >= x - 1 && mouseX < x + 11 && mouseY >= y - 1 && mouseY < y + 11;
        if (hover) {
            g.fill(x - 1, y - 1, x + 11, y + 11, HIGHLIGHT);
        }
        HiRes.image(g, tex("seta_" + dir + (enabled ? "" : "_off")), x + 1, y + 1, 8, 8);
    }

    private void button(GuiGraphics g, int x, int y, int w, String label, boolean enabled, int mouseX, int mouseY) {
        boolean hover = enabled && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + 13;
        g.fill(x, y, x + w, y + 13, enabled ? GOLD : GOLD_DIM);
        g.fill(x + 1, y + 1, x + w - 1, y + 12, hover ? BUTTON_ON : enabled ? BUTTON : BUTTON_OFF);
        HiRes.centered(g, label, HiRes.Style.BOLD, 7.5f, x + w / 2f, y + 3, enabled ? TITLE : GOLD_DIM);
    }

    private String totalLine(ItemStack pouch) {
        long total = CoinPouchItem.total(pouch);
        if (total == 0) {
            return tr("screen.hogwartsmod.pouch.empty");
        }
        long g = total / Coin.GALEAO.getValue();
        long s = total % Coin.GALEAO.getValue() / Coin.SICLE.getValue();
        long n = total % Coin.SICLE.getValue();
        return tr("screen.hogwartsmod.pouch.total", g, s, n);
    }

    // -- Interação

    private void send(PouchActions.Action action, Coin coin, int amount) {
        ModNetwork.CHANNEL.sendToServer(new PouchActionPacket(hand, action, coin, amount));
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) {
            return super.mouseClicked(mx, my, button);
        }
        int amount = hasShiftDown() ? 10 : 1;
        Coin[] coins = Coin.values();
        for (int i = 0; i < coins.length; i++) {
            Coin coin = coins[i];
            int cx = colX(i) + COL / 2;
            int y = top + 53;
            int by = y + COIN + 35;
            if (inside(mx, my, cx - 39, by, 38, 13)) {
                send(PouchActions.Action.WITHDRAW, coin, amount);
                return true;
            }
            if (inside(mx, my, cx + 1, by, 38, 13)) {
                send(PouchActions.Action.DEPOSIT, coin, amount);
                return true;
            }
            if (i < coins.length - 1) {
                int x = colX(i + 1) - 5;
                if (inside(mx, my, x - 1, y + 8 - 1, 12, 12)) {
                    send(PouchActions.Action.BREAK, coin, amount);
                    return true;
                }
                if (inside(mx, my, x - 1, y + 22 - 1, 12, 12)) {
                    send(PouchActions.Action.JOIN, coins[i + 1], amount);
                    return true;
                }
            }
        }
        if (inside(mx, my, left + W / 2 - 70, top + 170, 140, 13)) {
            send(PouchActions.Action.DEPOSIT_ALL, Coin.NUQUE, 1);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }
}
