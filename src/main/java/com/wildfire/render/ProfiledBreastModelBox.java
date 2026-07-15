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

import com.wildfire.main.config.enums.BreastShape;
import com.wildfire.main.uvs.UVDirection;
import com.wildfire.main.uvs.UVLayout;
import com.wildfire.main.uvs.UVQuad;
import com.wildfire.render.WildfireModelRenderer.ModelBox;
import com.wildfire.render.WildfireModelRenderer.PositionTextureVertex;
import com.wildfire.render.WildfireModelRenderer.TexturedQuad;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * A profile-driven, softly faceted breast mesh that preserves the user's existing UV layout.
 *
 * <p>The mesh uses six vertical rings and five horizontal samples. This keeps the recognizable
 * Minecraft aesthetic while giving each profile a real silhouette and curved lighting instead of
 * stretching the legacy cuboid.</p>
 */
public final class ProfiledBreastModelBox extends ModelBox {
    private static final float[] ROWS = {0f, 0.18f, 0.38f, 0.60f, 0.82f, 1f};
    private static final float[] COLUMNS = {-1f, -0.5f, 0f, 0.5f, 1f};

    public ProfiledBreastModelBox(int textureWidth, int textureHeight, float x, BreastShape shape,
                                  UVLayout uvLayout) {
        super(x, 0, 0, x + 4, 5, 3, uvLayout,
                createQuads(textureWidth, textureHeight, x, shape, uvLayout));
    }

    public static float nippleY(BreastShape shape) {
        return profile(shape).nippleY * 5f;
    }

    public static float nippleFrontZ(BreastShape shape) {
        Profile profile = profile(shape);
        float target = profile.nippleY;
        for(int row = 0; row < ROWS.length - 1; row++) {
            if(target >= ROWS[row] && target <= ROWS[row + 1]) {
                float progress = (target - ROWS[row]) / (ROWS[row + 1] - ROWS[row]);
                return lerp(profile.depth[row], profile.depth[row + 1], progress);
            }
        }
        return profile.depth[profile.depth.length - 1];
    }

    public static Vector3f nippleSurfaceNormal(BreastShape shape) {
        Profile profile = profile(shape);
        float target = profile.nippleY;
        for(int row = 0; row < ROWS.length - 1; row++) {
            if(target >= ROWS[row] && target <= ROWS[row + 1]) {
                float ySpan = (ROWS[row + 1] - ROWS[row]) * 5f;
                float slope = (profile.depth[row + 1] - profile.depth[row]) / ySpan;
                return new Vector3f(0, slope, -1).normalize();
            }
        }
        return new Vector3f(0, 0, -1);
    }

    private static TexturedQuad[] createQuads(int textureWidth, int textureHeight, float x,
                                               BreastShape shape, UVLayout layout) {
        Profile profile = profile(shape);
        int rowCount = ROWS.length;
        int columnCount = COLUMNS.length;
        Point[][] front = new Point[rowCount][columnCount];
        float centerX = x + 2f;

        for(int row = 0; row < rowCount; row++) {
            for(int column = 0; column < columnCount; column++) {
                float columnPosition = COLUMNS[column];
                float horizontalCurve = (float) Math.pow(Math.abs(columnPosition), 1.65);
                front[row][column] = new Point(
                        centerX + 2f * profile.width[row] * columnPosition,
                        ROWS[row] * 5f,
                        Math.min(profile.depth[row] + profile.edgeCurve * horizontalCurve, 2.75f)
                );
            }
        }

        List<TexturedQuad> quads = new ArrayList<>(40);
        UVQuad frontUv = layout.get(UVDirection.NORTH);
        for(int row = 0; row < rowCount - 1; row++) {
            for(int column = 0; column < columnCount - 1; column++) {
                addQuad(quads, frontUv, textureWidth, textureHeight,
                        column / (float) (columnCount - 1), ROWS[row],
                        (column + 1) / (float) (columnCount - 1), ROWS[row + 1],
                        vertex(front[row][column + 1]),
                        vertex(front[row][column]),
                        vertex(front[row + 1][column]),
                        vertex(front[row + 1][column + 1]));
            }
        }

        UVQuad eastUv = layout.get(UVDirection.EAST);
        UVQuad westUv = layout.get(UVDirection.WEST);
        for(int row = 0; row < rowCount - 1; row++) {
            float yTop = ROWS[row] * 5f;
            float yBottom = ROWS[row + 1] * 5f;

            addQuad(quads, eastUv, textureWidth, textureHeight, 0, ROWS[row], 1, ROWS[row + 1],
                    vertex(x + 4f, yTop, 3f),
                    vertex(front[row][columnCount - 1]),
                    vertex(front[row + 1][columnCount - 1]),
                    vertex(x + 4f, yBottom, 3f));

            addQuad(quads, westUv, textureWidth, textureHeight, 0, ROWS[row], 1, ROWS[row + 1],
                    vertex(front[row][0]),
                    vertex(x, yTop, 3f),
                    vertex(x, yBottom, 3f),
                    vertex(front[row + 1][0]));
        }

        UVQuad topUv = layout.get(UVDirection.DOWN);
        UVQuad bottomUv = layout.get(UVDirection.UP);
        for(int column = 0; column < columnCount - 1; column++) {
            float lowFraction = column / (float) (columnCount - 1);
            float highFraction = (column + 1) / (float) (columnCount - 1);
            float rearLowX = x + 4f * lowFraction;
            float rearHighX = x + 4f * highFraction;

            addQuad(quads, topUv, textureWidth, textureHeight, lowFraction, 0, highFraction, 1,
                    vertex(rearHighX, 0, 3f),
                    vertex(rearLowX, 0, 3f),
                    vertex(front[0][column]),
                    vertex(front[0][column + 1]));

            addQuad(quads, bottomUv, textureWidth, textureHeight, lowFraction, 0, highFraction, 1,
                    vertex(front[rowCount - 1][column + 1]),
                    vertex(front[rowCount - 1][column]),
                    vertex(rearLowX, 5f, 3f),
                    vertex(rearHighX, 5f, 3f));
        }

        return quads.toArray(TexturedQuad[]::new);
    }

    private static void addQuad(List<TexturedQuad> output, UVQuad uv, int textureWidth, int textureHeight,
                                float u1, float v1, float u2, float v2,
                                PositionTextureVertex... vertices) {
        if(uv == null) return;
        output.add(new TexturedQuad(
                lerp(uv.x1(), uv.x2(), u1),
                lerp(uv.y1(), uv.y2(), v1),
                lerp(uv.x1(), uv.x2(), u2),
                lerp(uv.y1(), uv.y2(), v2),
                textureWidth,
                textureHeight,
                vertices
        ));
    }

    private static PositionTextureVertex vertex(Point point) {
        return vertex(point.x, point.y, point.z);
    }

    private static PositionTextureVertex vertex(float x, float y, float z) {
        return new PositionTextureVertex(x, y, z, 0, 0);
    }

    private static float lerp(float start, float end, float progress) {
        return start + (end - start) * progress;
    }

    private static Profile profile(BreastShape shape) {
        return switch(shape) {
            case CLASSIC -> new Profile(
                    new float[]{1, 1, 1, 1, 1, 1},
                    new float[]{0, 0, 0, 0, 0, 0},
                    0, 0.55f);
            case ROUND -> new Profile(
                    new float[]{0.72f, 0.92f, 1, 1, 0.92f, 0.70f},
                    new float[]{1.05f, 0.36f, 0.02f, 0, 0.38f, 1.05f},
                    0.48f, 0.55f);
            case NATURAL -> new Profile(
                    new float[]{0.67f, 0.88f, 0.98f, 1, 0.96f, 0.76f},
                    new float[]{1.28f, 0.58f, 0.16f, 0, 0.18f, 0.84f},
                    0.38f, 0.62f);
            case TEARDROP -> new Profile(
                    new float[]{0.42f, 0.66f, 0.88f, 0.99f, 1, 0.72f},
                    new float[]{1.68f, 1.02f, 0.46f, 0.08f, 0, 0.82f},
                    0.34f, 0.69f);
            case BELL -> new Profile(
                    new float[]{0.56f, 0.75f, 0.90f, 0.98f, 1, 0.86f},
                    new float[]{1.45f, 0.82f, 0.36f, 0.12f, 0, 0.48f},
                    0.30f, 0.73f);
        };
    }

    private record Point(float x, float y, float z) {
    }

    private record Profile(float[] width, float[] depth, float edgeCurve, float nippleY) {
    }
}
