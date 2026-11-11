package com.gabriel.hogwartsmod.mail;

import net.minecraft.world.item.ItemStack;

public final class Mail {

    private Mail() {
    }

    public static boolean isMail(ItemStack stack) {
        return stack.getItem() instanceof LetterItem || stack.getItem() instanceof PackageItem;
    }

    public static boolean isSealed(ItemStack stack) {
        return isMail(stack) && stack.getTag() != null && stack.getTag().getBoolean("Selada");
    }

    public static boolean isPackage(ItemStack stack) {
        return stack.getItem() instanceof PackageItem;
    }
}
