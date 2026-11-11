package com.gabriel.hogwartsmod.item;

import com.gabriel.hogwartsmod.broom.BroomType;
import com.gabriel.hogwartsmod.broom.Brooms;
import com.gabriel.hogwartsmod.entity.BroomEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BroomItem extends Item {
    private final BroomType type;

    public BroomItem(BroomType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public BroomType getType() {
        return type;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        Vec3 pos = context.getClickLocation().add(0, 0.6, 0);
        if (!level.noCollision(new net.minecraft.world.phys.AABB(pos.x - 0.6, pos.y, pos.z - 0.6, pos.x + 0.6, pos.y + 0.5, pos.z + 0.6))) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            BroomEntity broom = BroomEntity.create(level, type, pos, player.getYRot(), player.getUUID());
            broom.loadFromItem(context.getItemInHand());
            level.addFreshEntity(broom);
            if (!player.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Brooms.appendTooltip(type, stack, tooltip);
    }
}
