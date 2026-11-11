package com.gabriel.hogwartsmod.wand;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HandleItem extends Item {
    private final WandHandle handle;

    public HandleItem(WandHandle handle, Properties properties) {
        super(properties);
        this.handle = handle;
    }

    public WandHandle getHandle() {
        return handle;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.hogwartsmod.cabo.hint").withStyle(ChatFormatting.GRAY));
    }
}
