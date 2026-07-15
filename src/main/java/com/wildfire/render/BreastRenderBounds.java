/*
 * Wildfire's Female Gender Mod is a female gender mod created for Minecraft.
 * Copyright (C) 2023-present WildfireRomeo
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License, version 3 or later.
 */

package com.wildfire.render;

import com.wildfire.main.entitydata.Breasts;
import com.wildfire.main.entitydata.EntityConfig;

/** Conservative visual bounds for generated geometry that can extend beyond the vanilla entity box. */
public final class BreastRenderBounds {
    private static final double MAX_RADIUS_BLOCKS = 128D;

    private BreastRenderBounds() {
        throw new UnsupportedOperationException();
    }

    public static double estimateRadius(EntityConfig config) {
        // Geometry eases between configured sizes, so bounds must follow both interpolation endpoints.
        // This also covers the brief shrink-out after changing to a gender without breast rendering.
        float bust = maxFinite(config.getBustSize(),
                config.getLeftBreastPhysics().getBreastSize(),
                config.getLeftBreastPhysics().getPreBreastSize(),
                config.getRightBreastPhysics().getBreastSize(),
                config.getRightBreastPhysics().getPreBreastSize());
        bust = finiteClamp(bust, 0F, 8F);
        if(bust < 0.02F) return 0D;
        Breasts breasts = config.getBreasts();
        float fullness = finiteClamp(breasts.getOuterFullness(), 0.1F, 5F);
        float root = finiteClamp(breasts.getRootWidth(), 0.25F, 4F);
        float width = finiteClamp(breasts.getWidth(), 0.1F, 5F);
        float height = finiteClamp(breasts.getHeight(), 0.1F, 5F);
        float projectionScale = finiteClamp(breasts.getProjection(), 0.1F, 7F);
        float balanceScale = 1F + Math.abs(finiteClamp(breasts.getBalance(), -0.95F, 0.95F));
        float drop = Math.abs(finiteClamp(breasts.getDrop(), -3F, 5F));

        float baseX = 1.55F + 0.55F * bust + 0.07F * bust * bust;
        float baseY = 2F + 0.58F * bust + 0.07F * bust * bust;
        float projection = (1.35F + 1.4F * bust + 0.22F * bust * bust)
                * (0.78F + 0.22F * (float) Math.sqrt(fullness));
        float rootX = 1.94F * root * (1F + 0.035F * bust);
        float outerX = baseX * fullness;
        float outerY = baseY * (0.72F + 0.28F * fullness);

        // Includes the widest Anime ring, its lateral fullness, maximum bounded wobble, asymmetric
        // scaling, configured sag, armor inflation, offsets, nipple detail, and physics travel.
        float x = Math.max(rootX, 1.6124F * outerX) * width * balanceScale * (1F + 0.22F * 0.62F);
        float y = (1.728F * outerY + 0.23F * outerY
                + drop * Math.max(1F, 0.52F * outerY))
                * height * balanceScale * (1F + 0.42F * 0.62F);
        float z = projection * projectionScale * balanceScale * (1F + 0.34F * 0.62F);
        double detailMargin = 0D;
        if(breasts.hasNipples()) {
            float nippleSize = finiteClamp(breasts.getNippleSize(), 0.1F, 3F);
            float detailPixels = 0.28F + nippleSize * 0.34F
                    + (0.45F + nippleSize * 0.65F) * 0.5F;
            float detailScale = Math.max(width * balanceScale * (1F + 0.22F * 0.62F),
                    Math.max(height * balanceScale * (1F + 0.42F * 0.62F),
                            projectionScale * balanceScale * (1F + 0.34F * 0.62F)));
            detailMargin = detailPixels * detailScale * 1.1D / 16D;
        }

        double radius = 1.11D * Math.sqrt((double) x * x + (double) y * y + (double) z * z) / 16D
                + 0.75D + detailMargin;
        return Double.isFinite(radius) ? Math.min(MAX_RADIUS_BLOCKS, Math.max(0D, radius)) : MAX_RADIUS_BLOCKS;
    }

    private static float maxFinite(float... values) {
        float maximum = 0F;
        for(float value : values) {
            if(Float.isFinite(value)) maximum = Math.max(maximum, value);
        }
        return maximum;
    }

    private static float finiteClamp(float value, float min, float max) {
        return Float.isFinite(value) ? Math.max(min, Math.min(max, value)) : min;
    }
}
