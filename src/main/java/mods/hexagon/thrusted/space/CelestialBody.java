package mods.hexagon.thrusted.space;

import mods.hexagon.thrusted.space.api.AtmosphereData;
import mods.hexagon.thrusted.space.api.CelestialBodyType;
import mods.hexagon.thrusted.space.api.GravityData;
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
    private final AtmosphereData atmosphere;
    private final GravityData gravity;
    private final boolean hasRings;
    private final double ringInnerRadius;
    private final double ringOuterRadius;
    private final int ringColor;
    private final String description;

    private Vector3d currentPosition = new Vector3d();
    private float currentRotation;

    public CelestialBody(String name, CelestialBodyType type, CelestialBody parent, OrbitData orbitData,
                         ResourceLocation texture, int color) {
        this(name, type, parent, orbitData, texture, color, AtmosphereData.VACUUM, GravityData.ZERO, "");
    }

    public CelestialBody(String name, CelestialBodyType type, CelestialBody parent, OrbitData orbitData,
                         ResourceLocation texture, int color, AtmosphereData atmosphere,
                         GravityData gravity, String description) {
        this.name = name;
        this.type = type;
        this.parent = parent;
        this.orbitData = orbitData;
        this.texture = texture;
        this.color = color;
        this.tags = new HashSet<>();
        this.atmosphere = atmosphere;
        this.gravity = gravity;
        this.hasRings = false;
        this.ringInnerRadius = 0;
        this.ringOuterRadius = 0;
        this.ringColor = 0;
        this.description = description;
    }

    public CelestialBody(String name, CelestialBodyType type, CelestialBody parent, OrbitData orbitData,
                         ResourceLocation texture, int color, AtmosphereData atmosphere,
                         GravityData gravity, String description,
                         boolean hasRings, double ringInner, double ringOuter, int ringColor) {
        this.name = name;
        this.type = type;
        this.parent = parent;
        this.orbitData = orbitData;
        this.texture = texture;
        this.color = color;
        this.tags = new HashSet<>();
        this.atmosphere = atmosphere;
        this.gravity = gravity;
        this.hasRings = hasRings;
        this.ringInnerRadius = ringInner;
        this.ringOuterRadius = ringOuter;
        this.ringColor = ringColor;
        this.description = description;
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
    public AtmosphereData getAtmosphere() { return atmosphere; }
    public GravityData getGravity() { return gravity; }
    public boolean hasRings() { return hasRings; }
    public double getRingInnerRadius() { return ringInnerRadius; }
    public double getRingOuterRadius() { return ringOuterRadius; }
    public int getRingColor() { return ringColor; }
    public String getDescription() { return description; }

    public CelestialBody addTag(String tag) { tags.add(tag); return this; }
    public boolean hasTag(String tag) { return tags.contains(tag); }

    public boolean isLandable() {
        return type == CelestialBodyType.PLANET || type == CelestialBodyType.MOON
                || type == CelestialBodyType.DWARF_PLANET || type == CelestialBodyType.ASTEROID;
    }

    public boolean isGaseous() {
        return type == CelestialBodyType.GAS_GIANT || type == CelestialBodyType.SUN;
    }

    public double getDistanceTo(CelestialBody other) {
        return currentPosition.distance(other.currentPosition);
    }
}
