package com.gabriel.hogwartsmod.client;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.registry.ModEntities;
import com.gabriel.hogwartsmod.client.fx.SpellProjectileRenderer;
import com.gabriel.hogwartsmod.client.particle.MagicParticle;
import com.gabriel.hogwartsmod.particle.ModParticles;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
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
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SPELL_PROJECTILE.get(), SpellProjectileRenderer::new);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "magic_hud", MagicHud.INSTANCE);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.GLOW.get(), sprites -> new MagicParticle.Provider(sprites, MagicParticle.Shape.GLOW));
        event.registerSpriteSet(ModParticles.SPARK.get(), sprites -> new MagicParticle.Provider(sprites, MagicParticle.Shape.SPARK));
        event.registerSpriteSet(ModParticles.EMBER.get(), sprites -> new MagicParticle.Provider(sprites, MagicParticle.Shape.EMBER));
        event.registerSpriteSet(ModParticles.SMOKE.get(), sprites -> new MagicParticle.Provider(sprites, MagicParticle.Shape.SMOKE));
        event.registerSpriteSet(ModParticles.FIRE.get(), sprites -> new MagicParticle.Provider(sprites, MagicParticle.Shape.FIRE));
        event.registerSpriteSet(ModParticles.RING.get(), sprites -> new MagicParticle.Provider(sprites, MagicParticle.Shape.RING));
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(ModKeys.SPELL_MODIFIER);
        event.register(ModKeys.OPEN_GRIMOIRE);
    }
}
