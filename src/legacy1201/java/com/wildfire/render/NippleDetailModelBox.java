package com.wildfire.render;

import com.wildfire.render.WildfireModelRenderer.ModelBox;
import com.wildfire.render.WildfireModelRenderer.PositionTextureVertex;
import com.wildfire.render.WildfireModelRenderer.TexturedQuad;

import java.util.ArrayList;
import java.util.List;

/** Small softly faceted skin detail, deliberately omitted from clothes and armor layers. */
public final class NippleDetailModelBox extends ModelBox {
    private static final int SEGMENTS = 10;

    public NippleDetailModelBox(int tw, int th, float centerX, float centerY, float surfaceZ,
                                float size, int textureU, int textureV) {
        super(centerX - size, centerY - size, surfaceZ - size,
                centerX + size, centerY + size, surfaceZ,
                createQuads(tw, th, centerX, centerY, surfaceZ, size, textureU, textureV));
    }

    private static TexturedQuad[] createQuads(int tw, int th, float cx, float cy, float z,
                                               float size, int u, int v) {
        List<TexturedQuad> result = new ArrayList<>(SEGMENTS * 2);
        float baseZ = z + 0.04F;
        float tipZ = z - (0.28F + size * 0.35F);
        float baseRadius = 0.34F + size * 0.48F;
        float tipRadius = baseRadius * 0.55F;
        for(int segment = 0; segment < SEGMENTS; segment++) {
            float a = (float)(Math.PI * 2 * segment / SEGMENTS);
            float b = (float)(Math.PI * 2 * (segment + 1) / SEGMENTS);
            PositionTextureVertex baseA = vertex(cx + Math.cos(a) * baseRadius, cy + Math.sin(a) * baseRadius, baseZ);
            PositionTextureVertex baseB = vertex(cx + Math.cos(b) * baseRadius, cy + Math.sin(b) * baseRadius, baseZ);
            PositionTextureVertex tipA = vertex(cx + Math.cos(a) * tipRadius, cy + Math.sin(a) * tipRadius, tipZ);
            PositionTextureVertex tipB = vertex(cx + Math.cos(b) * tipRadius, cy + Math.sin(b) * tipRadius, tipZ);
            result.add(new TexturedQuad(u, v, u + 1, v + 1, tw, th, baseB, baseA, tipA, tipB));
            PositionTextureVertex center = vertex(cx, cy, tipZ - 0.02F);
            result.add(new TexturedQuad(u, v, u + 1, v + 1, tw, th, center, tipB, tipA, center));
        }
        return result.toArray(new TexturedQuad[0]);
    }

    private static PositionTextureVertex vertex(double x, double y, double z) {
        return new PositionTextureVertex((float)x, (float)y, (float)z, 0, 0);
    }
}
