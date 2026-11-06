package com.gabriel.hogwartsmod.money;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CoinPouchItem extends Item {
    private static final String TAG = "Moedas";

    public CoinPouchItem(Properties properties) {
        super(properties);
    }

    public static int get(ItemStack pouch, Coin coin) {
        CompoundTag tag = pouch.getTag();
        return tag == null ? 0 : tag.getCompound(TAG).getInt(coin.getId());
    }

    public static void set(ItemStack pouch, Coin coin, int amount) {
        CompoundTag coins = pouch.getOrCreateTag().getCompound(TAG);
        if (amount <= 0) {
            coins.remove(coin.getId());
        } else {
            coins.putInt(coin.getId(), amount);
        }
        pouch.getOrCreateTag().put(TAG, coins);
    }

    public static void add(ItemStack pouch, Coin coin, int amount) {
        set(pouch, coin, get(pouch, coin) + amount);
    }

    public static long total(ItemStack pouch) {
        long total = 0;
        for (Coin coin : Coin.values()) {
            total += (long) get(pouch, coin) * coin.getValue();
        }
        return total;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.gabriel.hogwartsmod.client.gui.CoinPouchScreen.open(hand));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        boolean empty = true;
        for (Coin coin : Coin.values()) {
            int amount = get(stack, coin);
            if (amount > 0) {
                empty = false;
                tooltip.add(Component.translatable("money.hogwartsmod.line", amount, Component.translatable("money.hogwartsmod." + coin.getId()))
                        .withStyle(coin == Coin.GALEAO ? ChatFormatting.GOLD : coin == Coin.SICLE ? ChatFormatting.GRAY : ChatFormatting.RED));
            }
        }
        if (empty) {
            tooltip.add(Component.translatable("item.hogwartsmod.bolsa_moedas.empty").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
        tooltip.add(Component.translatable("item.hogwartsmod.bolsa_moedas.tip").withStyle(ChatFormatting.DARK_GRAY));
    }
}
