package com.gabriel.hogwartsmod.wand;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BranchItem extends Item {
    private final WandWood wood;

    public BranchItem(WandWood wood, Properties properties) {
        super(properties);
        this.wood = wood;
    }

    public WandWood getWood() {
        return wood;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.hogwartsmod.galho.hint").withStyle(ChatFormatting.GRAY));
    }
}
