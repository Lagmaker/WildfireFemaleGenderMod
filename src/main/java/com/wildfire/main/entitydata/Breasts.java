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
            ByteBufCodecs.FLOAT, Breasts::getWidth,
            ByteBufCodecs.FLOAT, Breasts::getHeight,
            ByteBufCodecs.FLOAT, Breasts::getProjection,
            ByteBufCodecs.FLOAT, Breasts::getBalance,
            (x, y, z, uniboob, cleavage, width, height, projection, balance) -> {
                Breasts breasts = new Breasts();
                breasts.updateXOffset(x);
                breasts.updateYOffset(y);
                breasts.updateZOffset(z);
                breasts.updateUniboob(uniboob);
                breasts.updateCleavage(cleavage);
                breasts.updateWidth(width);
                breasts.updateHeight(height);
                breasts.updateProjection(projection);
                breasts.updateBalance(balance);
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
     * How far up or down the player's breasts should be rendered, also referred to as Height in the UI
     *
     * @implNote Negative values renders the breasts lower down, while positive values renders them higher up
     *
     * @return  A {@code float} between {@code -1f} and {@code 1f}
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
     * How far back the player's breasts should be rendered, also referred to as Depth in the UI
     *
     * @return  A {@code float} between {@code 0f} and {@code 1f}
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
     * How much rotation outward there should be on each of the player's breasts
     *
     * @return  A {@code float} between {@code 0f} and {@code 0.1f}
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

    public void resetShape() {
        updateWidth(Configuration.BREASTS_WIDTH.getDefault());
        updateHeight(Configuration.BREASTS_HEIGHT.getDefault());
        updateProjection(Configuration.BREASTS_PROJECTION.getDefault());
        updateBalance(Configuration.BREASTS_BALANCE.getDefault());
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
    }
}
