package com.gabriel.hogwartsmod.wand;

import com.gabriel.hogwartsmod.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class Wands {
    public static final int MIN_LENGTH = 36;
    public static final int MAX_LENGTH = 56;
    private static final String TAG = "Varinha";

    private Wands() {
    }

    public static void init() {
        for (WandCore core : WandCore.values()) {
            core.bind(ModItems.ITEMS.register(core.getItemId(), () -> new Item(new Item.Properties())));
        }
        for (WandHandle handle : WandHandle.values()) {
            if (handle != WandHandle.NENHUM) {
                handle.bind(ModItems.ITEMS.register("cabo_" + handle.getId(), () -> new HandleItem(handle, new Item.Properties().stacksTo(16))));
            }
        }
    }

    public static int clampLength(int quarters) {
        return Math.max(MIN_LENGTH, Math.min(MAX_LENGTH, quarters));
    }

    public static ItemStack create(WandWood wood, WandCore core, int quarters, WandHandle handle, @Nullable String maker) {
        ItemStack stack = new ItemStack(ModItems.WAND.get());
        CompoundTag tag = new CompoundTag();
        tag.putString("Madeira", wood.getId());
        tag.putString("Nucleo", core.getId());
        tag.putInt("Comprimento", clampLength(quarters));
        tag.putString("Cabo", handle.getId());
        if (maker != null) {
            tag.putString("Fabricante", maker);
        }
        stack.getOrCreateTag().put(TAG, tag);
        return stack;
    }

    @Nullable
    private static CompoundTag tag(ItemStack stack) {
        CompoundTag root = stack.getTag();
        return root != null && root.contains(TAG) ? root.getCompound(TAG) : null;
    }

    public static boolean isCustom(ItemStack stack) {
        return wood(stack) != null && core(stack) != null;
    }

    @Nullable
    public static WandWood wood(ItemStack stack) {
        CompoundTag tag = tag(stack);
        return tag == null ? null : WandWood.byId(tag.getString("Madeira"));
    }

    @Nullable
    public static WandCore core(ItemStack stack) {
        CompoundTag tag = tag(stack);
        return tag == null ? null : WandCore.byId(tag.getString("Nucleo"));
    }

    public static WandHandle handle(ItemStack stack) {
        CompoundTag tag = tag(stack);
        WandHandle handle = tag == null ? null : WandHandle.byId(tag.getString("Cabo"));
        return handle == null ? WandHandle.NENHUM : handle;
    }

    public static void setHandle(ItemStack stack, WandHandle handle) {
        CompoundTag tag = tag(stack);
        if (tag != null) {
            tag.putString("Cabo", handle.getId());
        }
    }

    public static int length(ItemStack stack) {
        CompoundTag tag = tag(stack);
        return tag == null ? 44 : clampLength(tag.getInt("Comprimento"));
    }

    public static void copyParts(ItemStack from, ItemStack to) {
        CompoundTag tag = tag(from);
        if (tag != null) {
            to.getOrCreateTag().put(TAG, tag.copy());
        }
    }

    public static void setOwner(ItemStack stack, Player player) {
        CompoundTag tag = tag(stack);
        if (tag != null) {
            tag.putString("Dono", player.getGameProfile().getName());
            tag.putUUID("DonoId", player.getUUID());
        }
    }

    public static String owner(ItemStack stack) {
        CompoundTag tag = tag(stack);
        return tag == null ? "" : tag.getString("Dono");
    }

    public static String maker(ItemStack stack) {
        CompoundTag tag = tag(stack);
        return tag == null ? "" : tag.getString("Fabricante");
    }

    public static WandStats stats(ItemStack stack) {
        WandWood wood = wood(stack);
        WandCore core = core(stack);
        return wood == null || core == null ? WandStats.NEUTRAL : WandStats.of(wood, core, length(stack));
    }

    public static WandStats held(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.is(ModItems.WAND.get())) {
            return stats(main);
        }
        ItemStack off = player.getOffhandItem();
        return off.is(ModItems.WAND.get()) ? stats(off) : WandStats.NEUTRAL;
    }

    // -- Texto

    public static String formatLength(int quarters) {
        int whole = quarters / 4;
        String frac = switch (quarters % 4) {
            case 1 -> "¼";
            case 2 -> "½";
            case 3 -> "¾";
            default -> "";
        };
        return whole + frac;
    }

    public static void appendTooltip(ItemStack stack, List<Component> tooltip) {
        WandWood wood = wood(stack);
        WandCore core = core(stack);
        if (wood == null || core == null) {
            return;
        }
        tooltip.add(Component.translatable("tooltip.hogwartsmod.wand.wood", wood.getDisplayName()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.hogwartsmod.wand.core", core.getDisplayName()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.hogwartsmod.wand.length", formatLength(length(stack)), handle(stack).getDisplayName()).withStyle(ChatFormatting.GRAY));
        WandStats s = stats(stack);
        stat(tooltip, "damage", s.damage(), true);
        stat(tooltip, "range", s.range(), true);
        stat(tooltip, "cooldown", s.cooldown(), false);
        stat(tooltip, "cost", s.cost(), false);
        stat(tooltip, "shield", s.shield(), true);
        stat(tooltip, "mastery", s.mastery(), true);
        String maker = maker(stack);
        if (!maker.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.hogwartsmod.wand.maker", maker).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    private static void stat(List<Component> tooltip, String key, float value, boolean higherIsBetter) {
        int pct = Math.round((value - 1f) * 100f);
        if (pct == 0) {
            return;
        }
        boolean good = (pct > 0) == higherIsBetter;
        MutableComponent line = Component.literal((pct > 0 ? "+" : "−") + Math.abs(pct) + "% ")
                .append(Component.translatable("tooltip.hogwartsmod.wand.stat." + key));
        tooltip.add(line.withStyle(good ? ChatFormatting.BLUE : ChatFormatting.RED));
    }
}
