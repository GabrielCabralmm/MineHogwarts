package com.gabriel.hogwartsmod.client;

import com.gabriel.hogwartsmod.HogwartsMod;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

public final class CastAnimations {
    public static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "cast");
    private static final int FADE_TICKS = 3;

    private CastAnimations() {
    }

    public static void registerLayer() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, 1000, player -> new ModifierLayer<IAnimation>());
    }

    @SuppressWarnings("unchecked")
    public static void play(AbstractClientPlayer player, String animation) {
        var data = PlayerAnimationAccess.getPlayerAssociatedData(player);
        if (!(data.get(LAYER) instanceof ModifierLayer<?> raw)) {
            return;
        }
        ModifierLayer<IAnimation> layer = (ModifierLayer<IAnimation>) raw;
        AbstractFadeModifier fade = AbstractFadeModifier.standardFadeIn(FADE_TICKS, Ease.INOUTSINE);
        if (animation.isEmpty()) {
            layer.replaceAnimationWithFade(fade, null);
            return;
        }
        KeyframeAnimation keyframes = PlayerAnimationRegistry.getAnimation(ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, animation));
        if (keyframes == null) {
            return;
        }
        KeyframeAnimationPlayer player1 = new KeyframeAnimationPlayer(keyframes)
                .setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL)
                .setFirstPersonConfiguration(new FirstPersonConfiguration().setShowRightArm(true).setShowRightItem(true)
                        .setShowLeftArm(false).setShowLeftItem(false));
        layer.replaceAnimationWithFade(fade, player1, true);
    }
}
