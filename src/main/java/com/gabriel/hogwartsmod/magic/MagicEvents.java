package com.gabriel.hogwartsmod.magic;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.network.ModNetwork;
import com.gabriel.hogwartsmod.item.WandItem;
import com.gabriel.hogwartsmod.spell.SpellCategory;
import com.gabriel.hogwartsmod.spell.Spells;
import com.gabriel.hogwartsmod.spell.impl.LumosSpell;
import com.gabriel.hogwartsmod.spell.impl.ProtegoSpell;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public final class MagicEvents {
    private static final ResourceLocation KEY = ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "magic");

    private MagicEvents() {
    }

    @Mod.EventBusSubscriber(modid = HogwartsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        @SubscribeEvent
        public static void registerCapabilities(RegisterCapabilitiesEvent event) {
            event.register(MagicData.class);
        }
    }

    @Mod.EventBusSubscriber(modid = HogwartsMod.MOD_ID)
    public static final class ForgeBus {

        @SubscribeEvent
        public static void attach(AttachCapabilitiesEvent<Entity> event) {
            if (event.getObject() instanceof Player) {
                MagicDataProvider provider = new MagicDataProvider();
                event.addCapability(KEY, provider);
                event.addListener(provider::invalidate);
            }
        }

        @SubscribeEvent
        public static void clone(PlayerEvent.Clone event) {
            event.getOriginal().reviveCaps();
            MagicData oldData = MagicData.get(event.getOriginal());
            MagicData newData = MagicData.get(event.getEntity());
            if (oldData != null && newData != null) {
                newData.copyFrom(oldData);
                if (event.isWasDeath()) {
                    newData.setReserve(newData.getMaxReserve());
                }
            }
            event.getOriginal().invalidateCaps();
        }

        @SubscribeEvent
        public static void login(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                MagicData data = MagicData.get(player);
                if (data != null) {
                    BlockPos light = data.getLumosLight();
                    if (light != null) {
                        LumosSpell.removeLight(player.level(), light);
                        data.setLumosLight(null);
                    }
                    ModNetwork.sync(player, data);
                }
            }
        }

        @SubscribeEvent
        public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                MagicData data = MagicData.get(player);
                if (data != null && data.getLumosLight() != null) {
                    LumosSpell.removeLight(player.level(), data.getLumosLight());
                    data.setLumosLight(null);
                }
            }
        }

        @SubscribeEvent
        public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                MagicData data = MagicData.get(player);
                if (data != null) {
                    ModNetwork.sync(player, data);
                }
            }
        }

        @SubscribeEvent
        public static void changeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                MagicData data = MagicData.get(player);
                if (data != null) {
                    ModNetwork.sync(player, data);
                }
            }
        }

        @SubscribeEvent
        public static void tick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
                return;
            }
            MagicData data = MagicData.get(player);
            if (data == null) {
                return;
            }
            float regen = (float) (MagicConfig.RESERVE_REGEN_PER_SECOND.get() / 20.0);
            boolean usingWand = player.isUsingItem() && player.getUseItem().getItem() instanceof WandItem;
            if (data.getReserve() < data.getMaxReserve() && !usingWand) {
                data.setReserve(data.getReserve() + regen);
            }
            if (data.consumeLevelUp()) {
                playLevelUp(player);
            }
            if (data.isShielding() && !usingWand) {
                data.stopShield();
            }
            if (player.tickCount % 4 == 0 || data.getLumosLight() != null && data.getLumosUntil() <= player.level().getGameTime()) {
                Spells.LUMOS.tickLight(player, data);
            }
            if (data.isDirty() && (data.isForceSync() || player.tickCount % 10 == 0)) {
                ModNetwork.sync(player, data);
            }
        }

        @SubscribeEvent
        public static void shieldPhysicalDamage(LivingAttackEvent event) {
            if (!(event.getEntity() instanceof Player defender) || defender.level().isClientSide) {
                return;
            }
            if (event.getSource().is(DamageTypeTags.BYPASSES_SHIELD)) {
                return;
            }
            Vec3 from = event.getSource().getSourcePosition();
            if (from == null) {
                return;
            }
            ProtegoSpell.BlockResult result = ProtegoSpell.tryBlock(defender, from,
                    SpellCategory.DANO, event.getAmount());
            if (result != ProtegoSpell.BlockResult.NONE) {
                event.setCanceled(true);
            }
        }
    }

    public static void playLevelUp(ServerPlayer player) {
        if (player.level() instanceof ServerLevel server) {
            DustParticleOptions gold = new DustParticleOptions(new Vector3f(1.0f, 0.82f, 0.3f), 1.2f);
            server.sendParticles(gold, player.getX(), player.getY() + 1.0, player.getZ(), 50, 0.6, 0.9, 0.6, 0.05);
        }
    }
}
