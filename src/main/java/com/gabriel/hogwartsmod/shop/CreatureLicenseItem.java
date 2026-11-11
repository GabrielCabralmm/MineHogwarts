package com.gabriel.hogwartsmod.shop;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CreatureLicenseItem extends Item {

    public CreatureLicenseItem(Properties properties) {
        super(properties);
    }

    public static void bind(ItemStack stack, Player player) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putUUID("Dono", player.getUUID());
        tag.putString("NomeDono", player.getGameProfile().getName());
    }

    public static boolean isOwner(ItemStack stack, Player player) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID("Dono") && tag.getUUID("Dono").equals(player.getUUID());
    }

    public static boolean has(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof CreatureLicenseItem && isOwner(stack, player)) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.getItem() instanceof CreatureLicenseItem && isOwner(stack, player)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && entity instanceof Player player && (stack.getTag() == null || !stack.getTag().hasUUID("Dono"))) {
            bind(stack, player);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("NomeDono")) {
            tooltip.add(Component.translatable("item.hogwartsmod.licenca_criaturas.owner", tag.getString("NomeDono")).withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(Component.translatable("item.hogwartsmod.licenca_criaturas.hint").withStyle(ChatFormatting.GRAY));
    }
}
