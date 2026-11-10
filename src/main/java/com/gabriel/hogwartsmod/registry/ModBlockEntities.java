package com.gabriel.hogwartsmod.registry;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.block.PulpitBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, HogwartsMod.MOD_ID);

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<PulpitBlockEntity>> PULPIT = BLOCK_ENTITIES.register("pulpito_professor",
            () -> BlockEntityType.Builder.of(PulpitBlockEntity::new, ModBlocks.PULPIT.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<com.gabriel.hogwartsmod.potion.CauldronBlockEntity>> CAULDRON = BLOCK_ENTITIES.register("caldeirao",
            () -> BlockEntityType.Builder.of(com.gabriel.hogwartsmod.potion.CauldronBlockEntity::new, ModBlocks.CAULDRON.get()).build(null));

    private ModBlockEntities() {
    }
}
