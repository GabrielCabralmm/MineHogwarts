package com.gabriel.hogwartsmod.money;

import com.gabriel.hogwartsmod.HogwartsMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = HogwartsMod.MOD_ID)
public final class PouchActions {

    public enum Action { DEPOSIT_ALL, DEPOSIT, WITHDRAW, BREAK, JOIN }

    private PouchActions() {
    }

    public static void handle(ServerPlayer player, InteractionHand hand, Action action, Coin coin, int amount) {
        ItemStack pouch = player.getItemInHand(hand);
        if (!(pouch.getItem() instanceof CoinPouchItem)) {
            return;
        }
        amount = Math.max(1, Math.min(amount, 6400));
        switch (action) {
            case DEPOSIT_ALL -> {
                int moved = 0;
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    Coin found = Coin.of(stack);
                    if (found != null) {
                        CoinPouchItem.add(pouch, found, stack.getCount());
                        moved += stack.getCount();
                        player.getInventory().setItem(i, ItemStack.EMPTY);
                    }
                }
                if (moved > 0) {
                    jingle(player, 1.0f);
                }
            }
            case DEPOSIT -> {
                int left = amount;
                for (int i = 0; i < player.getInventory().getContainerSize() && left > 0; i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (Coin.of(stack) == coin) {
                        int take = Math.min(left, stack.getCount());
                        stack.shrink(take);
                        left -= take;
                    }
                }
                if (left < amount) {
                    CoinPouchItem.add(pouch, coin, amount - left);
                    jingle(player, 1.0f);
                }
            }
            case WITHDRAW -> {
                int have = CoinPouchItem.get(pouch, coin);
                int take = Math.min(have, amount);
                if (take > 0) {
                    CoinPouchItem.set(pouch, coin, have - take);
                    ItemStack coins = new ItemStack(coin.item(), take);
                    if (!player.getInventory().add(coins)) {
                        player.drop(coins, false);
                    }
                    jingle(player, 0.9f);
                }
            }
            case BREAK -> {
                if (coin == Coin.NUQUE) {
                    return;
                }
                Coin lower = coin == Coin.GALEAO ? Coin.SICLE : Coin.NUQUE;
                int rate = coin.getValue() / lower.getValue();
                int have = CoinPouchItem.get(pouch, coin);
                int n = Math.min(have, amount);
                if (n > 0) {
                    CoinPouchItem.set(pouch, coin, have - n);
                    CoinPouchItem.add(pouch, lower, n * rate);
                    jingle(player, 1.2f);
                }
            }
            case JOIN -> {
                if (coin == Coin.GALEAO) {
                    return;
                }
                Coin higher = coin == Coin.NUQUE ? Coin.SICLE : Coin.GALEAO;
                int rate = higher.getValue() / coin.getValue();
                int have = CoinPouchItem.get(pouch, coin);
                int n = Math.min(have / rate, amount);
                if (n > 0) {
                    CoinPouchItem.set(pouch, coin, have - n * rate);
                    CoinPouchItem.add(pouch, higher, n);
                    jingle(player, 1.3f);
                }
            }
        }
    }

    private static void jingle(Player player, float pitch) {
        player.level().playSound(null, player.blockPosition(), SoundEvents.CHAIN_STEP, SoundSource.PLAYERS, 0.6f, pitch * 1.6f);
        player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.15f, pitch * 1.4f);
    }

    @SubscribeEvent
    public static void pickup(EntityItemPickupEvent event) {
        ItemEntity entity = event.getItem();
        ItemStack stack = entity.getItem();
        Coin coin = Coin.of(stack);
        Player player = event.getEntity();
        if (coin == null || player.level().isClientSide || entity.hasPickUpDelay()) {
            return;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack pouch = player.getInventory().getItem(i);
            if (pouch.getItem() instanceof CoinPouchItem) {
                CoinPouchItem.add(pouch, coin, stack.getCount());
                player.take(entity, stack.getCount());
                entity.discard();
                event.setCanceled(true);
                jingle(player, 1.1f);
                return;
            }
        }
    }
}
