package com.gabriel.hogwartsmod.house.door;

import com.gabriel.hogwartsmod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class HouseDoorBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final RawAnimation CLOSED = RawAnimation.begin().thenLoop("fechada");
    private static final RawAnimation OPENED = RawAnimation.begin().thenLoop("aberta");
    private static final RawAnimation OPEN = RawAnimation.begin().thenPlayAndHold("abrir");
    private static final RawAnimation CLOSE = RawAnimation.begin().thenPlayAndHold("fechar");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private Boolean lastOpen;

    public HouseDoorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HOUSE_DOOR.get(), pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "porta", 0, state -> {
            boolean open = getBlockState().getValue(HouseDoorBlock.OPEN);
            if (lastOpen == null) {
                state.getController().setAnimation(open ? OPENED : CLOSED);
            } else if (open != lastOpen) {
                state.getController().forceAnimationReset();
                state.getController().setAnimation(open ? OPEN : CLOSE);
            }
            lastOpen = open;
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(2.5).expandTowards(0, 2, 0);
    }
}
