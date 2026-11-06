package com.gabriel.hogwartsmod.zone;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.WeakHashMap;

public final class ZoneShape {
    private static final Map<Zone, ZoneShape> CACHE = new WeakHashMap<>();

    private final int[] xs;
    private final int[] ys;
    private final int[] zs;
    private final boolean[][][] filled;
    private long volume = -1;
    private List<double[]> edges;

    private ZoneShape(Zone zone) {
        TreeSet<Integer> sx = new TreeSet<>();
        TreeSet<Integer> sy = new TreeSet<>();
        TreeSet<Integer> sz = new TreeSet<>();
        for (Zone.Box box : zone.boxes()) {
            sx.add(box.min().getX());
            sx.add(box.max().getX() + 1);
            sy.add(box.min().getY());
            sy.add(box.max().getY() + 1);
            sz.add(box.min().getZ());
            sz.add(box.max().getZ() + 1);
        }
        xs = sx.stream().mapToInt(Integer::intValue).toArray();
        ys = sy.stream().mapToInt(Integer::intValue).toArray();
        zs = sz.stream().mapToInt(Integer::intValue).toArray();
        filled = new boolean[xs.length - 1][ys.length - 1][zs.length - 1];
        for (int i = 0; i < xs.length - 1; i++) {
            for (int j = 0; j < ys.length - 1; j++) {
                for (int k = 0; k < zs.length - 1; k++) {
                    Vec3 center = new Vec3((xs[i] + xs[i + 1]) / 2.0, (ys[j] + ys[j + 1]) / 2.0, (zs[k] + zs[k + 1]) / 2.0);
                    for (Zone.Box box : zone.boxes()) {
                        if (box.contains(center)) {
                            filled[i][j][k] = true;
                            break;
                        }
                    }
                }
            }
        }
    }

    public static synchronized ZoneShape of(Zone zone) {
        return CACHE.computeIfAbsent(zone, ZoneShape::new);
    }

    private boolean at(int i, int j, int k) {
        return i >= 0 && j >= 0 && k >= 0 && i < filled.length && j < filled[0].length && k < filled[0][0].length && filled[i][j][k];
    }

    public long volume() {
        if (volume < 0) {
            long total = 0;
            for (int i = 0; i < xs.length - 1; i++) {
                for (int j = 0; j < ys.length - 1; j++) {
                    for (int k = 0; k < zs.length - 1; k++) {
                        if (filled[i][j][k]) {
                            total += (long) (xs[i + 1] - xs[i]) * (ys[j + 1] - ys[j]) * (zs[k + 1] - zs[k]);
                        }
                    }
                }
            }
            volume = total;
        }
        return volume;
    }

    private static boolean isEdge(boolean a, boolean b, boolean c, boolean d) {
        int n = (a ? 1 : 0) + (b ? 1 : 0) + (c ? 1 : 0) + (d ? 1 : 0);
        if (n == 1 || n == 3) {
            return true;
        }
        return n == 2 && a == d;
    }

    public List<double[]> edges() {
        if (edges != null) {
            return edges;
        }
        List<double[]> list = new ArrayList<>();
        for (int i = 0; i < xs.length - 1; i++) {
            for (int j = 0; j < ys.length; j++) {
                for (int k = 0; k < zs.length; k++) {
                    if (isEdge(at(i, j - 1, k - 1), at(i, j, k - 1), at(i, j - 1, k), at(i, j, k))) {
                        list.add(new double[]{xs[i], ys[j], zs[k], xs[i + 1], ys[j], zs[k]});
                    }
                }
            }
        }
        for (int j = 0; j < ys.length - 1; j++) {
            for (int i = 0; i < xs.length; i++) {
                for (int k = 0; k < zs.length; k++) {
                    if (isEdge(at(i - 1, j, k - 1), at(i, j, k - 1), at(i - 1, j, k), at(i, j, k))) {
                        list.add(new double[]{xs[i], ys[j], zs[k], xs[i], ys[j + 1], zs[k]});
                    }
                }
            }
        }
        for (int k = 0; k < zs.length - 1; k++) {
            for (int i = 0; i < xs.length; i++) {
                for (int j = 0; j < ys.length; j++) {
                    if (isEdge(at(i - 1, j - 1, k), at(i, j - 1, k), at(i - 1, j, k), at(i, j, k))) {
                        list.add(new double[]{xs[i], ys[j], zs[k], xs[i], ys[j], zs[k + 1]});
                    }
                }
            }
        }
        edges = list;
        return list;
    }
}
