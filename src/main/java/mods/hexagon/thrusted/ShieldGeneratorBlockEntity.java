package mods.hexagon.thrusted;

import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;

public class ShieldGeneratorBlockEntity extends BlockEntity implements BlockEntitySubLevelActor, MenuProvider {
    private static final double BASE_FALLBACK_RADIUS = 20.0;
    private static final double PADDING = 3.0;

    private static final float MAX_TOP_SPEED = 30.0f;
    private static final float TOP_ACCEL = 0.05f;
    private static final float TOP_DECEL = 0.10f;

    private boolean active = false;
    private float shieldAlpha = 0.0f;
    private int redstoneSignal = 0;
    private float shieldStrength = 0.0f;
    private float bootupProgress = 0.0f;

    private float topRotation = 0.0f;
    private float topSpeed = 0.0f;

    private double shieldCenterX = 0;
    private double shieldCenterY = 0;
    private double shieldCenterZ = 0;
    private double radiusX = BASE_FALLBACK_RADIUS;
    private double radiusY = BASE_FALLBACK_RADIUS * 0.6;
    private double radiusZ = BASE_FALLBACK_RADIUS;

    private int glowFaceIndex = -1;
    private int glowTimer = 0;
    private int subdivLevel = 2;
    private int shieldColor = 0x3366D9;

    private double hitDirX = 0, hitDirY = 0, hitDirZ = 0;
    private long hitTick = -20;
    private int playerSoundCooldown = 0;

    private Level worldLevel = null;

    private int baseAlpha = 255;
    private boolean glowing = true;
    private boolean whole = true;
    private float arcDegrees = 180.0f;
    private float rotX = 0, rotY = 0, rotZ = 0;
    private double offX = 0, offY = 0, offZ = 0;
    private double customRx = 0, customRy = 0, customRz = 0;
    public static final int FILTER_PROJECTILES = 1;
    public static final int FILTER_MOBS = 2;
    public static final int FILTER_PLAYERS = 4;
    public static final int FILTER_VEHICLES = 8;
    public static final int FILTER_MISC = 16;

    private boolean onShip = false;
    private boolean beamEnabled = true;
    private boolean showFriendlyNames = false;
    private int entityFilterFlags = FILTER_PROJECTILES;
    private List<String> customEntityTypes = new ArrayList<>();
    private List<String> friendlyPlayerNames = new ArrayList<>();

    private boolean wasActiveForSound = false;
    private int loopSoundCooldown = 0;
    private boolean isPlayingShutdown = false;
    private Object ambientSoundInstance = null;

    // Multiblock
    private int multiblockSize = 1;
    private boolean isController = true;
    private BlockPos controllerPos = null;

    public ShieldGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(Thrusted.SHIELD_GENERATOR_BLOCK_ENTITY.get(), pos, state);
        this.controllerPos = pos;
        this.shieldCenterX = pos.getX() + 0.5;
        this.shieldCenterY = pos.getY() + 0.5;
        this.shieldCenterZ = pos.getZ() + 0.5;
    }

    public int getMultiblockSize() { return multiblockSize; }

    public boolean isController() { return isController; }

    public double getMultiblockRadius() {
        return BASE_FALLBACK_RADIUS * (1 << (multiblockSize - 1));
    }

    public ShieldGeneratorBlockEntity getController() {
        if (isController) return this;
        if (level == null || controllerPos == null) return null;
        if (level.getBlockEntity(controllerPos) instanceof ShieldGeneratorBlockEntity be && be.isController()) {
            return be;
        }
        return null;
    }

    public void applyMultiblockScaling() {
        double br = getMultiblockRadius();
        if (radiusX < br) radiusX = br;
        if (radiusY < br * 0.6) radiusY = br * 0.6;
        if (radiusZ < br) radiusZ = br;
    }

    public static void detectMultiblock(Level level, BlockPos pos) {
        if (level.isClientSide) return;
        // Flood-fill BFS to find all connected shield generators
        Set<BlockPos> connected = new HashSet<>();
        java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
        queue.add(pos);
        connected.add(pos);
        while (!queue.isEmpty()) {
            BlockPos cur = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos np = cur.offset(dx, dy, dz);
                        if (connected.contains(np)) continue;
                        if (level.getBlockState(np).is(Thrusted.SHIELD_GENERATOR_BLOCK.get())) {
                            connected.add(np);
                            queue.add(np);
                        }
                    }
                }
            }
        }
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos p : connected) {
            if (p.getX() < minX) minX = p.getX();
            if (p.getY() < minY) minY = p.getY();
            if (p.getZ() < minZ) minZ = p.getZ();
            if (p.getX() > maxX) maxX = p.getX();
            if (p.getY() > maxY) maxY = p.getY();
            if (p.getZ() > maxZ) maxZ = p.getZ();
        }
        int dx = maxX - minX + 1;
        int dy = maxY - minY + 1;
        int dz = maxZ - minZ + 1;
        if (dx != dy || dy != dz || dx < 1 || dx > 5) {
            setSingleBlock(level, pos);
            return;
        }
        int size = dx;
        int expected = size * size * size;
        if (connected.size() != expected) {
            setSingleBlock(level, pos);
            return;
        }
        if (size >= 2) {
            Component msg = Component.literal("\u00A7aMultiblock assembled!");
            for (var player : level.players()) {
                if (player.blockPosition().distSqr(new BlockPos(minX, minY, minZ)) < 25 * 25) {
                    player.displayClientMessage(msg, true);
                }
            }
        }
        BlockPos ctrlPos = new BlockPos(minX, minY, minZ);
        for (BlockPos bp : connected) {
            if (level.getBlockEntity(bp) instanceof ShieldGeneratorBlockEntity be) {
                be.multiblockSize = size;
                be.isController = bp.equals(ctrlPos);
                be.controllerPos = ctrlPos;
                if (be.isController) {
                    be.shieldCenterX = ctrlPos.getX() + size / 2.0;
                    be.shieldCenterY = ctrlPos.getY() + size / 2.0;
                    be.shieldCenterZ = ctrlPos.getZ() + size / 2.0;
                }
                be.applyMultiblockScaling();
                be.setChanged();
                level.sendBlockUpdated(bp, level.getBlockState(bp), level.getBlockState(bp), 3);
            }
        }
    }

    private static void setSingleBlock(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof ShieldGeneratorBlockEntity be) {
            be.multiblockSize = 1;
            be.isController = true;
            be.controllerPos = pos;
            be.shieldCenterX = pos.getX() + 0.5;
            be.shieldCenterY = pos.getY() + 0.5;
            be.shieldCenterZ = pos.getZ() + 0.5;
            be.applyMultiblockScaling();
            be.setChanged();
            level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
        }
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle body, double dt) {
        this.worldLevel = subLevel.getLevel();
        this.onShip = true;
        var bb = subLevel.getPlot().getBoundingBox();
        if (bb != null) {
            shieldCenterX = (bb.minX() + bb.maxX()) / 2.0;
            shieldCenterY = (bb.minY() + bb.maxY()) / 2.0;
            shieldCenterZ = (bb.minZ() + bb.maxZ()) / 2.0;
            radiusX = Math.max((bb.maxX() - bb.minX()) / 2.0 + PADDING, 8.0);
            radiusY = Math.max((bb.maxY() - bb.minY()) / 2.0 + PADDING, 6.0);
            radiusZ = Math.max((bb.maxZ() - bb.minZ()) / 2.0 + PADDING, 8.0);
            subdivLevel = ThrustedIcosphere.getSubdivisionForRadius(getEffectiveRadiusX(), getEffectiveRadiusY(), getEffectiveRadiusZ());
            markAndSync();
        }
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) {
            clientTick();
        } else {
            serverTick(level, pos);
        }
    }

    private void serverTick(Level level, BlockPos pos) {
        updateTopRotation();

        if (!isController) {
            if (glowTimer > 0) {
                glowTimer--;
                if (glowTimer == 0) glowFaceIndex = -1;
            }
            return;
        }

        int signal = 0;
        BlockPos ctrlPos = controllerPos != null ? controllerPos : pos;
        for (int dx = 0; dx < multiblockSize; dx++) {
            for (int dy = 0; dy < multiblockSize; dy++) {
                for (int dz = 0; dz < multiblockSize; dz++) {
                    signal = Math.max(signal, level.getBestNeighborSignal(ctrlPos.offset(dx, dy, dz)));
                }
            }
        }
        boolean wasActive = this.active;
        this.redstoneSignal = signal;
        this.shieldStrength = signal / 15.0f;

        if (signal <= 0) {
            this.active = false;
            this.bootupProgress = Math.max(this.bootupProgress - 0.02f, 0.0f);
        } else {
            this.active = true;
            this.bootupProgress = Math.min(this.bootupProgress + 0.02f, 1.0f);
        }

        if (glowTimer > 0) {
            glowTimer--;
            if (glowTimer == 0) glowFaceIndex = -1;
        }
        if (playerSoundCooldown > 0) playerSoundCooldown--;

        if (this.active && this.bootupProgress > 0.1f) {
            Level targetLevel = this.worldLevel != null ? this.worldLevel : level;
            Vec3 localCenter = new Vec3(shieldCenterX + offX, shieldCenterY + offY, shieldCenterZ + offZ);
            Vec3 worldCenter = localCenter;
            if (this.worldLevel != null) {
                worldCenter = SableCompanion.INSTANCE.projectOutOfSubLevel(level, localCenter);
            }
            scanAndDeflectProjectiles(targetLevel, worldCenter, level.getGameTime());
        }

        boolean changed = wasActive != this.active || (glowTimer > 0 && level.getGameTime() % 4 == 0);
        if (changed) {
            setChanged();
            level.sendBlockUpdated(pos, getBlockState(), getBlockState(), 3);
        }
    }

    private void clientTick() {
        this.shieldStrength = this.redstoneSignal / 15.0f;

        if (glowTimer > 0) {
            glowTimer--;
            if (glowTimer == 0) glowFaceIndex = -1;
        }

        float targetAlpha = 0.15f + this.shieldStrength * 0.80f;
        boolean wasActive = this.active;
        if (this.active) {
            this.bootupProgress = Math.min(this.bootupProgress + 0.02f, 1.0f);
            this.shieldAlpha = Math.min(this.shieldAlpha + 0.03f, targetAlpha);
        } else {
            this.bootupProgress = Math.max(this.bootupProgress - 0.02f, 0.0f);
            this.shieldAlpha = Math.max(this.shieldAlpha - 0.03f, 0.0f);
        }

        updateTopRotation();
        handleSoundPlayback();
    }

    @OnlyIn(Dist.CLIENT)
    private void handleSoundPlayback() {
        if (level == null) return;
        boolean effectivelyActive = this.active && this.bootupProgress > 0.1f;
        if (effectivelyActive) {
            isPlayingShutdown = false;
            if (!wasActiveForSound) {
                playSound("shield_bootup");
                startLoopingAmbient();
            }
        } else {
            if (wasActiveForSound) {
                stopLoopingAmbient();
                if (!isPlayingShutdown) {
                    playSound("shield_shutdown");
                    isPlayingShutdown = true;
                }
            }
            if (this.bootupProgress <= 0.001f) isPlayingShutdown = false;
        }
        wasActiveForSound = effectivelyActive;
    }

    @OnlyIn(Dist.CLIENT)
    private void startLoopingAmbient() {
        if (ambientSoundInstance != null || level == null) return;
        try {
            var event = Thrusted.SHIELD_AMBIENT_SOUND.value();
            float vol = 1.5f * getSoundScale();
            var instance = new net.minecraft.client.resources.sounds.SimpleSoundInstance(
                    event, SoundSource.BLOCKS, vol, 1.0f,
                    net.minecraft.client.resources.sounds.SoundInstance.createUnseededRandom(),
                    worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5
            );
            var field = net.minecraft.client.resources.sounds.AbstractSoundInstance.class.getDeclaredField("looping");
            field.setAccessible(true);
            field.set(instance, true);
            ambientSoundInstance = instance;
            net.minecraft.client.Minecraft.getInstance().getSoundManager().play((net.minecraft.client.resources.sounds.SoundInstance) ambientSoundInstance);
        } catch (Exception e) {
            Thrusted.LOGGER.error("Failed to start looping ambient", e);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void stopLoopingAmbient() {
        if (ambientSoundInstance instanceof net.minecraft.client.resources.sounds.SoundInstance inst) {
            net.minecraft.client.Minecraft.getInstance().getSoundManager().stop(inst);
            ambientSoundInstance = null;
        }
    }

    private void playSound(String soundName) {
        if (level == null) return;
        SoundEvent soundEvent = switch (soundName) {
            case "shield_bootup" -> Thrusted.SHIELD_BOOTUP_SOUND.value();
            case "shield_shutdown" -> Thrusted.SHIELD_SHUTDOWN_SOUND.value();
            default -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Thrusted.MODID, soundName));
        };
        level.playLocalSound(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, soundEvent, SoundSource.BLOCKS, 1.0f * getSoundScale(), 1.0f, false);
    }

    private void scanAndDeflectProjectiles(Level level, Vec3 center, long gameTime) {
        if (this.shieldStrength < 0.01f) return;
        double srX = getEffectiveRadiusX();
        double srY = getEffectiveRadiusY();
        double srZ = getEffectiveRadiusZ();
        AABB scanArea = new AABB(
                center.x - srX - 2, center.y - srY - 2, center.z - srZ - 2,
                center.x + srX + 2, center.y + srY + 2, center.z + srZ + 2);
        List<net.minecraft.world.entity.Entity> targets = level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, scanArea,
                e -> matchesFilter(e));
        for (net.minecraft.world.entity.Entity p : targets) {
            Vec3 pos = p.position();
            Vec3 relPos = pos.subtract(center);
            double dist = relPos.length();
            if (dist < 0.01) continue;
            Vec3 dir = relPos.scale(1.0 / dist);

            // Determine if projectile is inside or outside the shield ellipsoid
            double insideDist = (relPos.x * relPos.x) / (srX * srX)
                              + (relPos.y * relPos.y) / (srY * srY)
                              + (relPos.z * relPos.z) / (srZ * srZ);
            boolean inside = insideDist < 1.0;

            // Check if moving outward (away from center) or inward (toward center)
            Vec3 vel = p.getDeltaMovement();
            double radialDot = vel.dot(dir);
            boolean movingOutward = radialDot > 0;

            boolean isPlayer = p instanceof net.minecraft.world.entity.player.Player;
            if (isPlayer) {
                // For players: deflect when inside (push out) OR outside moving inward (hold back)
                // But let them through when outside moving outward (leave freely)
                if (!inside && movingOutward) continue;
            } else {
                // Only deflect non-player entities from OUTSIDE moving INWARD
                if (inside || movingOutward) continue;
            }

            // Skip if the hit face isn't visible (arc-culled or not yet booted up)
            int fi = findClosestFace(dir);
            if (fi >= 0 && !isFaceVisible(fi, srX, srY, srZ)) continue;

            double dot = vel.dot(dir);
            if (isPlayer) {
                // Strong continuous outward push for players
                Vec3 reflected = vel.subtract(dir.scale(2 * dot)).scale(1.5);
                p.setDeltaMovement(reflected);
            } else {
                Vec3 reflected = vel.subtract(dir.scale(2 * dot)).scale(0.85);
                p.setDeltaMovement(reflected);
            }

            double t = 1.0 / Math.sqrt(
                    (dir.x / srX) * (dir.x / srX) +
                    (dir.y / srY) * (dir.y / srY) +
                    (dir.z / srZ) * (dir.z / srZ));
            double nudge = t + 0.5;
            p.setPos(center.x + dir.x * nudge, center.y + dir.y * nudge, center.z + dir.z * nudge);
            p.hurtMarked = true;

            spawnDeflectParticles(level, p.position());

            hitDirX = dir.x;
            hitDirY = dir.y;
            hitDirZ = dir.z;
            hitTick = gameTime;

            if (fi >= 0) {
                glowFaceIndex = fi;
                glowTimer = 20;
            }

            if (level != null && (!isPlayer || playerSoundCooldown <= 0)) {
                BlockPos hitPos = BlockPos.containing(p.position());
                level.playSound(null, hitPos, Thrusted.SHIELD_HIT_SOUND.value(), SoundSource.BLOCKS, 0.6f * getSoundScale(), 0.8f + level.random.nextFloat() * 0.4f);
                if (isPlayer) playerSoundCooldown = 20;
            }
        }
    }

    private boolean isFaceVisible(int faceIdx, double srX, double srY, double srZ) {
        var mesh = ThrustedIcosphere.getMesh(getSubdivLevel());
        if (!this.whole) {
            double px = mesh.faceNormX[faceIdx] * srX;
            double py = mesh.faceNormY[faceIdx] * srY;
            double pz = mesh.faceNormZ[faceIdx] * srZ;
            double len = Math.sqrt(px * px + py * py + pz * pz);
            float yDir;
            if (this.rotX != 0 || this.rotY != 0 || this.rotZ != 0) {
                float[] rotMat = buildRotationMatrix(this.rotX, this.rotY, this.rotZ);
                yDir = (float)((px / len) * rotMat[3] + (py / len) * rotMat[4] + (pz / len) * rotMat[5]);
            } else {
                yDir = (float)(py / len);
            }
            float angle = (float) Math.toDegrees(Math.acos(Mth.clamp(yDir, -1f, 1f)));
            if (angle > this.arcDegrees / 2f) return false;
        }
        if (this.bootupProgress < 1.0f) {
            int activeFaces = (int)(this.bootupProgress * mesh.faces.length);
            if (faceIdx >= activeFaces) return false;
        }
        return true;
    }

    private static float[] buildRotationMatrix(float rotX, float rotY, float rotZ) {
        float rx = (float) Math.toRadians(rotX);
        float ry = (float) Math.toRadians(rotY);
        float rz = (float) Math.toRadians(rotZ);
        float cx = (float) Math.cos(rx), sx = (float) Math.sin(rx);
        float cy = (float) Math.cos(ry), sy = (float) Math.sin(ry);
        float cz = (float) Math.cos(rz), sz = (float) Math.sin(rz);
        return new float[]{
                cy*cz,  sx*sy*cz - cx*sz,  cx*sy*cz + sx*sz,
                cy*sz,  sx*sy*sz + cx*cz,  cx*sy*sz - sx*cz,
                -sy,    sx*cy,              cx*cy
        };
    }

    private int findClosestFace(Vec3 dir) {
        var mesh = ThrustedIcosphere.getMesh(getSubdivLevel());
        double srX = getEffectiveRadiusX();
        double srY = getEffectiveRadiusY();
        double srZ = getEffectiveRadiusZ();
        // Transform projectile direction into object space so comparisons with
        // mesh face surface positions are correct even when the shield is rotated.
        double dx = dir.x, dy = dir.y, dz = dir.z;
        if (rotX != 0 || rotY != 0 || rotZ != 0) {
            float[] rotMat = buildRotationMatrix(rotX, rotY, rotZ);
            dx = dir.x * rotMat[0] + dir.y * rotMat[3] + dir.z * rotMat[6];
            dy = dir.x * rotMat[1] + dir.y * rotMat[4] + dir.z * rotMat[7];
            dz = dir.x * rotMat[2] + dir.y * rotMat[5] + dir.z * rotMat[8];
        }
        double bestDot = -2;
        int bestIdx = -1;
        for (int i = 0; i < mesh.faceNormX.length; i++) {
            // Ellipsoid surface position direction for this face (not the normal).
            // The projectile direction is a position direction, so comparing with
            // surface positions (normalized) correctly identifies the face hit.
            double px = mesh.faceNormX[i] * srX;
            double py = mesh.faceNormY[i] * srY;
            double pz = mesh.faceNormZ[i] * srZ;
            double len = Math.sqrt(px * px + py * py + pz * pz);
            double dot = (dx * px + dy * py + dz * pz) / len;
            if (dot > bestDot) {
                bestDot = dot;
                bestIdx = i;
            }
        }
        return bestIdx;
    }

    private void spawnDeflectParticles(Level level, Vec3 pos) {
        level.addParticle(
                net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                pos.x, pos.y, pos.z,
                (level.random.nextDouble() - 0.5) * 0.8,
                (level.random.nextDouble() - 0.5) * 0.8,
                (level.random.nextDouble() - 0.5) * 0.8);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Active", active);
        tag.putInt("RedstoneSignal", redstoneSignal);
        tag.putDouble("CX", shieldCenterX);
        tag.putDouble("CY", shieldCenterY);
        tag.putDouble("CZ", shieldCenterZ);
        tag.putDouble("RX", radiusX);
        tag.putDouble("RY", radiusY);
        tag.putDouble("RZ", radiusZ);
        tag.putInt("GF", glowFaceIndex);
        tag.putInt("GT", glowTimer);
        tag.putInt("SL", subdivLevel);
        tag.putInt("SC", shieldColor);
        tag.putDouble("HX", hitDirX);
        tag.putDouble("HY", hitDirY);
        tag.putDouble("HZ", hitDirZ);
        tag.putLong("HT", hitTick);
        tag.putInt("BA", baseAlpha);
        tag.putBoolean("GW", glowing);
        tag.putBoolean("WH", whole);
        tag.putBoolean("BE", beamEnabled);
        tag.putBoolean("FN", showFriendlyNames);
        tag.putInt("EF", entityFilterFlags);
        var ctList = new net.minecraft.nbt.ListTag();
        for (String s : customEntityTypes) ctList.add(net.minecraft.nbt.StringTag.valueOf(s));
        tag.put("CT", ctList);
        var fpnList = new net.minecraft.nbt.ListTag();
        for (String s : friendlyPlayerNames) fpnList.add(net.minecraft.nbt.StringTag.valueOf(s));
        tag.put("FPN", fpnList);
        tag.putFloat("AD", arcDegrees);
        tag.putFloat("ROTX", rotX);
        tag.putFloat("ROTY", rotY);
        tag.putFloat("ROTZ", rotZ);
        tag.putDouble("OX", offX);
        tag.putDouble("OY", offY);
        tag.putDouble("OZ", offZ);
        tag.putDouble("CRX", customRx);
        tag.putDouble("CRY", customRy);
        tag.putDouble("CRZ", customRz);
        tag.putInt("MB", multiblockSize);
        tag.putBoolean("MBC", isController);
        if (controllerPos != null) {
            tag.putInt("CPX", controllerPos.getX());
            tag.putInt("CPY", controllerPos.getY());
            tag.putInt("CPZ", controllerPos.getZ());
        }
        tag.putFloat("BP", bootupProgress);
        tag.putFloat("TR", topRotation);
        tag.putFloat("TS", topSpeed);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.active = tag.getBoolean("Active");
        this.redstoneSignal = tag.getInt("RedstoneSignal");
        this.shieldCenterX = tag.getDouble("CX");
        this.shieldCenterY = tag.getDouble("CY");
        this.shieldCenterZ = tag.getDouble("CZ");
        this.radiusX = tag.getDouble("RX");
        this.radiusY = tag.getDouble("RY");
        this.radiusZ = tag.getDouble("RZ");
        this.glowFaceIndex = tag.getInt("GF");
        this.glowTimer = tag.getInt("GT");
        this.subdivLevel = tag.getInt("SL");
        if (this.subdivLevel < 1) this.subdivLevel = 2;
        this.shieldColor = tag.getInt("SC");
        if (this.shieldColor == 0) this.shieldColor = 0x3366D9;
        this.hitDirX = tag.getDouble("HX");
        this.hitDirY = tag.getDouble("HY");
        this.hitDirZ = tag.getDouble("HZ");
        this.hitTick = tag.getLong("HT");
        if (tag.contains("BA")) this.baseAlpha = tag.getInt("BA");
        if (tag.contains("GW")) this.glowing = tag.getBoolean("GW");
        if (tag.contains("WH")) this.whole = tag.getBoolean("WH");
        if (tag.contains("BE")) this.beamEnabled = tag.getBoolean("BE");
        if (tag.contains("FN")) this.showFriendlyNames = tag.getBoolean("FN");
        if (tag.contains("EF")) this.entityFilterFlags = tag.getInt("EF");
        this.customEntityTypes.clear();
        if (tag.contains("CT")) {
            var ctList = tag.getList("CT", net.minecraft.nbt.Tag.TAG_STRING);
            for (int i = 0; i < ctList.size(); i++) customEntityTypes.add(ctList.getString(i));
        }
        this.friendlyPlayerNames.clear();
        if (tag.contains("FPN")) {
            var fpnList = tag.getList("FPN", net.minecraft.nbt.Tag.TAG_STRING);
            for (int i = 0; i < fpnList.size(); i++) friendlyPlayerNames.add(fpnList.getString(i));
        }
        if (tag.contains("AD")) this.arcDegrees = tag.getFloat("AD");
        if (tag.contains("ROTX")) this.rotX = tag.getFloat("ROTX");
        if (tag.contains("ROTY")) this.rotY = tag.getFloat("ROTY");
        if (tag.contains("ROTZ")) this.rotZ = tag.getFloat("ROTZ");
        if (tag.contains("OX")) this.offX = tag.getDouble("OX");
        if (tag.contains("OY")) this.offY = tag.getDouble("OY");
        if (tag.contains("OZ")) this.offZ = tag.getDouble("OZ");
        if (tag.contains("CRX")) this.customRx = tag.getDouble("CRX");
        if (tag.contains("CRY")) this.customRy = tag.getDouble("CRY");
        if (tag.contains("CRZ")) this.customRz = tag.getDouble("CRZ");
        if (tag.contains("MB")) this.multiblockSize = tag.getInt("MB");
        if (tag.contains("MBC")) this.isController = tag.getBoolean("MBC");
        if (tag.contains("CPX") && tag.contains("CPY") && tag.contains("CPZ")) {
            this.controllerPos = new BlockPos(tag.getInt("CPX"), tag.getInt("CPY"), tag.getInt("CPZ"));
        }
        if (tag.contains("BP")) this.bootupProgress = tag.getFloat("BP");
        if (tag.contains("TR")) this.topRotation = tag.getFloat("TR");
        if (tag.contains("TS")) this.topSpeed = tag.getFloat("TS");
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

    // MenuProvider
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.turbotech.shield_color");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        ShieldColorMenu menu = new ShieldColorMenu(containerId, inv);
            menu.init(worldPosition, shieldColor, baseAlpha, glowing, whole, (int)arcDegrees,
                    (int)(rotX * 10), (int)(rotY * 10), (int)(rotZ * 10),
                    (int)(offX * 100), (int)(offY * 100), (int)(offZ * 100),
                    (int)(customRx * 100), (int)(customRy * 100), (int)(customRz * 100), onShip, beamEnabled,
                    (int)getMultiblockRadius(), entityFilterFlags, showFriendlyNames);
        return menu;
    }

    // Getters
    public float getShieldAlpha() { return shieldAlpha; }
    public boolean isActive() { return active; }
    public float getShieldStrength() { return shieldStrength; }
    public float getBootupProgress() { return bootupProgress; }
    public double getShieldCenterX() { return shieldCenterX; }
    public double getShieldCenterY() { return shieldCenterY; }
    public double getShieldCenterZ() { return shieldCenterZ; }
    public double getRadiusX() { return radiusX; }
    public double getRadiusY() { return radiusY; }
    public double getRadiusZ() { return radiusZ; }
    public int getGlowFaceIndex() { return glowFaceIndex; }
    public int getGlowTimer() { return glowTimer; }
    public int getSubdivLevel() {
        return ThrustedIcosphere.getSubdivisionForRadius(getEffectiveRadiusX(), getEffectiveRadiusY(), getEffectiveRadiusZ());
    }
    public int getShieldColor() { return shieldColor; }
    public double getHitDirX() { return hitDirX; }
    public double getHitDirY() { return hitDirY; }
    public double getHitDirZ() { return hitDirZ; }
    public long getHitTick() { return hitTick; }
    public int getBaseAlpha() { return baseAlpha; }
    public boolean isGlowing() { return glowing; }
    public boolean isWhole() { return whole; }
    public float getArcDegrees() { return arcDegrees; }
    public float getRotX() { return rotX; }
    public float getRotY() { return rotY; }
    public float getRotZ() { return rotZ; }
    public double getOffX() { return offX; }
    public double getOffY() { return offY; }
    public double getOffZ() { return offZ; }
    public double getCustomRx() { return customRx; }
    public double getCustomRy() { return customRy; }
    public double getCustomRz() { return customRz; }
    public boolean isOnShip() { return onShip; }
    public boolean isBeamEnabled() { return beamEnabled; }
    public boolean showFriendlyNames() { return showFriendlyNames; }
    public int getEntityFilterFlags() { return entityFilterFlags; }
    public List<String> getCustomEntityTypes() { return customEntityTypes; }
    public List<String> getFriendlyPlayerNames() { return friendlyPlayerNames; }
    public float getTopRotation() { return topRotation; }
    public double getEffectiveRadiusX() { return customRx > 0 ? customRx : radiusX; }
    public double getEffectiveRadiusY() { return customRy > 0 ? customRy : radiusY; }
    public double getEffectiveRadiusZ() { return customRz > 0 ? customRz : radiusZ; }

    // Setters
    public void setShieldColor(int color) {
        this.shieldColor = color;
        markAndSync();
    }

    public void setBaseAlpha(int alpha) {
        this.baseAlpha = Math.max(0, Math.min(255, alpha));
        markAndSync();
    }

    public void setGlowing(boolean g) {
        this.glowing = g;
        markAndSync();
    }

    public void setWhole(boolean w) {
        this.whole = w;
        markAndSync();
    }

    public void setArcDegrees(float deg) {
        this.arcDegrees = Math.max(0, Math.min(360, deg));
        markAndSync();
    }

    public void setRotX(float v) { this.rotX = v; markAndSync(); }
    public void setRotY(float v) { this.rotY = v; markAndSync(); }
    public void setRotZ(float v) { this.rotZ = v; markAndSync(); }
    public void setOffX(double v) { this.offX = v; markAndSync(); }
    public void setOffY(double v) { this.offY = v; markAndSync(); }
    public void setOffZ(double v) { this.offZ = v; markAndSync(); }
    public void setCustomRx(double v) { this.customRx = Math.max(0, v); markAndSync(); }
    public void setCustomRy(double v) { this.customRy = Math.max(0, v); markAndSync(); }
    public void setCustomRz(double v) { this.customRz = Math.max(0, v); markAndSync(); }
    public void setBeamEnabled(boolean v) { this.beamEnabled = v; markAndSync(); }
    public void setShowFriendlyNames(boolean v) { this.showFriendlyNames = v; markAndSync(); }
    public void setEntityFilterFlags(int flags) { this.entityFilterFlags = flags; markAndSync(); }
    public void addCustomEntityType(String id) { if (!customEntityTypes.contains(id)) { customEntityTypes.add(id); markAndSync(); } }
    public void removeCustomEntityType(String id) { customEntityTypes.remove(id); markAndSync(); }
    public void addFriendlyPlayerName(String name) { if (!friendlyPlayerNames.contains(name)) { friendlyPlayerNames.add(name); markAndSync(); } }
    public void removeFriendlyPlayerName(String name) { friendlyPlayerNames.remove(name); markAndSync(); }

    private void updateTopRotation() {
        if (this.active) {
            this.topSpeed = Math.min(this.topSpeed + TOP_ACCEL, MAX_TOP_SPEED);
        } else {
            this.topSpeed = Math.max(this.topSpeed - TOP_DECEL, 0.0f);
        }
        this.topRotation = (this.topRotation + this.topSpeed) % 360.0f;
    }

    private float getSoundScale() {
        return 1.0f + (multiblockSize - 1) * 0.25f;
    }

    private boolean matchesFilter(net.minecraft.world.entity.Entity e) {
        // Friendly players bypass the shield entirely
        if (!friendlyPlayerNames.isEmpty() && e instanceof net.minecraft.world.entity.player.Player) {
            if (friendlyPlayerNames.contains(e.getName().getString())) return false;
        }
        String key = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString();
        if (customEntityTypes.contains(key)) return true;
        int flags = entityFilterFlags;
        if ((flags & FILTER_PROJECTILES) != 0 && e instanceof Projectile) return true;
        if ((flags & FILTER_MOBS) != 0 && e instanceof net.minecraft.world.entity.Mob) return true;
        if ((flags & FILTER_PLAYERS) != 0 && e instanceof net.minecraft.world.entity.player.Player) return true;
        if ((flags & FILTER_VEHICLES) != 0 && (e instanceof net.minecraft.world.entity.vehicle.Boat || e instanceof net.minecraft.world.entity.vehicle.Minecart)) return true;
        if ((flags & FILTER_MISC) != 0) return true;
        return false;
    }

    private void markAndSync() {
        if (level != null) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public AABB getRenderBoundingBox() {
        double rx = getEffectiveRadiusX();
        double ry = getEffectiveRadiusY();
        double rz = getEffectiveRadiusZ();
        double cx = shieldCenterX + offX;
        double cy = shieldCenterY + offY;
        double cz = shieldCenterZ + offZ;
        double beamTop = shieldCenterY + offY + ry;
        double minY = Math.min(cy - ry, worldPosition.getY());
        double maxY = Math.max(cy + ry, beamTop);
        double mbMinX = worldPosition.getX();
        double mbMinZ = worldPosition.getZ();
        double mbMaxX = worldPosition.getX() + multiblockSize;
        double mbMaxZ = worldPosition.getZ() + multiblockSize;
        double minX = Math.min(cx - rx, mbMinX);
        double minZ = Math.min(cz - rz, mbMinZ);
        double maxX = Math.max(cx + rx, mbMaxX);
        double maxZ = Math.max(cz + rz, mbMaxZ);
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ).inflate(5);
    }

}
