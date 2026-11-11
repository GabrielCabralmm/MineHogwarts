package com.gabriel.hogwartsmod.client;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.registry.ModEntities;
import com.gabriel.hogwartsmod.client.particle.MagicParticle;
import net.minecraft.client.renderer.entity.NoopRenderer;
import com.gabriel.hogwartsmod.particle.ModParticles;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import com.gabriel.hogwartsmod.client.render.PulpitRenderer;
import com.gabriel.hogwartsmod.registry.ModBlockEntities;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = HogwartsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    private ClientSetup() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(CastAnimations::registerLayer);
        event.enqueueWork(() -> net.minecraft.client.renderer.item.ItemProperties.register(com.gabriel.hogwartsmod.registry.ModItems.ENVELOPE.get(),
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "selada"),
                (stack, level, entity, seed) -> com.gabriel.hogwartsmod.mail.LetterItem.isSealed(stack) ? 1f : 0f));
        event.enqueueWork(() -> net.minecraft.client.renderer.item.ItemProperties.register(com.gabriel.hogwartsmod.registry.ModItems.WAND.get(),
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "modelo"), (stack, level, entity, seed) -> wandModel(stack)));
        event.enqueueWork(() -> net.minecraft.client.renderer.item.ItemProperties.register(com.gabriel.hogwartsmod.registry.ModItems.PACOTE.get(),
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "selada"),
                (stack, level, entity, seed) -> com.gabriel.hogwartsmod.mail.Mail.isSealed(stack) ? 1f : 0f));
        event.enqueueWork(() -> net.minecraft.client.gui.screens.MenuScreens.register(com.gabriel.hogwartsmod.registry.ModMenus.PACOTE.get(),
                com.gabriel.hogwartsmod.client.gui.PackageScreen::new));
        event.enqueueWork(() -> net.minecraft.client.gui.screens.MenuScreens.register(com.gabriel.hogwartsmod.registry.ModMenus.BANCADA_VARINHAS.get(),
                com.gabriel.hogwartsmod.client.gui.WandBenchScreen::new));
        event.enqueueWork(() -> net.minecraft.client.gui.screens.MenuScreens.register(com.gabriel.hogwartsmod.registry.ModMenus.BANCADA_VASSOURAS.get(),
                com.gabriel.hogwartsmod.client.gui.BroomBenchScreen::new));
        com.gabriel.hogwartsmod.entity.BroomEntity.clientPilot = ClientBroom::pilot;
        com.gabriel.hogwartsmod.creature.MountCreature.clientPilot = com.gabriel.hogwartsmod.client.creature.ClientCreatures::pilot;
        com.gabriel.hogwartsmod.creature.MountCreature.clientSeesAll = com.gabriel.hogwartsmod.client.creature.ClientCreatures::seesThestrals;
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(com.gabriel.hogwartsmod.client.creature.ClientCreatures::hideMountBars);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(com.gabriel.hogwartsmod.registry.ModBlockEntities.CAULDRON.get(),
                com.gabriel.hogwartsmod.client.potion.CauldronRenderer::new);
        event.registerEntityRenderer(ModEntities.SPELL_PROJECTILE.get(), NoopRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.PULPIT.get(), PulpitRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.HOUSE_DOOR.get(), com.gabriel.hogwartsmod.client.render.HouseDoorRenderer::new);
        event.registerEntityRenderer(ModEntities.PROFESSOR_NPC.get(), com.gabriel.hogwartsmod.client.npc.NpcRenderer::new);
        event.registerEntityRenderer(ModEntities.MISSION_NPC.get(), com.gabriel.hogwartsmod.client.npc.NpcRenderer::new);
        event.registerEntityRenderer(ModEntities.SHOP_NPC.get(), com.gabriel.hogwartsmod.client.npc.NpcRenderer::new);
        event.registerEntityRenderer(ModEntities.HIPPOGRIFF.get(), c -> new com.gabriel.hogwartsmod.client.creature.MountRenderer<>(c, "hipogrifo", 1.6f, 1.2f));
        event.registerEntityRenderer(ModEntities.OWL.get(), com.gabriel.hogwartsmod.client.creature.OwlRenderer::new);
        event.registerEntityRenderer(ModEntities.THESTRAL.get(), c -> new com.gabriel.hogwartsmod.client.creature.MountRenderer<>(c, "testralio", 1.55f, 1.0f));
        event.registerEntityRenderer(ModEntities.BROOM.get(), com.gabriel.hogwartsmod.client.render.BroomRenderer::new);
        event.registerEntityRenderer(ModEntities.BROOM_CARGO.get(), com.gabriel.hogwartsmod.client.render.BroomRenderer::new);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "magic_hud", MagicHud.INSTANCE);
        event.registerAboveAll("zone_banner", com.gabriel.hogwartsmod.client.zone.ZoneBanner.INSTANCE);
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "mission_tracker", com.gabriel.hogwartsmod.client.mission.MissionTracker.INSTANCE);
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "creature_health", com.gabriel.hogwartsmod.client.creature.ClientCreatures.INSTANCE);
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "lives", LivesHud.INSTANCE);
        event.registerAboveAll("duel_cue", DuelCueHud.INSTANCE);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.GLOW.get(), sprites -> new MagicParticle.Provider(sprites, MagicParticle.Shape.GLOW));
        event.registerSpriteSet(ModParticles.SPARK.get(), sprites -> new MagicParticle.Provider(sprites, MagicParticle.Shape.SPARK));
        event.registerSpriteSet(ModParticles.EMBER.get(), sprites -> new MagicParticle.Provider(sprites, MagicParticle.Shape.EMBER));
        event.registerSpriteSet(ModParticles.SMOKE.get(), sprites -> new MagicParticle.Provider(sprites, MagicParticle.Shape.SMOKE));
        event.registerSpriteSet(ModParticles.FIRE.get(), sprites -> new MagicParticle.Provider(sprites, MagicParticle.Shape.FIRE));
        event.registerSpriteSet(ModParticles.RING.get(), sprites -> new MagicParticle.Provider(sprites, MagicParticle.Shape.RING));
        event.registerSpriteSet(ModParticles.STAR.get(), sprites -> new MagicParticle.Provider(sprites, MagicParticle.Shape.STAR));
        event.registerSpriteSet(ModParticles.ORB.get(), sprites -> new MagicParticle.Provider(sprites, MagicParticle.Shape.ORB));
        event.registerSpriteSet(ModParticles.BOLT.get(), sprites -> new MagicParticle.Provider(sprites, MagicParticle.Shape.BOLT));
    }

    @SubscribeEvent
    public static void addLayers(net.minecraftforge.client.event.EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            net.minecraft.client.renderer.entity.LivingEntityRenderer<?, ?> renderer = event.getSkin(skin);
            if (renderer instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer player) {
                player.addLayer(new com.gabriel.hogwartsmod.client.house.SortingHatLayer(player));
            }
        }
    }

    @SubscribeEvent
    public static void registerExtraModels(net.minecraftforge.client.event.ModelEvent.RegisterAdditional event) {
        event.register(com.gabriel.hogwartsmod.client.potion.CauldronRenderer.SPOON);
        for (net.minecraft.resources.ResourceLocation door : com.gabriel.hogwartsmod.client.render.HouseDoorRenderer.ALL) {
            event.register(door);
        }
        for (com.gabriel.hogwartsmod.wand.WandHandle handle : com.gabriel.hogwartsmod.wand.WandHandle.values()) {
            if (handle != com.gabriel.hogwartsmod.wand.WandHandle.NENHUM) {
                event.register(WandPreview.handleModel(handle));
            }
        }
    }

    private static float wandModel(net.minecraft.world.item.ItemStack stack) {
        if (!com.gabriel.hogwartsmod.wand.Wands.isCustom(stack)) {
            return 0f;
        }
        int inch = Math.min(14, com.gabriel.hogwartsmod.wand.Wands.length(stack) / 4);
        return (1 + com.gabriel.hogwartsmod.wand.Wands.handle(stack).ordinal() * 6 + inch - 9) / 100f;
    }

    @SubscribeEvent
    public static void registerTooltips(net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(WandPreview.Data.class, WandPreview.Renderer::new);
    }

    @SubscribeEvent
    public static void registerItemColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Item event) {
        for (com.gabriel.hogwartsmod.broom.BroomType type : com.gabriel.hogwartsmod.broom.BroomType.values()) {
            event.register((stack, tint) -> {
                if (tint != 0) {
                    return 0xFFFFFF;
                }
                com.gabriel.hogwartsmod.wand.WandWood wood = com.gabriel.hogwartsmod.broom.Brooms.wood(stack);
                return wood == null ? type.getDefaultColor() : wood.getColor();
            }, type.item());
        }
        for (com.gabriel.hogwartsmod.wand.WandHandle handle : com.gabriel.hogwartsmod.wand.WandHandle.values()) {
            if (handle.getItem() != null) {
                event.register((stack, tint) -> tint == 1 ? com.gabriel.hogwartsmod.wand.HandleItem.COLOR : 0xFFFFFF, handle.getItem());
            }
        }
        event.register((stack, tint) -> {
            com.gabriel.hogwartsmod.wand.WandWood wood = com.gabriel.hogwartsmod.wand.Wands.wood(stack);
            if (wood == null || !com.gabriel.hogwartsmod.wand.Wands.isCustom(stack)) {
                return 0xFFFFFF;
            }
            int c = wood.getColor();
            if (tint != 1 || com.gabriel.hogwartsmod.wand.Wands.handle(stack) == com.gabriel.hogwartsmod.wand.WandHandle.NENHUM) {
                return c;
            }
            return com.gabriel.hogwartsmod.wand.HandleItem.COLOR;
        }, com.gabriel.hogwartsmod.registry.ModItems.WAND.get());
    }

    @SubscribeEvent
    public static void registerBlockColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> {
            if (level != null && pos != null && level.getBlockEntity(pos) instanceof com.gabriel.hogwartsmod.potion.CauldronBlockEntity cauldron) {
                return cauldron.getColor();
            }
            return com.gabriel.hogwartsmod.potion.CauldronBlockEntity.WATER_COLOR;
        }, com.gabriel.hogwartsmod.registry.ModBlocks.CAULDRON.get());
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(ModKeys.SPELL_MODIFIER);
        event.register(ModKeys.OPEN_GRIMOIRE);
        event.register(ModKeys.DODGE);
        event.register(ModKeys.APPARATE);
        event.register(ModKeys.BROOM_DISMOUNT);
    }
}
