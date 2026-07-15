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

package com.wildfire.gui.screen;

import com.wildfire.gui.WildfireSlider;
import com.wildfire.main.WildfireGender;
import com.wildfire.main.config.ClientConfig;
import com.wildfire.main.config.Configuration;
import com.wildfire.main.config.enums.Gender;
import com.wildfire.main.entitydata.PlayerConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Pose;
import org.jetbrains.annotations.UnknownNullability;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class WildfireBreastCustomizationScreen extends BaseWildfireScreen {

    private static final int FULL_WIDTH = 166;
    private static final int HALF_WIDTH = FULL_WIDTH / 2 - 2;

    private static final Component ENABLED = Component.translatable("wildfire_gender.label.enabled").withStyle(ChatFormatting.GREEN);
    private static final Component DISABLED = Component.translatable("wildfire_gender.label.disabled").withStyle(ChatFormatting.RED);

    private static final Identifier BACKGROUND_FEMALE = Identifier.fromNamespaceAndPath(WildfireGender.MODID, "textures/gui/breast_customization.png");
    private static final Identifier BACKGROUND_OTHER = Identifier.fromNamespaceAndPath(WildfireGender.MODID, "textures/gui/breast_customization_other.png");

    private static final Identifier BACKGROUND_MISC = Identifier.fromNamespaceAndPath(WildfireGender.MODID, "textures/gui/tabs/miscellaneous_tab.png");

    private static final float ENTITY_SCALE = 0.0625F;

    private Tab currentTab = Tab.VOLUME;
    private PreviewView previewView = PreviewView.FRONT;
    private boolean previewAutoFit = true;
    private float previewZoom = 1f;

    public WildfireBreastCustomizationScreen(Screen parent, UUID uuid) {
        super(Component.translatable("wildfire_gender.appearance_settings.title"), parent, uuid);
    }

    @Override
    public void init() {
        int y = this.height / 2 - 11;

        Tab[] tabs = Tab.values();
        int nextTabX = this.width / 2 - 127;
        for(Tab tab : tabs) {
            int tabX = nextTabX;
            addButton(builder -> builder
                    .message(() -> Component.translatable(tab.translationKey))
                    .position(tabX, y - 52)
                    .size(tab.width, 14)
                    .onPress(_ -> {
                        currentTab = tab;
                        rebuildWidgets();
                    })
                    .active(currentTab != tab));
            nextTabX += tab.width + 3;
        }

        initPreviewControls();

        final int tabOffsetY = y - 3 - 21;
        switch(currentTab) {
            case VOLUME -> initVolumeTab(tabOffsetY);
            case SHAPE -> initShapeTab(tabOffsetY);
            case PLACEMENT -> initPlacementTab(tabOffsetY);
            case MOTION -> initPhysicsTab(tabOffsetY);
            case DETAILS -> initDetailsTab(tabOffsetY);
        }

    }

    private void initPreviewControls() {
        int x = this.width / 2 - 134;
        int viewY = this.height / 2 + 44;
        for(PreviewView view : PreviewView.values()) {
            addButton(builder -> builder
                    .message(() -> Component.translatable(view.translationKey))
                    .position(x + view.index * 31, viewY)
                    .size(29, 13)
                    .onPress(_ -> {
                        previewView = view;
                        rebuildWidgets();
                    })
                    .active(previewView != view)
                    .tooltip(Tooltip.create(Component.translatable(view.tooltipKey))));
        }

        int zoomY = viewY + 15;
        addButton(builder -> builder
                .message(() -> Component.translatable("wildfire_gender.preview.zoom_out"))
                .position(x, zoomY)
                .size(22, 13)
                .onPress(_ -> adjustPreviewZoom(false))
                .tooltip(Tooltip.create(Component.translatable("wildfire_gender.preview.zoom_out.tooltip"))));

        addButton(builder -> builder
                .message(() -> Component.translatable("wildfire_gender.preview.auto_fit"))
                .position(x + 24, zoomY)
                .size(43, 13)
                .onPress(_ -> {
                    previewAutoFit = true;
                    rebuildWidgets();
                })
                .active(!previewAutoFit)
                .tooltip(Tooltip.create(Component.translatable("wildfire_gender.preview.auto_fit.tooltip"))));

        addButton(builder -> builder
                .message(() -> Component.translatable("wildfire_gender.preview.zoom_in"))
                .position(x + 69, zoomY)
                .size(22, 13)
                .onPress(_ -> adjustPreviewZoom(true))
                .tooltip(Tooltip.create(Component.translatable("wildfire_gender.preview.zoom_in.tooltip"))));
    }

    private void adjustPreviewZoom(boolean zoomIn) {
        float current = effectivePreviewZoom();
        previewAutoFit = false;
        previewZoom = clamp(current * (zoomIn ? 1.2f : 1f / 1.2f), 0.015f, 1.5f);
        rebuildWidgets();
    }

    private void initVolumeTab(final int tabOffsetY) {
        final var plr = Objects.requireNonNull(getPlayer(), "getPlayer()");
        final var breasts = plr.getBreasts();

        addSlider(builder -> builder
                .message(value -> Component.translatable("wildfire_gender.wardrobe.slider.expansion",
                        formatMultiplier(value / Configuration.BUST_SIZE.getDefault())))
                .position(this.width / 2 - 36, tabOffsetY - 2)
                .size(FULL_WIDTH, 20)
                .range(Configuration.BUST_SIZE)
                .current(plr.getBustSize())
                .update(plr::updateBustSize)
                .step(0.02)
                .mouseStep(0.01)
                .curve(2.4)
                .tooltip(Tooltip.create(Component.translatable("wildfire_gender.tooltip.expansion"))));

        addSlider(builder -> builder
                .message(value -> Component.translatable("wildfire_gender.wardrobe.slider.width", formatMultiplier(value)))
                .position(this.width / 2 - 36, tabOffsetY + 22)
                .size(HALF_WIDTH, 20)
                .range(Configuration.BREASTS_WIDTH)
                .current(breasts.getWidth())
                .update(breasts::updateWidth)
                .step(0.02)
                .mouseStep(0.01)
                .curve(1.8));

        addSlider(builder -> builder
                .message(value -> Component.translatable("wildfire_gender.wardrobe.slider.vertical_fullness", formatMultiplier(value)))
                .position(this.width / 2 - 36 + HALF_WIDTH + 4, tabOffsetY + 22)
                .size(HALF_WIDTH, 20)
                .range(Configuration.BREASTS_HEIGHT)
                .current(breasts.getHeight())
                .update(breasts::updateHeight)
                .step(0.02)
                .mouseStep(0.01)
                .curve(1.8));

        addSlider(builder -> builder
                .message(value -> Component.translatable("wildfire_gender.wardrobe.slider.projection", formatMultiplier(value)))
                .position(this.width / 2 - 36, tabOffsetY + 46)
                .size(HALF_WIDTH, 20)
                .range(Configuration.BREASTS_PROJECTION)
                .current(breasts.getProjection())
                .update(breasts::updateProjection)
                .step(0.02)
                .mouseStep(0.01)
                .curve(2));

        addSlider(builder -> builder
                .message(value -> Component.translatable("wildfire_gender.wardrobe.slider.balance", formatBalance(value)))
                .position(this.width / 2 - 36 + HALF_WIDTH + 4, tabOffsetY + 46)
                .size(HALF_WIDTH, 20)
                .range(Configuration.BREASTS_BALANCE)
                .current(breasts.getBalance())
                .update(breasts::updateBalance)
                .step(0.0125)
                .mouseStep(0.0125)
                .centerZero()
                .tooltip(Tooltip.create(Component.translatable("wildfire_gender.tooltip.balance"))));

        addButton(builder -> builder
                .message(() -> Component.translatable("wildfire_gender.breast_customization.reset_volume"))
                .position(this.width / 2 - 36, tabOffsetY + 94)
                .size(FULL_WIDTH, 20)
                .onPress(_ -> {
                    plr.updateBustSize(Configuration.BUST_SIZE.getDefault());
                    breasts.resetVolume();
                    plr.save();
                    rebuildWidgets();
                })
                .tooltip(Tooltip.create(Component.translatable("wildfire_gender.tooltip.reset_volume"))));
    }

    private void initShapeTab(final int tabOffsetY) {
        final var plr = Objects.requireNonNull(getPlayer(), "getPlayer()");
        final var breasts = plr.getBreasts();

        addButton(builder -> builder
                .message(() -> Component.translatable("wildfire_gender.breast_customization.profile",
                        breasts.getShape().displayName()))
                .position(this.width / 2 - 36, tabOffsetY - 2)
                .size(FULL_WIDTH, 20)
                .onPress(button -> {
                    breasts.updateShape(breasts.getShape().next());
                    plr.save();
                    button.updateMessage();
                })
                .tooltip(Tooltip.create(Component.translatable("wildfire_gender.tooltip.profile"))));

        addSlider(builder -> builder
                .message(value -> Component.translatable("wildfire_gender.wardrobe.slider.root_width",
                        formatMultiplier(value)))
                .position(this.width / 2 - 36, tabOffsetY + 22)
                .size(HALF_WIDTH, 20)
                .range(Configuration.BREASTS_ROOT_WIDTH)
                .current(breasts.getRootWidth())
                .update(breasts::updateRootWidth)
                .step(0.02)
                .mouseStep(0.01)
                .curve(1.7)
                .tooltip(Tooltip.create(Component.translatable("wildfire_gender.tooltip.root_width"))));

        addSlider(builder -> builder
                .message(value -> Component.translatable("wildfire_gender.wardrobe.slider.outer_fullness",
                        formatMultiplier(value)))
                .position(this.width / 2 - 36 + HALF_WIDTH + 4, tabOffsetY + 22)
                .size(HALF_WIDTH, 20)
                .range(Configuration.BREASTS_OUTER_FULLNESS)
                .current(breasts.getOuterFullness())
                .update(breasts::updateOuterFullness)
                .step(0.02)
                .mouseStep(0.01)
                .curve(1.9)
                .tooltip(Tooltip.create(Component.translatable("wildfire_gender.tooltip.outer_fullness"))));

        addSlider(builder -> builder
                .message(value -> Component.translatable("wildfire_gender.wardrobe.slider.drop", formatSigned(value)))
                .position(this.width / 2 - 36, tabOffsetY + 46)
                .size(HALF_WIDTH, 20)
                .range(Configuration.BREASTS_DROP)
                .current(breasts.getDrop())
                .update(breasts::updateDrop)
                .step(0.05)
                .mouseStep(0.025)
                .centerZero()
                .tooltip(Tooltip.create(Component.translatable("wildfire_gender.tooltip.drop"))));

        addSlider(builder -> builder
                .message(value -> Component.translatable("wildfire_gender.wardrobe.slider.angle",
                        Math.round(value * 100)))
                .position(this.width / 2 - 36 + HALF_WIDTH + 4, tabOffsetY + 46)
                .size(HALF_WIDTH, 20)
                .range(Configuration.BREASTS_CLEAVAGE)
                .current(breasts.getCleavage())
                .update(breasts::updateCleavage)
                .step(0.01)
                .mouseStep(0.005)
                .centerZero()
                .tooltip(Tooltip.create(Component.translatable("wildfire_gender.tooltip.angle"))));

        addButton(builder -> builder
                .message(() -> Component.translatable("wildfire_gender.breast_customization.reset_shape"))
                .position(this.width / 2 - 36, tabOffsetY + 94)
                .size(FULL_WIDTH, 20)
                .onPress(_ -> {
                    breasts.resetShape();
                    plr.save();
                    rebuildWidgets();
                }));
    }

    private static Component formatBalance(float value) {
        int percent = Math.round(Math.abs(value) * 100);
        if(percent == 0) {
            return Component.translatable("wildfire_gender.wardrobe.slider.balance_even");
        }
        return Component.translatable(
                value > 0 ? "wildfire_gender.wardrobe.slider.balance_left" : "wildfire_gender.wardrobe.slider.balance_right",
                percent
        );
    }

    private void initPlacementTab(final int tabOffsetY) {
        final var plr = Objects.requireNonNull(getPlayer(), "getPlayer()");
        final var breasts = plr.getBreasts();

        addSlider(builder -> builder
                .message(value -> Component.translatable("wildfire_gender.wardrobe.slider.separation", formatSigned(value)))
                .position(this.width / 2 - 36, tabOffsetY - 2)
                .size(FULL_WIDTH, 20)
                .range(Configuration.BREASTS_OFFSET_X)
                .current(breasts.getXOffset())
                .update(breasts::updateXOffset)
                .step(0.05)
                .mouseStep(0.025)
                .centerZero()
                .tooltip(Tooltip.create(Component.translatable("wildfire_gender.tooltip.separation"))));

        addSlider(builder -> builder
                .message(value -> Component.translatable("wildfire_gender.wardrobe.slider.position_y", formatSigned(value)))
                .position(this.width / 2 - 36, tabOffsetY + 22)
                .size(HALF_WIDTH, 20)
                .range(Configuration.BREASTS_OFFSET_Y)
                .current(breasts.getYOffset())
                .update(breasts::updateYOffset)
                .step(0.05)
                .mouseStep(0.025)
                .centerZero());

        addSlider(builder -> builder
                .message(value -> Component.translatable("wildfire_gender.wardrobe.slider.position_z", formatSigned(value)))
                .position(this.width / 2 - 36 + HALF_WIDTH + 4, tabOffsetY + 22)
                .size(HALF_WIDTH, 20)
                .range(Configuration.BREASTS_OFFSET_Z)
                .current(breasts.getZOffset())
                .update(breasts::updateZOffset)
                .step(0.05)
                .mouseStep(0.025)
                .centerZero());

        addButton(builder -> builder
                .message(() -> Component.translatable("wildfire_gender.breast_customization.reset_placement"))
                .position(this.width / 2 - 36, tabOffsetY + 94)
                .size(HALF_WIDTH, 20)
                .onPress(_ -> {
                    breasts.resetPlacement();
                    plr.save();
                    rebuildWidgets();
                }));

        addButton(builder -> builder
                .message(() -> Component.translatable("wildfire_gender.presets.button"))
                .position(this.width / 2 - 36 + HALF_WIDTH + 4, tabOffsetY + 94)
                .size(HALF_WIDTH, 20)
                .onPress(_ -> {
                    //~ if >=26.2 'minecraft.setScreen' -> 'minecraft.gui.setScreen'
                    minecraft.gui.setScreen(new WildfirePresetScreen(this, playerUUID));
                }));
    }

    private static String formatSigned(float value) {
        return String.format(Locale.ROOT, "%+.2f", value);
    }

    private static String formatMultiplier(float value) {
        return String.format(Locale.ROOT, "%.2f×", value);
    }

    private void initPhysicsTab(final int tabOffsetY) {
        final var plr = Objects.requireNonNull(getPlayer(), "getPlayer()");
        final var breasts = plr.getBreasts();
        final var ref = new Object() {
            @UnknownNullability
            AbstractWidget bounceSlider, floppySlider, overridePhysics, dualPhysics,
                    wobbleToggle, wobbleIntensity, wobbleSpeed, previewMotion;
        };

        ref.previewMotion = addButton(builder -> builder
                .message(() -> Component.translatable("wildfire_gender.char_settings.preview_motion"))
                .position(this.width / 2 - 36, tabOffsetY + 94)
                .size(HALF_WIDTH, 20)
                .onPress(_ -> {
                    plr.getLeftBreastPhysics().addPreviewImpulse(-0.34f);
                    plr.getRightBreastPhysics().addPreviewImpulse(-0.30f);
                })
                .active(plr.hasBreastPhysics()));

        addButton(builder -> builder
                .message(() -> Component.translatable("wildfire_gender.char_settings.physics", plr.hasBreastPhysics() ? ENABLED : DISABLED))
                .position(this.width / 2 - 36, tabOffsetY - 2)
                .size(FULL_WIDTH, 20)
                .onPress(button -> {
                    plr.updateBreastPhysics(!plr.hasBreastPhysics());
                    plr.save();
                    button.updateMessage();
                    ref.bounceSlider.active = plr.hasBreastPhysics();
                    ref.floppySlider.active = plr.hasBreastPhysics();
                    ref.overridePhysics.active = plr.hasBreastPhysics();
                    ref.dualPhysics.active = plr.hasBreastPhysics();
                    ref.wobbleToggle.active = plr.hasBreastPhysics();
                    ref.wobbleIntensity.active = plr.hasBreastPhysics() && plr.hasWobble();
                    ref.wobbleSpeed.active = plr.hasBreastPhysics() && plr.hasWobble();
                    ref.previewMotion.active = plr.hasBreastPhysics();
                }));

        ref.dualPhysics = addButton(builder -> builder
                .message(() -> Component.translatable("wildfire_gender.breast_customization.dual_physics", Component.translatable(breasts.isUniboob() ? "wildfire_gender.label.no" : "wildfire_gender.label.yes")))
                .position(this.width / 2 - 36, tabOffsetY + 22)
                .size(HALF_WIDTH, 20)
                .onPress(button -> {
                    breasts.updateUniboob(!breasts.isUniboob());
                    plr.save();
                    button.updateMessage();
                })
                .active(plr.hasBreastPhysics()));

        ref.wobbleToggle = addButton(builder -> builder
                .message(() -> Component.translatable("wildfire_gender.breast_customization.wobble",
                        plr.hasWobble() ? ENABLED : DISABLED))
                .position(this.width / 2 - 36 + HALF_WIDTH + 4, tabOffsetY + 22)
                .size(HALF_WIDTH, 20)
                .onPress(button -> {
                    plr.updateWobble(!plr.hasWobble());
                    plr.save();
                    ref.wobbleIntensity.active = plr.hasBreastPhysics() && plr.hasWobble();
                    ref.wobbleSpeed.active = plr.hasBreastPhysics() && plr.hasWobble();
                    button.updateMessage();
                })
                .active(plr.hasBreastPhysics())
                .tooltip(Tooltip.create(Component.translatable("wildfire_gender.tooltip.wobble"))));

        ref.overridePhysics = addButton(builder -> builder
                .message(() -> {
                    var value = ClientConfig.INSTANCE.get(ClientConfig.ARMOR_PHYSICS_OVERRIDE);
                    return Component.translatable("wildfire_gender.char_settings.override_armor_physics", value ? ENABLED : DISABLED);
                })
                .position(this.width / 2 - 36 + HALF_WIDTH + 4, tabOffsetY + 94)
                .size(HALF_WIDTH, 20)
                .onPress(button -> {
                    ClientConfig.INSTANCE.toggle(ClientConfig.ARMOR_PHYSICS_OVERRIDE);
                    ClientConfig.INSTANCE.save();
                    button.updateMessage();
                })
                .tooltip(Tooltip.create(Component.translatable("wildfire_gender.tooltip.override_armor_physics.line1")
                        .append("\n\n")
                        .append(Component.translatable("wildfire_gender.tooltip.override_armor_physics.line2"))))
                .active(plr.hasBreastPhysics()));

        ref.bounceSlider = addSlider(builder -> builder
                .message(value -> Component.translatable("wildfire_gender.slider.motion_range",
                        formatMultiplier(value / Configuration.BOUNCE_MULTIPLIER.getDefault())))
                .position(this.width / 2 - 36, tabOffsetY + 46)
                .size(HALF_WIDTH, 20)
                .range(Configuration.BOUNCE_MULTIPLIER)
                .current(plr.getBounceMultiplier())
                .update(plr::updateBounceMultiplier)
                .step(0.02)
                .mouseStep(0.01)
                .curve(1.5)
                .tooltip(Tooltip.create(Component.translatable("wildfire_gender.tooltip.motion_multiplier")))
                .active(plr.hasBreastPhysics()));

        ref.floppySlider = addSlider(builder -> builder
                .message(value -> Component.translatable("wildfire_gender.slider.softness",
                        formatMultiplier(value / Configuration.FLOPPY_MULTIPLIER.getDefault())))
                .position(this.width / 2 - 36 + HALF_WIDTH + 4, tabOffsetY + 46)
                .size(HALF_WIDTH, 20)
                .range(Configuration.FLOPPY_MULTIPLIER)
                .current(plr.getFloppiness())
                .update(plr::updateFloppiness)
                .step(0.02)
                .mouseStep(0.01)
                .curve(1.5)
                .tooltip(Tooltip.create(Component.translatable("wildfire_gender.tooltip.motion_multiplier")))
                .active(plr.hasBreastPhysics()));

        ref.wobbleIntensity = addSlider(builder -> builder
                .message(value -> Component.translatable("wildfire_gender.slider.wobble_intensity",
                        formatMultiplier(value / Configuration.WOBBLE_INTENSITY.getDefault())))
                .position(this.width / 2 - 36, tabOffsetY + 70)
                .size(HALF_WIDTH, 20)
                .range(Configuration.WOBBLE_INTENSITY)
                .current(plr.getWobbleIntensity())
                .update(plr::updateWobbleIntensity)
                .step(0.05)
                .mouseStep(0.025)
                .curve(1.5)
                .tooltip(Tooltip.create(Component.translatable("wildfire_gender.tooltip.motion_multiplier")))
                .active(plr.hasBreastPhysics() && plr.hasWobble()));

        ref.wobbleSpeed = addSlider(builder -> builder
                .message(value -> Component.translatable("wildfire_gender.slider.wobble_speed",
                        formatMultiplier(value / Configuration.WOBBLE_SPEED.getDefault())))
                .position(this.width / 2 - 36 + HALF_WIDTH + 4, tabOffsetY + 70)
                .size(HALF_WIDTH, 20)
                .range(Configuration.WOBBLE_SPEED)
                .current(plr.getWobbleSpeed())
                .update(plr::updateWobbleSpeed)
                .step(0.05)
                .mouseStep(0.025)
                .curve(1.5)
                .tooltip(Tooltip.create(Component.translatable("wildfire_gender.tooltip.motion_multiplier")))
                .active(plr.hasBreastPhysics() && plr.hasWobble()));
    }

    private void initDetailsTab(final int tabOffsetY) {
        final var plr = Objects.requireNonNull(getPlayer(), "getPlayer()");
        final var breasts = plr.getBreasts();
        final var config = ClientConfig.INSTANCE;
        final var ref = new Object() {
            @UnknownNullability
            AbstractWidget nippleSize;
        };

        addButton(builder -> builder
                .message(() -> Component.translatable("wildfire_gender.breast_customization.nipples",
                        breasts.hasNipples() ? ENABLED : DISABLED))
                .position(this.width / 2 - 36, tabOffsetY - 2)
                .size(HALF_WIDTH, 20)
                .onPress(button -> {
                    breasts.updateNipples(!breasts.hasNipples());
                    plr.save();
                    ref.nippleSize.active = breasts.hasNipples();
                    button.updateMessage();
                })
                .tooltip(Tooltip.create(Component.translatable("wildfire_gender.tooltip.nipples"))));

        ref.nippleSize = addSlider(builder -> builder
                .message(value -> Component.translatable("wildfire_gender.slider.nipple_size", Math.round(value * 100)))
                .position(this.width / 2 - 36 + HALF_WIDTH + 4, tabOffsetY - 2)
                .size(HALF_WIDTH, 20)
                .range(Configuration.BREASTS_NIPPLE_SIZE)
                .current(breasts.getNippleSize())
                .update(breasts::updateNippleSize)
                .step(0.05)
                .mouseStep(0.025)
                .curve(1.5)
                .active(breasts.hasNipples()));

        addButton(builder -> builder
                .message(() -> Component.translatable("wildfire_gender.uv_editor"))
                .position(this.width / 2 - 36, tabOffsetY + 22)
                .size(FULL_WIDTH, 20)
                .onPress(_ -> {
                    //~ if >=26.2 'minecraft.setScreen' -> 'minecraft.gui.setScreen'
                    minecraft.gui.setScreen(new WildfireBreastUVEditorScreen(this, playerUUID));
                }));

        addButton(builder -> builder
                .message(() -> Component.translatable("wildfire_gender.char_settings.hide_in_armor",
                        plr.showBreastsInArmor() ? DISABLED : ENABLED))
                .position(this.width / 2 - 36, tabOffsetY + 46)
                .size(FULL_WIDTH, 20)
                .onPress(button -> {
                    plr.updateShowBreastsInArmor(!plr.showBreastsInArmor());
                    plr.save();
                    button.updateMessage();
                }));

        addButton(builder -> builder
                .message(() -> Component.translatable("wildfire_gender.char_settings.show_armor_stat",
                        config.get(ClientConfig.ARMOR_STAT) ? ENABLED : DISABLED))
                .position(this.width / 2 - 36, tabOffsetY + 70)
                .size(FULL_WIDTH, 20)
                .onPress(button -> {
                    config.toggle(ClientConfig.ARMOR_STAT);
                    config.save();
                    button.updateMessage();
                }));

        addButton(builder -> builder
                .message(() -> Component.translatable("wildfire_gender.breast_customization.reset_details"))
                .position(this.width / 2 - 36, tabOffsetY + 94)
                .size(FULL_WIDTH, 20)
                .onPress(_ -> {
                    breasts.updateNipples(Configuration.BREASTS_NIPPLES.getDefault());
                    breasts.updateNippleSize(Configuration.BREASTS_NIPPLE_SIZE.getDefault());
                    plr.save();
                    rebuildWidgets();
                }));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        extractTransparentBackground(graphics);

        PlayerConfig plr = getPlayer();
        if(plr == null) return;
        Identifier backgroundTexture = switch(plr.getGender()) {
            case Gender.MALE -> null;
            case Gender.FEMALE -> BACKGROUND_FEMALE;
            case Gender.OTHER -> BACKGROUND_OTHER;
        };

        if(backgroundTexture != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, backgroundTexture, (this.width - 272) / 2, (this.height - 138) / 2, 0, 0, 272, 130, 512, 512);
        }

        graphics.blit(RenderPipelines.GUI_TEXTURED, currentTab.background, (this.width) / 2 - 42, (this.height) / 2 - 43, 0, 0, 178, currentTab.backgroundHeight, 512, 512);
        graphics.text(font, getTitle(), (width / 2) - font.width(getTitle()) / 2, (height / 2) - 82, 0xFFFFFF, false);

        renderBreastPreview(graphics, mouseY);
    }

    private void renderBreastPreview(GuiGraphicsExtractor graphics, int mouseY) {
        var player = minecraft.player;
        if(player == null) return;

        int centerX = this.width / 2 - 89;
        int centerY = this.height / 2 + 38;
        int left = centerX - 43;
        int top = centerY - 79;
        int right = centerX + 43;
        int scissorBottom = centerY + 4;
        int scale = Math.max(1, Math.round(70 * effectivePreviewZoom()));

        graphics.enableScissor(left, top, right, scissorBottom);
        drawPreviewEntity(graphics, left, top, right, centerY + 68, scale, mouseY, player);
        graphics.disableScissor();
    }

    private void drawPreviewEntity(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2,
                                   int size, float mouseY, net.minecraft.world.entity.LivingEntity entity) {
        float centerY = (y1 + y2) / 2f;
        float verticalAngle = (float) Math.atan((centerY - mouseY) / 40f);
        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf xRotation = new Quaternionf().rotateX(verticalAngle * 20f * (float) (Math.PI / 180));
        rotation.mul(xRotation);

        EntityRenderState renderState = InventoryScreen.extractRenderState(entity);
        if(renderState instanceof LivingEntityRenderState livingState) {
            livingState.bodyRot = 180f + previewView.yaw;
            livingState.yRot = previewView.yaw;
            if(livingState.pose != Pose.FALL_FLYING) {
                livingState.xRot = -verticalAngle * 20f;
            } else {
                livingState.xRot = 0f;
            }

            livingState.boundingBoxWidth /= livingState.scale;
            livingState.boundingBoxHeight /= livingState.scale;
            livingState.scale = 1f;
        }

        Vector3f translation = new Vector3f(0f, renderState.boundingBoxHeight / 2f + ENTITY_SCALE, 0f);
        graphics.entity(renderState, size, translation, rotation, xRotation, x1, y1, x2, y2);
    }

    private float effectivePreviewZoom() {
        return previewAutoFit ? calculateAutoFitZoom() : previewZoom;
    }

    private float calculateAutoFitZoom() {
        PlayerConfig plr = getPlayer();
        if(plr == null) return 1f;
        var breasts = plr.getBreasts();

        float bust = clamp(plr.getBustSize(), .02f, 8f);
        float width = clamp(breasts.getWidth(), .1f, 5f);
        float height = clamp(breasts.getHeight(), .1f, 5f);
        float projectionScale = clamp(breasts.getProjection(), .1f, 7f);
        float root = clamp(breasts.getRootWidth(), .05f, 5f);
        float fullness = clamp(breasts.getOuterFullness(), .05f, 5.5f);
        float balance = clamp(Math.abs(breasts.getBalance()), 0f, .95f);
        float drop = clamp(breasts.getDrop(), -5f, 7f);

        float baseX = 1.55f + .55f * bust + .07f * bust * bust;
        float baseY = 2f + .58f * bust + .07f * bust * bust;
        float sideScale = 1f + balance;
        float rootX = Math.min(80f, 1.94f * root * (1f + .035f * bust))
                * width * sideScale * (1f + .22f * .62f);
        float outerX = Math.min(120f, baseX * fullness) * width * sideScale * 1.9f;
        float outerY = Math.min(120f, baseY * (.72f + .28f * fullness))
                * height * sideScale * (1f + .42f * .62f);
        float projection = Math.min(160f, (1.35f + 1.4f * bust + .22f * bust * bust)
                * (.78f + .22f * (float) Math.sqrt(fullness)))
                * projectionScale * sideScale * (1f + .34f * .62f);

        float horizontalSpan = 4f + 2f * Math.max(rootX, outerX) + Math.abs(breasts.getXOffset()) * 2f;
        float sag = Math.abs(drop) * Math.max(1f, outerY * .52f);
        float verticalSpan = outerY * 3.05f + sag * 2f + Math.abs(breasts.getYOffset()) * 2f;
        float depthSpan = projection + Math.abs(breasts.getZOffset());

        float verticalRatio = verticalSpan / 31f;
        float frontRatio = Math.max(horizontalSpan / 10f, verticalRatio);
        float sideRatio = Math.max(depthSpan / 10f, verticalRatio);
        float viewRatio = switch(previewView) {
            case FRONT -> frontRatio;
            case THREE_QUARTER -> Math.max((horizontalSpan * .72f + depthSpan * .70f) / 10f, verticalRatio);
            case SIDE -> sideRatio;
        };
        return clamp(.94f / Math.max(1f, viewRatio), .015f, 1.1f);
    }

    private static float clamp(float value, float min, float max) {
        if(!Float.isFinite(value)) return min;
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent arg) {
        //Ensure all sliders are saved
        children().forEach(child -> {
            if(child instanceof WildfireSlider slider) {
                slider.save();
            }
        });
        return super.mouseReleased(arg);
    }

    private enum PreviewView {
        FRONT(0, 0f, "wildfire_gender.preview.front", "wildfire_gender.preview.front.tooltip"),
        THREE_QUARTER(1, 38f, "wildfire_gender.preview.three_quarter", "wildfire_gender.preview.three_quarter.tooltip"),
        SIDE(2, 90f, "wildfire_gender.preview.side", "wildfire_gender.preview.side.tooltip"),
        ;

        final int index;
        final float yaw;
        final String translationKey;
        final String tooltipKey;

        PreviewView(int index, float yaw, String translationKey, String tooltipKey) {
            this.index = index;
            this.yaw = yaw;
            this.translationKey = translationKey;
            this.tooltipKey = tooltipKey;
        }
    }

    private enum Tab {
        VOLUME("wildfire_gender.breast_customization.tab_volume", 45),
        SHAPE("wildfire_gender.breast_customization.tab_shape", 42),
        PLACEMENT("wildfire_gender.breast_customization.tab_placement", 62),
        MOTION("wildfire_gender.breast_customization.tab_motion", 46),
        DETAILS("wildfire_gender.breast_customization.tab_details", 47),
        ;

        final String translationKey;
        final int width;
        final Identifier background = BACKGROUND_MISC;
        final int backgroundHeight = 128;

        Tab(String translationKey, int width) {
            this.translationKey = translationKey;
            this.width = width;
        }
    }
}
