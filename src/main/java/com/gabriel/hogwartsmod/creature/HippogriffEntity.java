package com.gabriel.hogwartsmod.creature;

import com.gabriel.hogwartsmod.entity.FlyingMount;
import com.gabriel.hogwartsmod.magic.MagicConfig;
import com.gabriel.hogwartsmod.zone.ZoneRule;
import com.gabriel.hogwartsmod.zone.Zones;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PlayerRideable;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class HippogriffEntity extends TamableAnimal implements GeoEntity, FlyingMount, PlayerRideable {
    private static final EntityDataAccessor<Boolean> BOOST = SynchedEntityData.defineId(HippogriffEntity.class, EntityDataSerializers.BOOLEAN);
    private static final Set<Item> MEAT = Set.of(Items.CHICKEN, Items.BEEF, Items.PORKCHOP, Items.MUTTON, Items.RABBIT, Items.COD, Items.SALMON);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.hipogrifo.parado");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.hipogrifo.andar");
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.hipogrifo.voar");
    private static final RawAnimation SIT = RawAnimation.begin().thenLoop("animation.hipogrifo.sentado");
    private static final RawAnimation BOW = RawAnimation.begin().thenPlay("animation.hipogrifo.reverencia");
    private static final RawAnimation PECK = RawAnimation.begin().thenPlay("animation.hipogrifo.bicar");
    public static final float MAX_STAMINA = 120f;
    private static final int BOW_TICKS = 40;

    public static Consumer<HippogriffEntity> clientPilot = h -> {
    };

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Map<UUID, Integer> bowing = new HashMap<>();
    @Nullable
    private UUID accepted;
    private int acceptedTimer;

    private boolean flying;
    private boolean boosting;
    private float speed;
    private float stamina = MAX_STAMINA;
    private float roll;
    private float rollOld;
    private float inForward;
    private float inVertical;
    private boolean inBoost;
    private float inYaw;
    private float inPitch;

    public HippogriffEntity(EntityType<? extends HippogriffEntity> type, Level level) {
        super(type, level);
        setMaxUpStep(1.1f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(BOOST, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.3, true));
        goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.1, 10f, 3f, false));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    public static boolean isMeat(ItemStack stack) {
        return MEAT.contains(stack.getItem());
    }

    // -- Domesticação

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            return;
        }
        if (acceptedTimer > 0 && --acceptedTimer == 0) {
            accepted = null;
        }
        if (!isTame() && getTarget() == null && tickCount % 2 == 0) {
            for (Player player : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(6))) {
                if (player.isCrouching() && isLookingAtMe(player)) {
                    int ticks = bowing.merge(player.getUUID(), 2, Integer::sum);
                    getLookControl().setLookAt(player);
                    getNavigation().stop();
                    if (ticks >= BOW_TICKS && !player.getUUID().equals(accepted)) {
                        bowBack(player);
                    }
                } else {
                    bowing.remove(player.getUUID());
                }
            }
        }
        if (!isVehicle() && !onGround() && getDeltaMovement().y < -0.1) {
            setDeltaMovement(getDeltaMovement().multiply(1, 0.6, 1));
            fallDistance = 0;
        }
    }

    private boolean isLookingAtMe(Player player) {
        Vec3 look = player.getViewVector(1f).normalize();
        Vec3 to = getEyePosition().subtract(player.getEyePosition()).normalize();
        return look.dot(to) > 0.8;
    }

    private void bowBack(Player player) {
        accepted = player.getUUID();
        acceptedTimer = 600;
        triggerAnim("acao", "reverencia");
        playSound(SoundEvents.HORSE_BREATHE, 1f, 0.8f);
        ((ServerLevel) level()).sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 1.6, getZ(), 8, 0.4, 0.3, 0.4, 0.0);
        player.displayClientMessage(Component.translatable("message.hogwartsmod.hippogriff_bowed").withStyle(ChatFormatting.GOLD), true);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof CreatureBagItem) {
            return InteractionResult.PASS;
        }
        if (!isTame()) {
            if (!isMeat(held)) {
                return InteractionResult.PASS;
            }
            if (level().isClientSide) {
                return InteractionResult.SUCCESS;
            }
            if (player.getUUID().equals(accepted)) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                tame(player);
                setOrderedToSit(false);
                navigation.stop();
                setTarget(null);
                level().broadcastEntityEvent(this, (byte) 7);
                playSound(SoundEvents.HORSE_EAT, 1f, 1f);
                player.displayClientMessage(Component.translatable("message.hogwartsmod.hippogriff_tamed").withStyle(ChatFormatting.GOLD), true);
            } else {
                setTarget(player);
                triggerAnim("acao", "bicar");
                playSound(SoundEvents.HORSE_ANGRY, 1f, 1.2f);
                player.displayClientMessage(Component.translatable("message.hogwartsmod.hippogriff_offended").withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.CONSUME;
        }
        if (isMeat(held) && getHealth() < getMaxHealth()) {
            if (!level().isClientSide) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                heal(6f);
                playSound(SoundEvents.HORSE_EAT, 1f, 1f);
                level().broadcastEntityEvent(this, (byte) 7);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (isOwnedBy(player) && player.isSecondaryUseActive()) {
            if (!level().isClientSide) {
                setOrderedToSit(!isOrderedToSit());
                navigation.stop();
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        boolean pilot = getPassengers().isEmpty() && isOwnedBy(player);
        boolean passenger = getPassengers().size() == 1 && getFirstPassenger() instanceof Player first && isOwnedBy(first);
        if (pilot || passenger) {
            if (!level().isClientSide) {
                if (pilot && Zones.blocks(this, ZoneRule.VOO)) {
                    Zones.warn(player, ZoneRule.VOO);
                    return InteractionResult.CONSUME;
                }
                setOrderedToSit(false);
                player.startRiding(this);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (!isOwnedBy(player) && !level().isClientSide) {
            player.displayClientMessage(Component.translatable("message.hogwartsmod.hippogriff_not_yours"), true);
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        triggerAnim("acao", "bicar");
        return super.doHurtTarget(target);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() != null && source.getEntity() == getControllingPassenger()) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    // -- Montaria

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player player && isOwnedBy(player) ? player : null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < 2;
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction move) {
        int index = getPassengers().indexOf(passenger);
        if (index < 0) {
            return;
        }
        double back = index == 0 ? 0.05 : -0.6;
        Vec3 offset = new Vec3(0, 0, back).yRot(-yBodyRot * Mth.DEG_TO_RAD);
        double lower = isInSittingPose() ? -0.35 : 0;
        move.accept(passenger, getX() + offset.x, getY() + 1.3 + lower + passenger.getMyRidingOffset(), getZ() + offset.z);
        if (passenger instanceof LivingEntity living) {
            living.setYBodyRot(yBodyRot);
        }
    }

    @Override
    public double getPassengersRidingOffset() {
        return 1.25;
    }

    @Override
    public void tick() {
        if (level().isClientSide && isControlledByLocalInstance() && getControllingPassenger() != null) {
            clientPilot.accept(this);
        }
        if (getControllingPassenger() == null) {
            flying = false;
            boosting = false;
            speed = 0;
            stamina = Math.min(MAX_STAMINA, stamina + 0.35f);
            rollOld = roll;
            roll *= 0.8f;
        }
        super.tick();
        if (!level().isClientSide && getControllingPassenger() == null && entityData.get(BOOST)) {
            entityData.set(BOOST, false);
        }
    }

    public void setInput(float forward, float vertical, boolean boost, float yaw, float pitch) {
        inForward = forward;
        inVertical = vertical;
        inBoost = boost;
        inYaw = yaw;
        inPitch = pitch;
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 input) {
        float forward = player.zza;
        if (forward <= 0) {
            forward *= 0.25f;
        }
        return new Vec3(player.xxa * 0.5f, 0, forward);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        return (float) getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.25f;
    }

    @Override
    protected void tickRidden(Player player, Vec3 input) {
        super.tickRidden(player, input);
        if (!flying) {
            setRot(player.getYRot(), 0);
            yRotO = yBodyRot = yHeadRot = getYRot();
            if (inVertical > 0 && onGround() && !Zones.blocks(this, ZoneRule.VOO)) {
                flying = true;
                speed = 0.2f;
                setDeltaMovement(getDeltaMovement().add(0, 0.6, 0));
            }
        }
    }

    @Override
    public void travel(Vec3 input) {
        if (flying && isVehicle() && getControllingPassenger() instanceof Player && isControlledByLocalInstance()) {
            flyStep();
            calculateEntityAnimation(false);
            return;
        }
        super.travel(input);
    }

    private void flyStep() {
        boosting = inBoost && inForward > 0 && stamina > 1f;
        stamina = boosting ? stamina - 1f : Math.min(MAX_STAMINA, stamina + 0.35f);
        float max = 0.8f * (boosting ? 1.6f : 1f);
        float accel = 0.035f;
        if (inForward > 0) {
            speed = Math.min(max, speed + accel * (boosting ? 1.6f : 1f));
        } else if (inForward < 0) {
            speed = Math.max(0, speed - accel * 2.5f);
        } else {
            speed = Math.max(0, speed - accel * 0.3f);
        }
        if (speed > max) {
            speed = Math.max(max, speed - accel);
        }
        float yawDelta = Mth.wrapDegrees(inYaw - getYRot());
        float turn = Mth.clamp(yawDelta, -7f, 7f);
        setYRot(getYRot() + turn);
        yRotO = getYRot() - turn;
        yBodyRot = yHeadRot = getYRot();
        rollOld = roll;
        roll = Mth.lerp(0.2f, roll, Mth.clamp(-turn * 3.5f, -25f, 25f));
        float vertical = inVertical;
        if (Zones.blocks(this, ZoneRule.VOO)) {
            vertical = -1;
        }
        Vec3 target;
        if (vertical != 0) {
            Vec3 flat = Vec3.directionFromRotation(0, getYRot());
            target = flat.scale(speed).add(0, vertical * Math.max(0.3, speed * 0.6), 0);
        } else {
            target = Vec3.directionFromRotation(Mth.clamp(inPitch, -60f, 60f), getYRot()).scale(speed);
        }
        if (speed < 0.05f && vertical == 0) {
            target = new Vec3(0, Math.sin(tickCount * 0.25) * 0.03, 0);
        }
        Vec3 motion = getDeltaMovement().lerp(target, 0.2);
        if (getY() + motion.y > MagicConfig.BROOM_MAX_HEIGHT.get()) {
            motion = new Vec3(motion.x, Math.min(0, motion.y), motion.z);
        }
        setDeltaMovement(motion);
        move(MoverType.SELF, motion);
        if (horizontalCollision) {
            speed *= 0.5f;
        }
        if (onGround() && vertical <= 0) {
            flying = false;
            boosting = false;
            speed = 0;
        }
    }

    public boolean isFlyingVisual() {
        return !onGround() && !isInWater() && (isVehicle() || getDeltaMovement().y < -0.05);
    }

    // -- FlyingMount

    @Override
    public float getStamina() {
        return stamina;
    }

    @Override
    public float getMaxStamina() {
        return MAX_STAMINA;
    }

    @Override
    public boolean isBoosting() {
        return boosting;
    }

    @Override
    public boolean isBoostPose() {
        return entityData.get(BOOST);
    }

    @Override
    public void setBoostSynced(boolean value) {
        entityData.set(BOOST, value);
    }

    @Override
    public float getRoll(float partialTick) {
        return Mth.lerp(partialTick, rollOld, roll);
    }

    @Override
    public boolean showsTurnKeys() {
        return false;
    }

    @Override
    public String upKey() {
        return "hud.hogwartsmod.hippogriff_up";
    }

    // -- Sons

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PARROT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.HORSE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.HORSE_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.6f;
    }

    // -- Animações

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "corpo", 5, state -> {
            if (isFlyingVisual()) {
                return state.setAndContinue(FLY);
            }
            if (isInSittingPose()) {
                return state.setAndContinue(SIT);
            }
            if (state.isMoving()) {
                return state.setAndContinue(WALK);
            }
            return state.setAndContinue(IDLE);
        }));
        controllers.add(new AnimationController<>(this, "acao", 3, state -> PlayState.STOP)
                .triggerableAnim("reverencia", BOW)
                .triggerableAnim("bicar", PECK));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // -- Salvamento

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Folego", stamina);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Folego")) {
            stamina = tag.getFloat("Folego");
        }
    }
}
