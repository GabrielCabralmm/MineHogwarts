package com.gabriel.hogwartsmod.entity;

import com.gabriel.hogwartsmod.broom.BroomType;
import com.gabriel.hogwartsmod.broom.Brooms;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class CargoBroomEntity extends BroomEntity implements HasCustomInventoryScreen {
    private final SimpleContainer cargo = new SimpleContainer(BroomType.CARGO_SLOTS) {
        @Override
        public boolean stillValid(Player player) {
            return isAlive() && player.distanceToSqr(CargoBroomEntity.this) < 64;
        }
    };

    public CargoBroomEntity(EntityType<? extends BroomEntity> type, Level level) {
        super(type, level);
    }

    public SimpleContainer getCargo() {
        return cargo;
    }

    @Override
    public void openCustomInventoryScreen(Player player) {
        if (level().isClientSide || !isOwner(player)) {
            return;
        }
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x2, id, inv, cargo, 2),
                getBroomType().getDisplayName()));
    }

    @Override
    public void loadFromItem(ItemStack stack) {
        super.loadFromItem(stack);
        Brooms.loadCargo(stack, cargo);
    }

    @Override
    public ItemStack toItem() {
        ItemStack stack = super.toItem();
        Brooms.saveCargo(stack, cargo);
        return stack;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        cargo.fromTag(tag.getList("Carga", 10));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ListTag list = cargo.createTag();
        tag.put("Carga", list);
    }
}
