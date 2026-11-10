package com.gabriel.hogwartsmod.creature;

import com.gabriel.hogwartsmod.mail.LetterItem;
import com.gabriel.hogwartsmod.mail.OwlPost;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class OwlEntity extends TamableAnimal implements GeoEntity, FlyingAnimal {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.coruja.parado");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.coruja.andar");
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.coruja.voar");
    private static final RawAnimation SIT = RawAnimation.begin().thenLoop("animation.coruja.sentado");
    private static final int DEPART_TICKS = 40;
    private static final int APPROACH_TIMEOUT = 400;

    public enum Phase {
        NONE, DEPART, APPROACH
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private Phase phase = Phase.NONE;
    private int phaseTicks;
    private ItemStack carried = ItemStack.EMPTY;
    @Nullable
    private UUID recipient;
    private String recipientName = "";
    private String senderName = "";
    private boolean returning;

    public OwlEntity(EntityType<? extends OwlEntity> type, Level level) {
        super(type, level);
        moveControl = new FlyingMoveControl(this, 10, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.0, 6.0f, 2.0f, true));
        goalSelector.addGoal(3, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    @Override
    public boolean isFlying() {
        return !onGround() && level().getBlockState(blockPosition().below()).isAir() && !isPassenger();
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
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

    public static boolean isTreat(ItemStack stack) {
        return stack.is(Items.RABBIT) || stack.is(Items.CHICKEN);
    }

    public boolean isBusy() {
        return phase != Phase.NONE;
    }

    // -- Interação

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof CreatureBagItem) {
            return InteractionResult.PASS;
        }
        if (held.getItem() instanceof LetterItem) {
            return interactLetter(player, hand, held);
        }
        if (isBusy()) {
            return InteractionResult.PASS;
        }
        if (!isTame() && isTreat(held)) {
            if (!level().isClientSide) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                if (random.nextInt(3) == 0) {
                    tame(player);
                    navigation.stop();
                    setOrderedToSit(true);
                    level().broadcastEntityEvent(this, (byte) 7);
                    player.displayClientMessage(Component.translatable("message.hogwartsmod.owl_tamed").withStyle(ChatFormatting.GOLD), true);
                } else {
                    level().broadcastEntityEvent(this, (byte) 6);
                }
                playSound(SoundEvents.PARROT_EAT, 1f, 0.8f);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (isTame() && isOwnedBy(player)) {
            if (isTreat(held) && getHealth() < getMaxHealth()) {
                if (!level().isClientSide) {
                    if (!player.getAbilities().instabuild) {
                        held.shrink(1);
                    }
                    heal(4f);
                    playSound(SoundEvents.PARROT_EAT, 1f, 0.8f);
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            if (held.isEmpty() && player.isShiftKeyDown()) {
                if (!level().isClientSide) {
                    setOrderedToSit(!isOrderedToSit());
                    navigation.stop();
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
        }
        return super.mobInteract(player, hand);
    }

    private InteractionResult interactLetter(Player player, InteractionHand hand, ItemStack letter) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        Component problem = null;
        if (!isTame() || !isOwnedBy(player)) {
            problem = Component.translatable("message.hogwartsmod.creature_not_yours");
        } else if (isBusy()) {
            problem = Component.translatable("message.hogwartsmod.owl_busy");
        } else if (!LetterItem.isSealed(letter)) {
            problem = Component.translatable("message.hogwartsmod.owl_unsigned");
        }
        if (problem != null) {
            if (!level().isClientSide) {
                player.displayClientMessage(problem.copy().withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (level().isClientSide) {
            int id = getId();
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.gabriel.hogwartsmod.client.gui.OwlPostScreen.open(id, hand));
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    // -- Entrega

    public void startDelivery(ItemStack letter, ServerPlayer sender, ServerPlayer target) {
        carried = letter;
        recipient = target.getUUID();
        recipientName = target.getGameProfile().getName();
        senderName = sender.getGameProfile().getName();
        returning = false;
        setOrderedToSit(false);
        startDepart();
    }

    private void startDepart() {
        phase = Phase.DEPART;
        phaseTicks = 0;
        navigation.stop();
        setNoGravity(true);
        playSound(SoundEvents.PARROT_FLY, 1f, 0.8f);
    }

    public void startApproach() {
        phase = Phase.APPROACH;
        phaseTicks = 0;
        setNoGravity(true);
    }

    @Nullable
    public UUID nextTarget() {
        return returning ? getOwnerUUID() : recipient;
    }

    public boolean isReturning() {
        return returning;
    }

    public void turnBack() {
        returning = true;
    }

    public ItemStack getCarried() {
        return carried;
    }

    public String getRecipientName() {
        return recipientName;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || phase == Phase.NONE) {
            return;
        }
        phaseTicks++;
        if (phase == Phase.DEPART) {
            Vec3 look = Vec3.directionFromRotation(0, getYRot());
            setDeltaMovement(look.x * 0.12, 0.32, look.z * 0.12);
            if (phaseTicks >= DEPART_TICKS) {
                OwlPost.depart(this);
            }
            return;
        }
        UUID id = nextTarget();
        ServerPlayer target = id == null ? null : ((ServerLevel) level()).getServer().getPlayerList().getPlayer(id);
        if (target == null || target.level() != level() || !target.isAlive()) {
            if (!returning) {
                returning = true;
            }
            startDepart();
            return;
        }
        Vec3 goal = target.position().add(0, target.getBbHeight() * 0.75, 0);
        Vec3 diff = goal.subtract(position());
        double dist = diff.length();
        if (phaseTicks > APPROACH_TIMEOUT && dist > 3) {
            moveTo(goal.x + 1, goal.y, goal.z + 1, getYRot(), 0);
            dist = 1;
        }
        if (dist < 1.8) {
            arrive(target);
            return;
        }
        Vec3 dir = diff.normalize().scale(Math.min(0.55, 0.15 + dist * 0.05));
        setDeltaMovement(dir);
        float yaw = (float) (Math.atan2(diff.z, diff.x) * 180 / Math.PI) - 90f;
        setYRot(yaw);
        setYBodyRot(yaw);
        setYHeadRot(yaw);
    }

    private void arrive(ServerPlayer target) {
        ServerLevel level = (ServerLevel) level();
        level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.3, getZ(), 6, 0.2, 0.2, 0.2, 0.02);
        if (!returning) {
            ItemStack letter = carried;
            carried = ItemStack.EMPTY;
            if (!target.getInventory().add(letter)) {
                target.drop(letter, false);
            }
            playSound(SoundEvents.ITEM_PICKUP, 0.8f, 0.9f);
            target.sendSystemMessage(Component.translatable("message.hogwartsmod.owl_delivered", getDisplayName(), senderName).withStyle(ChatFormatting.GOLD));
            if (getOwner() instanceof ServerPlayer owner) {
                owner.displayClientMessage(Component.translatable("message.hogwartsmod.owl_delivered_sender", recipientName).withStyle(ChatFormatting.GOLD), true);
            }
            returning = true;
            startDepart();
            return;
        }
        if (!carried.isEmpty()) {
            ItemStack letter = carried;
            carried = ItemStack.EMPTY;
            if (!target.getInventory().add(letter)) {
                target.drop(letter, false);
            }
            target.sendSystemMessage(Component.translatable("message.hogwartsmod.owl_undelivered", recipientName).withStyle(ChatFormatting.GRAY));
        } else {
            target.displayClientMessage(Component.translatable("message.hogwartsmod.owl_back", getDisplayName()).withStyle(ChatFormatting.GOLD), true);
        }
        phase = Phase.NONE;
        returning = false;
        recipient = null;
        setNoGravity(false);
        setDeltaMovement(Vec3.ZERO);
        playSound(SoundEvents.PARROT_AMBIENT, 1f, 0.7f);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (!carried.isEmpty()) {
            spawnAtLocation(carried);
            carried = ItemStack.EMPTY;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Fase", phase.name());
        tag.putInt("FaseTicks", phaseTicks);
        if (!carried.isEmpty()) {
            tag.put("Carta", carried.save(new CompoundTag()));
        }
        if (recipient != null) {
            tag.putUUID("Destinatario", recipient);
        }
        tag.putString("NomeDestinatario", recipientName);
        tag.putString("Remetente", senderName);
        tag.putBoolean("Voltando", returning);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        try {
            phase = Phase.valueOf(tag.getString("Fase"));
        } catch (IllegalArgumentException e) {
            phase = Phase.NONE;
        }
        phaseTicks = tag.getInt("FaseTicks");
        carried = tag.contains("Carta") ? ItemStack.of(tag.getCompound("Carta")) : ItemStack.EMPTY;
        recipient = tag.hasUUID("Destinatario") ? tag.getUUID("Destinatario") : null;
        recipientName = tag.getString("NomeDestinatario");
        senderName = tag.getString("Remetente");
        returning = tag.getBoolean("Voltando");
        setNoGravity(phase != Phase.NONE);
    }

    // -- Sons

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return isOrderedToSit() || random.nextInt(3) == 0 ? null : SoundEvents.PARROT_AMBIENT;
    }

    @Override
    public float getVoicePitch() {
        return 0.55f;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 300;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PARROT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PARROT_DEATH;
    }

    // -- Animações

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "corpo", 4, state -> {
            if (isFlying()) {
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
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
