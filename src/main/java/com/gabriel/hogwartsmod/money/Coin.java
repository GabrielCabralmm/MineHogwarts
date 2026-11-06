package com.gabriel.hogwartsmod.money;

import com.gabriel.hogwartsmod.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public enum Coin {
    GALEAO("galeao", 17 * 29),
    SICLE("sicle", 29),
    NUQUE("nuque", 1);

    public static final int SICLES_PER_GALEAO = 17;
    public static final int NUQUES_PER_SICLE = 29;

    private final String id;
    private final int value;

    Coin(String id, int value) {
        this.id = id;
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public int getValue() {
        return value;
    }

    public Item item() {
        return switch (this) {
            case GALEAO -> ModItems.GALEAO.get();
            case SICLE -> ModItems.SICLE.get();
            case NUQUE -> ModItems.NUQUE.get();
        };
    }

    @Nullable
    public static Coin of(ItemStack stack) {
        for (Coin coin : values()) {
            if (stack.is(coin.item())) {
                return coin;
            }
        }
        return null;
    }
}
