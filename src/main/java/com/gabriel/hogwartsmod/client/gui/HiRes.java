package com.gabriel.hogwartsmod.client.gui;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class HiRes {
    public enum Style {
        REGULAR("dejavu_serif.ttf"),
        BOLD("dejavu_serif_bold.ttf"),
        ITALIC("dejavu_serif_italic.ttf");

        private final String file;

        Style(String file) {
            this.file = file;
        }
    }

    public static final float TEXT_SIZE = 8.5f;
    private static final int CACHE_LIMIT = 400;

    private static final Map<Style, Font> FONTS = new EnumMap<>(Style.class);
    private static final Map<String, BufferedImage> SOURCES = new HashMap<>();
    private static final Map<String, Entry> CACHE = new LinkedHashMap<>(64, 0.75f, true);
    private static final BufferedImage MEASURE = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    private static int counter;

    private record Entry(ResourceLocation id, DynamicTexture texture, int pixelW, int pixelH, float guiW, float guiH, float offsetY) {
    }

    private HiRes() {
    }

    private static double scale() {
        return Minecraft.getInstance().getWindow().getGuiScale();
    }

    // -- Imagens

    public static void image(GuiGraphics g, ResourceLocation source, int x, int y, int w, int h) {
        double scale = scale();
        int pw = Math.max(1, (int) Math.round(w * scale));
        int ph = Math.max(1, (int) Math.round(h * scale));
        String key = "img|" + source + "|" + pw + "x" + ph;
        Entry entry = CACHE.get(key);
        if (entry == null) {
            BufferedImage src = loadSource(source);
            if (src == null) {
                return;
            }
            entry = upload(key, resize(src, pw, ph), w, h, 0);
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(entry.id(), x, y, w, h, 0, 0, entry.pixelW(), entry.pixelH(), entry.pixelW(), entry.pixelH());
        RenderSystem.disableBlend();
    }

    private static BufferedImage loadSource(ResourceLocation source) {
        String key = source.toString();
        if (SOURCES.containsKey(key)) {
            return SOURCES.get(key);
        }
        BufferedImage image = null;
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(source);
        if (resource.isPresent()) {
            try (InputStream in = resource.get().open()) {
                image = ImageIO.read(in);
            } catch (Exception e) {
                HogwartsMod.LOGGER.warn("Falha ao carregar {}", source, e);
            }
        }
        SOURCES.put(key, image);
        return image;
    }

    private static BufferedImage resize(BufferedImage src, int w, int h) {
        BufferedImage current = toArgb(src);
        while (current.getWidth() / 2 >= w && current.getHeight() / 2 >= h) {
            current = draw(current, current.getWidth() / 2, current.getHeight() / 2);
        }
        return draw(current, w, h);
    }

    private static BufferedImage toArgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_ARGB) {
            return src;
        }
        return draw(src, src.getWidth(), src.getHeight());
    }

    private static BufferedImage draw(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    // -- Texto

    private static Font font(Style style, float pixelSize) {
        Font base = FONTS.computeIfAbsent(style, s -> {
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "font/" + s.file);
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(location);
            try (InputStream in = resource.orElseThrow().open()) {
                return Font.createFont(Font.TRUETYPE_FONT, in);
            } catch (Exception e) {
                HogwartsMod.LOGGER.warn("Falha ao carregar a fonte {}", location, e);
                return new Font(Font.SERIF, s == Style.BOLD ? Font.BOLD : s == Style.ITALIC ? Font.ITALIC : Font.PLAIN, 12);
            }
        });
        return base.deriveFont(pixelSize);
    }

    private static Graphics2D configure(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        return g;
    }

    public static float width(String text, Style style) {
        return width(text, style, TEXT_SIZE);
    }

    public static float width(String text, Style style, float size) {
        float scale = (float) scale();
        Graphics2D g = configure(MEASURE.createGraphics());
        FontMetrics metrics = g.getFontMetrics(font(style, size * scale));
        float w = (float) metrics.getStringBounds(text, g).getWidth();
        g.dispose();
        return w / scale;
    }

    public static float lineHeight(Style style, float size) {
        float scale = (float) scale();
        Graphics2D g = configure(MEASURE.createGraphics());
        FontMetrics metrics = g.getFontMetrics(font(style, size * scale));
        float h = metrics.getAscent() + metrics.getDescent();
        g.dispose();
        return h / scale;
    }

    public static void text(GuiGraphics g, String text, Style style, float x, float y, int color) {
        text(g, text, style, TEXT_SIZE, x, y, color);
    }

    public static void text(GuiGraphics g, String text, Style style, float size, float x, float y, int color) {
        if (text.isEmpty()) {
            return;
        }
        double scale = scale();
        String key = "txt|" + style + "|" + size + "|" + color + "|" + scale + "|" + text;
        Entry entry = CACHE.get(key);
        if (entry == null) {
            Font font = font(style, (float) (size * scale));
            Graphics2D mg = configure(MEASURE.createGraphics());
            FontMetrics metrics = mg.getFontMetrics(font);
            int pad = (int) Math.ceil(scale);
            int pw = (int) Math.ceil(metrics.getStringBounds(text, mg).getWidth()) + pad * 2;
            int ph = metrics.getAscent() + metrics.getDescent() + pad * 2;
            mg.dispose();
            BufferedImage image = new BufferedImage(pw, ph, BufferedImage.TYPE_INT_ARGB);
            Graphics2D tg = configure(image.createGraphics());
            tg.setFont(font);
            tg.setColor(new Color(color, true));
            tg.drawString(text, pad, pad + metrics.getAscent());
            tg.dispose();
            entry = upload(key, image, (float) (pw / scale), (float) (ph / scale), (float) (pad / scale));
        }
        g.pose().pushPose();
        g.pose().translate(x - entry.offsetY(), y - entry.offsetY(), 0);
        g.pose().scale((float) (1 / scale), (float) (1 / scale), 1f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(entry.id(), 0, 0, 0, 0, entry.pixelW(), entry.pixelH(), entry.pixelW(), entry.pixelH());
        RenderSystem.disableBlend();
        g.pose().popPose();
    }

    public static void centered(GuiGraphics g, String text, Style style, float cx, float y, int color) {
        text(g, text, style, cx - width(text, style) / 2f, y, color);
    }

    public static void centered(GuiGraphics g, String text, Style style, float size, float cx, float y, int color) {
        text(g, text, style, size, cx - width(text, style, size) / 2f, y, color);
    }

    public static void right(GuiGraphics g, String text, Style style, float rx, float y, int color) {
        text(g, text, style, rx - width(text, style), y, color);
    }

    public static List<String> wrap(String text, Style style, float maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (width(candidate, style) > maxWidth && !line.isEmpty()) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    public static String ellipsize(String text, Style style, float maxWidth) {
        if (width(text, style) <= maxWidth) {
            return text;
        }
        String cut = text;
        while (!cut.isEmpty() && width(cut + "…", style) > maxWidth) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut.stripTrailing() + "…";
    }

    // -- Cache de texturas

    private static Entry upload(String key, BufferedImage image, float guiW, float guiH, float offset) {
        int w = image.getWidth();
        int h = image.getHeight();
        NativeImage nativeImage = new NativeImage(w, h, false);
        copy(image, nativeImage);
        DynamicTexture texture = new DynamicTexture(nativeImage);
        texture.setFilter(true, false);
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "hires/" + (counter++));
        Minecraft.getInstance().getTextureManager().register(id, texture);
        Entry entry = new Entry(id, texture, w, h, guiW, guiH, offset);
        CACHE.put(key, entry);
        if (CACHE.size() > CACHE_LIMIT) {
            var it = CACHE.entrySet().iterator();
            Map.Entry<String, Entry> eldest = it.next();
            it.remove();
            Minecraft.getInstance().getTextureManager().release(eldest.getValue().id());
        }
        return entry;
    }

    private static void copy(BufferedImage image, NativeImage target) {
        int w = Math.min(image.getWidth(), target.getWidth());
        int h = Math.min(image.getHeight(), target.getHeight());
        int[] row = new int[w];
        for (int y = 0; y < h; y++) {
            image.getRGB(0, y, w, 1, row, 0, w);
            for (int x = 0; x < w; x++) {
                int argb = row[x];
                target.setPixelRGBA(x, y, (argb & 0xFF00FF00) | ((argb >> 16) & 0xFF) | ((argb & 0xFF) << 16));
            }
        }
    }

    // -- Tela de desenho livre

    public static final class Canvas {
        @org.jetbrains.annotations.Nullable
        private DynamicTexture texture;
        @org.jetbrains.annotations.Nullable
        private ResourceLocation id;
        private int pixelW;
        private int pixelH;

        public double scale() {
            return HiRes.scale();
        }

        public Graphics2D begin(BufferedImage[] out, int w, int h) {
            int pw = Math.max(1, (int) Math.round(w * scale()));
            int ph = Math.max(1, (int) Math.round(h * scale()));
            out[0] = new BufferedImage(pw, ph, BufferedImage.TYPE_INT_ARGB);
            return configure(out[0].createGraphics());
        }

        public void end(BufferedImage image) {
            if (texture == null || pixelW != image.getWidth() || pixelH != image.getHeight()) {
                close();
                pixelW = image.getWidth();
                pixelH = image.getHeight();
                texture = new DynamicTexture(pixelW, pixelH, true);
                texture.setFilter(true, false);
                id = ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "hires/canvas_" + (counter++));
                Minecraft.getInstance().getTextureManager().register(id, texture);
            }
            NativeImage pixels = texture.getPixels();
            if (pixels != null) {
                copy(image, pixels);
                texture.upload();
            }
        }

        public void draw(GuiGraphics g, int x, int y, int w, int h) {
            if (id == null) {
                return;
            }
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            g.blit(id, x, y, w, h, 0, 0, pixelW, pixelH, pixelW, pixelH);
            RenderSystem.disableBlend();
        }

        public void close() {
            if (id != null) {
                Minecraft.getInstance().getTextureManager().release(id);
            }
            id = null;
            texture = null;
        }
    }
}
