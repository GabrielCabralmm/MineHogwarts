package com.gabriel.hogwartsmod.money;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class Wallet {

    private Wallet() {
    }

    public static int count(Player player, Coin coin) {
        int n = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof CoinPouchItem) {
                n += CoinPouchItem.get(stack, coin);
            } else if (Coin.of(stack) == coin) {
                n += stack.getCount();
            }
        }
        return n;
    }

    public static long value(Player player) {
        long total = 0;
        for (Coin coin : Coin.values()) {
            total += (long) count(player, coin) * coin.getValue();
        }
        return total;
    }

    public static boolean pay(Player player, int galeoes, int sicles, int nuques) {
        long price = (long) galeoes * Coin.GALEAO.getValue() + (long) sicles * Coin.SICLE.getValue() + nuques;
        if (price <= 0) {
            return true;
        }
        if (value(player) < price) {
            return false;
        }
        int[] want = {galeoes, sicles, nuques};
        Coin[] coins = Coin.values();
        boolean exact = true;
        for (int i = 0; i < coins.length; i++) {
            exact &= count(player, coins[i]) >= want[i];
        }
        if (exact) {
            for (int i = 0; i < coins.length; i++) {
                take(player, coins[i], want[i]);
            }
            return true;
        }
        long change = value(player) - price;
        for (Coin coin : coins) {
            take(player, coin, Integer.MAX_VALUE);
        }
        int g = (int) (change / Coin.GALEAO.getValue());
        change -= (long) g * Coin.GALEAO.getValue();
        int s = (int) (change / Coin.SICLE.getValue());
        change -= (long) s * Coin.SICLE.getValue();
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            MoneyHelper.give(serverPlayer, g, s, (int) change);
        }
        return true;
    }

    private static void take(Player player, Coin coin, int amount) {
        int left = amount;
        for (ItemStack stack : player.getInventory().items) {
            if (left <= 0) {
                return;
            }
            if (Coin.of(stack) == coin) {
                int n = Math.min(left, stack.getCount());
                stack.shrink(n);
                left -= n;
            }
        }
        for (ItemStack stack : player.getInventory().items) {
            if (left <= 0) {
                return;
            }
            if (stack.getItem() instanceof CoinPouchItem) {
                int n = Math.min(left, CoinPouchItem.get(stack, coin));
                CoinPouchItem.add(stack, coin, -n);
                left -= n;
            }
        }
    }
}
