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
        event.registerEntityRenderer(ModEntities.PROFESSOR_NPC.get(), com.gabriel.hogwartsmod.client.npc.NpcRenderer::new);
        event.registerEntityRenderer(ModEntities.MISSION_NPC.get(), com.gabriel.hogwartsmod.client.npc.NpcRenderer::new);
        event.registerEntityRenderer(ModEntities.SHOP_NPC.get(), com.gabriel.hogwartsmod.client.npc.NpcRenderer::new);
        event.registerEntityRenderer(ModEntities.HIPPOGRIFF.get(), c -> new com.gabriel.hogwartsmod.client.creature.MountRenderer<>(c, "hipogrifo", 1.6f, 1.2f));
        event.registerEntityRenderer(ModEntities.OWL.get(), com.gabriel.hogwartsmod.client.creature.OwlRenderer::new);
        event.registerEntityRenderer(ModEntities.THESTRAL.get(), c -> new com.gabriel.hogwartsmod.client.creature.MountRenderer<>(c, "testralio", 1.55f, 1.0f));
        event.registerEntityRenderer(ModEntities.BROOM.get(), com.gabriel.hogwartsmod.client.render.BroomRenderer::new);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "magic_hud", MagicHud.INSTANCE);
        event.registerAboveAll("zone_banner", com.gabriel.hogwartsmod.client.zone.ZoneBanner.INSTANCE);
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "mission_tracker", com.gabriel.hogwartsmod.client.mission.MissionTracker.INSTANCE);
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "creature_health", com.gabriel.hogwartsmod.client.creature.ClientCreatures.INSTANCE);
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "lives", LivesHud.INSTANCE);
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
