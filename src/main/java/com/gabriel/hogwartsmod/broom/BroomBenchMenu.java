package com.gabriel.hogwartsmod.broom;

import com.gabriel.hogwartsmod.registry.ModBlocks;
import com.gabriel.hogwartsmod.registry.ModItems;
import com.gabriel.hogwartsmod.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class BroomBenchMenu extends AbstractContainerMenu {
    public static final int PART_X = 20;
    public static final int HANDLE_Y = 32;
    public static final int BRISTLE_Y = 52;
    public static final int CHEST_Y = 72;
    public static final int INV_Y = 150;
    public static final int BRISTLES = 2;

    private final ContainerLevelAccess access;
    private final SimpleContainer parts = new SimpleContainer(3);

    public BroomBenchMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public BroomBenchMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(ModMenus.BANCADA_VASSOURAS.get(), id);
        this.access = access;
        parts.addListener(this::slotsChanged);
        addSlot(new Slot(parts, 0, PART_X, HANDLE_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isHandle(stack);
            }
        });
        addSlot(new Slot(parts, 1, PART_X, BRISTLE_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isBristle(stack);
            }
        });
        addSlot(new Slot(parts, 2, PART_X, CHEST_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isChest(stack);
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

    public static boolean isHandle(ItemStack stack) {
        return stack.getItem() instanceof BroomHandleItem;
    }

    public static boolean isBristle(ItemStack stack) {
        return stack.is(ModItems.FEIXE_CERDAS.get());
    }

    public static boolean isChest(ItemStack stack) {
        return stack.is(Items.CHEST);
    }

    public ItemStack handle() {
        return parts.getItem(0);
    }

    public ItemStack bristles() {
        return parts.getItem(1);
    }

    public ItemStack chest() {
        return parts.getItem(2);
    }

    public boolean hasParts(BroomType type) {
        return isHandle(handle()) && bristles().getCount() >= BRISTLES && (type != BroomType.CARGO || isChest(chest()));
    }

    public ContainerLevelAccess access() {
        return access;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.BANCADA_VASSOURAS.get()) && BroomBenchBlock.canUse(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < 3) {
            if (!moveItemStackTo(stack, 3, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (isHandle(stack)) {
            if (!moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (isBristle(stack)) {
            if (!moveItemStackTo(stack, 1, 2, false)) {
                return ItemStack.EMPTY;
            }
        } else if (isChest(stack)) {
            if (!moveItemStackTo(stack, 2, 3, false)) {
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
