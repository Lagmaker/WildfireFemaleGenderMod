/*
    Wildfire's Female Gender Mod is a female gender mod created for Minecraft.
    Copyright (C) 2023 WildfireRomeo

    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 3 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package com.wildfire.render;

import com.wildfire.api.IGenderArmor;
import com.wildfire.main.entitydata.Breasts;
import com.wildfire.main.WildfireGender;
import com.wildfire.main.WildfireHelper;
import com.wildfire.main.entitydata.EntityConfig;
import com.wildfire.physics.BreastPhysics;
import com.wildfire.render.WildfireModelRenderer.ModelBox;
import com.wildfire.render.WildfireModelRenderer.PositionTextureVertex;

import java.lang.Math;
import java.util.ConcurrentModificationException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import org.joml.*;

public class GenderLayer<T extends LivingEntity, M extends BipedEntityModel<T>> extends FeatureRenderer<T, M> {

	private ModelBox lBreast, rBreast;
	private ModelBox lBreastWear, rBreastWear;
	private ModelBox lNipple, rNipple;

	private float meshSignature = Float.NaN;
	private Breasts breasts;
	protected ItemStack armorStack;
	protected IGenderArmor genderArmor;
	protected boolean isChestplateOccupied, bounceEnabled, breathingAnimation;
	protected float breastOffsetX, breastOffsetY, breastOffsetZ, lPhysPositionY, lPhysPositionX, rPhysPositionY, rTotalX,
			lPhysBounceRotation, rPhysBounceRotation, breastSize, zOffset, outwardAngle,
			wobbleIntensity, wobbleSpeed;
	protected boolean wobbleEnabled, nippleDetail;

	public GenderLayer(FeatureRendererContext<T, M> render) {
		super(render);
		// Real meshes are built lazily from the entity's complete editor state.
	}

	private @Nullable RenderLayer getRenderLayer(T entity) {
		boolean bodyVisible = !entity.isInvisible();
		boolean translucent = !bodyVisible && !entity.isInvisibleTo(MinecraftClient.getInstance().player);
		Identifier texture = getTexture(entity);
		if(translucent) {
			return RenderLayer.getItemEntityTranslucentCull(texture);
		} else if(bodyVisible) {
			return RenderLayer.getEntityTranslucent(texture);
		} else if(entity.isGlowing()) {
			return RenderLayer.getOutline(texture);
		}
		return null;
	}

	protected @Nullable EntityConfig getConfig(T entity) {
		try {
			return EntityConfig.getEntity(entity);
		} catch(ConcurrentModificationException e) {
			// likely a temporary failure, try again later
			return null;
		}
	}

	@Override
	public void render(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int packedLightIn, @Nonnull T ent, float limbAngle,
					   float limbDistance, float partialTicks, float animationProgress, float headYaw, float headPitch) {
		MinecraftClient client = MinecraftClient.getInstance();
		if(client.player == null) {
			// we're currently in a menu, give up rendering before we crash the game
			return;
		}

		EntityConfig entityConfig = getConfig(ent);
		if(entityConfig == null) return;

		try {
			if(!setupRender(ent, entityConfig, partialTicks)) return;
			int combineTex = LivingEntityRenderer.getOverlay(ent, 0);
			BipedEntityModel<T> model = getContextModel();

			// Render left
			matrixStack.push();
			try {
				setupTransformations(ent, model.body, matrixStack, BreastSide.LEFT);
				renderBreast(ent, matrixStack, vertexConsumerProvider, packedLightIn, combineTex, BreastSide.LEFT);
			} finally {
				matrixStack.pop();
			}

			// Render right
			matrixStack.push();
			try {
				setupTransformations(ent, model.body, matrixStack, BreastSide.RIGHT);
				renderBreast(ent, matrixStack, vertexConsumerProvider, packedLightIn, combineTex, BreastSide.RIGHT);
			} finally {
				matrixStack.pop();
			}
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
	protected boolean setupRender(T entity, EntityConfig entityConfig, float partialTicks) {
		// Rendering breaks quite spectacularly on baby mobs, so just immediately give up before we even
		// attempt rendering on such an entity.
		if(entity.isBaby()) return false;

		armorStack = entity.getEquippedStack(EquipmentSlot.CHEST);
		//Note: When the stack is empty the helper will fall back to an implementation that returns the proper data
		genderArmor = WildfireHelper.getArmorConfig(armorStack);
		isChestplateOccupied = genderArmor.coversBreasts() && !entityConfig.getArmorPhysicsOverride();
		if(genderArmor.alwaysHidesBreasts() || !entityConfig.showBreastsInArmor() && isChestplateOccupied) {
			//If the armor always hides breasts or there is armor and the player configured breasts
			// to be hidden when wearing armor, we can just exit early rather than doing any calculations
			return false;
		}

		RenderLayer type = getRenderLayer(entity);
		if(type == null && !isChestplateOccupied) {
			// the entity is invisible and doesn't have a chestplate equipped
			return false;
		}

		breasts = entityConfig.getBreasts();
		breastOffsetX = Math.round((Math.round(breasts.getXOffset() * 100f) / 100f) * 10) / 10f;
		breastOffsetY = -Math.round((Math.round(breasts.getYOffset() * 100f) / 100f) * 10) / 10f;
		breastOffsetZ = -Math.round((Math.round(breasts.getZOffset() * 100f) / 100f) * 10) / 10f;

		BreastPhysics leftBreastPhysics = entityConfig.getLeftBreastPhysics();
		final float bSize = leftBreastPhysics.getBreastSize(partialTicks);
		outwardAngle = MathHelper.clamp(breasts.getCleavage() * 100f, -35f, 35f);
		wobbleEnabled = entityConfig.hasWobble();
		wobbleIntensity = entityConfig.getWobbleIntensity();
		wobbleSpeed = entityConfig.getWobbleSpeed();
		nippleDetail = breasts.hasNipples();

		float configuredSize = entityConfig.getBustSize();
		float signature = configuredSize * 31F + breasts.getWidth() * 37F + breasts.getHeight() * 41F
				+ breasts.getProjection() * 43F + breasts.getBalance() * 47F
				+ breasts.getShape().ordinal() * 53F + breasts.getNippleSize() * 59F;
		if(Float.compare(meshSignature, signature) != 0) {
			float leftScale = Math.max(0.05F, 1F + breasts.getBalance());
			float rightScale = Math.max(0.05F, 1F - breasts.getBalance());
			lBreast = new ProfiledBreastModelBox(64, 64, -4F, configuredSize, breasts.getWidth(), breasts.getHeight(),
					breasts.getProjection(), leftScale, breasts.getShape(), 20, 21);
			rBreast = new ProfiledBreastModelBox(64, 64, 0F, configuredSize, breasts.getWidth(), breasts.getHeight(),
					breasts.getProjection(), rightScale, breasts.getShape(), 24, 21);
			lBreastWear = new ProfiledBreastModelBox(64, 64, -4F, configuredSize, breasts.getWidth(), breasts.getHeight(),
					breasts.getProjection(), leftScale * 1.025F, breasts.getShape(), 20, 37);
			rBreastWear = new ProfiledBreastModelBox(64, 64, 0F, configuredSize, breasts.getWidth(), breasts.getHeight(),
					breasts.getProjection(), rightScale * 1.025F, breasts.getShape(), 24, 37);
			float detailSize = breasts.getNippleSize();
			lNipple = new NippleDetailModelBox(64, 64, -2F,
					ProfiledBreastModelBox.nippleY(configuredSize, breasts.getHeight() * leftScale, breasts.getShape()),
					ProfiledBreastModelBox.nippleZ(configuredSize, breasts.getProjection() * leftScale), detailSize, 22, 23);
			rNipple = new NippleDetailModelBox(64, 64, 2F,
					ProfiledBreastModelBox.nippleY(configuredSize, breasts.getHeight() * rightScale, breasts.getShape()),
					ProfiledBreastModelBox.nippleZ(configuredSize, breasts.getProjection() * rightScale), detailSize, 26, 23);
			meshSignature = signature;
		}

		lPhysPositionY = MathHelper.lerp(partialTicks, leftBreastPhysics.getPrePositionY(), leftBreastPhysics.getPositionY());
		lPhysPositionX = MathHelper.lerp(partialTicks, leftBreastPhysics.getPrePositionX(), leftBreastPhysics.getPositionX());
		lPhysBounceRotation = MathHelper.lerp(partialTicks, leftBreastPhysics.getPreBounceRotation(), leftBreastPhysics.getBounceRotation());
		if(breasts.isUniboob()) {
			rPhysPositionY = lPhysPositionY;
			rTotalX = lPhysPositionX;
			rPhysBounceRotation = lPhysBounceRotation;
		} else {
			BreastPhysics rightBreastPhysics = entityConfig.getRightBreastPhysics();
			rPhysPositionY = MathHelper.lerp(partialTicks, rightBreastPhysics.getPrePositionY(), rightBreastPhysics.getPositionY());
			rTotalX = MathHelper.lerp(partialTicks, rightBreastPhysics.getPrePositionX(), rightBreastPhysics.getPositionX());
			rPhysBounceRotation = MathHelper.lerp(partialTicks, rightBreastPhysics.getPreBounceRotation(), rightBreastPhysics.getBounceRotation());
		}
		breastSize = bSize * 1.5f;
		if(breastSize > 0.7f) breastSize = 0.7f;
		if(bSize > 0.7f) breastSize = bSize;
		if(breastSize < 0.02f) return false;

		zOffset = 0F;
		breastSize = bSize;

		float resistance = MathHelper.clamp(genderArmor.physicsResistance(), 0, 1);
		//Note: We only check if the breathing animation should be enabled if the chestplate's physics resistance
		// is less than or equal to 0.5 so that if we won't be rendering it we can avoid doing extra calculations
		breathingAnimation = ((entityConfig.getArmorPhysicsOverride() || resistance <= 0.5F) &&
				(!entity.isSubmergedInWater() || StatusEffectUtil.hasWaterBreathing(entity) ||
						entity.getWorld().getBlockState(new BlockPos(entity.getBlockX(), entity.getBlockY(), entity.getBlockZ())).isOf(Blocks.BUBBLE_COLUMN)));
		bounceEnabled = entityConfig.hasBreastPhysics() && (!isChestplateOccupied || resistance < 1); //oh, you found this?
		return true;
	}

	protected void setupTransformations(T entity, ModelPart body, MatrixStack matrixStack, BreastSide side) {
		boolean left = side == BreastSide.LEFT;
		matrixStack.translate(body.pivotX * 0.0625f, body.pivotY * 0.0625f, body.pivotZ * 0.0625f);
		if(body.roll != 0.0F) {
			matrixStack.multiply(new Quaternionf().rotationXYZ(0f, 0f, body.roll));
		}
		if(body.yaw != 0.0F) {
			matrixStack.multiply(new Quaternionf().rotationXYZ(0f, body.yaw, 0f));
		}
		if(body.pitch != 0.0F) {
			matrixStack.multiply(new Quaternionf().rotationXYZ(body.pitch, 0f, 0f));
		}

		if(bounceEnabled) {
			matrixStack.translate((left ? lPhysPositionX : rTotalX) / 32f, 0, 0);
			matrixStack.translate(0, (left ? lPhysPositionY : rPhysPositionY) / 32f, 0);
		}

		matrixStack.translate((left ? breastOffsetX : -breastOffsetX) * 0.0625f, 0.05625f + (breastOffsetY * 0.0625f), -0.125f + (breastOffsetZ * 0.0625f));

		if(!breasts.isUniboob()) {
			matrixStack.translate(-0.0625f * 2 * (left ? 1 : -1), 0, 0);
		}
		if(bounceEnabled) {
			matrixStack.multiply(new Quaternionf().rotationXYZ(0, (float)((left ? lPhysBounceRotation : rPhysBounceRotation) * (Math.PI / 180f)), 0));
		}
		if(!breasts.isUniboob()) {
			matrixStack.translate(0.0625f * 2 * (left ? 1 : -1), 0, 0);
		}

		float dynamicRotation = bounceEnabled ? MathHelper.clamp(-(left ? lPhysPositionY : rPhysPositionY) / 12f, -0.35F, 0.35F) : 0F;

		if(isChestplateOccupied) {
			matrixStack.translate(0, 0, 0.01f);
		}

		matrixStack.multiply(new Quaternionf().rotationXYZ(0, (float)((left ? outwardAngle : -outwardAngle) * (Math.PI / 180f)), 0));
		matrixStack.multiply(new Quaternionf().rotationXYZ((float)(-35f * dynamicRotation * (Math.PI / 180f)), 0, 0));

		if(breathingAnimation) {
			float f5 = -MathHelper.cos(entity.age * 0.09F) * 0.45F + 0.45F;
			matrixStack.multiply(new Quaternionf().rotationXYZ((float)(f5 * (Math.PI / 180f)), 0, 0));
		}

		float wobble = 0F;
		if(bounceEnabled && wobbleEnabled) {
			float phase = (entity.age + (left ? 0F : 2.35F)) * 0.24F * wobbleSpeed;
			float physicsDrive = (left ? lPhysPositionY : rPhysPositionY) * 0.018F;
			wobble = MathHelper.clamp(((float)Math.sin(phase) * 0.035F + physicsDrive) * wobbleIntensity, -0.28F, 0.28F);
		}
		matrixStack.scale((1F - wobble * 0.20F) * 0.9995F, 1F + wobble * 0.34F, 1F - wobble * 0.28F);
	}

	private void renderBreast(T entity, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int packedLightIn, int packedOverlayIn, BreastSide side) {
		RenderLayer breastRenderType = getRenderLayer(entity);
		if(breastRenderType == null) return; // only render if the player is visible in some capacity
		float alpha = entity.isInvisible() ? 0.15F : 1;
		VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(breastRenderType);
		renderBox(side == BreastSide.LEFT ? lBreast : rBreast, matrixStack, vertexConsumer, packedLightIn, packedOverlayIn, 1f, 1f, 1f, alpha);
		boolean jacketVisible = entity instanceof AbstractClientPlayerEntity player && player.isPartVisible(PlayerModelPart.JACKET);
		if(nippleDetail && !jacketVisible && !isChestplateOccupied) {
			renderBox(side == BreastSide.LEFT ? lNipple : rNipple, matrixStack, vertexConsumer, packedLightIn, packedOverlayIn, 1f, 1f, 1f, alpha);
		}
		if(jacketVisible) {
			matrixStack.translate(0, 0, -0.015f);
			matrixStack.scale(1.05f, 1.05f, 1.05f);
			renderBox(side == BreastSide.LEFT ? lBreastWear : rBreastWear, matrixStack, vertexConsumer, packedLightIn, packedOverlayIn, 1f, 1f, 1f, alpha);
		}
	}

	protected static void renderBox(WildfireModelRenderer.ModelBox model, MatrixStack matrixStack, VertexConsumer bufferIn, int packedLightIn, int packedOverlayIn,
	                              float red, float green, float blue, float alpha) {
		Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();
		Matrix3f matrix3f = matrixStack.peek().getNormalMatrix();
		for (WildfireModelRenderer.TexturedQuad quad : model.quads) {
			Vector3f vector3f = new Vector3f(quad.normal.x, quad.normal.y, quad.normal.z);
			vector3f.mul(matrix3f);
			float normalX = vector3f.x;
			float normalY = vector3f.y;
			float normalZ = vector3f.z;
			for (PositionTextureVertex vertex : quad.vertexPositions) {
				float j = vertex.x() / 16.0F;
				float k = vertex.y() / 16.0F;
				float l = vertex.z() / 16.0F;
				Vector4f vector4f = new Vector4f(j, k, l, 1.0F);
				vector4f.mul(matrix4f);
				bufferIn.vertex(vector4f.x, vector4f.y, vector4f.z, red, green, blue, alpha, vertex.texturePositionX(), vertex.texturePositionY(), packedOverlayIn, packedLightIn, normalX, normalY, normalZ);
			}
		}
	}
}
