package com.gabriel.hogwartsmod.client.mission;

import com.gabriel.hogwartsmod.client.gui.HiRes;
import com.gabriel.hogwartsmod.magic.MagicData;
import com.gabriel.hogwartsmod.mission.ActiveMission;
import com.gabriel.hogwartsmod.mission.Mission;
import com.gabriel.hogwartsmod.mission.ObjectiveType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.HashMap;
import java.util.Map;

public final class MissionTracker implements IGuiOverlay {
    public static final MissionTracker INSTANCE = new MissionTracker();
    private static final int TITLE = 0xFFF3E4C4;
    private static final int TEXT = 0xFFD2C29E;
    private static final int GOLD = 0xFFE6B43C;
    private static final int READY = 0xFF9CD06A;
    private static final int SHADOW = 0xA0140C06;
    private static final float X = 6;
    private static final float Y = 6;
    private static final float MAX_W = 120;

    private final Map<String, Integer> lastProgress = new HashMap<>();
    private final Map<String, Long> flashUntil = new HashMap<>();

    private MissionTracker() {
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.options.renderDebug || mc.screen != null) {
            return;
        }
        MagicData data = MagicData.get(mc.player);
        if (data == null) {
            return;
        }
        long now = System.currentTimeMillis();
        float y = Y;
        for (ActiveMission a : data.getMissions().active()) {
            if (!a.tracked()) {
                continue;
            }
            Mission m = a.mission();
            int progress = progress(mc, a);
            boolean ready = progress >= m.amount();
            Integer before = lastProgress.put(a.key(), progress);
            if (before != null && before != progress) {
                flashUntil.put(a.key(), now + 1500);
            }
            boolean flash = flashUntil.getOrDefault(a.key(), 0L) > now;
            String title = HiRes.ellipsize(m.title().isBlank() ? "?" : m.title(), HiRes.Style.BOLD, MAX_W);
            shadowed(g, title, HiRes.Style.BOLD, 6.5f, X, y, ready ? READY : TITLE);
            String line;
            if (ready) {
                line = Component.translatable("hud.hogwartsmod.mission_ready", a.npcName()).getString();
            } else if (m.type() == ObjectiveType.IR) {
                line = Component.translatable("hud.hogwartsmod.mission_go", m.target()).getString();
            } else {
                line = m.targetName().getString() + "  " + progress + "/" + m.amount();
            }
            shadowed(g, HiRes.ellipsize(line, HiRes.Style.ITALIC, MAX_W), HiRes.Style.ITALIC, 6f, X, y + 8, flash ? GOLD : ready ? READY : TEXT);
            y += 19;
        }
    }

    private static void shadowed(GuiGraphics g, String text, HiRes.Style style, float size, float x, float y, int color) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        HiRes.text(g, text, style, size, x + 0.5f, y + 0.5f, SHADOW);
        HiRes.text(g, text, style, size, x, y, color);
    }

    private static int progress(Minecraft mc, ActiveMission a) {
        Mission m = a.mission();
        if (m.type() == ObjectiveType.ENTREGAR && mc.player != null) {
            int n = 0;
            for (ItemStack stack : mc.player.getInventory().items) {
                if (stack.is(m.item())) {
                    n += stack.getCount();
                }
            }
            return Math.min(m.amount(), n);
        }
        return Math.min(m.amount(), a.progress());
    }
}
