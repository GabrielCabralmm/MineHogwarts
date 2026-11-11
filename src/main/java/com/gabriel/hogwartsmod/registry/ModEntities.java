package com.gabriel.hogwartsmod.registry;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.entity.BroomEntity;
import com.gabriel.hogwartsmod.entity.ProfessorNpcEntity;
import com.gabriel.hogwartsmod.entity.SpellProjectileEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, HogwartsMod.MOD_ID);

    public static final RegistryObject<EntityType<SpellProjectileEntity>> SPELL_PROJECTILE = ENTITIES.register("spell_projectile",
            () -> EntityType.Builder.<SpellProjectileEntity>of(SpellProjectileEntity::new, MobCategory.MISC)
                    .sized(0.3f, 0.3f)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("spell_projectile"));

    public static final RegistryObject<EntityType<com.gabriel.hogwartsmod.creature.HippogriffEntity>> HIPPOGRIFF = ENTITIES.register("hipogrifo",
            () -> EntityType.Builder.<com.gabriel.hogwartsmod.creature.HippogriffEntity>of(com.gabriel.hogwartsmod.creature.HippogriffEntity::new, MobCategory.CREATURE)
                    .sized(2.0f, 2.6f)
                    .clientTrackingRange(10)
                    .build("hipogrifo"));

    public static final RegistryObject<EntityType<com.gabriel.hogwartsmod.creature.OwlEntity>> OWL = ENTITIES.register("coruja",
            () -> EntityType.Builder.<com.gabriel.hogwartsmod.creature.OwlEntity>of(com.gabriel.hogwartsmod.creature.OwlEntity::new, MobCategory.CREATURE)
                    .sized(0.5f, 0.8f)
                    .clientTrackingRange(10)
                    .build("coruja"));

    public static final RegistryObject<EntityType<com.gabriel.hogwartsmod.creature.ThestralEntity>> THESTRAL = ENTITIES.register("testralio",
            () -> EntityType.Builder.<com.gabriel.hogwartsmod.creature.ThestralEntity>of(com.gabriel.hogwartsmod.creature.ThestralEntity::new, MobCategory.CREATURE)
                    .sized(1.7f, 2.6f)
                    .clientTrackingRange(10)
                    .build("testralio"));

    public static final RegistryObject<EntityType<com.gabriel.hogwartsmod.mission.MissionNpcEntity>> MISSION_NPC = ENTITIES.register("npc_missoes",
            () -> EntityType.Builder.<com.gabriel.hogwartsmod.mission.MissionNpcEntity>of(com.gabriel.hogwartsmod.mission.MissionNpcEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.8f)
                    .clientTrackingRange(10)
                    .build("npc_missoes"));

    public static final RegistryObject<EntityType<com.gabriel.hogwartsmod.shop.ShopNpcEntity>> SHOP_NPC = ENTITIES.register("npc_loja",
            () -> EntityType.Builder.<com.gabriel.hogwartsmod.shop.ShopNpcEntity>of(com.gabriel.hogwartsmod.shop.ShopNpcEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.8f)
                    .clientTrackingRange(10)
                    .build("npc_loja"));

    public static final RegistryObject<EntityType<ProfessorNpcEntity>> PROFESSOR_NPC = ENTITIES.register("professor_npc",
            () -> EntityType.Builder.<ProfessorNpcEntity>of(ProfessorNpcEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.8f)
                    .clientTrackingRange(10)
                    .build("professor_npc"));

    public static final RegistryObject<EntityType<BroomEntity>> BROOM = ENTITIES.register("vassoura",
            () -> EntityType.Builder.<BroomEntity>of(BroomEntity::new, MobCategory.MISC)
                    .sized(1.0f, 0.4f)
                    .clientTrackingRange(10)
                    .build("vassoura"));

    public static final RegistryObject<EntityType<BroomEntity>> BROOM_CARGO = ENTITIES.register("vassoura_carga",
            () -> EntityType.Builder.<BroomEntity>of(com.gabriel.hogwartsmod.entity.CargoBroomEntity::new, MobCategory.MISC)
                    .sized(1.0f, 0.4f)
                    .clientTrackingRange(10)
                    .build("vassoura_carga"));

    private ModEntities() {
    }
}
