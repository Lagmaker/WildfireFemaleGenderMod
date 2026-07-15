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

package com.wildfire.main.entitydata;

import com.wildfire.main.config.Configuration;
import com.wildfire.main.config.enums.BreastShape;
import com.wildfire.main.config.types.ConfigKey;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.joml.Vector3f;

import java.util.function.Consumer;

/**
 * Data class representing an entity's breast appearance settings
 */
@SuppressWarnings("UnusedReturnValue")
public final class Breasts {

    public static final StreamCodec<ByteBuf, Breasts> CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, Breasts::getXOffset,
            ByteBufCodecs.FLOAT, Breasts::getYOffset,
            ByteBufCodecs.FLOAT, Breasts::getZOffset,
            ByteBufCodecs.BOOL, Breasts::isUniboob,
            ByteBufCodecs.FLOAT, Breasts::getCleavage,
            ShapeTuning.CODEC, Breasts::shapeTuning,
            BreastShape.STREAM_CODEC, Breasts::getShape,
            ByteBufCodecs.BOOL, Breasts::hasNipples,
            ByteBufCodecs.FLOAT, Breasts::getNippleSize,
            (x, y, z, uniboob, cleavage, tuning, shape, nipples, nippleSize) -> {
                Breasts breasts = new Breasts();
                breasts.updateXOffset(x);
                breasts.updateYOffset(y);
                breasts.updateZOffset(z);
                breasts.updateUniboob(uniboob);
                breasts.updateCleavage(cleavage);
                tuning.applyTo(breasts);
                breasts.updateShape(shape);
                breasts.updateNipples(nipples);
                breasts.updateNippleSize(nippleSize);
                return breasts;
            }
    );

    private float xOffset = Configuration.BREASTS_OFFSET_X.getDefault(),
            yOffset = Configuration.BREASTS_OFFSET_Y.getDefault(),
            zOffset = Configuration.BREASTS_OFFSET_Z.getDefault();
    private float cleavage = Configuration.BREASTS_CLEAVAGE.getDefault();
    private float width = Configuration.BREASTS_WIDTH.getDefault();
    private float height = Configuration.BREASTS_HEIGHT.getDefault();
    private float projection = Configuration.BREASTS_PROJECTION.getDefault();
    private float balance = Configuration.BREASTS_BALANCE.getDefault();
    private float rootWidth = Configuration.BREASTS_ROOT_WIDTH.getDefault();
    private float outerFullness = Configuration.BREASTS_OUTER_FULLNESS.getDefault();
    private float drop = Configuration.BREASTS_DROP.getDefault();
    private BreastShape shape = Configuration.BREASTS_SHAPE.getDefault();
    private boolean nipples = Configuration.BREASTS_NIPPLES.getDefault();
    private float nippleSize = Configuration.BREASTS_NIPPLE_SIZE.getDefault();
    private boolean uniboob = Configuration.BREASTS_UNIBOOB.getDefault();

    private <VALUE> boolean updateValue(ConfigKey<VALUE> key, VALUE value, Consumer<VALUE> setter) {
        if (key.validate(value)) {
            setter.accept(value);
            return true;
        }
        return false;
    }

    public Vector3f getOffsets() {
        return new Vector3f(xOffset, yOffset, zOffset);
    }

    public void updateOffsets(Vector3f offsets) {
        updateXOffset(offsets.x);
        updateYOffset(offsets.y);
        updateZOffset(offsets.z);
    }

    /**
     * How far apart the player's breasts should be rendered from each other, also referred to as Separation in the UI
     *
     * @implNote Negative float values renders the breasts further apart, while positive values renders them closer together
     *
     * @return  A {@code float} between {@code -1f} and {@code 1f}
     */
    public float getXOffset() {
        return xOffset;
    }

    /**
     * @see #getXOffset()
     */
    public boolean updateXOffset(float value) {
        return updateValue(Configuration.BREASTS_OFFSET_X, value, v -> this.xOffset = v);
    }

    /**
     * How far up or down the generated breast volume is placed on the torso.
     *
     * @implNote Negative values renders the breasts lower down, while positive values renders them higher up
     *
     * @return a value validated by {@link Configuration#BREASTS_OFFSET_Y}
     */
    public float getYOffset() {
        return yOffset;
    }

    /**
     * @see #getYOffset()
     */
    public boolean updateYOffset(float value) {
        return updateValue(Configuration.BREASTS_OFFSET_Y, value, v -> this.yOffset = v);
    }

    /**
     * How far forward or back the generated breast volume is placed on the torso.
     *
     * @return a value validated by {@link Configuration#BREASTS_OFFSET_Z}
     */
    public float getZOffset() {
        return zOffset;
    }

    /**
     * @see #getZOffset()
     */
    public boolean updateZOffset(float value) {
        return updateValue(Configuration.BREASTS_OFFSET_Z, value, v -> this.zOffset = v);
    }

    /**
     * Signed inward/outward angle applied independently from volume expansion.
     *
     * @return a value validated by {@link Configuration#BREASTS_CLEAVAGE}
     */
    public float getCleavage() {
        return cleavage;
    }

    /**
     * @see #getCleavage()
     */
    public boolean updateCleavage(float value) {
        return updateValue(Configuration.BREASTS_CLEAVAGE, value, v -> this.cleavage = v);
    }

    public float getWidth() {
        return width;
    }

    public boolean updateWidth(float value) {
        return updateValue(Configuration.BREASTS_WIDTH, value, v -> this.width = v);
    }

    public float getHeight() {
        return height;
    }

    public boolean updateHeight(float value) {
        return updateValue(Configuration.BREASTS_HEIGHT, value, v -> this.height = v);
    }

    public float getProjection() {
        return projection;
    }

    public boolean updateProjection(float value) {
        return updateValue(Configuration.BREASTS_PROJECTION, value, v -> this.projection = v);
    }

    /**
     * Relative size difference between the left and right breast. Positive values enlarge the
     * left side and reduce the right side by the same amount.
     */
    public float getBalance() {
        return balance;
    }

    public boolean updateBalance(float value) {
        return updateValue(Configuration.BREASTS_BALANCE, value, v -> this.balance = v);
    }

    public float getRootWidth() {
        return rootWidth;
    }

    public boolean updateRootWidth(float value) {
        return updateValue(Configuration.BREASTS_ROOT_WIDTH, value, v -> this.rootWidth = v);
    }

    public float getOuterFullness() {
        return outerFullness;
    }

    public boolean updateOuterFullness(float value) {
        return updateValue(Configuration.BREASTS_OUTER_FULLNESS, value, v -> this.outerFullness = v);
    }

    public float getDrop() {
        return drop;
    }

    public boolean updateDrop(float value) {
        return updateValue(Configuration.BREASTS_DROP, value, v -> this.drop = v);
    }

    public BreastShape getShape() {
        return shape;
    }

    public boolean updateShape(BreastShape value) {
        return updateValue(Configuration.BREASTS_SHAPE, value, v -> this.shape = v);
    }

    public boolean hasNipples() {
        return nipples;
    }

    public boolean updateNipples(boolean value) {
        return updateValue(Configuration.BREASTS_NIPPLES, value, v -> this.nipples = v);
    }

    public float getNippleSize() {
        return nippleSize;
    }

    public boolean updateNippleSize(float value) {
        return updateValue(Configuration.BREASTS_NIPPLE_SIZE, value, v -> this.nippleSize = v);
    }

    public void resetVolume() {
        updateWidth(Configuration.BREASTS_WIDTH.getDefault());
        updateHeight(Configuration.BREASTS_HEIGHT.getDefault());
        updateProjection(Configuration.BREASTS_PROJECTION.getDefault());
        updateBalance(Configuration.BREASTS_BALANCE.getDefault());
    }

    public void resetShape() {
        updateCleavage(Configuration.BREASTS_CLEAVAGE.getDefault());
        updateRootWidth(Configuration.BREASTS_ROOT_WIDTH.getDefault());
        updateOuterFullness(Configuration.BREASTS_OUTER_FULLNESS.getDefault());
        updateDrop(Configuration.BREASTS_DROP.getDefault());
        updateShape(Configuration.BREASTS_SHAPE.getDefault());
    }

    /**
     * Compatibility reset for callers that treated expansion, placement, and silhouette as one panel.
     */
    public void resetExpansion() {
        resetPlacement();
        updateRootWidth(Configuration.BREASTS_ROOT_WIDTH.getDefault());
        updateOuterFullness(Configuration.BREASTS_OUTER_FULLNESS.getDefault());
        updateDrop(Configuration.BREASTS_DROP.getDefault());
    }

    public void resetPlacement() {
        updateXOffset(Configuration.BREASTS_OFFSET_X.getDefault());
        updateYOffset(Configuration.BREASTS_OFFSET_Y.getDefault());
        updateZOffset(Configuration.BREASTS_OFFSET_Z.getDefault());
    }

    /**
     * Determines if breast physics should be independent of each other; also referred to as Dual-Physics in the UI
     *
     * @return {@code false} if physics should be independent on each breast, {@code true} if both should use the same physics
     */
    public boolean isUniboob() {
        return uniboob;
    }

    /**
     * @see #isUniboob()
     */
    public boolean updateUniboob(boolean value) {
        return updateValue(Configuration.BREASTS_UNIBOOB, value, v -> this.uniboob = v);
    }

    /**
     * Copy settings from the provided {@link Breasts breasts data} onto the current instance
     */
    public void copyFrom(Breasts breasts) {
        updateXOffset(breasts.xOffset);
        updateYOffset(breasts.yOffset);
        updateZOffset(breasts.zOffset);
        updateCleavage(breasts.cleavage);
        updateUniboob(breasts.uniboob);
        updateWidth(breasts.width);
        updateHeight(breasts.height);
        updateProjection(breasts.projection);
        updateBalance(breasts.balance);
        updateRootWidth(breasts.rootWidth);
        updateOuterFullness(breasts.outerFullness);
        updateDrop(breasts.drop);
        updateShape(breasts.shape);
        updateNipples(breasts.nipples);
        updateNippleSize(breasts.nippleSize);
    }

    private ShapeTuning shapeTuning() {
        return new ShapeTuning(width, height, projection, balance, rootWidth, outerFullness, drop);
    }

    private record ShapeTuning(float width, float height, float projection, float balance,
                               float rootWidth, float outerFullness, float drop) {
        private static final StreamCodec<ByteBuf, ShapeTuning> CODEC = StreamCodec.composite(
                ByteBufCodecs.FLOAT, ShapeTuning::width,
                ByteBufCodecs.FLOAT, ShapeTuning::height,
                ByteBufCodecs.FLOAT, ShapeTuning::projection,
                ByteBufCodecs.FLOAT, ShapeTuning::balance,
                ByteBufCodecs.FLOAT, ShapeTuning::rootWidth,
                ByteBufCodecs.FLOAT, ShapeTuning::outerFullness,
                ByteBufCodecs.FLOAT, ShapeTuning::drop,
                ShapeTuning::new
        );

        private void applyTo(Breasts breasts) {
            breasts.updateWidth(width);
            breasts.updateHeight(height);
            breasts.updateProjection(projection);
            breasts.updateBalance(balance);
            breasts.updateRootWidth(rootWidth);
            breasts.updateOuterFullness(outerFullness);
            breasts.updateDrop(drop);
        }
    }
}
