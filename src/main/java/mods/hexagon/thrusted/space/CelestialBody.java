package mods.hexagon.thrusted.space;

import mods.hexagon.thrusted.space.api.CelestialBodyType;
import mods.hexagon.thrusted.space.api.OrbitData;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3d;

import java.util.HashSet;
import java.util.Set;

public class CelestialBody {
    private final String name;
    private final CelestialBodyType type;
    private final CelestialBody parent;
    private final OrbitData orbitData;
    private final ResourceLocation texture;
    private final int color;
    private final Set<String> tags;

    private Vector3d currentPosition = new Vector3d();
    private float currentRotation;

    public CelestialBody(String name, CelestialBodyType type, CelestialBody parent, OrbitData orbitData,
                         ResourceLocation texture, int color) {
        this.name = name;
        this.type = type;
        this.parent = parent;
        this.orbitData = orbitData;
        this.texture = texture;
        this.color = color;
        this.tags = new HashSet<>();
    }

    public void computePosition(long gameTime) {
        Vector3d localPos = orbitData.getPosition(gameTime);
        if (parent != null) {
            Vector3d parentPos = parent.currentPosition;
            currentPosition = new Vector3d(
                    parentPos.x + localPos.x,
                    parentPos.y + localPos.y,
                    parentPos.z + localPos.z
            );
        } else {
            currentPosition = localPos;
        }
        currentRotation = orbitData.getRotationAngle(gameTime);
    }

    public String getName() { return name; }
    public CelestialBodyType getType() { return type; }
    public CelestialBody getParent() { return parent; }
    public OrbitData getOrbitData() { return orbitData; }
    public ResourceLocation getTexture() { return texture; }
    public int getColor() { return color; }
    public Set<String> getTags() { return tags; }
    public Vector3d getCurrentPosition() { return currentPosition; }
    public float getCurrentRotation() { return currentRotation; }

    public CelestialBody addTag(String tag) { tags.add(tag); return this; }
    public boolean hasTag(String tag) { return tags.contains(tag); }
}
