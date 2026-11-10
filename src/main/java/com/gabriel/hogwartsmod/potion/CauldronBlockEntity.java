package com.gabriel.hogwartsmod.potion;

import com.gabriel.hogwartsmod.item.WandItem;
import com.gabriel.hogwartsmod.particle.ModParticles;
import com.gabriel.hogwartsmod.registry.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CauldronBlockEntity extends BlockEntity {
    public enum State { EMPTY, WATER, BREWING, READY, RUINED }

    public static final int WATER_COLOR = 0x3F76E4;
    public static final int RUINED_COLOR = 0x5E5A2E;

    private State state = State.EMPTY;
    private final List<String> tokens = new ArrayList<>();
    @Nullable
    private Brew brew;
    private int timer;
    private int doses;

    public CauldronBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CAULDRON.get(), pos, blockState);
    }

    // -- Estado

    public State getState() {
        return state;
    }

    public boolean isHeated() {
        return getBlockState().getValue(CauldronBlock.FIRE);
    }

    @Nullable
    private Brew bestMatch() {
        Brew best = null;
        for (Brew b : Brew.values()) {
            List<String> t = b.getTokens();
            if (tokens.size() <= t.size() && t.subList(0, tokens.size()).equals(tokens)) {
                if (best == null || t.size() < best.getTokens().size()) {
                    best = b;
                }
            }
        }
        return best;
    }

    public int getColor() {
        return switch (state) {
            case EMPTY, WATER -> {
                Brew target = tokens.isEmpty() ? null : bestMatch();
                if (target == null) {
                    yield WATER_COLOR;
                }
                float t = tokens.size() / (float) target.getTokens().size();
                yield mix(WATER_COLOR, target.getColor(), t * 0.8f);
            }
            case BREWING -> {
                float t = brew == null ? 1f : 1f - timer / (float) (brew.getSeconds() * 20);
                yield mix(mix(WATER_COLOR, brew == null ? WATER_COLOR : brew.getColor(), 0.8f), brew == null ? WATER_COLOR : brew.getColor(), t);
            }
            case READY -> brew == null ? WATER_COLOR : brew.getColor();
            case RUINED -> RUINED_COLOR;
        };
    }

    private static int mix(int a, int b, float t) {
        t = Mth.clamp(t, 0f, 1f);
        int r = Math.round(Mth.lerp(t, a >> 16 & 255, b >> 16 & 255));
        int g = Math.round(Mth.lerp(t, a >> 8 & 255, b >> 8 & 255));
        int bl = Math.round(Mth.lerp(t, a & 255, b & 255));
        return r << 16 | g << 8 | bl;
    }

    // -- Interação

    public InteractionResult interact(Player player, ItemStack held) {
        if (level == null) {
            return InteractionResult.PASS;
        }
        boolean client = level.isClientSide;
        if (held.is(Items.WATER_BUCKET) && state == State.EMPTY) {
            if (!client) {
                if (!player.getAbilities().instabuild) {
                    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
                }
                reset(State.WATER);
                level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1f, 1f);
            }
            return InteractionResult.sidedSuccess(client);
        }
        if (held.is(Items.BUCKET) && state != State.EMPTY) {
            if (!client) {
                boolean lost = state != State.WATER || !tokens.isEmpty();
                held.shrink(1);
                ItemStack water = new ItemStack(Items.WATER_BUCKET);
                if (held.isEmpty()) {
                    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, water);
                } else if (!player.getInventory().add(water)) {
                    player.drop(water, false);
                }
                reset(State.EMPTY);
                level.playSound(null, worldPosition, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1f, 1f);
                if (lost) {
                    say(player, Component.translatable("message.hogwartsmod.cauldron_lost"), ChatFormatting.GRAY);
                }
            }
            return InteractionResult.sidedSuccess(client);
        }
        if (held.isEmpty() && player.isShiftKeyDown()) {
            if (!client) {
                pickUp(player);
            }
            return InteractionResult.sidedSuccess(client);
        }
        if (held.is(Items.GLASS_BOTTLE)) {
            if (!client) {
                collect(player, held);
            }
            return InteractionResult.sidedSuccess(client);
        }
        if (held.getItem() instanceof WandItem) {
            if (state == State.WATER) {
                if (!client) {
                    add(player, Brew.STIR);
                }
                return InteractionResult.sidedSuccess(client);
            }
            if (!client) {
                status(player);
            }
            return InteractionResult.sidedSuccess(client);
        }
        String id = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
        if (!held.isEmpty() && Brew.usesItem(id) && state == State.WATER) {
            if (!client && add(player, id) && !player.getAbilities().instabuild) {
                ItemStack remainder = held.getCraftingRemainingItem();
                if (held.is(Items.HONEY_BOTTLE)) {
                    remainder = new ItemStack(Items.GLASS_BOTTLE);
                }
                held.shrink(1);
                if (!remainder.isEmpty() && !player.getInventory().add(remainder)) {
                    player.drop(remainder, false);
                }
            }
            return InteractionResult.sidedSuccess(client);
        }
        if (held.isEmpty()) {
            if (!client) {
                status(player);
            }
            return InteractionResult.sidedSuccess(client);
        }
        return InteractionResult.PASS;
    }

    private boolean add(Player player, String token) {
        if (!isHeated()) {
            say(player, Component.translatable("message.hogwartsmod.cauldron_no_fire"), ChatFormatting.GRAY);
            return false;
        }
        tokens.add(token);
        ServerLevel server = (ServerLevel) level;
        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY() + topY();
        double z = worldPosition.getZ() + 0.5;
        if (token.equals(Brew.STIR)) {
            server.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.4f, 1.6f);
            for (int i = 0; i < 10; i++) {
                double a = i / 10.0 * Math.PI * 2;
                server.sendParticles(ModParticles.GLOW.get().with(getColor(), 0.3f, 12),
                        x + Math.cos(a) * 0.25, y, z + Math.sin(a) * 0.25, 0, -Math.sin(a) * 0.04, 0.02, Math.cos(a) * 0.04, 1.0);
            }
        } else {
            server.playSound(null, worldPosition, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.5f, 1.4f);
            server.sendParticles(ParticleTypes.SPLASH, x, y, z, 8, 0.15, 0.02, 0.15, 0.05);
        }
        Brew match = bestMatch();
        if (match == null) {
            explode(player);
            return true;
        }
        if (match.getTokens().size() == tokens.size()) {
            brew = match;
            state = State.BREWING;
            timer = match.getSeconds() * 20;
            server.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1f, 0.8f);
            say(player, Component.translatable("message.hogwartsmod.cauldron_brewing", match.getDisplayName(), match.getSeconds()), ChatFormatting.GOLD);
        } else {
            say(player, Component.translatable(token.equals(Brew.STIR) ? "message.hogwartsmod.cauldron_stirred" : "message.hogwartsmod.cauldron_added",
                    token.equals(Brew.STIR) ? "" : BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.tryParse(token)).getDescription(),
                    tokens.size()), ChatFormatting.GRAY);
        }
        changed();
        return true;
    }

    private void explode(Player player) {
        ServerLevel server = (ServerLevel) level;
        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY() + 0.9;
        double z = worldPosition.getZ() + 0.5;
        reset(State.EMPTY);
        server.explode(null, x, y, z, 1.4f, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
        server.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 24, 0.3, 0.2, 0.3, 0.04);
        server.sendParticles(ModParticles.SMOKE.get().with(RUINED_COLOR, 0.8f, 30), x, y, z, 16, 0.3, 0.3, 0.3, 0.05);
        say(player, Component.translatable("message.hogwartsmod.cauldron_exploded"), ChatFormatting.RED);
    }

    private void pickUp(Player player) {
        ItemStack item = new ItemStack(com.gabriel.hogwartsmod.registry.ModBlocks.CAULDRON_ITEM.get());
        item.getOrCreateTag().putBoolean(CauldronItem.NO_FIRE, true);
        if (state != State.EMPTY) {
            BlockItem.setBlockEntityData(item, getType(), saveWithoutMetadata());
        }
        boolean fire = isHeated();
        BlockPos pos = worldPosition;
        level.setBlock(pos, fire ? com.gabriel.hogwartsmod.registry.ModBlocks.POTION_FIRE.get().defaultBlockState()
                : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_IRON, SoundSource.BLOCKS, 1f, 0.8f);
        if (player.getMainHandItem().isEmpty()) {
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, item);
        } else if (!player.getInventory().add(item)) {
            player.drop(item, false);
        }
    }

    private void collect(Player player, ItemStack bottle) {
        if (state != State.READY || brew == null) {
            status(player);
            return;
        }
        ItemStack potion = new ItemStack(com.gabriel.hogwartsmod.registry.ModItems.potion(brew));
        if (!player.getAbilities().instabuild) {
            bottle.shrink(1);
        }
        if (!player.getInventory().add(potion)) {
            player.drop(potion, false);
        }
        level.playSound(null, worldPosition, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1f, 1f);
        doses--;
        if (doses <= 0) {
            reset(State.EMPTY);
        } else {
            changed();
        }
    }

    private void status(Player player) {
        Component text = switch (state) {
            case EMPTY -> Component.translatable("message.hogwartsmod.cauldron_status_empty");
            case WATER -> tokens.isEmpty()
                    ? Component.translatable(isHeated() ? "message.hogwartsmod.cauldron_status_water" : "message.hogwartsmod.cauldron_no_fire")
                    : Component.translatable("message.hogwartsmod.cauldron_status_steps", tokens.size());
            case BREWING -> Component.translatable("message.hogwartsmod.cauldron_status_brewing", brew == null ? "" : brew.getDisplayName(),
                    Math.max(1, timer / 20));
            case READY -> Component.translatable("message.hogwartsmod.cauldron_status_ready", brew == null ? "" : brew.getDisplayName(), doses);
            case RUINED -> Component.translatable("message.hogwartsmod.cauldron_status_ruined");
        };
        say(player, text, state == State.READY ? ChatFormatting.GREEN : ChatFormatting.GRAY);
    }

    private static void say(Player player, Component text, ChatFormatting color) {
        player.displayClientMessage(text.copy().withStyle(color), true);
    }

    private void reset(State next) {
        state = next;
        tokens.clear();
        brew = null;
        timer = 0;
        doses = 0;
        changed();
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState current = getBlockState();
            boolean filled = state != State.EMPTY;
            if (current.getValue(CauldronBlock.FILLED) != filled) {
                level.setBlock(worldPosition, current.setValue(CauldronBlock.FILLED, filled), 3);
            }
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private double topY() {
        return isHeated() ? 0.88 : 0.5;
    }

    // -- Tique

    public void serverTick() {
        if (state != State.BREWING || brew == null || !isHeated()) {
            return;
        }
        timer--;
        if (timer % 20 == 0) {
            changed();
        }
        if (timer <= 0) {
            state = State.READY;
            doses = brew.getDoses();
            ServerLevel server = (ServerLevel) level;
            server.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.2f, 1.2f);
            double x = worldPosition.getX() + 0.5;
            double z = worldPosition.getZ() + 0.5;
            double top = worldPosition.getY() + topY();
            for (int i = 0; i < 14; i++) {
                server.sendParticles(ModParticles.GLOW.get().with(brew.getColor(), 0.35f, 22), x, top + i * 0.12, z, 1, 0.05, 0.02, 0.05, 0.0);
            }
            server.sendParticles(ModParticles.STAR.get().with(brew.getColor(), 0.6f, 20), x, top + 0.2, z, 10, 0.25, 0.15, 0.25, 0.02);
            server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, top + 0.3, z, 2, 0.1, 0.05, 0.1, 0.005);
            changed();
        }
    }

    public void clientTick() {
        if (level == null || state == State.EMPTY || state == State.READY && level.random.nextInt(4) != 0) {
            return;
        }
        boolean hot = isHeated();
        if (!hot && state != State.RUINED) {
            return;
        }
        int rate = state == State.BREWING ? 2 : 6;
        if (level.random.nextInt(rate) != 0) {
            return;
        }
        double x = worldPosition.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.5;
        double y = worldPosition.getY() + topY();
        double z = worldPosition.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.5;
        if (state == State.RUINED) {
            level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.02, 0);
            return;
        }
        level.addParticle(ModParticles.GLOW.get().with(getColor(), 0.18f, 16), x, y, z, 0, 0.015, 0);
        if (level.random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.BUBBLE_POP, x, y, z, 0, 0.03, 0);
        }
    }

    // -- NBT e sincronização

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("Estado", state.name());
        ListTag list = new ListTag();
        tokens.forEach(t -> list.add(StringTag.valueOf(t)));
        tag.put("Passos", list);
        if (brew != null) {
            tag.putString("Pocao", brew.getId());
        }
        tag.putInt("Tempo", timer);
        tag.putInt("Doses", doses);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        try {
            state = State.valueOf(tag.getString("Estado"));
        } catch (IllegalArgumentException e) {
            state = State.EMPTY;
        }
        tokens.clear();
        tag.getList("Passos", Tag.TAG_STRING).forEach(t -> tokens.add(t.getAsString()));
        brew = Brew.byId(tag.getString("Pocao"));
        timer = tag.getInt("Tempo");
        doses = tag.getInt("Doses");
        if (level != null && level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 8);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
