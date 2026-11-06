package com.gabriel.hogwartsmod.client.zone;

import com.gabriel.hogwartsmod.client.gui.HiRes;
import com.gabriel.hogwartsmod.zone.Zone;
import com.gabriel.hogwartsmod.zone.ZoneRule;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.ArrayList;
import java.util.List;

public final class ZoneBanner implements IGuiOverlay {
    public static final ZoneBanner INSTANCE = new ZoneBanner();

    private ZoneBanner() {
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        Zone zone = ClientZones.banner;
        if (zone == null || ClientZones.bannerAge < 0 || mc.options.hideGui) {
            return;
        }
        float age = ClientZones.bannerAge + partialTick;
        float total = ClientZones.BANNER_TICKS;
        float alpha = Math.min(1f, Math.min(age / 8f, (total - age) / 14f));
        if (alpha <= 0.02f) {
            return;
        }
        float cx = width / 2f;
        int y = 26;
        String title = ClientZones.bannerLeaving
                ? Component.translatable("zone.hogwartsmod.leaving", zone.name()).getString()
                : zone.name();
        String rules = ClientZones.bannerLeaving ? "" : rulesLine(zone);
        float titleSize = ClientZones.bannerLeaving ? 10f : 15f;
        float titleW = HiRes.width(title, HiRes.Style.BOLD, titleSize);
        float rulesW = rules.isEmpty() ? 0 : HiRes.width(rules, HiRes.Style.ITALIC);
        int half = (int) (Math.max(titleW, rulesW) / 2f) + 30;
        int bandH = rules.isEmpty() ? (int) titleSize + 12 : (int) titleSize + 24;
        int a = (int) (alpha * 150);
        for (int i = 0; i < half; i += 2) {
            int fade = (int) (a * Math.min(1f, (half - i) / 26f));
            g.fill((int) cx + i, y - 6, (int) cx + i + 2, y - 6 + bandH, fade << 24 | 0x140C08);
            g.fill((int) cx - i - 2, y - 6, (int) cx - i, y - 6 + bandH, fade << 24 | 0x140C08);
        }
        int lineA = (int) (alpha * 255);
        int lineW = (int) Math.min(half - 20, titleW / 2f + 14);
        g.fill((int) cx - lineW, y - 6, (int) cx + lineW, y - 5, lineA << 24 | 0xBE8C28);
        g.fill((int) cx - lineW, y - 7 + bandH, (int) cx + lineW, y - 6 + bandH, lineA << 24 | 0xBE8C28);
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        HiRes.centered(g, title, HiRes.Style.BOLD, titleSize, cx, y, ClientZones.bannerLeaving ? 0xFFECDCB6 : 0xFFE6B43C);
        if (!rules.isEmpty()) {
            HiRes.centered(g, rules, HiRes.Style.ITALIC, cx, y + titleSize + 4, 0xFFECDCB6);
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private static String rulesLine(Zone zone) {
        List<String> parts = new ArrayList<>();
        for (ZoneRule rule : ZoneRule.values()) {
            if (zone.has(rule)) {
                parts.add(Component.translatable("zone.hogwartsmod.banner." + rule.getId()).getString());
            }
        }
        return String.join("  ·  ", parts);
    }
}
