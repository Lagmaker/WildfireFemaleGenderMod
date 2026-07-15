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

package com.wildfire.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wildfire.api.IGenderArmor;
import com.wildfire.main.WildfireGender;
import com.wildfire.main.WildfireHelper;
import com.wildfire.main.config.ClientConfig;
import com.wildfire.main.config.enums.BreastShape;
import com.wildfire.main.uvs.UVDirection;
import com.wildfire.main.uvs.UVLayout;
import com.wildfire.main.uvs.UVQuad;
import com.wildfire.mixins.accessors.LivingEntityRendererAccessor;
import com.wildfire.render.WildfireModelRenderer.ModelBox;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.joml.Quaternionf;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

// TODO split this into an AbstractGenderLayer?
@Environment(EnvType.CLIENT)
public class GenderLayer<S extends HumanoidRenderState, M extends HumanoidModel<S>> extends RenderLayer<S, M> {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180);

    @UnknownNullability("null until #resizeBox() is first called")
    private ModelBox lBreast, rBreast;
    @UnknownNullability("null until #resizeBox() is first called")
    private ModelBox lBreastWear, rBreastWear;
    @UnknownNullability("null until #resizeBox() is first called")
    private ModelBox lNipple, rNipple;

    private static final int GEOMETRY_CACHE_SIZE = 24;
    private final Map<GeometryKey, GeometryBundle> geometryCache = boundedCache(GEOMETRY_CACHE_SIZE);

    private final RenderLayerParent<S, M> context;

    private boolean isUniboob;
    // although ItemStack instances are mutable, this is safe to keep a reference to as this is a copy of the real stack
    protected ItemStack armorStack = ItemStack.EMPTY;
    protected IGenderArmor genderArmor = IGenderArmor.EMPTY;
    protected boolean isChestplateOccupied, armorCoversBreasts, bounceEnabled, wobbleEnabled,
            breathingAnimation, jacketLayerVisible, nippleDetail;
    protected float breastOffsetX, breastOffsetY, breastOffsetZ, lPhysPositionY, lPhysPositionX, rPhysPositionY, rPhysPositionX,
            lPhysBounceRotation, rPhysBounceRotation, outwardAngle,
            shapeWidth, shapeHeight, shapeProjection, shapeBalance, lPhysWobble, rPhysWobble;

    public GenderLayer(RenderLayerParent<S, M> render) {
        super(render);
        this.context = render;
    }

    /**
     * Convenience method around {@link LivingEntityRendererAccessor#invokeGetRenderType}
     */
    private @Nullable RenderType getRenderLayer(S state) {
        var renderer = (LivingEntityRenderer<?, ?, ?>) context;
        var accessor = (LivingEntityRendererAccessor) renderer;

        boolean bodyVisible = accessor.invokeIsBodyVisible(state);
        boolean translucent = !bodyVisible && !state.isInvisibleToPlayer;
        boolean glowing = state.appearsGlowing();

        return accessor.invokeGetRenderType(state, bodyVisible, translucent, glowing);
    }

    @Override
    public void submit(PoseStack matrixStack, SubmitNodeCollector queue, int light, S state, float limbAngle, float limbDistance) {
        var entityConfigState = GenderRenderState.get(state);
        if(entityConfigState == null) return;

        try {
            if(!setupRender(state, entityConfigState)) return;
            int overlay = LivingEntityRenderer.getOverlayCoords(state, 0);

            //noinspection CodeBlock2Expr
            renderSides(state, getParentModel(), matrixStack, side -> {
                renderBreast(state, matrixStack, queue, overlay, side);
            });
        } catch(Exception e) {
            WildfireGender.LOGGER.error("Failed to render breast layer", e);
        }
    }

    /**
     * Common logic for setting up breast rendering
     *
     * @return {@code true} if rendering should continue
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected boolean setupRender(S entityState, GenderRenderState genderState) {
        if(!ClientConfig.RENDER_BREASTS) return false;

        armorStack = entityState.chestEquipment;
        //Note: When the stack is empty the helper will fall back to an implementation that returns the proper data
        // TODO should this be moved into the render state?
        genderArmor = WildfireHelper.getArmorConfig(armorStack);
        armorCoversBreasts = !armorStack.isEmpty() && genderArmor.coversBreasts();
        isChestplateOccupied = genderArmor.coversBreasts() && !genderState.armorPhysicsOverride;
        if(genderArmor.alwaysHidesBreasts() || !genderState.showBreastsInArmor && isChestplateOccupied) {
            //If the armor always hides breasts or there is armor and the player configured breasts
            // to be hidden when wearing armor, we can just exit early rather than doing any calculations
            return false;
        }

        if(!isLayerVisible(entityState)) {
            return false;
        }

        GenderRenderState.BreastState breasts = genderState.breasts;
        breastOffsetX = WildfireHelper.round(breasts.xOffset, 1);
        breastOffsetY = -WildfireHelper.round(breasts.yOffset, 1);
        breastOffsetZ = -WildfireHelper.round(breasts.zOffset, 1);
        shapeWidth = breasts.width;
        shapeHeight = breasts.height;
        shapeProjection = breasts.projection;
        shapeBalance = breasts.balance;
        nippleDetail = breasts.nipples;
        jacketLayerVisible = genderState.hasJacketLayer;
        wobbleEnabled = genderState.hasWobble;

        isUniboob = breasts.uniboob;

        GenderRenderState.BreastPhysicsState leftPhysicsState = genderState.leftBreastPhysics;
        final float bSize = safeFinite(leftPhysicsState.getBreastSize(), 0f, 0f, 8f);
        if(bSize < 0.02f) return false;
        outwardAngle = safeFinite(breasts.cleavage * 100f, 0f, -75f, 75f);

        resizeBox(genderState, bSize);

        lPhysPositionY = leftPhysicsState.getPositionY();
        lPhysPositionX = leftPhysicsState.getPositionX();
        lPhysBounceRotation = leftPhysicsState.getBounceRotation();
        lPhysWobble = leftPhysicsState.getWobble();
        if(isUniboob) {
            rPhysPositionY = lPhysPositionY;
            rPhysPositionX = lPhysPositionX;
            rPhysBounceRotation = lPhysBounceRotation;
            rPhysWobble = lPhysWobble;
        } else {
            GenderRenderState.BreastPhysicsState rightPhysicsState = genderState.rightBreastPhysics;
            rPhysPositionY = rightPhysicsState.getPositionY();
            rPhysPositionX = rightPhysicsState.getPositionX();
            rPhysBounceRotation = rightPhysicsState.getBounceRotation();
            rPhysWobble = rightPhysicsState.getWobble();
        }

        float resistance = Mth.clamp(genderArmor.physicsResistance(), 0, 1);
        breathingAnimation = ((genderState.armorPhysicsOverride || resistance <= 0.5F) && genderState.isBreathing);
        bounceEnabled = genderState.hasBreastPhysics && (!isChestplateOccupied || resistance < 1); //oh, you found this?
        return true;
    }

    protected boolean isLayerVisible(S state) {
        return !state.isInvisibleToPlayer || state.appearsGlowing();
    }

    protected void resizeBox(GenderRenderState state, float breastSize) {
        float meshSize = geometryStep(breastSize);
        float rootWidth = geometryStep(state.breasts.rootWidth);
        float outerFullness = geometryStep(state.breasts.outerFullness);
        float drop = geometryStep(state.breasts.drop);
        float nippleSize = geometryStep(state.breasts.nippleSize);
        var key = new GeometryKey(LayoutSnapshot.of(state.leftBreastUVLayout),
                LayoutSnapshot.of(state.rightBreastUVLayout),
                LayoutSnapshot.of(state.leftBreastOverlayUVLayout),
                LayoutSnapshot.of(state.rightBreastOverlayUVLayout),
                state.breasts.shape, meshSize, rootWidth, outerFullness, drop, nippleSize);
        GeometryBundle models = geometryCache.computeIfAbsent(key, GenderLayer::createGeometry);
        this.lBreast = models.left();
        this.rBreast = models.right();
        this.lBreastWear = models.leftOverlay();
        this.rBreastWear = models.rightOverlay();
        this.lNipple = models.leftDetail();
        this.rNipple = models.rightDetail();
    }

    private static GeometryBundle createGeometry(GeometryKey key) {
        ModelBox left = createBreastModel(64, 64, -4F, key.shape(), key.bustSize(),
                key.rootWidth(), key.outerFullness(), key.drop(), key.leftUv().toLayout());
        ModelBox right = createBreastModel(64, 64, 0F, key.shape(), key.bustSize(),
                key.rootWidth(), key.outerFullness(), key.drop(), key.rightUv().toLayout());
        ModelBox leftOverlay = createBreastModel(64, 64, -4F, key.shape(), key.bustSize(),
                key.rootWidth(), key.outerFullness(), key.drop(), key.leftOverlayUv().toLayout());
        ModelBox rightOverlay = createBreastModel(64, 64, 0F, key.shape(), key.bustSize(),
                key.rootWidth(), key.outerFullness(), key.drop(), key.rightOverlayUv().toLayout());
        ModelBox leftDetail = createNippleModel(-2F, key.shape(), key.bustSize(), key.rootWidth(),
                key.outerFullness(), key.drop(), key.nippleSize(), key.leftUv().toLayout());
        ModelBox rightDetail = createNippleModel(2F, key.shape(), key.bustSize(), key.rootWidth(),
                key.outerFullness(), key.drop(), key.nippleSize(), key.rightUv().toLayout());
        return new GeometryBundle(left, right, leftOverlay, rightOverlay, leftDetail, rightDetail);
    }

    protected static <KEY, VALUE> Map<KEY, VALUE> boundedCache(int maximumSize) {
        return new LinkedHashMap<>(maximumSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<KEY, VALUE> eldest) {
                return size() > maximumSize;
            }
        };
    }

    protected record LayoutSnapshot(UVQuad east, UVQuad west, UVQuad down, UVQuad up, UVQuad north) {
        protected static LayoutSnapshot of(UVLayout layout) {
            return new LayoutSnapshot(layout.get(UVDirection.EAST), layout.get(UVDirection.WEST),
                    layout.get(UVDirection.DOWN), layout.get(UVDirection.UP), layout.get(UVDirection.NORTH));
        }

        protected UVLayout toLayout() {
            return new UVLayout(east, west, down, up, north);
        }
    }

    private record GeometryKey(LayoutSnapshot leftUv, LayoutSnapshot rightUv,
                               LayoutSnapshot leftOverlayUv, LayoutSnapshot rightOverlayUv,
                               BreastShape shape, float bustSize,
                               float rootWidth, float outerFullness, float drop, float nippleSize) {
    }

    private record GeometryBundle(ModelBox left, ModelBox right, ModelBox leftOverlay,
                                  ModelBox rightOverlay, ModelBox leftDetail, ModelBox rightDetail) {
    }

    protected static ModelBox createBreastModel(int textureWidth, int textureHeight, float x,
                                                 BreastShape shape, float bustSize, float rootWidth,
                                                 float outerFullness, float drop, UVLayout layout) {
        return new ProfiledBreastModelBox(textureWidth, textureHeight, x, shape, bustSize,
                rootWidth, outerFullness, drop, layout);
    }

    private static ModelBox createNippleModel(float centerX, BreastShape shape, float bustSize,
                                               float rootWidth, float outerFullness, float drop,
                                               float configuredSize, UVLayout sourceLayout) {
        float size = 0.45f + configuredSize * 0.65f;
        float projection = 0.28f + configuredSize * 0.34f;
        var anchor = ProfiledBreastModelBox.detailAnchor(centerX, shape, bustSize, rootWidth,
                outerFullness, drop);
        return new NippleDetailModelBox(64, 64, anchor.x(), anchor.y(), anchor.z(), size,
                projection, anchor.normal(), sourceLayout);
    }

    /** Quantization prevents rebuilding hundreds of vertices for sub-pixel interpolation noise. */
    protected static float geometryStep(float value) {
        if(!Float.isFinite(value)) return 0f;
        return Math.round(value * 64f) / 64f;
    }

    private static float safeFinite(float value, float fallback, float min, float max) {
        return Math.max(min, Math.min(max, Float.isFinite(value) ? value : fallback));
    }

    protected void setupTransformations(S state, M model, PoseStack matrixStack, BreastSide side) {
        if(state.isBaby) {
            matrixStack.scale(state.ageScale, state.ageScale, state.ageScale);
            matrixStack.translate(0f, 0.75f, 0f);
        }

        model.root().translateAndRotate(matrixStack);
        ModelPart body = model.body;
        body.translateAndRotate(matrixStack);

        if(bounceEnabled) {
            matrixStack.translate((side.isLeft ? lPhysPositionX : rPhysPositionX) / 32f, 0, 0);
            matrixStack.translate(0, (side.isLeft ? lPhysPositionY : rPhysPositionY) / 32f, 0);
        }

        // The mesh rear is z=3. Translating by five pixels places it directly on the body's
        // z=-2 front plane, so projection grows forward without needing any size-driven pitch.
        matrixStack.translate((side.isLeft ? breastOffsetX : -breastOffsetX) * 0.0625f,
                0.05625f + (breastOffsetY * 0.0625f),
                -0.3125f + (breastOffsetZ * 0.0625f));

        if(!isUniboob) {
            matrixStack.translate(-0.0625f * 2 * (side.isLeft ? 1 : -1), 0, 0);
        }
        if(bounceEnabled) {
            matrixStack.mulPose(new Quaternionf().rotationXYZ(0, (float)((side.isLeft ? lPhysBounceRotation : rPhysBounceRotation) * (Math.PI / 180f)), 0));
        }
        if(!isUniboob) {
            matrixStack.translate(0.0625f * 2 * (side.isLeft ? 1 : -1), 0, 0);
        }

        if(isChestplateOccupied) {
            matrixStack.translate(0, 0, 0.01f);
        }

        Quaternionf rotationTransform = new Quaternionf()
                .rotationY((side.isLeft ? outwardAngle : -outwardAngle) * DEG_TO_RAD);

        if(breathingAnimation) {
            float f5 = -Mth.cos(state.ageInTicks * 0.09F) * 0.45F + 0.45F;
            rotationTransform.rotateX(f5 * DEG_TO_RAD);
        }

        matrixStack.mulPose(rotationTransform);
        float sideScale = safeFinite(1f + (side.isLeft ? shapeBalance : -shapeBalance), 1f, .05f, 7f);
        float wobble = bounceEnabled && wobbleEnabled
                ? safeFinite(side.isLeft ? lPhysWobble : rPhysWobble, 0f, -.8f, .8f) : 0f;
        float widthWobble = 1f - wobble * 0.22f;
        float heightWobble = 1f + wobble * 0.42f;
        float projectionWobble = 1f - wobble * 0.34f;
        // Clamp only the final render transform, not the saved editor values. The small epsilon keeps
        // inverse-normal transforms finite when minimum width is combined with maximum asymmetry.
        float widthScale = safeFinite(
                safeFinite(shapeWidth, 1f, .05f, 7f) * sideScale * widthWobble * .9995f,
                1f, .01f, 16f);
        float heightScale = safeFinite(
                safeFinite(shapeHeight, 1f, .05f, 7f) * sideScale * heightWobble,
                1f, .01f, 16f);
        float projectionScale = safeFinite(
                safeFinite(shapeProjection, 1f, .05f, 9f) * sideScale * projectionWobble,
                1f, .01f, 24f);

        // Scale around the attachment center. This keeps the broad root planted while the outer
        // volume deforms, including during secondary wobble.
        float pivotX = (side.isLeft ? -2f : 2f) / 16f;
        matrixStack.translate(pivotX, 2.5f / 16f, 3f / 16f);
        matrixStack.scale(
                widthScale,
                heightScale,
                projectionScale
        );
        matrixStack.translate(-pivotX, -2.5f / 16f, -3f / 16f);
    }

    private void renderBreast(S state, PoseStack poseStack, SubmitNodeCollector collector, int overlay, BreastSide side) {
        RenderType type = getRenderLayer(state);
        if(type == null) return; // only render if the player is visible in some capacity

        int alpha = state.isInvisible ? ARGB.as8BitChannel(0.15f) : 255;
        int color = ARGB.color(alpha, 255, 255, 255);

        var model = side.isLeft ? lBreast : rBreast;
        collector.order(1).submitModel(new BreastModel(model), state, poseStack, type, state.lightCoords, overlay, color, null, state.outlineColor, null);

        if(nippleDetail && !jacketLayerVisible && !armorCoversBreasts) {
            var nipple = side.isLeft ? lNipple : rNipple;
            collector.order(2).submitModel(new BreastModel(nipple), state, poseStack, type, state.lightCoords,
                    overlay, color, null, state.outlineColor, null);
        }

        if(jacketLayerVisible) {
            poseStack.translate(0, 0, -0.015f);
            poseStack.scale(1.05f, 1.05f, 1.05f);
            var jacketModel = side.isLeft ? lBreastWear : rBreastWear;
            collector.order(3).submitModel(new BreastModel(jacketModel), state, poseStack, type, state.lightCoords, overlay, color, null, state.outlineColor, null);
        }
    }

    protected void renderSides(S state, M model, PoseStack matrixStack, Consumer<BreastSide> renderer) {
        matrixStack.pushPose();
        try {
            setupTransformations(state, model, matrixStack, BreastSide.LEFT);
            renderer.accept(BreastSide.LEFT);
        } finally {
            matrixStack.popPose();
        }

        matrixStack.pushPose();
        try {
            setupTransformations(state, model, matrixStack, BreastSide.RIGHT);
            renderer.accept(BreastSide.RIGHT);
        } finally {
            matrixStack.popPose();
        }
    }
}
