package com.gabriel.hogwartsmod.client.render;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.house.door.HouseDoorBlock;
import com.gabriel.hogwartsmod.house.door.HouseDoorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public final class SlytherinSerpent {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "textures/block/porta/sonserina_cobra.png");
    private static final float YS = 36, RC = 29, ZC = -12.2f;
    private static final float BODY = 176;
    private static final float T0 = 0.5f, TH = 3.5f, LF = 3.2f;
    private static final ResourceLocation DETAIL = ResourceLocation.fromNamespaceAndPath(HogwartsMod.MOD_ID, "textures/block/porta/sonserina_cobra_detalhes.png");

    private static final float[] PX, PY, PZ, ACC, TOPX, TOPY, TOPZ;
    private static final float S0, LEN, S_END_WALL;

    // -- Caminho
    static {
        List<float[]> pts = new ArrayList<>();
        for (int y = -260; y < 0; y += 4) {
            pts.add(new float[]{-RC, y, ZC});
        }
        for (int y = 0; y < YS; y += 3) {
            pts.add(new float[]{-RC, y, ZC});
        }
        for (int k = 0; k <= 60; k++) {
            double a = Math.PI - Math.PI * k / 60;
            pts.add(new float[]{(float) (RC * Math.cos(a)), (float) (YS + RC * Math.sin(a)), ZC});
        }
        for (int y = (int) YS - 3; y > 13; y -= 3) {
            pts.add(new float[]{RC, y, ZC});
        }
        float[][] tail = {{RC + 0.5f, 11, ZC}, {RC + 2.5f, 7.5f, ZC - 0.5f}, {RC + 6, 6.5f, ZC - 1}, {RC + 9, 9, ZC - 1.5f},
                {RC + 10.5f, 14, ZC - 2.5f}, {RC + 10.5f, 19, ZC - 4.5f}, {RC + 10, 23, ZC - 7.5f}, {RC + 9.8f, 25, ZC - 11.5f}, {RC + 9.6f, 24.5f, ZC - 15.5f}, {RC + 9.4f, 23.5f, ZC - 19}};
        for (float[] p : tail) {
            pts.add(p);
        }
        List<float[]> seg = new ArrayList<>();
        seg.add(pts.get(0));
        for (int i = 1; i < pts.size(); i++) {
            float[] q = seg.get(seg.size() - 1), p = pts.get(i);
            int n = Math.max(1, (int) (dist(p, q) / 0.5f));
            for (int k = 1; k <= n; k++) {
                float f = (float) k / n;
                seg.add(new float[]{q[0] + (p[0] - q[0]) * f, q[1] + (p[1] - q[1]) * f, q[2] + (p[2] - q[2]) * f});
            }
        }
        int n = seg.size();
        float[][] cur = seg.toArray(new float[0][]);
        for (int pass = 0; pass < 40; pass++) {
            float[][] next = new float[n][];
            next[0] = cur[0];
            next[n - 1] = cur[n - 1];
            for (int i = 1; i < n - 1; i++) {
                next[i] = new float[3];
                for (int j = 0; j < 3; j++) {
                    next[i][j] = (cur[i - 1][j] + 2 * cur[i][j] + cur[i + 1][j]) / 4;
                }
            }
            cur = next;
        }
        PX = new float[n];
        PY = new float[n];
        PZ = new float[n];
        ACC = new float[n];
        for (int i = 0; i < n; i++) {
            PX[i] = cur[i][0];
            PY[i] = cur[i][1];
            PZ[i] = cur[i][2];
            if (i > 0) {
                ACC[i] = ACC[i - 1] + dist(cur[i], cur[i - 1]);
            }
        }
        float s0 = 0, sEnd = 0;
        for (int i = 0; i < n; i++) {
            if (PY[i] >= 0) {
                s0 = ACC[i];
                break;
            }
        }
        for (int i = 0; i < n; i++) {
            if (PZ[i] < ZC - 0.5f) {
                sEnd = ACC[i];
                break;
            }
        }
        S0 = s0;
        S_END_WALL = sEnd;
        LEN = ACC[n - 1];
        TOPX = new float[n];
        TOPY = new float[n];
        TOPZ = new float[n];
        Vector3f top = new Vector3f(0, 0, -1);
        for (int i = 0; i < n; i++) {
            Vector3f t = tangentAt(i);
            top.sub(new Vector3f(t).mul(top.dot(t))).normalize();
            float u = smooth((ACC[i] - (S_END_WALL - 14)) / 26f);
            Vector3f blended = new Vector3f(top).mul(1 - u).add(0, u, 0);
            blended.sub(new Vector3f(t).mul(blended.dot(t))).normalize();
            TOPX[i] = blended.x;
            TOPY[i] = blended.y;
            TOPZ[i] = blended.z;
        }
    }

    private SlytherinSerpent() {
    }

    private static float dist(float[] a, float[] b) {
        float dx = a[0] - b[0], dy = a[1] - b[1], dz = a[2] - b[2];
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static float smooth(float u) {
        u = Math.max(0, Math.min(1, u));
        return u * u * (3 - 2 * u);
    }

    private static Vector3f tangentAt(int i) {
        int a = Math.max(0, i - 2), b = Math.min(PX.length - 1, i + 2);
        return new Vector3f(PX[b] - PX[a], PY[b] - PY[a], PZ[b] - PZ[a]).normalize();
    }

    private static int index(float s) {
        int lo = 0, hi = ACC.length - 1;
        while (hi - lo > 1) {
            int mid = (lo + hi) >>> 1;
            if (ACC[mid] <= s) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return lo;
    }

    // -- Animação
    private static float headOpening(float t) {
        float u = Math.max(0, Math.min(1, (t - T0) / (TH - T0)));
        float e = u * u * (3 - 2 * u) * 0.6f + u * 0.4f;
        return S0 + (LEN - S0) * e;
    }

    private static float headClosing(float t) {
        float u = Math.max(0, Math.min(1, (t - 0.4f) / (LF - 0.5f)));
        return LEN - (LEN - S0 + BODY + 4) * (u * u * (3 - 2 * u) * 0.3f + u * 0.7f);
    }

    private static float radius(float d) {
        if (d < 1.5f) {
            return 1.6f + d * 0.8f;
        }
        if (d < 9) {
            return 2.8f + (d - 1.5f) / 7.5f * 2.2f;
        }
        if (d < 12.5f) {
            return 5.0f - (d - 9) / 3.5f * 2.1f;
        }
        if (d < 40) {
            return 2.9f + (d - 12.5f) / 27.5f * 1.1f;
        }
        float f = (d - 40) / (BODY - 40);
        return Math.max(0.4f, 3.9f - (float) Math.pow(f, 1.4f) * 3.5f);
    }

    public static void render(HouseDoorBlockEntity door, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        boolean open = door.getBlockState().getValue(HouseDoorBlock.OPEN);
        float t = door.secondsSinceChange(partialTick);
        float head;
        float wave;
        boolean moving;
        if (open) {
            head = headOpening(t);
            moving = t < TH;
            wave = t * 5f;
        } else {
            if (t > LF) {
                return;
            }
            head = headClosing(t);
            moving = true;
            wave = -t * 5f;
        }
        if (head - BODY > LEN) {
            return;
        }
        Direction facing = door.getBlockState().getValue(HouseDoorBlock.FACING);
        Direction right = facing.getCounterClockWise();
        Direction back = facing.getOpposite();
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        Matrix4f basis = new Matrix4f(
                right.getStepX() / 16f, right.getStepY() / 16f, right.getStepZ() / 16f, 0,
                0, 1 / 16f, 0, 0,
                back.getStepX() / 16f, back.getStepY() / 16f, back.getStepZ() / 16f, 0,
                0, 0, 0, 1);
        pose.mulPoseMatrix(basis);
        Matrix4f matrix = pose.last().pose();
        Matrix3f normals = new Matrix3f(pose.last().normal());
        normals.scale(16f);
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        long time = door.getLevel() == null ? 0 : door.getLevel().getGameTime();
        float tongue = (open && (time / 3) % 6 < 2) ? 1 : 0;
        drawBody(buffer, matrix, normals, light, head, wave, moving);
        drawFace(buffers, matrix, normals, light, head, wave, moving, tongue);
        pose.popPose();
    }

    private record Frame(Vector3f p, Vector3f t, Vector3f up, Vector3f side) {
    }

    private static Frame frame(float s, float head, float wave, boolean moving) {
        s = Math.max(0, Math.min(LEN, s));
        int i = index(s);
        int j = Math.min(i + 1, ACC.length - 1);
        float f = j == i ? 0 : (s - ACC[i]) / Math.max(1e-5f, ACC[j] - ACC[i]);
        Vector3f p = new Vector3f(PX[i] + (PX[j] - PX[i]) * f, PY[i] + (PY[j] - PY[i]) * f, PZ[i] + (PZ[j] - PZ[i]) * f);
        Vector3f t = tangentAt(i);
        Vector3f up = new Vector3f(TOPX[i], TOPY[i], TOPZ[i]);
        up.sub(new Vector3f(t).mul(up.dot(t))).normalize();
        Vector3f side = new Vector3f(t).cross(up).normalize();
        float d = head - s;
        float damp = smooth((S_END_WALL - s) / 20f) * smooth(d / 14f) * smooth((d - 2) / 10f);
        float amp = moving ? 1.6f : 0.5f;
        p.add(new Vector3f(side).mul((float) Math.sin(s * 0.2f - wave) * amp * damp));
        return new Frame(p, t, up, side);
    }

    private static final float TW = 32, THT = 16;

    private static void drawBody(VertexConsumer buffer, Matrix4f matrix, Matrix3f normals, int light, float head, float wave, boolean moving) {
        for (float d = 10; d < BODY; d += 4) {
            float s = head - d;
            if (s < S0 + 1) {
                continue;
            }
            Frame f = frame(s, head, wave, moving);
            float r = radius(d);
            float hw = Math.max(0.5f, Math.round(r * 2) / 2f);
            float hh = Math.max(0.5f, Math.round(r * 1.6f) / 2f);
            float v0 = (d % 16);
            box(buffer, matrix, normals, light, f, 2.6f, hh, hw, 0, v0, v0 + 4);
        }
        Frame neck = frame(head - 9, head, wave, moving);
        box(buffer, matrix, normals, light, neck, 2.0f, 2.5f, 3.0f, 0, 0, 4);
        Frame hf = frame(head - 4, head, wave, moving);
        Frame main = new Frame(new Vector3f(hf.p).sub(new Vector3f(hf.t).mul(1.5f)), hf.t, hf.up, hf.side);
        box(buffer, matrix, normals, light, main, 4.0f, 2.5f, 4.5f, 24, 0, 8);
        Frame snout = new Frame(new Vector3f(hf.p).add(new Vector3f(hf.t).mul(3.5f)).sub(new Vector3f(hf.up).mul(0.3f)), hf.t, hf.up, hf.side);
        box(buffer, matrix, normals, light, snout, 1.5f, 2.0f, 3.0f, 24, 8, 11);
    }

    private static void box(VertexConsumer buffer, Matrix4f matrix, Matrix3f normals, int light, Frame f,
                            float hl, float hh, float hw, int topX, float v0, float v1) {
        Vector3f t = new Vector3f(f.t), u = new Vector3f(f.up), w = new Vector3f(f.side);
        Vector3f c = f.p;
        Vector3f[] corner = new Vector3f[8];
        for (int i = 0; i < 8; i++) {
            float a = (i & 1) == 0 ? -hl : hl, b = (i & 2) == 0 ? -hh : hh, e = (i & 4) == 0 ? -hw : hw;
            corner[i] = new Vector3f(c).add(new Vector3f(t).mul(a)).add(new Vector3f(u).mul(b)).add(new Vector3f(w).mul(e));
        }
        float vt0 = v0 / THT, vt1 = v1 / THT;
        face(buffer, matrix, normals, light, corner[2], corner[3], corner[7], corner[6], u, topX, topX + 8, vt0, vt1);
        face(buffer, matrix, normals, light, corner[0], corner[1], corner[5], corner[4], new Vector3f(u).negate(), 16, 24, vt0, vt1);
        face(buffer, matrix, normals, light, corner[4], corner[5], corner[7], corner[6], w, 8, 16, vt0, vt1);
        face(buffer, matrix, normals, light, corner[0], corner[1], corner[3], corner[2], new Vector3f(w).negate(), 8, 16, vt0, vt1);
        face(buffer, matrix, normals, light, corner[1], corner[3], corner[7], corner[5], t, 8, 16, 0, 2 / THT);
        face(buffer, matrix, normals, light, corner[0], corner[2], corner[6], corner[4], new Vector3f(t).negate(), 8, 16, 0, 2 / THT);
    }

    private static void face(VertexConsumer buffer, Matrix4f matrix, Matrix3f normals, int light, Vector3f a, Vector3f b, Vector3f c, Vector3f d,
                             Vector3f n, float x0, float x1, float vt0, float vt1) {
        float u0 = x0 / TW, u1 = x1 / TW;
        quad(buffer, matrix, normals, light, a, n, u0, vt1, b, n, u0, vt0, c, n, u1, vt0, d, n, u1, vt1);
    }

    private static void drawFace(MultiBufferSource buffers, Matrix4f matrix, Matrix3f normals, int light, float head, float wave, boolean moving, float tongue) {
        if (head < S0 + 1) {
            return;
        }
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(DETAIL));
        Frame hf = frame(head - 4, head, wave, moving);
        for (int sgn = -1; sgn <= 1; sgn += 2) {
            Vector3f n = new Vector3f(hf.side).mul(sgn);
            Vector3f across = new Vector3f(hf.up);
            Vector3f side = new Vector3f(hf.p).add(new Vector3f(n).mul(4.53f)).add(new Vector3f(hf.t).mul(2.25f)).add(new Vector3f(hf.up).mul(1.5f));
            patch(buffer, matrix, normals, LightTexture.FULL_BRIGHT, side, n, hf.t, across, 0.25f, 0.6f, 8);
            Vector3f front = new Vector3f(hf.p).add(new Vector3f(hf.t).mul(2.53f)).add(new Vector3f(hf.side).mul(sgn * 3.9f)).add(new Vector3f(hf.up).mul(1.5f));
            patch(buffer, matrix, normals, LightTexture.FULL_BRIGHT, front, hf.t, hf.side, hf.up, 0.6f, 0.6f, 8);
            patch(buffer, matrix, normals, LightTexture.FULL_BRIGHT, new Vector3f(front).add(new Vector3f(hf.t).mul(0.02f)), hf.t, hf.side, hf.up, 0.15f, 0.6f, 24);
            Vector3f nostril = new Vector3f(hf.p).add(new Vector3f(hf.t).mul(5.05f)).add(new Vector3f(hf.side).mul(sgn * 1.3f)).add(new Vector3f(hf.up).mul(1.1f));
            patch(buffer, matrix, normals, light, nostril, hf.t, hf.side, hf.up, 0.3f, 0.25f, 56);
            Vector3f top = new Vector3f(hf.p).add(new Vector3f(hf.up).mul(2.55f)).add(new Vector3f(hf.side).mul(sgn * 2.5f)).add(new Vector3f(hf.t).mul(0.5f));
            patch(buffer, matrix, normals, light, top, hf.up, hf.t, hf.side, 1.5f, 0.5f, 56);
        }
        if (tongue > 0) {
            Vector3f n = new Vector3f(hf.up);
            Vector3f startP = new Vector3f(hf.p).add(new Vector3f(hf.t).mul(5.0f)).sub(new Vector3f(hf.up).mul(1.0f));
            Vector3f end = new Vector3f(startP).add(new Vector3f(hf.t).mul(3f));
            ribbon(buffer, matrix, normals, light, startP, end, hf.side, 0.5f, n);
            for (int sgn = -1; sgn <= 1; sgn += 2) {
                Vector3f fork = new Vector3f(end).add(new Vector3f(hf.t).mul(1.0f)).add(new Vector3f(hf.side).mul(sgn * 1.0f));
                ribbon(buffer, matrix, normals, light, end, fork, hf.side, 0.5f, n);
            }
        }
    }

    private static void patchTri(VertexConsumer buffer, Matrix4f matrix, Matrix3f normals, int light, Vector3f a, Vector3f b, Vector3f c, Vector3f n, int texX) {
        float u = (texX + 4) / 64f, v = 0.5f;
        quad(buffer, matrix, normals, light, a, n, u, v, a, n, u, v, b, n, u, v, c, n, u, v);
    }

    private static void patch(VertexConsumer buffer, Matrix4f matrix, Matrix3f normals, int light, Vector3f c, Vector3f n,
                              Vector3f along, Vector3f across, float halfA, float halfB, int texX) {
        float u = (texX + 4) / 64f, v = 0.5f;
        Vector3f a = new Vector3f(along).mul(halfA), b = new Vector3f(across).mul(halfB);
        Vector3f p0 = new Vector3f(c).sub(a).sub(b), p1 = new Vector3f(c).add(a).sub(b);
        Vector3f p2 = new Vector3f(c).add(a).add(b), p3 = new Vector3f(c).sub(a).add(b);
        quad(buffer, matrix, normals, light, p0, n, u, v, p1, n, u, v, p2, n, u, v, p3, n, u, v);
    }

    private static void ribbon(VertexConsumer buffer, Matrix4f matrix, Matrix3f normals, int light, Vector3f a, Vector3f b,
                               Vector3f side, float half, Vector3f n) {
        float u = 44 / 64f, v = 0.5f;
        Vector3f s = new Vector3f(side).mul(half);
        Vector3f up = new Vector3f(n).mul(half);
        quad(buffer, matrix, normals, light, new Vector3f(a).sub(s), n, u, v, new Vector3f(b).sub(s), n, u, v,
                new Vector3f(b).add(s), n, u, v, new Vector3f(a).add(s), n, u, v);
        quad(buffer, matrix, normals, light, new Vector3f(a).sub(up), side, u, v, new Vector3f(b).sub(up), side, u, v,
                new Vector3f(b).add(up), side, u, v, new Vector3f(a).add(up), side, u, v);
    }

    private static void quad(VertexConsumer buffer, Matrix4f matrix, Matrix3f normals, int light,
                             Vector3f p0, Vector3f n0, float u0, float v0, Vector3f p1, Vector3f n1, float u1, float v1,
                             Vector3f p2, Vector3f n2, float u2, float v2, Vector3f p3, Vector3f n3, float u3, float v3) {
        vertex(buffer, matrix, normals, light, p0, n0, u0, v0);
        vertex(buffer, matrix, normals, light, p1, n1, u1, v1);
        vertex(buffer, matrix, normals, light, p2, n2, u2, v2);
        vertex(buffer, matrix, normals, light, p3, n3, u3, v3);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f matrix, Matrix3f normals, int light, Vector3f p, Vector3f n, float u, float v) {
        Vector3f nn = normals.transform(new Vector3f(n)).normalize();
        buffer.vertex(matrix, p.x, p.y, p.z).color(255, 255, 255, 255).uv(u, v)
                .overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(light).normal(nn.x, nn.y, nn.z).endVertex();
    }
}
