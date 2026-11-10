package com.gabriel.hogwartsmod.creature;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class HippogriffEntity extends MountCreature {
    private static final Set<Item> MEAT = Set.of(Items.CHICKEN, Items.BEEF, Items.PORKCHOP, Items.MUTTON, Items.RABBIT, Items.COD, Items.SALMON);

    public HippogriffEntity(EntityType<? extends HippogriffEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 50.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected boolean isTamingFood(ItemStack stack) {
        return MEAT.contains(stack.getItem());
    }

    @Override
    protected String animationPrefix() {
        return "hipogrifo";
    }

    @Override
    protected String messagePrefix() {
        return "hippogriff";
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
}
