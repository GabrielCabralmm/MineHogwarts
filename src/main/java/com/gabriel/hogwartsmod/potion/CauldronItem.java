package com.gabriel.hogwartsmod.potion;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CauldronItem extends BlockItem {
    public CauldronItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag data = getBlockEntityData(stack);
        if (data != null) {
            Brew brew = Brew.byId(data.getString("Pocao"));
            String state = data.getString("Estado");
            Component text = brew != null && state.equals("READY")
                    ? Component.translatable("item.hogwartsmod.caldeirao.holds_potion", brew.getDisplayName(), data.getInt("Doses"))
                    : Component.translatable("item.hogwartsmod.caldeirao.holds_" + state.toLowerCase());
            tooltip.add(text.copy().withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("item.hogwartsmod.caldeirao.hint").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
