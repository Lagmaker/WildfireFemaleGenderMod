package com.wildfire.render;

import com.wildfire.main.config.BreastShape;
import com.wildfire.render.WildfireModelRenderer.ModelBox;
import com.wildfire.render.WildfireModelRenderer.PositionTextureVertex;
import com.wildfire.render.WildfireModelRenderer.TexturedQuad;

import java.util.ArrayList;
import java.util.List;

/**
 * A broad-root, expansion-driven mesh. Size changes its actual volume instead of merely
 * pitching a cuboid away from the torso; profile samples keep very large settings smooth.
 */
public final class ProfiledBreastModelBox extends ModelBox {
    private static final int ROWS = 9;
    private static final int COLUMNS = 9;
    private static final float REAR_Z = 3F;

    public ProfiledBreastModelBox(int textureWidth, int textureHeight, float rootX, float bustSize,
                                  float width, float height, float projection, float sideScale,
                                  BreastShape shape, int textureU, int textureV) {
        this(bounds(rootX, bustSize, width, height, projection, sideScale),
                createQuads(textureWidth, textureHeight, rootX, bustSize, width, height,
                        projection, sideScale, shape, textureU, textureV));
    }

    private ProfiledBreastModelBox(float[] bounds, TexturedQuad[] quads) {
        super(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5], quads);
    }

    private static float[] bounds(float rootX, float bust, float width, float height,
                                  float projection, float sideScale) {
        Dimensions d = dimensions(bust, width, height, projection, sideScale);
        return new float[]{rootX + 2F - d.halfWidth, 2.5F - d.halfHeight, REAR_Z - d.depth,
                rootX + 2F + d.halfWidth, 2.5F + d.halfHeight, REAR_Z};
    }

    private static TexturedQuad[] createQuads(int textureWidth, int textureHeight, float rootX,
                                               float bust, float width, float height, float projection,
                                               float sideScale, BreastShape shape, int textureU, int textureV) {
        Dimensions d = dimensions(bust, width, height, projection, sideScale);
        Point[][] surface = new Point[ROWS][COLUMNS];
        float centerX = rootX + 2F;
        float peakShift = peakShift(shape);

        for(int row = 0; row < ROWS; row++) {
            float yNorm = -1F + 2F * row / (ROWS - 1F);
            float profileY = clamp(yNorm - peakShift, -1F, 1F);
            float rowWidth = rowWidth(shape, yNorm);
            for(int column = 0; column < COLUMNS; column++) {
                float xNorm = -1F + 2F * column / (COLUMNS - 1F);
                float radial = 1F - (float)Math.pow(Math.abs(xNorm), xExponent(shape))
                        - (float)Math.pow(Math.abs(profileY), yExponent(shape));
                radial = clamp(radial, 0F, 1F);
                float fullness = (float)Math.pow(radial, frontExponent(shape));
                float x = centerX + xNorm * d.halfWidth * rowWidth;
                float y = 2.5F + yNorm * d.halfHeight;
                float z = REAR_Z - d.depth * fullness;
                surface[row][column] = new Point(x, y, z);
            }
        }

        List<TexturedQuad> quads = new ArrayList<>(96);
        for(int row = 0; row < ROWS - 1; row++) {
            for(int column = 0; column < COLUMNS - 1; column++) {
                float u1 = textureU + 4F * column / (COLUMNS - 1F);
                float u2 = textureU + 4F * (column + 1) / (COLUMNS - 1F);
                float v1 = textureV + 5F * row / (ROWS - 1F);
                float v2 = textureV + 5F * (row + 1) / (ROWS - 1F);
                add(quads, textureWidth, textureHeight, u1, v1, u2, v2,
                        vertex(surface[row][column + 1]), vertex(surface[row][column]),
                        vertex(surface[row + 1][column]), vertex(surface[row + 1][column + 1]));
            }
        }

        // Join the expanded silhouette back to a torso-width 4x5 root. This is what keeps the
        // attachment broad and avoids the floating-ball look at extreme settings.
        for(int row = 0; row < ROWS - 1; row++) {
            float rootY1 = 5F * row / (ROWS - 1F);
            float rootY2 = 5F * (row + 1) / (ROWS - 1F);
            add(quads, textureWidth, textureHeight, textureU, textureV, textureU + 1, textureV + 5,
                    vertex(rootX, rootY1, REAR_Z), vertex(surface[row][0]),
                    vertex(surface[row + 1][0]), vertex(rootX, rootY2, REAR_Z));
            add(quads, textureWidth, textureHeight, textureU + 3, textureV, textureU + 4, textureV + 5,
                    vertex(surface[row][COLUMNS - 1]), vertex(rootX + 4F, rootY1, REAR_Z),
                    vertex(rootX + 4F, rootY2, REAR_Z), vertex(surface[row + 1][COLUMNS - 1]));
        }
        for(int column = 0; column < COLUMNS - 1; column++) {
            float rootX1 = rootX + 4F * column / (COLUMNS - 1F);
            float rootX2 = rootX + 4F * (column + 1) / (COLUMNS - 1F);
            add(quads, textureWidth, textureHeight, textureU, textureV, textureU + 4, textureV + 1,
                    vertex(rootX2, 0, REAR_Z), vertex(rootX1, 0, REAR_Z),
                    vertex(surface[0][column]), vertex(surface[0][column + 1]));
            add(quads, textureWidth, textureHeight, textureU, textureV + 4, textureU + 4, textureV + 5,
                    vertex(surface[ROWS - 1][column + 1]), vertex(surface[ROWS - 1][column]),
                    vertex(rootX1, 5F, REAR_Z), vertex(rootX2, 5F, REAR_Z));
        }
        return quads.toArray(new TexturedQuad[0]);
    }

    private static void add(List<TexturedQuad> output, int tw, int th, float u1, float v1,
                            float u2, float v2, PositionTextureVertex... vertices) {
        output.add(new TexturedQuad(u1, v1, u2, v2, tw, th, vertices));
    }

    private static PositionTextureVertex vertex(Point point) {
        return vertex(point.x, point.y, point.z);
    }

    private static PositionTextureVertex vertex(float x, float y, float z) {
        return new PositionTextureVertex(x, y, z, 0, 0);
    }

    public static float nippleY(float bust, float height, BreastShape shape) {
        Dimensions d = dimensions(bust, 1F, height, 1F, 1F);
        return 2.5F + peakShift(shape) * d.halfHeight;
    }

    public static float nippleZ(float bust, float projection) {
        return REAR_Z - dimensions(bust, 1F, 1F, projection, 1F).depth - 0.03F;
    }

    private static Dimensions dimensions(float bust, float width, float height, float projection, float sideScale) {
        float expansion = Math.max(0.01F, bust / 0.6F);
        float halfWidth = expanded(2F, 1.45F, expansion) * width * sideScale;
        float halfHeight = expanded(2.5F, 1.75F, expansion) * height * sideScale;
        float depth = expanded(3F, 3F, expansion) * projection * sideScale;
        return new Dimensions(Math.max(0.02F, halfWidth), Math.max(0.02F, halfHeight), Math.max(0.02F, depth));
    }

    private static float expanded(float base, float growth, float expansion) {
        return expansion <= 1F ? base * expansion : base + (expansion - 1F) * growth;
    }

    private static float peakShift(BreastShape shape) {
        return switch(shape) {
            case NATURAL -> 0.10F;
            case TEARDROP -> 0.23F;
            case BELL -> 0.28F;
            case ANIME -> 0.08F;
            default -> 0F;
        };
    }

    private static float rowWidth(BreastShape shape, float y) {
        return switch(shape) {
            case ANIME -> 0.86F + 0.14F * (y + 1F) * 0.5F;
            case TEARDROP -> 0.72F + 0.28F * (y + 1F) * 0.5F;
            case BELL -> 0.78F + 0.22F * (y + 1F) * 0.5F;
            case NATURAL -> 0.90F + 0.10F * (y + 1F) * 0.5F;
            default -> 1F;
        };
    }

    private static float frontExponent(BreastShape shape) {
        return switch(shape) {
            case CLASSIC -> 0.7F;
            case ANIME -> 0.24F;
            case ROUND -> 0.48F;
            case NATURAL -> 0.55F;
            case TEARDROP -> 0.44F;
            case BELL -> 0.5F;
        };
    }

    private static float xExponent(BreastShape shape) { return shape == BreastShape.ANIME ? 2.8F : 2.1F; }
    private static float yExponent(BreastShape shape) { return shape == BreastShape.ROUND ? 2.1F : 1.8F; }
    private static float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }

    private record Point(float x, float y, float z) {}
    private record Dimensions(float halfWidth, float halfHeight, float depth) {}
}
