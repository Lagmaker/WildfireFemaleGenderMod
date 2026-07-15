/*
 * Wildfire's Female Gender Mod is a female gender mod created for Minecraft.
 * Copyright (C) 2023-present WildfireRomeo
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 */

package com.wildfire.render;

import com.wildfire.main.uvs.UVDirection;
import com.wildfire.main.uvs.UVLayout;
import com.wildfire.main.uvs.UVQuad;
import com.wildfire.render.WildfireModelRenderer.ModelBox;
import com.wildfire.render.WildfireModelRenderer.PositionTextureVertex;
import com.wildfire.render.WildfireModelRenderer.TexturedQuad;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * A small tapered octagonal skin-detail mesh aligned to the selected breast profile's surface.
 */
public final class NippleDetailModelBox extends ModelBox {
    private static final int SEGMENTS = 8;

    public NippleDetailModelBox(int textureWidth, int textureHeight, float centerX, float centerY,
                                float surfaceZ, float diameter, float projection, Vector3f surfaceNormal,
                                UVLayout skinLayout) {
        super(centerX - diameter, centerY - diameter, surfaceZ - projection - diameter,
                centerX + diameter, centerY + diameter, surfaceZ + diameter,
                skinLayout, createQuads(textureWidth, textureHeight, centerX, centerY, surfaceZ,
                        diameter, projection, surfaceNormal, detailUv(skinLayout)));
    }

    private static TexturedQuad[] createQuads(int textureWidth, int textureHeight, float centerX,
                                               float centerY, float surfaceZ, float diameter,
                                               float projection, Vector3f suppliedNormal,
                                               @Nullable UVQuad uv) {
        if(uv == null) {
            return new TexturedQuad[0];
        }

        Vector3f axis = new Vector3f(suppliedNormal);
        if(axis.lengthSquared() < 1.0E-6f) {
            axis.set(0, 0, -1);
        } else {
            axis.normalize();
        }

        Vector3f tangentX = new Vector3f(1, 0, 0);
        Vector3f tangentY = new Vector3f(tangentX).cross(axis).normalize();
        Vector3f surface = new Vector3f(centerX, centerY, surfaceZ);
        Vector3f baseCenter = new Vector3f(surface).fma(-0.04f, axis);
        Vector3f tipCenter = new Vector3f(surface).fma(projection, axis);

        float baseRadius = diameter * 0.5f;
        float tipRadius = baseRadius * 0.58f;
        Vector3f[] base = ring(baseCenter, tangentX, tangentY, baseRadius);
        Vector3f[] tip = ring(tipCenter, tangentX, tangentY, tipRadius);

        List<TexturedQuad> quads = new ArrayList<>(SEGMENTS * 2);
        for(int segment = 0; segment < SEGMENTS; segment++) {
            int next = (segment + 1) % SEGMENTS;
            addQuad(quads, uv, textureWidth, textureHeight,
                    vertex(base[next]), vertex(base[segment]), vertex(tip[segment]), vertex(tip[next]));

            // A duplicated center vertex lets the existing quad renderer draw a clean triangular cap.
            addQuad(quads, uv, textureWidth, textureHeight,
                    vertex(tipCenter), vertex(tip[next]), vertex(tip[segment]), vertex(tipCenter));
        }
        return quads.toArray(TexturedQuad[]::new);
    }

    private static Vector3f[] ring(Vector3f center, Vector3f tangentX, Vector3f tangentY, float radius) {
        Vector3f[] points = new Vector3f[SEGMENTS];
        for(int segment = 0; segment < SEGMENTS; segment++) {
            float angle = (float) (Math.PI * 2 * segment / SEGMENTS);
            points[segment] = new Vector3f(center)
                    .fma(Mth.cos(angle) * radius, tangentX)
                    .fma(Mth.sin(angle) * radius, tangentY);
        }
        return points;
    }

    private static void addQuad(List<TexturedQuad> output, UVQuad uv, int textureWidth, int textureHeight,
                                PositionTextureVertex... vertices) {
        output.add(new TexturedQuad(uv.x1(), uv.y1(), uv.x2(), uv.y2(),
                textureWidth, textureHeight, vertices));
    }

    private static PositionTextureVertex vertex(Vector3f point) {
        return new PositionTextureVertex(point.x, point.y, point.z, 0, 0);
    }

    private static @Nullable UVQuad detailUv(UVLayout source) {
        UVQuad front = source.get(UVDirection.NORTH);
        if(front == null) return null;
        if(front.x1() == 0 && front.y1() == 0 && front.x2() == 0 && front.y2() == 0) {
            return front;
        }
        int x = Math.min(front.x1(), front.x2()) + Math.abs(front.x2() - front.x1()) / 2;
        int y = Math.min(front.y1(), front.y2()) + Math.abs(front.y2() - front.y1()) / 2;
        return new UVQuad(x, y, x + 1, y + 1);
    }
}
