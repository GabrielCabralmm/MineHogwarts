package com.gabriel.hogwartsmod.client.trace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TracePatterns {
    public static final int SAMPLES = 100;
    private static final Map<String, float[][]> PATTERNS = new HashMap<>();

    static {
        PATTERNS.put("lumos", new Path(0.5f, 0.92f).line(0.5f, 0.44f).arc(0.5f, 0.29f, 0.15f, 90, -360).build());
        PATTERNS.put("protego", new Path(0.14f, 0.62f).arc(0.5f, 0.62f, 0.36f, 180, 180).line(0.5f, 0.94f).build());
        PATTERNS.put("expelliarmus", new Path(0.16f, 0.2f).line(0.82f, 0.36f).line(0.2f, 0.62f).line(0.86f, 0.84f).build());
        PATTERNS.put("stupefy", new Path(0.64f, 0.07f).line(0.3f, 0.52f).line(0.64f, 0.47f).line(0.34f, 0.94f).build());
        PATTERNS.put("accio", spiral());
        PATTERNS.put("incendio", flame());
        PATTERNS.put("arresto_momentum", wave());
    }

    private TracePatterns() {
    }

    public static float[][] get(String spell) {
        float[][] pattern = PATTERNS.get(spell);
        return pattern != null ? pattern : new Path(0.5f, 0.15f).arc(0.5f, 0.5f, 0.35f, 270, 360).build();
    }

    private static float[][] spiral() {
        Path path = new Path(0.88f, 0.5f);
        for (int i = 1; i <= 180; i++) {
            double t = i / 180.0;
            double angle = -t * Math.PI * 3;
            double r = 0.38 - 0.32 * t;
            path.line((float) (0.5 + Math.cos(angle) * r), (float) (0.5 + Math.sin(angle) * r));
        }
        return path.build();
    }

    private static float[][] flame() {
        Path path = new Path(0.5f, 0.94f);
        for (int i = 1; i <= 120; i++) {
            double t = i / 120.0;
            double x = 0.5 + 0.22 * Math.sin(t * Math.PI * 3) * (1 - 0.35 * t);
            path.line((float) x, (float) (0.94 - 0.86 * t));
        }
        return path.build();
    }

    private static float[][] wave() {
        Path path = new Path(0.1f, 0.4f);
        for (int i = 1; i <= 120; i++) {
            double t = i / 120.0;
            double y = 0.4 + 0.16 * Math.sin(t * Math.PI * 2.5) * (1 - 0.6 * t);
            path.line((float) (0.1 + 0.8 * t), (float) y);
        }
        return path.line(0.9f, 0.88f).line(0.1f, 0.88f).build();
    }

    private static final class Path {
        private final List<float[]> points = new ArrayList<>();

        Path(float x, float y) {
            points.add(new float[]{x, y});
        }

        Path line(float x, float y) {
            float[] last = points.get(points.size() - 1);
            int steps = Math.max(1, (int) Math.ceil(Math.hypot(x - last[0], y - last[1]) / 0.01));
            for (int i = 1; i <= steps; i++) {
                float t = i / (float) steps;
                points.add(new float[]{last[0] + (x - last[0]) * t, last[1] + (y - last[1]) * t});
            }
            return this;
        }

        Path arc(float cx, float cy, float r, float startDeg, float sweepDeg) {
            int steps = Math.max(8, (int) Math.abs(sweepDeg / 3));
            for (int i = 0; i <= steps; i++) {
                double a = Math.toRadians(startDeg + sweepDeg * i / steps);
                line((float) (cx + Math.cos(a) * r), (float) (cy + Math.sin(a) * r));
            }
            return this;
        }

        float[][] build() {
            double total = 0;
            double[] acc = new double[points.size()];
            for (int i = 1; i < points.size(); i++) {
                float[] a = points.get(i - 1);
                float[] b = points.get(i);
                total += Math.hypot(b[0] - a[0], b[1] - a[1]);
                acc[i] = total;
            }
            float[][] out = new float[SAMPLES][2];
            int seg = 1;
            for (int s = 0; s < SAMPLES; s++) {
                double target = total * s / (SAMPLES - 1);
                while (seg < points.size() - 1 && acc[seg] < target) {
                    seg++;
                }
                float[] a = points.get(seg - 1);
                float[] b = points.get(seg);
                double len = acc[seg] - acc[seg - 1];
                float t = len <= 0 ? 0 : (float) ((target - acc[seg - 1]) / len);
                out[s][0] = a[0] + (b[0] - a[0]) * t;
                out[s][1] = a[1] + (b[1] - a[1]) * t;
            }
            return out;
        }
    }
}
