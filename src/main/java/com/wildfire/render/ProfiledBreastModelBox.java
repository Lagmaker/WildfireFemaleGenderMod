/*
 * Wildfire's Female Gender Mod is a female gender mod created for Minecraft.
 * Copyright (C) 2023-present WildfireRomeo
 * Licensed under the GNU Lesser General Public License, version 3 or later.
 */
package com.wildfire.render;

import com.wildfire.main.config.enums.BreastShape;
import com.wildfire.main.uvs.UVDirection;
import com.wildfire.main.uvs.UVLayout;
import com.wildfire.main.uvs.UVQuad;
import com.wildfire.render.WildfireModelRenderer.ModelBox;
import com.wildfire.render.WildfireModelRenderer.PositionTextureVertex;
import com.wildfire.render.WildfireModelRenderer.TexturedQuad;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Generated broad-root volume. Size is expressed directly in its X/Y/Z dimensions; it is never
 * faked by pitching a fixed box. Rings grow fuller toward the front before closing at a soft pole.
 */
public final class ProfiledBreastModelBox extends ModelBox {
    private static final float REAR_Z = 3f;
    private static final int SEGMENTS = 32;
    private static final float[] T = {0f, .10f, .23f, .39f, .56f, .71f, .84f, .93f};

    public ProfiledBreastModelBox(int texW, int texH, float x, BreastShape shape, float bust,
                                  float rootWidth, float outerFullness, float drop, UVLayout uvs) {
        this(texW, texH, x, shape, Dimensions.of(bust, rootWidth, outerFullness, drop), uvs);
    }

    private ProfiledBreastModelBox(int texW, int texH, float x, BreastShape shape,
                                   Dimensions d, UVLayout uvs) {
        super(x - d.extraX, d.minY, d.frontZ, x + 4f + d.extraX, d.maxY, REAR_Z,
                uvs, build(texW, texH, x, profile(shape), d, uvs));
    }

    /** Profile-aligned point and normal for raised skin detail. */
    public static SurfacePoint detailAnchor(float centerX, BreastShape shape, float bust,
                                            float rootWidth, float outerFullness, float drop) {
        Dimensions d = Dimensions.of(bust, rootWidth, outerFullness, drop);
        Profile p = profile(shape);
        float y = centerY(p, d, 1f);
        float priorT = T[T.length - 1];
        Vector3f normal = new Vector3f(0, y - centerY(p, d, priorT), d.frontZ - zAt(d, priorT));
        if(!Float.isFinite(normal.x) || !Float.isFinite(normal.y) || !Float.isFinite(normal.z)
                || normal.lengthSquared() < 1e-6f) normal.set(0, 0, -1);
        else normal.normalize();
        return new SurfacePoint(centerX, y, d.frontZ, normal);
    }

    private static TexturedQuad[] build(int texW, int texH, float x, Profile p,
                                        Dimensions d, UVLayout uvs) {
        Point[][] ring = new Point[T.length][SEGMENTS];
        float centerX = x + 2f;
        for(int r = 0; r < T.length; r++) {
            float t = T[r], blend = smoother(t);
            // The torso seam itself always fills one body half. Root Span then opens smoothly over
            // the first two depth stations, avoiding a nearly-flat flange at extreme values.
            float rootBlend = smoother(clamp01(t / T[2]));
            float plantedRootX = lerp(Math.min(2f, d.rootX), d.rootX, rootBlend);
            float plantedRootY = lerp(Math.min(2.48f, d.rootY), d.rootY, rootBlend);
            float rx = lerp(plantedRootX, d.outerX * p.width[r], blend);
            float ry = lerp(plantedRootY, d.outerY * p.height[r], blend);
            float cy = centerY(p, d, t), z = zAt(d, t);
            for(int s = 0; s < SEGMENTS; s++) {
                float a = (float) (Math.PI * 2 * s / SEGMENTS);
                float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
                float vertical = 1f + Math.max(0, sin) * p.lower * blend
                        - Math.max(0, -sin) * p.upperCompression * blend;
                float lateral = 1f + Math.abs(cos) * p.side * blend;
                ring[r][s] = new Point(finite(centerX + cos * rx * lateral, centerX),
                        finite(cy + sin * ry * vertical, cy), z);
            }
        }

        List<TexturedQuad> out = new ArrayList<>((T.length + 1) * SEGMENTS);
        UVQuad front = first(uvs.get(UVDirection.NORTH), uvs.get(UVDirection.EAST),
                uvs.get(UVDirection.WEST), uvs.get(UVDirection.UP));
        for(int r = 0; r < T.length - 1; r++) {
            for(int s = 0; s < SEGMENTS; s++) {
                int n = (s + 1) % SEGMENTS;
                quad(out, front, texW, texH, s / (float) SEGMENTS, T[r],
                        (s + 1f) / SEGMENTS, T[r + 1],
                        v(ring[r][n]), v(ring[r][s]), v(ring[r + 1][s]), v(ring[r + 1][n]));
            }
        }

        // A triangle fan encoded as degenerate quads closes the front without UV folds.
        Point pole = new Point(centerX, centerY(p, d, 1), d.frontZ);
        int last = T.length - 1;
        for(int s = 0; s < SEGMENTS; s++) {
            int n = (s + 1) % SEGMENTS;
            quad(out, front, texW, texH, s / (float) SEGMENTS, T[last],
                    (s + 1f) / SEGMENTS, 1f,
                    v(ring[last][n]), v(ring[last][s]), v(pole), v(pole));
        }

        // Bridge the configurable ellipse to an attachment that fills one body half. The back is
        // intentionally open so it cannot fight the torso surface.
        Point[] body = bodyRoot(x, p, d);
        for(int s = 0; s < SEGMENTS; s++) {
            int n = (s + 1) % SEGMENTS;
            UVQuad sideUv = first(uvs.get(sideFor(s)), front);
            quad(out, sideUv, texW, texH, s / (float) SEGMENTS, 0,
                    (s + 1f) / SEGMENTS, 1,
                    v(body[n]), v(body[s]), v(ring[0][s]), v(ring[0][n]));
        }
        return out.toArray(TexturedQuad[]::new);
    }

    private static Point[] bodyRoot(float x, Profile p, Dimensions d) {
        Point[] body = new Point[SEGMENTS];
        float cx = x + 2, cy = centerY(p, d, 0);
        float rx = Math.min(2, d.rootX), ry = Math.min(2.48f, d.rootY);
        for(int s = 0; s < SEGMENTS; s++) {
            float a = (float) (Math.PI * 2 * s / SEGMENTS);
            body[s] = new Point(cx + (float) Math.cos(a) * rx,
                    cy + (float) Math.sin(a) * ry, REAR_Z + .015f);
        }
        return body;
    }

    private static float centerY(Profile p, Dimensions d, float t) {
        float intrinsic = p.drop * d.outerY;
        float configured = d.drop * Math.max(1, d.outerY * .52f);
        return finite(2.5f + (intrinsic + configured) * smoother(t), 2.5f);
    }

    private static float zAt(Dimensions d, float t) {
        float forward = 1 - (float) Math.pow(1 - clamp01(t), 1.18);
        return finite(REAR_Z - d.projection * forward, REAR_Z);
    }

    private static UVDirection sideFor(int s) {
        float a = (float) (Math.PI * 2 * (s + .5f) / SEGMENTS);
        float x = (float) Math.cos(a), y = (float) Math.sin(a);
        if(Math.abs(x) > Math.abs(y)) return x > 0 ? UVDirection.EAST : UVDirection.WEST;
        return y > 0 ? UVDirection.UP : UVDirection.DOWN;
    }

    private static void quad(List<TexturedQuad> out, @Nullable UVQuad uv, int tw, int th,
                             float u1, float v1, float u2, float v2,
                             PositionTextureVertex... vertices) {
        if(uv == null) return;
        out.add(new TexturedQuad(lerp(uv.x1(), uv.x2(), clamp01(u1)),
                lerp(uv.y1(), uv.y2(), clamp01(v1)),
                lerp(uv.x1(), uv.x2(), clamp01(u2)),
                lerp(uv.y1(), uv.y2(), clamp01(v2)), tw, th, vertices));
    }

    @SafeVarargs
    private static @Nullable UVQuad first(@Nullable UVQuad... values) {
        for(UVQuad value : values) if(value != null) return value;
        return null;
    }

    private static PositionTextureVertex v(Point p) {
        return new PositionTextureVertex(p.x, p.y, p.z, 0, 0);
    }

    private static float clamp01(float n) { return Math.max(0, Math.min(1, n)); }
    private static float smoother(float n) {
        float t = clamp01(n);
        return t * t * t * (t * (t * 6 - 15) + 10);
    }
    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    private static float finite(float n, float fallback) { return Float.isFinite(n) ? n : fallback; }
    private static float safe(float n, float fallback, float min, float max) {
        return Math.max(min, Math.min(max, finite(n, fallback)));
    }

    private static Profile profile(BreastShape shape) {
        return switch(shape.id()) {
            case "classic" -> CLASSIC;
            case "round" -> ROUND;
            case "teardrop" -> TEARDROP;
            case "bell" -> BELL;
            case "anime" -> ANIME;
            default -> NATURAL;
        };
    }

    private static final Profile CLASSIC = p(
            new float[]{.96f,.99f,1.01f,1.02f,.99f,.90f,.70f,.40f},
            new float[]{.98f,1,1.02f,1.02f,.98f,.88f,.68f,.39f}, .02f,.01f,.02f,.02f);
    private static final Profile ROUND = p(
            new float[]{.94f,1,1.08f,1.13f,1.12f,1.03f,.82f,.49f},
            new float[]{.94f,1,1.08f,1.13f,1.12f,1.03f,.82f,.49f}, 0,.02f,.02f,.06f);
    private static final Profile NATURAL = p(
            new float[]{.92f,.98f,1.06f,1.13f,1.16f,1.10f,.90f,.54f},
            new float[]{.86f,.92f,1,1.10f,1.18f,1.18f,.98f,.59f}, .10f,.13f,.05f,.08f);
    private static final Profile TEARDROP = p(
            new float[]{.82f,.90f,1,1.13f,1.22f,1.22f,1.03f,.63f},
            new float[]{.76f,.84f,.95f,1.10f,1.26f,1.32f,1.12f,.68f}, .18f,.22f,.08f,.08f);
    private static final Profile BELL = p(
            new float[]{.88f,.94f,1.01f,1.10f,1.19f,1.24f,1.11f,.72f},
            new float[]{.80f,.87f,.96f,1.08f,1.22f,1.35f,1.25f,.78f}, .23f,.28f,.08f,.10f);
    private static final Profile ANIME = p(
            new float[]{.88f,.98f,1.10f,1.23f,1.35f,1.39f,1.22f,.76f},
            new float[]{.82f,.92f,1.04f,1.18f,1.31f,1.36f,1.19f,.72f}, .08f,.13f,.04f,.16f);
    private static Profile p(float[] w, float[] h, float d, float l, float u, float s) {
        return new Profile(w, h, d, l, u, s);
    }

    private record Point(float x, float y, float z) {}
    public record SurfacePoint(float x, float y, float z, Vector3f normal) {}
    private record Profile(float[] width, float[] height, float drop, float lower,
                           float upperCompression, float side) {}

    private record Dimensions(float rootX, float rootY, float outerX, float outerY,
                              float projection, float drop, float frontZ,
                              float minY, float maxY, float extraX) {
        private static Dimensions of(float bustValue, float rootValue, float fullnessValue, float dropValue) {
            float bust = safe(bustValue, .7f, .02f, 8);
            float root = safe(rootValue, 1, .05f, 5);
            float fullness = safe(fullnessValue, 1, .05f, 5.5f);
            float drop = safe(dropValue, 0, -5, 7);
            float bx = 1.55f + .55f * bust + .070f * bust * bust;
            float by = 2f + .58f * bust + .070f * bust * bust;
            float projection = (1.35f + 1.40f * bust + .220f * bust * bust)
                    * (.78f + .22f * (float) Math.sqrt(fullness));
            float rootX = 1.94f * root * (1 + .035f * bust);
            float rootY = 2.40f * (.88f + .12f * Math.min(root, 2));
            float outerX = bx * fullness;
            float outerY = by * (.72f + .28f * fullness);
            rootX = safe(rootX, 1.94f, .08f, 80);
            rootY = safe(rootY, 2.4f, .08f, 24);
            outerX = safe(outerX, 2, .08f, 120);
            outerY = safe(outerY, 2.5f, .08f, 120);
            projection = safe(projection, 3, .08f, 160);
            float sagBounds = (Math.abs(drop) + .4f) * Math.max(1, outerY * .52f);
            float halfYBounds = outerY * 1.8f;
            float extraX = Math.max(rootX, outerX * 1.65f) - 2;
            return new Dimensions(rootX, rootY, outerX, outerY, projection, drop,
                    REAR_Z - projection, 2.5f - halfYBounds - sagBounds,
                    2.5f + halfYBounds + sagBounds, Math.max(0, extraX));
        }
    }
}
