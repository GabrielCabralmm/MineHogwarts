package com.gabriel.hogwartsmod.client;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.registry.ModItems;
import com.gabriel.hogwartsmod.wand.HandleItem;
import com.gabriel.hogwartsmod.wand.WandHandle;
import com.gabriel.hogwartsmod.wand.Wands;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Either;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = HogwartsMod.MOD_ID, value = Dist.CLIENT)
public final class WandPreview {

    private WandPreview() {
    }

    public static ResourceLocation handleModel(WandHandle handle) {
        return ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "item/varinha/cabo3d_" + handle.getId());
    }

    private static boolean previewable(ItemStack stack) {
        return stack.getItem() instanceof HandleItem || (stack.is(ModItems.WAND.get()) && Wands.isCustom(stack));
    }

    @SubscribeEvent
    public static void gather(RenderTooltipEvent.GatherComponents event) {
        ItemStack stack = event.getItemStack();
        if (!previewable(stack)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && stack.is(ModItems.WAND.get()) && Wands.isForeign(stack, mc.player)) {
            event.getTooltipElements().add(Either.left(Component.translatable("tooltip.hogwartsmod.wand.foreign",
                    com.gabriel.hogwartsmod.magic.MagicConfig.FOREIGN_WAND_DAMAGE.get(), com.gabriel.hogwartsmod.magic.MagicConfig.FOREIGN_WAND_COOLDOWN.get())
                    .withStyle(ChatFormatting.RED)));
        }
        if (Screen.hasShiftDown()) {
            event.getTooltipElements().add(Either.right(new Data(stack)));
        } else {
            event.getTooltipElements().add(Either.left(Component.translatable("tooltip.hogwartsmod.preview_hint").withStyle(ChatFormatting.DARK_GRAY)));
        }
    }

    public record Data(ItemStack stack) implements TooltipComponent {
    }

    // -- Desenho

    public static void render(GuiGraphics g, ItemStack stack, float cx, float cy, float width, float spin, float tilt) {
        Minecraft mc = Minecraft.getInstance();
        BakedModel model;
        float length;
        float center;
        if (stack.getItem() instanceof HandleItem item) {
            model = mc.getModelManager().getModel(handleModel(item.getHandle()));
            length = 6f;
            center = 2f;
        } else {
            model = mc.getItemRenderer().getModel(stack, mc.level, null, 0);
            length = Math.min(14, Wands.length(stack) / 4) + 6f;
            center = -1f + length / 2f;
        }
        float scale = width / (length / 16f);
        g.flush();
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(cx, cy, 150);
        pose.scale(scale, -scale, scale);
        pose.mulPose(Axis.YP.rotationDegrees(tilt));
        pose.mulPose(Axis.XP.rotationDegrees(spin));
        pose.translate(-(center - 8f) / 16f, 0, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(-45));
        Lighting.setupFor3DItems();
        mc.getItemRenderer().render(stack, ItemDisplayContext.NONE, false, pose, g.bufferSource(), 0xF000F0, OverlayTexture.NO_OVERLAY, model);
        g.flush();
        pose.popPose();
    }

    public static final class Renderer implements ClientTooltipComponent {
        private final ItemStack stack;
        private final boolean handle;

        public Renderer(Data data) {
            this.stack = data.stack();
            this.handle = data.stack().getItem() instanceof HandleItem;
        }

        @Override
        public int getHeight() {
            return handle ? 34 : 30;
        }

        @Override
        public int getWidth(Font font) {
            return handle ? 70 : 120;
        }

        @Override
        public void renderImage(Font font, int x, int y, GuiGraphics g) {
            float spin = (System.currentTimeMillis() % 6000L) / 6000f * 360f;
            render(g, stack, x + getWidth(font) / 2f, y + getHeight() / 2f - 1, getWidth(font) - 8, spin, -20f);
        }
    }
}
