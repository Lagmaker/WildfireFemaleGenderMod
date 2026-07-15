/*
 * Wildfire's Female Gender Mod is a female gender mod created for Minecraft.
 * Copyright (C) 2023-present WildfireRomeo
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.wildfire.physics;

import com.wildfire.api.IGenderArmor;
import com.wildfire.main.entitydata.EntityConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A deterministic secondary-motion rig driven by the entity's measured motion.
 *
 * <p>Each visible degree of freedom is a critically/under-damped spring. The spring's mass is derived
 * from the configured breast size, while floppiness controls stiffness and damping independently of
 * the motion amplitude. This keeps the editor's extreme ranges expressive without making the numeric
 * integration unstable.</p>
 */
public class BreastPhysics {

    public static final float TIGHTNESS_REDUCTION_FACTOR = 0.15F;

    private static final float MAX_CONFIGURED_SIZE = 8F;
    private static final float MAX_BOUNCE = 2F;
    private static final float MIN_FLOPPINESS = 0.05F;
    private static final float MAX_FLOPPINESS = 1.5F;
    private static final float MAX_WOBBLE_INTENSITY = 3F;
    private static final float MIN_WOBBLE_SPEED = 0.1F;
    private static final float MAX_WOBBLE_SPEED = 4F;

    // Large discontinuities are teleports/dimension transitions, not useful physical impulses.
    private static final double TELEPORT_DISTANCE_SQUARED = 16D;

    private final SpringAxis lateral = new SpringAxis();
    private final SpringAxis vertical = new SpringAxis();
    private final SpringAxis rotation = new SpringAxis();
    private final SpringAxis shape = new SpringAxis();

    private float breastSize;
    private float preBreastSize;

    private final EntityConfig entityConfig;
    private final float sidePhase;

    private @Nullable Vec3 previousPosition;
    private Vec3 previousMotion = Vec3.ZERO;
    private float previousTurnRate;
    private float previousArmSignal;

    public BreastPhysics(EntityConfig entityConfig) {
        this(entityConfig, 0F);
    }

    /**
     * @param sidePhase phase offset in radians, used to give separately simulated breasts coherent but
     *                  non-identical gait and arm responses
     */
    public BreastPhysics(EntityConfig entityConfig, float sidePhase) {
        this.entityConfig = entityConfig;
        this.sidePhase = Float.isFinite(sidePhase) ? sidePhase : 0F;
    }

    // This class cannot be blanket marked as client-side only, as it is constructed by EntityConfig.
    @Environment(EnvType.CLIENT)
    public void update(LivingEntity entity, IGenderArmor armor) {
        snapshot();

        if(entity instanceof ArmorStand || entityConfig.forceSimplifiedPhysics) {
            simplifiedTick(armor);
            return;
        }

        float tightness = entityConfig.getArmorPhysicsOverride()
                ? 0F : finiteClamp(armor.tightness(), 0F, 1F);
        float resistance = entityConfig.getArmorPhysicsOverride()
                ? 0F : finiteClamp(armor.physicsResistance(), 0F, 1F);
        float configuredSize = configuredSize();
        float targetSize = configuredSize * (1F - TIGHTNESS_REDUCTION_FACTOR * tightness);

        Vec3 currentPosition = entity.position();
        if(previousPosition == null) {
            rebase(entity, currentPosition, targetSize);
            return;
        }

        // Shape changes are presentation changes, not forces. Smooth them without moving the spring equilibrium.
        breastSize += (targetSize - breastSize) * 0.38F;
        if(Math.abs(targetSize - breastSize) < 0.0001F) {
            breastSize = targetSize;
        }

        Vec3 motion = currentPosition.subtract(previousPosition);
        previousPosition = currentPosition;
        if(motion.lengthSqr() > TELEPORT_DISTANCE_SQUARED || !isFinite(motion)) {
            previousMotion = Vec3.ZERO;
            previousTurnRate = currentTurnRate(entity);
            previousArmSignal = 0F;
            settleWithoutDrive(tightness, resistance, configuredSize);
            return;
        }

        Vec3 acceleration = motion.subtract(previousMotion);
        previousMotion = motion;

        float yaw = entity.yBodyRot * Mth.DEG_TO_RAD;
        float cosYaw = Mth.cos(yaw);
        float sinYaw = Mth.sin(yaw);
        float localSideAcceleration = finiteClamp(
                (float) acceleration.x * cosYaw + (float) acceleration.z * sinYaw, -1.25F, 1.25F);
        float localForwardAcceleration = finiteClamp(
                -(float) acceleration.x * sinYaw + (float) acceleration.z * cosYaw, -1.25F, 1.25F);
        float verticalAcceleration = finiteClamp((float) acceleration.y, -1.5F, 1.5F);

        float turnRate = currentTurnRate(entity);
        float turnAcceleration = Mth.clamp(turnRate - previousTurnRate, -45F, 45F);
        previousTurnRate = turnRate;

        AnimationDrive animation = animationDrive(entity);
        tickRig(configuredSize, tightness, resistance, verticalAcceleration, localSideAcceleration,
                localForwardAcceleration, turnRate, turnAcceleration, animation);
    }

    private void snapshot() {
        lateral.snapshot();
        vertical.snapshot();
        rotation.snapshot();
        shape.snapshot();
        preBreastSize = breastSize;
    }

    private float configuredSize() {
        if(!entityConfig.getGender().canHaveBreasts()) {
            return 0F;
        }
        return finiteClamp(entityConfig.getBustSize(), 0F, MAX_CONFIGURED_SIZE);
    }

    private void rebase(LivingEntity entity, Vec3 position, float targetSize) {
        previousPosition = position;
        previousMotion = Vec3.ZERO;
        previousTurnRate = currentTurnRate(entity);
        previousArmSignal = 0F;
        breastSize = preBreastSize = targetSize;
    }

    private void simplifiedTick(IGenderArmor armor) {
        float tightness = entityConfig.getArmorPhysicsOverride()
                ? 0F : finiteClamp(armor.tightness(), 0F, 1F);
        breastSize = configuredSize() * (1F - TIGHTNESS_REDUCTION_FACTOR * tightness);
        preBreastSize = breastSize;
        lateral.reset();
        vertical.reset();
        rotation.reset();
        shape.reset();
        previousPosition = null;
        previousMotion = Vec3.ZERO;
        previousTurnRate = 0F;
        previousArmSignal = 0F;
    }

    private void settleWithoutDrive(float tightness, float resistance, float size) {
        tickRig(size, tightness, resistance, 0F, 0F, 0F, 0F, 0F, AnimationDrive.NONE);
    }

    private void tickRig(float size, float tightness, float resistance,
                         float verticalAcceleration, float sideAcceleration, float forwardAcceleration,
                         float turnRate, float turnAcceleration, AnimationDrive animation) {
        boolean physicsEnabled = entityConfig.hasBreastPhysics() && entityConfig.getGender().canHaveBreasts();
        float bounce = physicsEnabled ? finiteClamp(entityConfig.getBounceMultiplier(), 0F, MAX_BOUNCE) : 0F;
        float floppiness = finiteClamp(entityConfig.getFloppiness(), MIN_FLOPPINESS, MAX_FLOPPINESS);
        float floppyUnit = (floppiness - MIN_FLOPPINESS) / (MAX_FLOPPINESS - MIN_FLOPPINESS);

        // Mass grows continuously throughout the full 0..8 editor range. Large sizes respond more slowly and
        // carry more momentum; an exponential response keeps small sizes precise without flattening the high end.
        float sizeResponse = 1F - (float) Math.exp(-size * 0.42F);
        float mass = 0.78F + size * 0.28F + (float) Math.sqrt(size) * 0.22F;
        float rootMass = (float) Math.sqrt(mass);

        float support = (1F - resistance * 0.78F) * (1F - tightness * 0.58F);
        float driveGain = bounce * (0.45F + sizeResponse * 1.25F) * support;

        float baseStiffness = (0.29F - floppyUnit * 0.215F) / rootMass;
        float supportStiffness = 1F + tightness * 0.9F + resistance * 0.55F;
        float verticalStiffness = Mth.clamp(baseStiffness * supportStiffness, 0.035F, 0.38F);
        float lateralStiffness = Mth.clamp(verticalStiffness * 1.12F, 0.04F, 0.42F);
        float rotationStiffness = Mth.clamp(verticalStiffness * 0.84F, 0.03F, 0.34F);
        float dampingRatio = Mth.clamp(0.98F - floppyUnit * 0.64F
                + tightness * 0.34F + resistance * 0.24F, 0.30F, 1.35F);

        // Local acceleration is inertial drive. Gait and arm movement approximate torso acceleration that is not
        // represented in the entity's root position. No random values or entity/vehicle-specific branches are used.
        float targetY = (verticalAcceleration * 5.4F
                - forwardAcceleration * 1.7F
                + animation.vertical() * 0.72F
                + animation.arm() * 0.18F) * driveGain;
        float targetX = (-sideAcceleration * 4.25F
                - turnAcceleration * 0.025F
                + animation.lateral() * 0.42F
                + animation.arm() * 0.62F) * driveGain;
        float targetRotation = (-turnRate * (0.62F + sizeResponse * 0.58F)
                - sideAcceleration * 10F
                + animation.arm() * 6.5F) * driveGain;

        float yLimit = 0.65F + sizeResponse * 2.25F + bounce * 0.55F;
        float xLimit = 0.45F + sizeResponse * 1.55F + bounce * 0.42F;
        float rotationLimit = 8F + sizeResponse * 22F + bounce * 4F;
        targetY = Mth.clamp(targetY, -yLimit, yLimit);
        targetX = Mth.clamp(targetX, -xLimit, xLimit);
        targetRotation = Mth.clamp(targetRotation, -rotationLimit, rotationLimit);

        float oldVerticalVelocity = vertical.velocity;
        float oldLateralVelocity = lateral.velocity;
        float oldAngularVelocity = rotation.velocity;

        vertical.step(targetY, 0F, verticalStiffness, dampingRatio, yLimit, 1.9F, 1.25F);
        lateral.step(targetX, 0F, lateralStiffness, dampingRatio + 0.06F, xLimit, 1.35F, 0.95F);
        rotation.step(targetRotation, 0F, rotationStiffness, dampingRatio + 0.08F,
                rotationLimit, 13F, 9F);

        float primaryVerticalAcceleration = vertical.velocity - oldVerticalVelocity;
        float primaryLateralAcceleration = lateral.velocity - oldLateralVelocity;
        float primaryAngularAcceleration = rotation.velocity - oldAngularVelocity;
        tickShapeWobble(sizeResponse, mass, floppyUnit, tightness, resistance,
                primaryVerticalAcceleration, primaryLateralAcceleration, primaryAngularAcceleration,
                forwardAcceleration, animation.vertical());

        if(!physicsEnabled) {
            // Disabled physics should be calm when re-enabled instead of preserving an invisible impulse forever.
            previousArmSignal = 0F;
        }
    }

    private void tickShapeWobble(float sizeResponse, float mass, float floppyUnit,
                                 float tightness, float resistance,
                                 float verticalAcceleration, float lateralAcceleration,
                                 float angularAcceleration, float forwardAcceleration, float gaitDrive) {
        boolean enabled = entityConfig.hasBreastPhysics() && entityConfig.hasWobble()
                && entityConfig.getGender().canHaveBreasts();
        float intensity = enabled
                ? finiteClamp(entityConfig.getWobbleIntensity(), 0F, MAX_WOBBLE_INTENSITY) : 0F;
        float speed = finiteClamp(entityConfig.getWobbleSpeed(), MIN_WOBBLE_SPEED, MAX_WOBBLE_SPEED);

        // Shape deformation is coupled to changes in the primary rig, not a second copy of position bounce.
        // This produces a delayed squash/stretch response after impacts and direction changes.
        float coupling = 0.48F + sizeResponse * 0.82F;
        float drive = -(verticalAcceleration * 0.34F
                + lateralAcceleration * 0.075F
                + angularAcceleration * 0.006F
                + forwardAcceleration * 0.075F
                + gaitDrive * 0.012F) * intensity * coupling;
        drive = Mth.clamp(drive, -0.18F, 0.18F);

        float speedUnit = (speed - MIN_WOBBLE_SPEED) / (MAX_WOBBLE_SPEED - MIN_WOBBLE_SPEED);
        float stiffness = (0.028F + speedUnit * 0.225F) / (float) Math.pow(mass, 0.18F);
        float dampingRatio = Mth.clamp(0.42F + (1F - floppyUnit) * 0.24F
                + tightness * 0.42F + resistance * 0.36F, 0.34F, 1.4F);

        if(!enabled) {
            stiffness = Math.max(stiffness, 0.12F);
            dampingRatio = Math.max(dampingRatio, 1F);
        }

        float limit = Mth.clamp(0.055F + intensity * 0.22F * (0.4F + sizeResponse * 0.6F),
                0.055F, 0.62F);
        shape.step(0F, drive, stiffness, dampingRatio, limit, 0.32F, 0.18F);
    }

    private AnimationDrive animationDrive(LivingEntity entity) {
        float walkSpeed = finiteClamp(entity.walkAnimation.speed(), 0F, 1.5F);
        float walkAmount = walkSpeed * walkSpeed;
        float walkPhase = entity.walkAnimation.position() * 0.6662F;
        float gaitVertical = Mth.sin(walkPhase * 2F + sidePhase * 0.35F) * walkAmount;
        float gaitLateral = Mth.sin(walkPhase + sidePhase) * walkAmount;

        float armSignal = 0F;
        if(entity.swinging) {
            int duration = Math.max(entity.getCurrentSwingDuration(), 1);
            float progress = Mth.clamp((float) entity.swingTime / duration, 0F, 1F);
            HumanoidArm arm = entity.swingingArm == InteractionHand.MAIN_HAND
                    ? entity.getMainArm() : entity.getMainArm().getOpposite();
            float armSide = arm == HumanoidArm.RIGHT ? 1F : -1F;
            armSignal = Mth.sin(progress * Mth.PI) * armSide;
        }

        // The change in the arm animation is an impulse on the torso. Keeping this stateful makes a stopped or
        // interrupted swing naturally produce a counter-impulse, while remaining completely deterministic.
        float armImpulse = Mth.clamp(armSignal - previousArmSignal, -1F, 1F);
        previousArmSignal = armSignal;
        return new AnimationDrive(gaitVertical, gaitLateral, armImpulse);
    }

    private static float currentTurnRate(LivingEntity entity) {
        return Mth.clamp(Mth.wrapDegrees(entity.yBodyRot - entity.yBodyRotO), -90F, 90F);
    }

    private static boolean isFinite(Vec3 value) {
        return Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
    }

    private static float finiteClamp(float value, float min, float max) {
        return Float.isFinite(value) ? Mth.clamp(value, min, max) : min;
    }

    public float getPrePositionY() {
        return vertical.previous;
    }

    public float getPositionY() {
        return vertical.position;
    }

    public float getPrePositionX() {
        return lateral.previous;
    }

    public float getPositionX() {
        return lateral.position;
    }

    public float getBounceRotation() {
        return rotation.position;
    }

    public float getPreBounceRotation() {
        return rotation.previous;
    }

    public float getBreastSize() {
        return breastSize;
    }

    public float getPreBreastSize() {
        return preBreastSize;
    }

    public float getWobble() {
        return shape.position;
    }

    public float getPreWobble() {
        return shape.previous;
    }

    /** Adds a bounded impulse for the customization-screen motion preview. */
    public void addPreviewImpulse(float strength) {
        if(!Float.isFinite(strength)) {
            return;
        }
        float impulse = Mth.clamp(strength, -2F, 2F);
        vertical.impulse(impulse * 1.35F, 1.9F);
        lateral.impulse(Mth.sin(sidePhase) * impulse * 0.18F, 1.35F);
        rotation.impulse(Mth.sin(sidePhase) * impulse * 2.4F, 13F);
        shape.impulse(-impulse * 0.42F, 0.32F);
    }

    private record AnimationDrive(float vertical, float lateral, float arm) {
        private static final AnimationDrive NONE = new AnimationDrive(0F, 0F, 0F);
    }

    /** Fixed-timestep, semi-implicit damped spring with hard safety bounds. */
    private static final class SpringAxis {
        private float position;
        private float previous;
        private float velocity;

        private void snapshot() {
            previous = position;
        }

        private void step(float target, float drive, float stiffness, float dampingRatio,
                          float positionLimit, float velocityLimit, float accelerationLimit) {
            stiffness = Mth.clamp(stiffness, 0.005F, 0.45F);
            dampingRatio = Mth.clamp(dampingRatio, 0.2F, 1.5F);
            float damping = 2F * dampingRatio * (float) Math.sqrt(stiffness);
            // Semi-implicit Euler is stable only below its discrete damping boundary. Tight armor can
            // legitimately request the maximum lateral stiffness and damping together, so constrain
            // the integrator coefficient rather than the user's physical controls.
            damping = Math.min(damping, 1.98F - stiffness * 0.5F);
            float acceleration = (target - position) * stiffness + drive - velocity * damping;
            acceleration = Mth.clamp(acceleration, -accelerationLimit, accelerationLimit);
            velocity = Mth.clamp(velocity + acceleration, -velocityLimit, velocityLimit);
            position += velocity;

            if(position > positionLimit) {
                position = positionLimit;
                if(velocity > 0F) velocity = 0F;
            } else if(position < -positionLimit) {
                position = -positionLimit;
                if(velocity < 0F) velocity = 0F;
            }

            if(!Float.isFinite(position) || !Float.isFinite(velocity)) {
                reset();
            }
        }

        private void impulse(float amount, float velocityLimit) {
            if(Float.isFinite(amount)) {
                velocity = Mth.clamp(velocity + amount, -velocityLimit, velocityLimit);
            }
        }

        private void reset() {
            position = 0F;
            previous = 0F;
            velocity = 0F;
        }
    }
}
