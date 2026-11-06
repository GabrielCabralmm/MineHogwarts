package com.gabriel.hogwartsmod.money;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class MoneyHelper {

    private MoneyHelper() {
    }

    public static void give(ServerPlayer player, int galeoes, int sicles, int nuques) {
        ItemStack pouch = ItemStack.EMPTY;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof CoinPouchItem) {
                pouch = stack;
                break;
            }
        }
        int[] amounts = {galeoes, sicles, nuques};
        Coin[] coins = {Coin.GALEAO, Coin.SICLE, Coin.NUQUE};
        for (int i = 0; i < coins.length; i++) {
            if (amounts[i] <= 0) {
                continue;
            }
            if (!pouch.isEmpty()) {
                CoinPouchItem.add(pouch, coins[i], amounts[i]);
                continue;
            }
            int left = amounts[i];
            while (left > 0) {
                int n = Math.min(64, left);
                ItemStack stack = new ItemStack(coins[i].item(), n);
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
                left -= n;
            }
        }
    }
}
