package com.gabriel.hogwartsmod.mail;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PackageItem extends Item {

    public PackageItem(Properties properties) {
        super(properties);
    }

    // -- Dados do pacote

    public static ItemStack content(ItemStack box) {
        CompoundTag tag = box.getTag();
        return tag == null || !tag.contains("Conteudo") ? ItemStack.EMPTY : ItemStack.of(tag.getCompound("Conteudo"));
    }

    public static void setContent(ItemStack box, ItemStack content) {
        if (content.isEmpty()) {
            if (box.getTag() != null) {
                box.getTag().remove("Conteudo");
                if (box.getTag().isEmpty()) {
                    box.setTag(null);
                }
            }
            return;
        }
        box.getOrCreateTag().put("Conteudo", content.save(new CompoundTag()));
    }

    public static String author(ItemStack box) {
        return box.getTag() == null ? "" : box.getTag().getString("Autor");
    }

    public static void seal(ItemStack box, String author) {
        CompoundTag tag = box.getOrCreateTag();
        tag.putBoolean("Selada", true);
        tag.putString("Autor", author);
    }

    // -- Item

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (Mail.isSealed(stack)) {
            if (level.isClientSide) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.gabriel.hogwartsmod.client.gui.PackageViewScreen.open(hand));
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (!level.isClientSide && player instanceof ServerPlayer server) {
            if (stack.getCount() > 1) {
                ItemStack rest = stack.copyWithCount(stack.getCount() - 1);
                stack.setCount(1);
                int free = player.getInventory().getFreeSlot();
                if (free >= 0) {
                    player.getInventory().setItem(free, rest);
                } else {
                    player.drop(rest, false);
                }
            }
            NetworkHooks.openScreen(server, new SimpleMenuProvider((id, inv, p) -> new PackageMenu(id, inv, hand),
                    Component.translatable("item.hogwartsmod.pacote")), buf -> buf.writeEnum(hand));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    @Override
    public Component getName(ItemStack stack) {
        if (Mail.isSealed(stack)) {
            return Component.translatable("item.hogwartsmod.pacote.from", author(stack));
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (Mail.isSealed(stack)) {
            tooltip.add(Component.translatable("item.hogwartsmod.envelope.sealed_hint").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("item.hogwartsmod.pacote.open_hint").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        ItemStack content = content(stack);
        if (content.isEmpty()) {
            tooltip.add(Component.translatable("item.hogwartsmod.pacote.hint").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.literal(content.getCount() + "× ").append(content.getHoverName()).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("item.hogwartsmod.envelope.draft").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }
}
