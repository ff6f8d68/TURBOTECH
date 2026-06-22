package mods.hexagon.thrusted.blockentity;

import dev.ryanhcode.sable.api.block.propeller.BlockEntityPropeller;
import dev.ryanhcode.sable.api.block.propeller.BlockEntitySubLevelPropellerActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;

import mods.hexagon.thrusted.Thrusted;
import mods.hexagon.thrusted.block.IonThrusterBlock;
import mods.hexagon.thrusted.space.SableIntegration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

public class IonThrusterBlockEntity extends BlockEntity implements BlockEntitySubLevelPropellerActor, BlockEntityPropeller {

    public static final Vector3d THRUST_VECTOR = new Vector3d();
    public static final Vector3d THRUST_POSITION = new Vector3d();

    public static final double MAX_THRUST_NEWTONS = 1000.0;
    public static final double MIN_THRUST_NEWTONS = 2.0;

    private static final double SCALE_HEIGHT = 8500.0;
    private static final double BLOCKS_TO_METERS = 1.0;

    private static final double SPOOL_UP_SPEED = MAX_THRUST_NEWTONS / 1000.0;
    private static final double SPOOL_DOWN_SPEED = MAX_THRUST_NEWTONS / 1500.0;

    private float rotationAngle = 0.0f;
    private boolean isActive = false;
    private float thrustPower = 0.0f;
    private double currentThrust = 0.0;

    public IonThrusterBlockEntity(BlockPos pos, BlockState state) {
        super(Thrusted.ION_THRUSTER_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("RotationAngle", rotationAngle);
        tag.putBoolean("IsActive", isActive);
        tag.putFloat("ThrustPower", thrustPower);
        tag.putDouble("CurrentThrust", currentThrust);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.rotationAngle = tag.getFloat("RotationAngle");
        this.isActive = tag.getBoolean("IsActive");
        this.thrustPower = tag.getFloat("ThrustPower");
        this.currentThrust = tag.getDouble("CurrentThrust");
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(@NotNull HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void clientTick(Level level, BlockPos pos, BlockState state) {
        if (!level.isClientSide) return;

        double airDensityFactor = getAirDensityFactor(pos.getY());
        double maxTargetThrust = this.isActive ? (MAX_THRUST_NEWTONS * this.thrustPower * airDensityFactor) : 0.0;
        
        if (this.currentThrust < maxTargetThrust) {
            if (this.currentThrust == 0.0) this.currentThrust = MIN_THRUST_NEWTONS;
            this.currentThrust = Math.min(this.currentThrust + SPOOL_UP_SPEED, maxTargetThrust);
        } else if (this.currentThrust > maxTargetThrust) {
            this.currentThrust = Math.max(this.currentThrust - SPOOL_DOWN_SPEED, maxTargetThrust);
            if (this.currentThrust < MIN_THRUST_NEWTONS) this.currentThrust = 0.0;
        }

        if (currentThrust > 0.1) {
            float speedFactor = (float)(currentThrust / (MAX_THRUST_NEWTONS * airDensityFactor));
            rotationAngle += (speedFactor * 20.0f);
            if (rotationAngle > 360.0f) {
                rotationAngle -= 360.0f;
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, IonThrusterBlockEntity blockEntity) {
        if (level.isClientSide) return;
        int redstoneSignal = level.getBestNeighborSignal(pos);
        boolean isPowered = redstoneSignal > 0;
        float targetThrustPower = redstoneSignal / 15.0f;
        if (isPowered != blockEntity.isActive || targetThrustPower != blockEntity.thrustPower) {
            blockEntity.thrustPower = targetThrustPower;
            blockEntity.isActive = isPowered;
            blockEntity.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
        double airDensityFactor = getAirDensityFactor(pos.getY());
        double maxTargetThrust = blockEntity.isActive ? (MAX_THRUST_NEWTONS * blockEntity.thrustPower * airDensityFactor) : 0.0;
        if (blockEntity.currentThrust < maxTargetThrust) {
            if (blockEntity.currentThrust == 0.0) blockEntity.currentThrust = MIN_THRUST_NEWTONS;
            blockEntity.currentThrust = Math.min(blockEntity.currentThrust + SPOOL_UP_SPEED, maxTargetThrust);
        } else if (blockEntity.currentThrust > maxTargetThrust) {
            blockEntity.currentThrust = Math.max(blockEntity.currentThrust - SPOOL_DOWN_SPEED, maxTargetThrust);
            if (blockEntity.currentThrust < MIN_THRUST_NEWTONS) blockEntity.currentThrust = 0.0;
        }
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle body, double dt) {
        SableIntegration.applyAntiGravity(subLevel, body, dt);
        if (currentThrust <= 0.01 || level == null || level.isClientSide) return;
        Direction facing = getBlockState().getValue(IonThrusterBlock.FACING);
        Vec3 thrustDirection = Vec3.atLowerCornerOf(facing.getNormal());
        applyForces(subLevel, thrustDirection, dt);
    }

    public void applyForces(ServerSubLevel subLevel, Vec3 direction, double dt) {
        double PHYSICS_SCALE = 0.005;
        double airDensityFactor = getAirDensityFactor(getBlockPos().getY());
        double thrust = getThrust() * PHYSICS_SCALE * airDensityFactor;
        if (thrust <= 0.01) return;
        Vec3 thrustForce = direction.scale(thrust);
        THRUST_POSITION.set(getBlockPos().getX() + 0.5, getBlockPos().getY() + 0.5, getBlockPos().getZ() + 0.5);
        THRUST_VECTOR.set(thrustForce.x, thrustForce.y, thrustForce.z);
        var propulsionKey = ResourceLocation.fromNamespaceAndPath("sable", "propulsion");
        var forceGroup = dev.ryanhcode.sable.api.physics.force.ForceGroups.REGISTRY.get(propulsionKey);
        if (forceGroup == null) return;
        var queuedForceGroup = subLevel.getOrCreateQueuedForceGroup(forceGroup);
        queuedForceGroup.applyAndRecordPointForce(new Vector3d(THRUST_POSITION), new Vector3d(THRUST_VECTOR));
    }

    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(5.0);
    }

    @Override public Direction getBlockDirection() { return getBlockState().getValue(IonThrusterBlock.FACING); }
    @Override public double getAirflow() {
        if (level != null && SableIntegration.isSpaceEnvironment(level)) return 0.0;
        return (currentThrust / MAX_THRUST_NEWTONS) * 500.0;
    }
    @Override public double getThrust() {
        if (level != null && SableIntegration.isSpaceEnvironment(level)) return 0.0;
        return currentThrust;
    }
    @Override public boolean isActive() { return currentThrust > 0.0; }
    @Override public Level getLevel() { return level; }
    @Override public @NotNull BlockPos getBlockPos() { return worldPosition; }
    @Override public BlockEntityPropeller getPropeller() { return this; }
    public float getRotationAngle() { return rotationAngle; }
    public void setRotationAngle(float rotationAngle) { this.rotationAngle = rotationAngle; }
    public float getThrustPower() { return thrustPower; }
    public static double getAirDensityFactor(double yCoord) {
        double altitudeMeters = (yCoord - 64.0) * BLOCKS_TO_METERS;
        double densityRatio = Math.exp(-altitudeMeters / SCALE_HEIGHT);
        return Math.max(0.0, Math.min(1.0, densityRatio));
    }
}
