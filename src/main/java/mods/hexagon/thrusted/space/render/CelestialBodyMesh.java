package mods.hexagon.thrusted.space.render;

import com.mojang.blaze3d.vertex.*;
import mods.hexagon.thrusted.space.CelestialBody;
import mods.hexagon.thrusted.space.OrbitManager;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.Map;

public class CelestialBodyMesh {
    private static final int SPHERE_SEGMENTS = 32;
    private static final int SPHERE_RINGS = 16;
    private static final Map<String, MeshBuffers> BUFFER_CACHE = new HashMap<>();

    private record MeshBuffers(int vertexCount, float[] positions, float[] normals, int[] colors) {}

    public static void renderBody(Matrix4f modelViewMatrix, CelestialBody body, Vector3d viewerPos,
                                   float partialTick, RenderType renderType) {
        Vector3d bodyPos = OrbitManager.getPosition(body.getName());
        double dx = bodyPos.x - viewerPos.x;
        double dy = bodyPos.y - viewerPos.y;
        double dz = bodyPos.z - viewerPos.z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 0.5) return;

        double radius = body.getOrbitData().radius();
        double angularRadius = Math.atan2(radius, dist);

        Matrix4f mat = new Matrix4f(modelViewMatrix)
                .translate((float) dx, (float) dy, (float) dz);

        float scale = (float) (radius * 0.5);
        mat.scale(scale, scale, scale);

        float rotation = OrbitManager.getRotation(body.getName());
        mat.rotate(new Quaternionf().rotationY(rotation));

        MeshBuffers mesh = getOrCreateMesh(body.getName(), body.getColor());
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < mesh.vertexCount; i++) {
            float px = mesh.positions[i * 3];
            float py = mesh.positions[i * 3 + 1];
            float pz = mesh.positions[i * 3 + 2];
            int color = mesh.colors[i];
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            buffer.addVertex(mat, px, py, pz).setColor(r, g, b, 220);
        }

        var rendered = buffer.build();
        if (rendered != null) {
            BufferUploader.draw(rendered);
        }
    }

    private static MeshBuffers getOrCreateMesh(String name, int color) {
        return BUFFER_CACHE.computeIfAbsent(name, k -> generateSphere(color));
    }

    private static MeshBuffers generateSphere(int color) {
        int vertexCount = SPHERE_SEGMENTS * SPHERE_RINGS * 6;
        float[] positions = new float[vertexCount * 3];
        int[] colors = new int[vertexCount];

        int idx = 0;
        for (int ring = 0; ring < SPHERE_RINGS; ring++) {
            float theta1 = (float) (ring * Math.PI / SPHERE_RINGS);
            float theta2 = (float) ((ring + 1) * Math.PI / SPHERE_RINGS);

            for (int seg = 0; seg < SPHERE_SEGMENTS; seg++) {
                float phi1 = (float) (seg * 2.0 * Math.PI / SPHERE_SEGMENTS);
                float phi2 = (float) ((seg + 1) * 2.0 * Math.PI / SPHERE_SEGMENTS);

                int[] corners = addQuad(positions, colors, idx, theta1, phi1, theta1, phi2, theta2, phi1, theta2, phi2, color);
                idx = corners[0];
            }
        }

        return new MeshBuffers(idx, positions, null, colors);
    }

    private static int[] addQuad(float[] positions, int[] colors, int idx,
                                  float t1, float p1, float t2, float p2,
                                  float t3, float p3, float t4, float p4,
                                  int color) {
        vertex(positions, idx, t1, p1); colors[idx] = color; idx++;
        vertex(positions, idx, t2, p2); colors[idx] = color; idx++;
        vertex(positions, idx, t4, p4); colors[idx] = color; idx++;

        vertex(positions, idx, t2, p2); colors[idx] = color; idx++;
        vertex(positions, idx, t3, p3); colors[idx] = color; idx++;
        vertex(positions, idx, t4, p4); colors[idx] = color; idx++;

        return new int[]{idx};
    }

    private static void vertex(float[] positions, int idx, float theta, float phi) {
        float x = (float) (Math.sin(theta) * Math.cos(phi));
        float y = (float) Math.cos(theta);
        float z = (float) (Math.sin(theta) * Math.sin(phi));
        positions[idx * 3] = x;
        positions[idx * 3 + 1] = y;
        positions[idx * 3 + 2] = z;
    }

    public static void invalidateCache() {
        BUFFER_CACHE.clear();
    }
}
