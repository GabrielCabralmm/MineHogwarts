package com.gabriel.hogwartsmod.client.fx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class GlowDraw {

    private GlowDraw() {
    }

    public static void vertex(VertexConsumer vc, Matrix4f m, Vec3 p, float r, float g, float b, float a) {
        vc.vertex(m, (float) p.x, (float) p.y, (float) p.z).color(r, g, b, a).endVertex();
    }

    public static void quad(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                            float[] colA, float[] colB, float[] colC, float[] colD) {
        vertex(vc, m, a, colA[0], colA[1], colA[2], colA[3]);
        vertex(vc, m, b, colB[0], colB[1], colB[2], colB[3]);
        vertex(vc, m, c, colC[0], colC[1], colC[2], colC[3]);
        vertex(vc, m, d, colD[0], colD[1], colD[2], colD[3]);
        vertex(vc, m, d, colD[0], colD[1], colD[2], colD[3]);
        vertex(vc, m, c, colC[0], colC[1], colC[2], colC[3]);
        vertex(vc, m, b, colB[0], colB[1], colB[2], colB[3]);
        vertex(vc, m, a, colA[0], colA[1], colA[2], colA[3]);
    }

    public static void ribbon(VertexConsumer vc, Matrix4f m, Vec3 head, Vec3 tail, Vec3 side, float width,
                              float r, float g, float b, float headAlpha, float tailAlpha) {
        Vec3 off = side.scale(width);
        Vec3 tailOff = side.scale(width * 0.35);
        float[] h = {r, g, b, headAlpha};
        float[] t = {r, g, b, tailAlpha};
        quad(vc, m, head.add(off), head.subtract(off), tail.subtract(tailOff), tail.add(tailOff), h, h, t, t);
    }

    public static void disc(VertexConsumer vc, Matrix4f m, Vec3 center, Vec3 left, Vec3 up, float radius,
                            float r, float g, float b, float alpha) {
        int segments = 16;
        float[] in = {r, g, b, alpha};
        float[] out = {r, g, b, 0f};
        for (int i = 0; i < segments; i++) {
            double a0 = i * Math.PI * 2 / segments;
            double a1 = (i + 1) * Math.PI * 2 / segments;
            Vec3 p0 = center.add(left.scale(Math.cos(a0) * radius)).add(up.scale(Math.sin(a0) * radius));
            Vec3 p1 = center.add(left.scale(Math.cos(a1) * radius)).add(up.scale(Math.sin(a1) * radius));
            quad(vc, m, center, center, p1, p0, in, in, out, out);
        }
    }
}
