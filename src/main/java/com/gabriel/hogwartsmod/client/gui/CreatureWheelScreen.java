package com.gabriel.hogwartsmod.client.gui;

import com.gabriel.hogwartsmod.creature.CreatureStore;
import com.gabriel.hogwartsmod.magic.MagicData;
import com.gabriel.hogwartsmod.network.ModNetwork;
import com.gabriel.hogwartsmod.network.SummonCreaturePacket;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class CreatureWheelScreen extends Screen {
    private int OUTER = 92;
    private int INNER = 34;

    private static final int INK = 0xFF38261A;
    private static final int TITLE = 0xFF6E281E;
    private static final int MUTED = 0xFF82684C;
    private static final int GREEN = 0xFF4E7A2E;

    private record Slot(CreatureStore.Entry entry, LivingEntity preview) {
        boolean summoned() {
            return entry.summoned() != null;
        }
    }

    private final HiRes.Canvas canvas = new HiRes.Canvas();
    private final List<Slot> slots = new ArrayList<>();
    private int hovered = -1;
    private int drawnHover = -2;
    private boolean released;

    public CreatureWheelScreen() {
        super(Component.translatable("screen.hogwartsmod.creature_wheel"));
    }

    public static void open(InteractionHand hand) {
        Minecraft.getInstance().setScreen(new CreatureWheelScreen());
    }

    @Override
    protected void init() {
        OUTER = Math.max(60, Math.min(92, (height - 64) / 2));
        INNER = Math.round(OUTER * 0.42f);
        slots.clear();
        if (minecraft == null || minecraft.player == null || minecraft.level == null) {
            return;
        }
        MagicData data = MagicData.get(minecraft.player);
        if (data == null) {
            return;
        }
        for (CreatureStore.Entry entry : data.getCreatures().all()) {
            Entity created = entry.species().getType().create(minecraft.level);
            if (created instanceof LivingEntity living) {
                living.load(entry.data());
                living.yBodyRot = 210f;
                living.setYRot(210f);
                living.yHeadRot = 210f;
                slots.add(new Slot(entry, living));
            }
        }
        drawnHover = -2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g) {
        g.fillGradient(0, 0, width, height, 0x90000000, 0xA0000000);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        int cx = width / 2;
        int cy = height / 2 - 6;
        hovered = sliceAt(mouseX - cx, mouseY - cy);
        if (hovered != drawnHover) {
            redraw();
            drawnHover = hovered;
        }
        int size = OUTER * 2 + 8;
        canvas.draw(g, cx - size / 2, cy - size / 2, size, size);
        int n = Math.max(1, slots.size());
        for (int i = 0; i < slots.size(); i++) {
            Slot slot = slots.get(i);
            double mid = Math.toRadians(n == 1 ? -90 : -90 + (i + 0.5) * 360.0 / n);
            float r = (OUTER + INNER) / 2f + 2;
            int tx = Math.round(cx + (float) Math.cos(mid) * r);
            int ty = Math.round(cy + (float) Math.sin(mid) * r);
            int scale = Math.round((OUTER - INNER) * 0.28f);
            InventoryScreen.renderEntityInInventory(g, tx, ty + 6, scale, new Quaternionf().rotationXYZ(0.25f, 0f, (float) Math.PI), null, slot.preview);
            String name = HiRes.ellipsize(slot.entry.name(), HiRes.Style.BOLD, (OUTER - INNER) * 0.95f);
            HiRes.centered(g, name, HiRes.Style.BOLD, 7f, tx, ty + 9, slot.summoned() ? MUTED : i == hovered ? TITLE : INK);
            if (slot.summoned()) {
                HiRes.centered(g, tr("screen.hogwartsmod.creature_wheel.summoned"), HiRes.Style.ITALIC, 6.5f, tx, ty + 17, MUTED);
            }
        }
        Slot current = hovered >= 0 && hovered < slots.size() ? slots.get(hovered) : null;
        if (current != null) {
            HiRes.centered(g, HiRes.ellipsize(current.entry.name(), HiRes.Style.BOLD, INNER * 2 - 8), HiRes.Style.BOLD, 8f, cx, cy - 18, TITLE);
            String species = current.entry.species().getDisplayName().getString();
            if (!species.equals(current.entry.name())) {
                HiRes.centered(g, species, HiRes.Style.ITALIC, 6.5f, cx, cy - 9, MUTED);
            }
            float ratio = current.entry.maxHealth() <= 0 ? 1f : current.entry.health() / current.entry.maxHealth();
            HiRes.bar(g, cx - INNER + 10, cy, INNER * 2 - 20, 4, ratio, 0xFFC4AA7C, GREEN);
            String hint = current.summoned() ? tr("screen.hogwartsmod.creature_wheel.summoned") : tr("screen.hogwartsmod.creature_wheel.release");
            List<String> lines = HiRes.wrap(hint, HiRes.Style.ITALIC, INNER * 2 - 16);
            for (int i = 0; i < Math.min(2, lines.size()); i++) {
                HiRes.centered(g, lines.get(i), HiRes.Style.ITALIC, 6f, cx, cy + 8 + i * 7, MUTED);
            }
        } else {
            List<String> lines = HiRes.wrap(tr("screen.hogwartsmod.creature_wheel.cancel"), HiRes.Style.ITALIC, INNER * 2 - 12);
            for (int i = 0; i < lines.size(); i++) {
                HiRes.centered(g, lines.get(i), HiRes.Style.ITALIC, 6.5f, cx, cy - lines.size() * 4 + i * 8, MUTED);
            }
        }
        HiRes.centered(g, title.getString(), HiRes.Style.BOLD, 8f, cx, cy + OUTER + 12, 0xFFECDCB6);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private int sliceAt(double dx, double dy) {
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < INNER || dist > OUTER + 30 || slots.isEmpty()) {
            return -1;
        }
        double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90;
        if (angle < 0) {
            angle += 360;
        }
        return Math.min(slots.size() - 1, (int) (angle / (360.0 / slots.size())));
    }

    private void redraw() {
        int size = OUTER * 2 + 8;
        BufferedImage[] holder = new BufferedImage[1];
        Graphics2D g = canvas.begin(holder, size, size);
        float s = (float) canvas.scale();
        float c = size / 2f * s;
        float ro = OUTER * s;
        float ri = INNER * s;
        int n = Math.max(1, slots.size());
        Ellipse2D inner = new Ellipse2D.Float(c - ri, c - ri, ri * 2, ri * 2);
        g.setColor(new Color(0x20, 0x14, 0x0C, 120));
        g.fill(new Ellipse2D.Float(c - ro - 3 * s, c - ro - 3 * s, (ro + 3 * s) * 2, (ro + 3 * s) * 2));
        for (int i = 0; i < n; i++) {
            double start = 90 - i * 360.0 / n;
            Area slice = new Area(new Arc2D.Float(c - ro, c - ro, ro * 2, ro * 2, (float) start, (float) (-360.0 / n), Arc2D.PIE));
            slice.subtract(new Area(inner));
            boolean on = i == hovered;
            boolean available = i < slots.size() && !slots.get(i).summoned();
            g.setColor(on && available ? new Color(0xFFF4DC) : available ? new Color(0xECDCB6) : new Color(0xC8B894));
            g.fill(slice);
            g.setStroke(new BasicStroke(1.2f * s));
            g.setColor(new Color(0x5C3A22));
            if (n > 1) {
                g.draw(slice);
            }
            if (on && available && n > 1) {
                g.setStroke(new BasicStroke(2.2f * s));
                g.setColor(new Color(0xBE8C28));
                g.draw(slice);
            }
        }
        g.setColor(new Color(0xF4E8C8));
        g.fill(inner);
        g.setStroke(new BasicStroke(1.6f * s));
        g.setColor(new Color(0xBE8C28));
        g.draw(inner);
        g.setColor(new Color(0x5C3A22));
        g.draw(new Ellipse2D.Float(c - ro, c - ro, ro * 2, ro * 2));
        g.dispose();
        canvas.end(holder[0]);
    }

    // -- Interação

    @Override
    public void tick() {
        if (minecraft == null || released) {
            return;
        }
        boolean down = GLFW.glfwGetMouseButton(minecraft.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS
;
        if (!down) {
            select();
        }
    }

    public void select() {
        released = true;
        Slot choice = hovered >= 0 && hovered < slots.size() ? slots.get(hovered) : null;
        if (choice != null && !choice.summoned()) {
            ModNetwork.CHANNEL.sendToServer(new SummonCreaturePacket(choice.entry.species().getId()));
        }
        onClose();
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 1) {
            select();
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            select();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void removed() {
        canvas.close();
    }

    private static String tr(String key) {
        return Component.translatable(key).getString();
    }
}
