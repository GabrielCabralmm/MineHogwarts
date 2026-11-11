package com.gabriel.hogwartsmod.mail;

import com.gabriel.hogwartsmod.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class PackageMenu extends AbstractContainerMenu {
    public static final int SLOT_X = 80;
    public static final int SLOT_Y = 44;
    public static final int INV_Y = 108;

    private final InteractionHand hand;
    private final Player player;
    private final SimpleContainer box = new SimpleContainer(1);

    public PackageMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
        this(id, inventory, buf.readEnum(InteractionHand.class));
    }

    public PackageMenu(int id, Inventory inventory, InteractionHand hand) {
        super(ModMenus.PACOTE.get(), id);
        this.hand = hand;
        this.player = inventory.player;
        box.setItem(0, PackageItem.content(held()));
        box.addListener(this::save);
        addSlot(new Slot(box, 0, SLOT_X, SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return accepts(stack);
            }
        });
        int locked = hand == InteractionHand.MAIN_HAND ? inventory.selected : -1;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(col == locked ? new Slot(inventory, col, 8 + col * 18, INV_Y + 58) {
                @Override
                public boolean mayPickup(Player p) {
                    return false;
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            } : new Slot(inventory, col, 8 + col * 18, INV_Y + 58));
        }
    }

    public static boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem().canFitInsideContainerItems() && !Mail.isMail(stack);
    }

    public InteractionHand hand() {
        return hand;
    }

    public ItemStack held() {
        return player.getItemInHand(hand);
    }

    public ItemStack content() {
        return box.getItem(0);
    }

    private void save(Container container) {
        if (!player.level().isClientSide && held().getItem() instanceof PackageItem && !Mail.isSealed(held())) {
            PackageItem.setContent(held(), box.getItem(0).copy());
        }
    }

    @Override
    public boolean stillValid(Player p) {
        return held().getItem() instanceof PackageItem && !Mail.isSealed(held());
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!accepts(stack) || !moveItemStackTo(stack, 0, 1, false)) {
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
    public void removed(Player p) {
        super.removed(p);
        save(box);
    }
}
