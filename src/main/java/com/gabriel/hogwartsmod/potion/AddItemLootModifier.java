package com.gabriel.hogwartsmod.potion;

import com.gabriel.hogwartsmod.magic.MagicConfig;
import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class AddItemLootModifier extends LootModifier {
    public static final Supplier<Codec<AddItemLootModifier>> CODEC = Suppliers.memoize(() -> RecordCodecBuilder.create(inst -> codecStart(inst)
            .and(ForgeRegistries.ITEMS.getCodec().fieldOf("item").forGetter(m -> m.item))
            .and(Codec.FLOAT.fieldOf("chance").forGetter(m -> m.chance))
            .apply(inst, AddItemLootModifier::new)));

    private final Item item;
    private final float chance;

    public AddItemLootModifier(LootItemCondition[] conditions, Item item, float chance) {
        super(conditions);
        this.item = item;
        this.chance = chance;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (MagicConfig.POTION_BOOKS_IN_CHESTS.get() && context.getRandom().nextFloat() < chance) {
            loot.add(new ItemStack(item));
        }
        return loot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
