package com.gabriel.hogwartsmod.wand;

import com.gabriel.hogwartsmod.registry.ModMenus;
import com.gabriel.hogwartsmod.registry.ModBlocks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class WandBenchMenu extends AbstractContainerMenu {
    public static final int BRANCH_X = 20;
    public static final int BRANCH_Y = 36;
    public static final int CORE_X = 20;
    public static final int CORE_Y = 60;
    public static final int INV_Y = 148;

    private final ContainerLevelAccess access;
    private final SimpleContainer parts = new SimpleContainer(2);

    public WandBenchMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public WandBenchMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(ModMenus.BANCADA_VARINHAS.get(), id);
        this.access = access;
        parts.addListener(this::slotsChanged);
        addSlot(new Slot(parts, 0, BRANCH_X, BRANCH_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isBranch(stack);
            }
        });
        addSlot(new Slot(parts, 1, CORE_X, CORE_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isCore(stack);
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, INV_Y + 58));
        }
    }

    public static boolean isBranch(ItemStack stack) {
        return stack.getItem() instanceof BranchItem;
    }

    public static boolean isCore(ItemStack stack) {
        return WandCore.byItem(stack.getItem()) != null;
    }

    public ItemStack branch() {
        return parts.getItem(0);
    }

    public ItemStack core() {
        return parts.getItem(1);
    }

    public ContainerLevelAccess access() {
        return access;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.BANCADA_VARINHAS.get()) && WandBenchBlock.canUse(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < 2) {
            if (!moveItemStackTo(stack, 2, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (isBranch(stack)) {
            if (!moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (isCore(stack)) {
            if (!moveItemStackTo(stack, 1, 2, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, pos) -> clearContainer(player, parts));
    }
}
