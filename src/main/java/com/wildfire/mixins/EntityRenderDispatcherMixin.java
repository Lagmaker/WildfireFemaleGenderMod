/*
 * Wildfire's Female Gender Mod is a female gender mod created for Minecraft.
 * Copyright (C) 2023-present WildfireRomeo
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License, version 3 or later.
 */

package com.wildfire.mixins;

import com.wildfire.main.config.ClientConfig;
import com.wildfire.main.entitydata.EntityConfig;
import com.wildfire.render.BreastRenderBounds;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps permissive generated geometry from being culled against the much smaller vanilla body. */
@Mixin(EntityRenderDispatcher.class)
@Environment(EnvType.CLIENT)
abstract class EntityRenderDispatcherMixin {
    @Inject(
            method = "shouldRender(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/culling/Frustum;DDD)Z",
            at = @At("RETURN"),
            cancellable = true
    )
    private void wildfiregender$expandVisualBounds(Entity entity, Frustum frustum,
                                                    double cameraX, double cameraY, double cameraZ,
                                                    CallbackInfoReturnable<Boolean> result) {
        if(result.getReturnValueZ() || !ClientConfig.RENDER_BREASTS
                || !(entity instanceof LivingEntity living) || !EntityConfig.isSupportedEntity(living)) {
            return;
        }

        EntityConfig config = EntityConfig.getEntity(living);
        double radius = BreastRenderBounds.estimateRadius(config);
        if(radius <= 0D) return;

        double centerDistance = Math.sqrt(entity.distanceToSqr(cameraX, cameraY, cameraZ));
        double surfaceDistance = Math.max(0D, centerDistance - radius);
        if(!entity.shouldRenderAtSqrDistance(surfaceDistance * surfaceDistance)) return;

        if(frustum.isVisible(entity.getBoundingBox().inflate(radius))) {
            result.setReturnValue(true);
        }
    }
}
