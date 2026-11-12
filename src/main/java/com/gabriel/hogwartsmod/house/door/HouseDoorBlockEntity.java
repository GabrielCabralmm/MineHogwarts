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
    private static final RawAnimation AMBIENT = RawAnimation.begin().thenLoop("ambiente");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private Boolean lastOpen;
    private boolean wasOpen;
    private int openTicks = -1;
    private Boolean seenOpen;
    private long changeTick = Long.MIN_VALUE;

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
        controllers.add(new AnimationController<>(this, "ambiente", 0, state -> state.setAndContinue(AMBIENT)));
    }

    public void clientTick() {
        boolean open = getBlockState().getValue(HouseDoorBlock.OPEN);
        if (seenOpen != null && seenOpen != open && level != null) {
            changeTick = level.getGameTime();
        }
        seenOpen = open;
        if (open && !wasOpen) {
            openTicks = 0;
        }
        wasOpen = open;
        if (open && openTicks >= 0) {
            HouseDoorEffects.opening(this, openTicks++);
        }
    }

    public float secondsSinceChange(float partialTick) {
        if (changeTick == Long.MIN_VALUE || level == null) {
            return Float.MAX_VALUE;
        }
        return (level.getGameTime() - changeTick + partialTick) / 20f;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(6.5).expandTowards(0, 3, 0);
    }
}
