package com.gabriel.hogwartsmod.client.creature;

import com.gabriel.hogwartsmod.client.ModKeys;
import com.gabriel.hogwartsmod.client.gui.HiRes;
import com.gabriel.hogwartsmod.creature.HippogriffEntity;
import com.gabriel.hogwartsmod.creature.Species;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.ArrayList;
import java.util.List;

public final class ClientCreatures implements IGuiOverlay {
    public static final ClientCreatures INSTANCE = new ClientCreatures();
    private static final float LOW = 0.3f;

    private ClientCreatures() {
    }

    public static void pilot(HippogriffEntity hippogriff) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || hippogriff.getControllingPassenger() != player) {
            return;
        }
        float forward = Math.signum(player.input.forwardImpulse);
        float vertical = (player.input.jumping ? 1f : 0f) - (Minecraft.getInstance().options.keyShift.isDown() ? 1f : 0f);
        hippogriff.setInput(forward, vertical, ModKeys.isModifierDown(), player.getYRot(), player.getXRot());
    }

    // -- Aviso de vida baixa

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui || mc.screen != null) {
            return;
        }
        List<TamableAnimal> low = new ArrayList<>();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof TamableAnimal creature && creature.isAlive() && Species.of(creature) != null
                    && creature.isOwnedBy(mc.player) && creature.getHealth() / creature.getMaxHealth() < LOW) {
                low.add(creature);
            }
        }
        if (low.isEmpty()) {
            return;
        }
        float pulse = 0.65f + 0.35f * (float) Math.sin((mc.level.getGameTime() + partialTick) * 0.25);
        float y = height / 2f - low.size() * 12f;
        for (TamableAnimal creature : low.subList(0, Math.min(3, low.size()))) {
            String name = creature.getDisplayName().getString();
            int boxW = 112;
            int x = width - boxW - 8;
            int top = Math.round(y);
            g.fill(x, top, x + boxW, top + 21, 0xB0140C06);
            g.fill(x, top, x + 2, top + 21, 0xFF000000 | (int) (0xC0 * pulse) << 16 | 0x2020);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            HiRes.text(g, HiRes.ellipsize(name, HiRes.Style.BOLD, (boxW - 12) * HiRes.TEXT_SIZE / 7f), HiRes.Style.BOLD, 7f, x + 6, top + 3, 0xFFF3E4C4);
            float ratio = Math.max(0f, creature.getHealth() / creature.getMaxHealth());
            HiRes.bar(g, x + 6, top + 13, boxW - 12, 4, ratio, 0xFF3A2A20, 0xFFC03028);
            y += 25;
        }
    }
}
