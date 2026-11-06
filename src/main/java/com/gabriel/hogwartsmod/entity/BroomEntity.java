package com.gabriel.hogwartsmod.entity;

import com.gabriel.hogwartsmod.broom.BroomType;
import com.gabriel.hogwartsmod.magic.MagicConfig;
import com.gabriel.hogwartsmod.registry.ModEntities;
import com.gabriel.hogwartsmod.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class BroomEntity extends Entity {
    private static final EntityDataAccessor<Integer> TYPE = SynchedEntityData.defineId(BroomEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> BOOST = SynchedEntityData.defineId(BroomEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(BroomEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    public static java.util.function.Consumer<BroomEntity> clientPilot = broom -> {
    };

    private float speed;
    private float stamina = -1;
    private boolean boosting;
    private float roll;
    private float rollOld;
    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private double lerpYRot;

    public BroomEntity(EntityType<? extends BroomEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        blocksBuilding = true;
    }

    public static BroomEntity create(Level level, BroomType type, Vec3 pos, float yaw, UUID owner) {
        BroomEntity broom = new BroomEntity(ModEntities.BROOM.get(), level);
        broom.entityData.set(TYPE, type.ordinal());
        broom.entityData.set(OWNER, Optional.of(owner));
        broom.moveTo(pos.x, pos.y, pos.z, yaw, 0);
        return broom;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(TYPE, 0);
        entityData.define(OWNER, Optional.empty());
        entityData.define(BOOST, false);
    }

    public BroomType getBroomType() {
        return BroomType.byOrdinal(entityData.get(TYPE));
    }

    @Nullable
    public UUID getOwner() {
        return entityData.get(OWNER).orElse(null);
    }

    public boolean isOwner(Player player) {
        return player.getUUID().equals(getOwner());
    }

    private boolean canPilot(Player player) {
        return isOwner(player) || MagicConfig.BROOM_THEFT.get();
    }

    // -- Montar e guardar

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || player.isSecondaryUseActive() && !isOwner(player)) {
            return InteractionResult.PASS;
        }
        if (player.isSecondaryUseActive()) {
            if (!level().isClientSide && getPassengers().isEmpty()) {
                ItemStack stack = new ItemStack(getBroomType() == BroomType.SPORT ? ModItems.BROOM_SPORT.get() : ModItems.BROOM_TRANSPORT.get());
                if (!player.addItem(stack)) {
                    player.drop(stack, false);
                }
                discard();
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (getPassengers().isEmpty()) {
            if (!canPilot(player)) {
                player.displayClientMessage(Component.translatable("message.hogwartsmod.broom_not_yours"), true);
                return InteractionResult.CONSUME;
            }
        } else if (getPassengers().size() >= getBroomType().getSeats() || !(getFirstPassenger() instanceof Player pilot) || !isOwner(pilot)) {
            player.displayClientMessage(Component.translatable("message.hogwartsmod.broom_full"), true);
            return InteractionResult.CONSUME;
        }
        player.startRiding(this);
        return InteractionResult.CONSUME;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < getBroomType().getSeats();
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player player && canPilot(player) ? player : null;
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction move) {
        int index = getPassengers().indexOf(passenger);
        if (index < 0) {
            return;
        }
        double back = index == 0 ? 0.0 : -0.6;
        Vec3 offset = new Vec3(0, 0, back).yRot(-getYRot() * Mth.DEG_TO_RAD);
        move.accept(passenger, getX() + offset.x, getY() + 0.1 + passenger.getMyRidingOffset(), getZ() + offset.z);
        alignBody(passenger);
    }

    private void alignBody(Entity passenger) {
        if (passenger instanceof LivingEntity living) {
            living.yBodyRotO = yRotO;
            living.setYBodyRot(getYRot());
            float head = Mth.wrapDegrees(passenger.getYRot() - getYRot());
            float limited = Mth.clamp(head, -100f, 100f);
            if (head != limited) {
                passenger.yRotO += limited - head;
                passenger.setYRot(passenger.getYRot() + limited - head);
            }
            passenger.setYHeadRot(passenger.getYRot());
        }
    }

    @Override
    public void onPassengerTurned(Entity passenger) {
        alignBody(passenger);
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.1;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(getBroomType() == BroomType.SPORT ? ModItems.BROOM_SPORT.get() : ModItems.BROOM_TRANSPORT.get());
    }

    // -- Voo

    public float getSpeed() {
        return speed;
    }

    public float getStamina() {
        return stamina < 0 ? getBroomType().getStamina() : stamina;
    }

    public boolean isBoosting() {
        return boosting;
    }

    public boolean isBoostPose() {
        return isControlledByLocalInstance() ? boosting : entityData.get(BOOST);
    }

    public void setBoostSynced(boolean value) {
        entityData.set(BOOST, value);
    }

    public float getRoll(float partialTick) {
        return Mth.lerp(partialTick, rollOld, roll);
    }

    public void fly(float forward, float vertical, boolean boost, float targetYaw, float targetPitch) {
        BroomType type = getBroomType();
        float stamina = getStamina();
        boosting = boost && forward > 0 && stamina > 1f;
        stamina = boosting ? stamina - 1f : Math.min(type.getStamina(), stamina + 0.35f);
        this.stamina = stamina;
        float max = type.getMaxSpeed() * (boosting ? type.getBoost() : 1f);
        if (forward > 0) {
            speed = Math.min(max, speed + type.getAcceleration() * (boosting ? 1.6f : 1f));
        } else if (forward < 0) {
            speed = Math.max(0, speed - type.getAcceleration() * 2.5f);
        } else {
            speed = Math.max(0, speed - type.getAcceleration() * 0.35f);
        }
        if (speed > max) {
            speed = Math.max(max, speed - type.getAcceleration());
        }
        float yawDelta = Mth.wrapDegrees(targetYaw - getYRot());
        float turn = Mth.clamp(yawDelta, -type.getHandling(), type.getHandling());
        setYRot(getYRot() + turn);
        rollOld = roll;
        roll = Mth.lerp(0.2f, roll, Mth.clamp(-turn * 4.5f * Math.min(1f, speed / type.getMaxSpeed() + 0.2f), -30f, 30f));
        Vec3 look = Vec3.directionFromRotation(Mth.clamp(targetPitch, -70f, 70f), getYRot());
        Vec3 target = look.scale(speed).add(0, vertical * 0.35, 0);
        if (speed < 0.05f && vertical == 0) {
            target = new Vec3(0, Math.sin(tickCount * 0.12) * 0.012, 0);
        }
        Vec3 motion = getDeltaMovement().lerp(target, 0.22);
        if (getY() + motion.y > MagicConfig.BROOM_MAX_HEIGHT.get()) {
            motion = new Vec3(motion.x, Math.min(0, motion.y), motion.z);
        }
        setDeltaMovement(motion);
        move(MoverType.SELF, motion);
        if (horizontalCollision) {
            speed *= 0.4f;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && getControllingPassenger() == null && entityData.get(BOOST)) {
            entityData.set(BOOST, false);
        }
        if (isControlledByLocalInstance()) {
            lerpSteps = 0;
            syncPacketPositionCodec(getX(), getY(), getZ());
            if (level().isClientSide && getControllingPassenger() != null) {
                clientPilot.accept(this);
            } else if (getControllingPassenger() == null) {
                speed = 0;
                rollOld = roll;
                roll *= 0.8f;
                stamina = Math.min(getBroomType().getStamina(), getStamina() + 0.35f);
                Vec3 motion = getDeltaMovement().scale(0.8).add(0, Math.sin(tickCount * 0.12) * 0.004, 0);
                setDeltaMovement(motion);
                move(MoverType.SELF, motion);
            }
        } else {
            tickLerp();
        }
    }

    private void tickLerp() {
        if (lerpSteps > 0) {
            double x = getX() + (lerpX - getX()) / lerpSteps;
            double y = getY() + (lerpY - getY()) / lerpSteps;
            double z = getZ() + (lerpZ - getZ()) / lerpSteps;
            setYRot(getYRot() + (float) Mth.wrapDegrees(lerpYRot - getYRot()) / lerpSteps);
            lerpSteps--;
            setPos(x, y, z);
        }
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpYRot = yRot;
        lerpSteps = 10;
    }

    // -- Salvamento

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(TYPE, tag.getInt("Type"));
        entityData.set(OWNER, tag.hasUUID("Owner") ? Optional.of(tag.getUUID("Owner")) : Optional.empty());
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Type", entityData.get(TYPE));
        UUID owner = getOwner();
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
    }
}
