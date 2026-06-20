package mods.hexagon.thrusted.space.body;

import net.minecraft.world.phys.Vec3;

public abstract class CelestialBody {
    protected final String name;
    protected Vec3 position;
    protected final double radius;
    protected final double mass;
    protected final int color;
    protected double rotationAngle;
    protected final double rotationSpeed;

    public CelestialBody(String name, double posX, double posY, double posZ, double radius, double mass, int color, double rotationSpeed) {
        this.name = name;
        this.position = new Vec3(posX, posY, posZ);
        this.radius = radius;
        this.mass = mass;
        this.color = color;
        this.rotationAngle = 0.0;
        this.rotationSpeed = rotationSpeed;
    }

    public abstract void update(float deltaTime);

    public String getName() { return name; }
    public Vec3 getPosition() { return position; }
    public double getRadius() { return radius; }
    public double getMass() { return mass; }
    public int getColor() { return color; }
    public double getRotationAngle() { return rotationAngle; }
    public void setPosition(Vec3 position) { this.position = position; }
}
