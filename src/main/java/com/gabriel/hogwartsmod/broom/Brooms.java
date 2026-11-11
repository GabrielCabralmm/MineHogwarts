package com.gabriel.hogwartsmod.broom;

import com.gabriel.hogwartsmod.wand.WandWood;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class Brooms {
    private static final String TAG = "Vassoura";

    private Brooms() {
    }

    public static ItemStack create(BroomType type, @Nullable WandWood wood, @Nullable String maker) {
        ItemStack stack = new ItemStack(type.item());
        CompoundTag tag = new CompoundTag();
        if (wood != null) {
            tag.putString("Madeira", wood.getId());
        }
        if (maker != null) {
            tag.putString("Fabricante", maker);
        }
        if (!tag.isEmpty()) {
            stack.getOrCreateTag().put(TAG, tag);
        }
        return stack;
    }

    @Nullable
    private static CompoundTag tag(ItemStack stack) {
        CompoundTag root = stack.getTag();
        return root != null && root.contains(TAG) ? root.getCompound(TAG) : null;
    }

    private static CompoundTag orCreate(ItemStack stack) {
        CompoundTag root = stack.getOrCreateTag();
        if (!root.contains(TAG)) {
            root.put(TAG, new CompoundTag());
        }
        return root.getCompound(TAG);
    }

    @Nullable
    public static WandWood wood(ItemStack stack) {
        CompoundTag tag = tag(stack);
        return tag == null ? null : WandWood.byId(tag.getString("Madeira"));
    }

    public static String maker(ItemStack stack) {
        CompoundTag tag = tag(stack);
        return tag == null ? "" : tag.getString("Fabricante");
    }

    public static void setParts(ItemStack stack, @Nullable WandWood wood, String maker) {
        if (wood == null && maker.isEmpty()) {
            return;
        }
        CompoundTag tag = orCreate(stack);
        if (wood != null) {
            tag.putString("Madeira", wood.getId());
        }
        if (!maker.isEmpty()) {
            tag.putString("Fabricante", maker);
        }
    }

    public static void saveCargo(ItemStack stack, Container cargo) {
        ListTag list = new ListTag();
        int count = 0;
        for (int i = 0; i < cargo.getContainerSize(); i++) {
            ItemStack item = cargo.getItem(i);
            if (!item.isEmpty()) {
                CompoundTag entry = item.save(new CompoundTag());
                entry.putByte("Slot", (byte) i);
                list.add(entry);
                count++;
            }
        }
        if (count > 0) {
            orCreate(stack).put("Carga", list);
        }
    }

    public static void loadCargo(ItemStack stack, Container cargo) {
        CompoundTag tag = tag(stack);
        if (tag == null) {
            return;
        }
        ListTag list = tag.getList("Carga", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int slot = entry.getByte("Slot") & 255;
            if (slot < cargo.getContainerSize()) {
                cargo.setItem(slot, ItemStack.of(entry));
            }
        }
    }

    public static int cargoCount(ItemStack stack) {
        CompoundTag tag = tag(stack);
        return tag == null ? 0 : tag.getList("Carga", 10).size();
    }

    public static void appendTooltip(BroomType type, ItemStack stack, List<Component> tooltip) {
        WandWood wood = wood(stack);
        if (wood != null) {
            tooltip.add(Component.translatable("tooltip.hogwartsmod.broom.wood", wood.getDisplayName()).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable(type == BroomType.CARGO ? "tooltip.hogwartsmod.broom.cargo" : "tooltip.hogwartsmod.broom.seats",
                type == BroomType.CARGO ? BroomType.CARGO_SLOTS : type.getSeats()).withStyle(ChatFormatting.GRAY));
        int items = cargoCount(stack);
        if (items > 0) {
            tooltip.add(Component.translatable("tooltip.hogwartsmod.broom.loaded", items).withStyle(ChatFormatting.GOLD));
        }
        String maker = maker(stack);
        if (!maker.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.hogwartsmod.wand.maker", maker).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
