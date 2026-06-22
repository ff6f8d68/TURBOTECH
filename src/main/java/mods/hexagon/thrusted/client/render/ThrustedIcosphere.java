package mods.hexagon.thrusted.client.render;
import mods.hexagon.thrusted.Thrusted;

import java.util.*;

public class ThrustedIcosphere {

    private static final float PHI = (float) ((1.0 + Math.sqrt(5.0)) / 2.0);
    private static final MeshData[] cache = new MeshData[8];

    public static class MeshData {
        public final float[] verts;
        public final int[][] faces;
        public final int[][] faceNeighbors;
        public final float[] faceNormX;
        public final float[] faceNormY;
        public final float[] faceNormZ;

        MeshData(float[] verts, int[][] faces, int[][] faceNeighbors) {
            this.verts = verts;
            this.faces = faces;
            this.faceNeighbors = faceNeighbors;
            int n = faces.length;
            faceNormX = new float[n];
            faceNormY = new float[n];
            faceNormZ = new float[n];
            for (int fi = 0; fi < n; fi++) {
                int[] f = faces[fi];
                float ax = verts[f[0] * 3], ay = verts[f[0] * 3 + 1], az = verts[f[0] * 3 + 2];
                float bx = verts[f[1] * 3], by = verts[f[1] * 3 + 1], bz = verts[f[1] * 3 + 2];
                float cx = verts[f[2] * 3], cy = verts[f[2] * 3 + 1], cz = verts[f[2] * 3 + 2];
                float nx = (ax + bx + cx) / 3f;
                float ny = (ay + by + cy) / 3f;
                float nz = (az + bz + cz) / 3f;
                float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                faceNormX[fi] = nx / len;
                faceNormY[fi] = ny / len;
                faceNormZ[fi] = nz / len;
            }
        }
    }

    public static int getSubdivisionForRadius(double rx, double ry, double rz) {
        double r = Math.pow(rx * ry * rz, 1.0 / 3.0);
        int level = (int) Math.floor(Math.log(r) / Math.log(2));
        return Math.max(1, Math.min(5, level));
    }

    public static MeshData getMesh(int subdivisions) {
        if (subdivisions < 0) subdivisions = 0;
        if (subdivisions > 7) subdivisions = 7;
        if (cache[subdivisions] != null) return cache[subdivisions];
        cache[subdivisions] = build(subdivisions);
        return cache[subdivisions];
    }

    private static MeshData build(int subdivisions) {
        float inv = (float) (1.0 / Math.sqrt(1.0 + PHI * PHI));
        float[] baseVerts = new float[]{
                -1, PHI, 0, 1, PHI, 0, -1, -PHI, 0, 1, -PHI, 0,
                0, -1, PHI, 0, 1, PHI, 0, -1, -PHI, 0, 1, -PHI,
                PHI, 0, -1, PHI, 0, 1, -PHI, 0, -1, -PHI, 0, 1
        };
        for (int i = 0; i < 12; i++) {
            baseVerts[i * 3] *= inv;
            baseVerts[i * 3 + 1] *= inv;
            baseVerts[i * 3 + 2] *= inv;
        }

        int[] baseFaces = new int[]{
                0, 11, 5, 0, 5, 1, 0, 1, 7, 0, 7, 10, 0, 10, 11,
                1, 5, 9, 5, 11, 4, 11, 10, 2, 10, 7, 6, 7, 1, 8,
                3, 9, 4, 3, 4, 2, 3, 2, 6, 3, 6, 8, 3, 8, 9,
                4, 9, 5, 2, 4, 11, 6, 2, 10, 8, 6, 7, 9, 8, 1
        };

        ArrayList<Float> vertList = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            vertList.add(baseVerts[i * 3]);
            vertList.add(baseVerts[i * 3 + 1]);
            vertList.add(baseVerts[i * 3 + 2]);
        }

        ArrayList<int[]> curFaces = new ArrayList<>();
        for (int f = 0; f < 20; f++) {
            curFaces.add(new int[]{baseFaces[f * 3], baseFaces[f * 3 + 1], baseFaces[f * 3 + 2]});
        }

        for (int level = 0; level < subdivisions; level++) {
            HashMap<Long, Integer> edgeMap = new HashMap<>();
            ArrayList<int[]> nextFaces = new ArrayList<>();
            for (int[] face : curFaces) {
                int i0 = face[0], i1 = face[1], i2 = face[2];
                int m01 = getOrCreateMidpoint(vertList, edgeMap, i0, i1);
                int m12 = getOrCreateMidpoint(vertList, edgeMap, i1, i2);
                int m20 = getOrCreateMidpoint(vertList, edgeMap, i2, i0);
                nextFaces.add(new int[]{i0, m01, m20});
                nextFaces.add(new int[]{i1, m12, m01});
                nextFaces.add(new int[]{i2, m20, m12});
                nextFaces.add(new int[]{m01, m12, m20});
            }
            curFaces = nextFaces;
        }

        float[] verts = new float[vertList.size()];
        for (int i = 0; i < vertList.size(); i++) verts[i] = vertList.get(i);
        int[][] faces = curFaces.toArray(new int[0][]);

        Integer[] order = new Integer[faces.length];
        for (int i = 0; i < faces.length; i++) order[i] = i;
        Arrays.sort(order, Comparator.comparingDouble((Integer i) -> {
            int[] f = faces[i];
            float sy = verts[f[0] * 3 + 1] + verts[f[1] * 3 + 1] + verts[f[2] * 3 + 1];
            return sy;
        }).reversed());
        int[][] sortedFaces = new int[faces.length][];
        int[] reverse = new int[faces.length];
        for (int i = 0; i < faces.length; i++) {
            sortedFaces[i] = faces[order[i]];
            reverse[order[i]] = i;
        }

        int[][] neighbors = computeAdjacency(sortedFaces);

        return new MeshData(verts, sortedFaces, neighbors);
    }

    private static int[][] computeAdjacency(int[][] sortedFaces) {
        HashMap<Long, ArrayList<Integer>> edgeToFaces = new HashMap<>();
        for (int fi = 0; fi < sortedFaces.length; fi++) {
            int[] f = sortedFaces[fi];
            addEdgeRef(edgeToFaces, f[0], f[1], fi);
            addEdgeRef(edgeToFaces, f[1], f[2], fi);
            addEdgeRef(edgeToFaces, f[2], f[0], fi);
        }
        int[][] neighbors = new int[sortedFaces.length][];
        for (int fi = 0; fi < sortedFaces.length; fi++) {
            HashSet<Integer> nset = new HashSet<>();
            int[] f = sortedFaces[fi];
            collectEdgeNeighbors(edgeToFaces, nset, f[0], f[1], fi);
            collectEdgeNeighbors(edgeToFaces, nset, f[1], f[2], fi);
            collectEdgeNeighbors(edgeToFaces, nset, f[2], f[0], fi);
            neighbors[fi] = nset.stream().mapToInt(Integer::intValue).toArray();
        }
        return neighbors;
    }

    private static void addEdgeRef(HashMap<Long, ArrayList<Integer>> map, int i1, int i2, int fi) {
        long key = i1 < i2 ? ((long) i1 << 32) | i2 : ((long) i2 << 32) | i1;
        map.computeIfAbsent(key, k -> new ArrayList<>()).add(fi);
    }

    private static void collectEdgeNeighbors(HashMap<Long, ArrayList<Integer>> map, HashSet<Integer> set,
                                              int i1, int i2, int fi) {
        long key = i1 < i2 ? ((long) i1 << 32) | i2 : ((long) i2 << 32) | i1;
        ArrayList<Integer> list = map.get(key);
        if (list != null) {
            for (int nfi : list) {
                if (nfi != fi) set.add(nfi);
            }
        }
    }

    private static int getOrCreateMidpoint(ArrayList<Float> vertList, HashMap<Long, Integer> edgeMap,
                                            int i1, int i2) {
        long key = i1 < i2 ? ((long) i1 << 32) | i2 : ((long) i2 << 32) | i1;
        Integer existing = edgeMap.get(key);
        if (existing != null) return existing;

        float x1 = vertList.get(i1 * 3);
        float y1 = vertList.get(i1 * 3 + 1);
        float z1 = vertList.get(i1 * 3 + 2);
        float x2 = vertList.get(i2 * 3);
        float y2 = vertList.get(i2 * 3 + 1);
        float z2 = vertList.get(i2 * 3 + 2);

        float mx = (x1 + x2) * 0.5f;
        float my = (y1 + y2) * 0.5f;
        float mz = (z1 + z2) * 0.5f;
        float len = (float) Math.sqrt(mx * mx + my * my + mz * mz);
        mx /= len;
        my /= len;
        mz /= len;

        int idx = vertList.size() / 3;
        vertList.add(mx);
        vertList.add(my);
        vertList.add(mz);
        edgeMap.put(key, idx);
        return idx;
    }
}
