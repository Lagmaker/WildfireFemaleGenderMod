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

package com.wildfire.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wildfire.gui.WildfireBreastPresetList;
import com.wildfire.gui.WildfireButton;
import com.wildfire.gui.WildfireSlider;
import com.wildfire.main.entitydata.Breasts;
import com.wildfire.main.entitydata.PlayerConfig;
import com.wildfire.main.config.Configuration;
import com.wildfire.main.config.BreastPresetConfiguration;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

public class WildfireBreastCustomizationScreen extends BaseWildfireScreen {

    private WildfireSlider breastSlider, xOffsetBoobSlider, yOffsetBoobSlider, zOffsetBoobSlider, cleavageSlider;
    private WildfireSlider widthSlider, heightSlider, projectionSlider, balanceSlider, nippleSizeSlider;
    private WildfireSlider bounceSlider, floppySlider, wobbleIntensitySlider, wobbleSpeedSlider;
    private WildfireButton btnDualPhysics, btnPresets, btnCustomization, btnShapeTab, btnMotionTab;
    private WildfireButton btnShape, btnNipples, btnPhysics, btnWobble;
    private WildfireButton btnAddPreset, btnDeletePreset;

    private WildfireBreastPresetList PRESET_LIST;
    private int currentTab = 0; // 0 = volume, 1 = shape, 2 = motion, 3 = presets

    public WildfireBreastCustomizationScreen(Screen parent, UUID uuid) {
        super(Text.translatable("wildfire_gender.appearance_settings.title"), parent, uuid);
    }

    @Override
    public void init() {
        int j = this.height / 2 - 11;

        PlayerConfig plr = getPlayer();
        Breasts breasts = plr.getBreasts();
        FloatConsumer onSave = value -> {
            //Just save as we updated the actual value in value change
            PlayerConfig.saveGenderInfo(plr);
        };

        this.addDrawableChild(new WildfireButton(this.width / 2 + 178, j - 72, 9, 9, Text.literal("X"),
              button -> MinecraftClient.getInstance().setScreen(parent)));

        int tabX = this.width / 2 + 30;
        this.addDrawableChild(btnCustomization = new WildfireButton(tabX, j - 60, 39, 10,
                Text.translatable("wildfire_gender.editor.volume"), button -> selectTab(0))).setActive(false);
        this.addDrawableChild(btnShapeTab = new WildfireButton(tabX + 40, j - 60, 39, 10,
                Text.translatable("wildfire_gender.editor.shape"), button -> selectTab(1)));
        this.addDrawableChild(btnMotionTab = new WildfireButton(tabX + 80, j - 60, 39, 10,
                Text.translatable("wildfire_gender.editor.motion"), button -> selectTab(2)));
        this.addDrawableChild(btnPresets = new WildfireButton(tabX + 120, j - 60, 38, 10,
                Text.translatable("wildfire_gender.editor.presets"), button -> {
            selectTab(3);
            PRESET_LIST.refreshList();
        }));
        this.addDrawableChild(btnAddPreset = new WildfireButton(this.width / 2 + 31 + 158/2, j + 80, 158 / 2 - 1, 12,
                Text.translatable("wildfire_gender.breast_customization.presets.add_new"), button -> {
            createNewPreset("Preset " + (PRESET_LIST.getPresetList().length + 1));
        }));
        btnAddPreset.visible = false;

        this.addDrawableChild(btnDeletePreset = new WildfireButton(this.width / 2 + 30, j + 80, 158 / 2 - 1, 12,
                Text.translatable("wildfire_gender.breast_customization.presets.delete"), button -> {

        })).setActive(false);
        btnDeletePreset.visible = false;

        //Customization Tab Below


        this.addDrawableChild(this.breastSlider = new WildfireSlider(this.width / 2 + 30, j - 48, 158, 20, Configuration.BUST_SIZE, plr.getBustSize(),
              plr::updateBustSize, value -> Text.translatable("wildfire_gender.wardrobe.slider.breast_size", Math.round(value * 1.25f * 100)), onSave));

        //Customization
        this.addDrawableChild(this.xOffsetBoobSlider = new WildfireSlider(this.width / 2 + 30, j - 27, 158, 20, Configuration.BREASTS_OFFSET_X, breasts.getXOffset(),
              breasts::updateXOffset, value -> Text.translatable("wildfire_gender.wardrobe.slider.separation", Math.round((Math.round(value * 100f) / 100f) * 10)), onSave));
        this.addDrawableChild(this.yOffsetBoobSlider = new WildfireSlider(this.width / 2 + 30, j - 6, 158, 20, Configuration.BREASTS_OFFSET_Y, breasts.getYOffset(),
              breasts::updateYOffset, value -> Text.translatable("wildfire_gender.wardrobe.slider.height", Math.round((Math.round(value * 100f) / 100f) * 10)), onSave));
        this.addDrawableChild(this.zOffsetBoobSlider = new WildfireSlider(this.width / 2 + 30, j + 15, 158, 20, Configuration.BREASTS_OFFSET_Z, breasts.getZOffset(),
              breasts::updateZOffset, value -> Text.translatable("wildfire_gender.wardrobe.slider.depth", Math.round((Math.round(value * 100f) / 100f) * 10)), onSave));

        this.addDrawableChild(this.cleavageSlider = new WildfireSlider(this.width / 2 + 30, j + 36, 158, 20, Configuration.BREASTS_CLEAVAGE, breasts.getCleavage(),
              breasts::updateCleavage, value -> Text.translatable("wildfire_gender.wardrobe.slider.rotation", Math.round((Math.round(value * 100f) / 100f) * 100)), onSave));

        this.addDrawableChild(this.btnDualPhysics =new WildfireButton(this.width / 2 + 30, j - 27, 78, 20,
                Text.translatable("wildfire_gender.breast_customization.dual_physics", Text.translatable(breasts.isUniboob() ? "wildfire_gender.label.no" : "wildfire_gender.label.yes")), button -> {
            boolean isUniboob = !breasts.isUniboob();
            if (breasts.updateUniboob(isUniboob)) {
                button.setMessage(Text.translatable("wildfire_gender.breast_customization.dual_physics", Text.translatable(isUniboob ? "wildfire_gender.label.no" : "wildfire_gender.label.yes")));
                PlayerConfig.saveGenderInfo(plr);
            }
        }));

        // Shape tab: independent silhouette controls and optional skin detail.
        this.addDrawableChild(this.btnShape = new WildfireButton(this.width / 2 + 30, j - 48, 158, 20,
                Text.translatable("wildfire_gender.editor.shape_value", breasts.getShape().displayName()), button -> {
            breasts.updateShape(breasts.getShape().next());
            button.setMessage(Text.translatable("wildfire_gender.editor.shape_value", breasts.getShape().displayName()));
            PlayerConfig.saveGenderInfo(plr);
        }));
        this.addDrawableChild(this.widthSlider = new WildfireSlider(this.width / 2 + 30, j - 27, 78, 20,
                Configuration.BREASTS_WIDTH, breasts.getWidth(), breasts::updateWidth,
                value -> Text.translatable("wildfire_gender.editor.width", Math.round(value * 100)), onSave));
        this.addDrawableChild(this.heightSlider = new WildfireSlider(this.width / 2 + 110, j - 27, 78, 20,
                Configuration.BREASTS_HEIGHT, breasts.getHeight(), breasts::updateHeight,
                value -> Text.translatable("wildfire_gender.editor.height", Math.round(value * 100)), onSave));
        this.addDrawableChild(this.projectionSlider = new WildfireSlider(this.width / 2 + 30, j - 6, 78, 20,
                Configuration.BREASTS_PROJECTION, breasts.getProjection(), breasts::updateProjection,
                value -> Text.translatable("wildfire_gender.editor.projection", Math.round(value * 100)), onSave));
        this.addDrawableChild(this.balanceSlider = new WildfireSlider(this.width / 2 + 110, j - 6, 78, 20,
                Configuration.BREASTS_BALANCE, breasts.getBalance(), breasts::updateBalance,
                value -> Text.translatable("wildfire_gender.editor.balance", Math.round(value * 100)), onSave));
        this.addDrawableChild(this.btnNipples = new WildfireButton(this.width / 2 + 30, j + 15, 78, 20,
                Text.translatable("wildfire_gender.editor.nipples", Text.translatable(breasts.hasNipples() ? "wildfire_gender.label.yes" : "wildfire_gender.label.no")), button -> {
            breasts.updateNipples(!breasts.hasNipples());
            button.setMessage(Text.translatable("wildfire_gender.editor.nipples", Text.translatable(breasts.hasNipples() ? "wildfire_gender.label.yes" : "wildfire_gender.label.no")));
            PlayerConfig.saveGenderInfo(plr);
        }));
        this.addDrawableChild(this.nippleSizeSlider = new WildfireSlider(this.width / 2 + 110, j + 15, 78, 20,
                Configuration.BREASTS_NIPPLE_SIZE, breasts.getNippleSize(), breasts::updateNippleSize,
                value -> Text.translatable("wildfire_gender.editor.detail_size", Math.round(value * 100)), onSave));

        // Motion tab: the legacy spring remains event-driven; wobble adds secondary soft-body deformation.
        this.addDrawableChild(this.btnPhysics = new WildfireButton(this.width / 2 + 30, j - 48, 158, 20,
                Text.translatable("wildfire_gender.editor.physics", Text.translatable(plr.hasBreastPhysics() ? "wildfire_gender.label.yes" : "wildfire_gender.label.no")), button -> {
            plr.updateBreastPhysics(!plr.hasBreastPhysics());
            button.setMessage(Text.translatable("wildfire_gender.editor.physics", Text.translatable(plr.hasBreastPhysics() ? "wildfire_gender.label.yes" : "wildfire_gender.label.no")));
            PlayerConfig.saveGenderInfo(plr);
        }));
        this.addDrawableChild(this.btnWobble = new WildfireButton(this.width / 2 + 110, j - 27, 78, 20,
                Text.translatable("wildfire_gender.editor.wobble", Text.translatable(plr.hasWobble() ? "wildfire_gender.label.yes" : "wildfire_gender.label.no")), button -> {
            plr.updateWobble(!plr.hasWobble());
            button.setMessage(Text.translatable("wildfire_gender.editor.wobble", Text.translatable(plr.hasWobble() ? "wildfire_gender.label.yes" : "wildfire_gender.label.no")));
            PlayerConfig.saveGenderInfo(plr);
        }));
        this.addDrawableChild(this.bounceSlider = new WildfireSlider(this.width / 2 + 30, j - 6, 78, 20,
                Configuration.BOUNCE_MULTIPLIER, plr.getBounceMultiplier(), plr::updateBounceMultiplier,
                value -> Text.translatable("wildfire_gender.editor.bounce", Math.round(value * 100)), onSave));
        this.addDrawableChild(this.floppySlider = new WildfireSlider(this.width / 2 + 110, j - 6, 78, 20,
                Configuration.FLOPPY_MULTIPLIER, plr.getFloppiness(), plr::updateFloppiness,
                value -> Text.translatable("wildfire_gender.editor.softness", Math.round(value * 100)), onSave));
        this.addDrawableChild(this.wobbleIntensitySlider = new WildfireSlider(this.width / 2 + 30, j + 15, 78, 20,
                Configuration.WOBBLE_INTENSITY, plr.getWobbleIntensity(), plr::updateWobbleIntensity,
                value -> Text.translatable("wildfire_gender.editor.wobble_amount", Math.round(value * 100)), onSave));
        this.addDrawableChild(this.wobbleSpeedSlider = new WildfireSlider(this.width / 2 + 110, j + 15, 78, 20,
                Configuration.WOBBLE_SPEED, plr.getWobbleSpeed(), plr::updateWobbleSpeed,
                value -> Text.translatable("wildfire_gender.editor.wobble_speed", Math.round(value * 100)), onSave));


        //Preset Tab Below
        PRESET_LIST = new WildfireBreastPresetList(this, 156, (j - 48), (j + 77));
        PRESET_LIST.setLeftPos(this.width / 2 + 30);

        this.addSelectableChild(this.PRESET_LIST);

        this.currentTab = 0;

        super.init();
    }

    private void createNewPreset(String presetName) {
        BreastPresetConfiguration cfg = new BreastPresetConfiguration(presetName);
        cfg.set(BreastPresetConfiguration.PRESET_NAME, presetName);
        cfg.set(BreastPresetConfiguration.BUST_SIZE, this.getPlayer().getBustSize());
        cfg.set(BreastPresetConfiguration.BREASTS_UNIBOOB, this.getPlayer().getBreasts().isUniboob());
        cfg.set(BreastPresetConfiguration.BREASTS_CLEAVAGE, this.getPlayer().getBreasts().getCleavage());
        cfg.set(BreastPresetConfiguration.BREASTS_OFFSET_X, this.getPlayer().getBreasts().getXOffset());
        cfg.set(BreastPresetConfiguration.BREASTS_OFFSET_Y, this.getPlayer().getBreasts().getYOffset());
        cfg.set(BreastPresetConfiguration.BREASTS_OFFSET_Z, this.getPlayer().getBreasts().getZOffset());
        cfg.set(BreastPresetConfiguration.BREASTS_WIDTH, this.getPlayer().getBreasts().getWidth());
        cfg.set(BreastPresetConfiguration.BREASTS_HEIGHT, this.getPlayer().getBreasts().getHeight());
        cfg.set(BreastPresetConfiguration.BREASTS_PROJECTION, this.getPlayer().getBreasts().getProjection());
        cfg.set(BreastPresetConfiguration.BREASTS_BALANCE, this.getPlayer().getBreasts().getBalance());
        cfg.set(BreastPresetConfiguration.BREASTS_SHAPE, this.getPlayer().getBreasts().getShape());
        cfg.set(BreastPresetConfiguration.BREASTS_NIPPLES, this.getPlayer().getBreasts().hasNipples());
        cfg.set(BreastPresetConfiguration.BREASTS_NIPPLE_SIZE, this.getPlayer().getBreasts().getNippleSize());
        cfg.set(BreastPresetConfiguration.BREAST_PHYSICS, this.getPlayer().hasBreastPhysics());
        cfg.set(BreastPresetConfiguration.BOUNCE_MULTIPLIER, this.getPlayer().getBounceMultiplier());
        cfg.set(BreastPresetConfiguration.FLOPPY_MULTIPLIER, this.getPlayer().getFloppiness());
        cfg.set(BreastPresetConfiguration.WOBBLE_ENABLED, this.getPlayer().hasWobble());
        cfg.set(BreastPresetConfiguration.WOBBLE_INTENSITY, this.getPlayer().getWobbleIntensity());
        cfg.set(BreastPresetConfiguration.WOBBLE_SPEED, this.getPlayer().getWobbleSpeed());
        cfg.save();

        PRESET_LIST.refreshList();
    }

    private void selectTab(int tab) {
        currentTab = tab;
        btnCustomization.active = tab != 0;
        btnShapeTab.active = tab != 1;
        btnMotionTab.active = tab != 2;
        btnPresets.active = tab != 3;
        btnAddPreset.visible = tab == 3;
        btnDeletePreset.visible = tab == 3;
    }

    private void updatePresetTab() {
        boolean canHaveBreasts = getPlayer().getGender().canHaveBreasts();
        breastSlider.visible = canHaveBreasts && currentTab == 0;
        xOffsetBoobSlider.visible = canHaveBreasts && currentTab == 0;
        yOffsetBoobSlider.visible = canHaveBreasts && currentTab == 0;
        zOffsetBoobSlider.visible = canHaveBreasts && currentTab == 0;
        cleavageSlider.visible = canHaveBreasts && currentTab == 0;
        btnDualPhysics.visible = canHaveBreasts && currentTab == 2;

        btnShape.visible = canHaveBreasts && currentTab == 1;
        widthSlider.visible = canHaveBreasts && currentTab == 1;
        heightSlider.visible = canHaveBreasts && currentTab == 1;
        projectionSlider.visible = canHaveBreasts && currentTab == 1;
        balanceSlider.visible = canHaveBreasts && currentTab == 1;
        btnNipples.visible = canHaveBreasts && currentTab == 1;
        nippleSizeSlider.visible = canHaveBreasts && currentTab == 1;
        nippleSizeSlider.active = getPlayer().getBreasts().hasNipples();

        btnPhysics.visible = canHaveBreasts && currentTab == 2;
        btnWobble.visible = canHaveBreasts && currentTab == 2;
        bounceSlider.visible = canHaveBreasts && currentTab == 2;
        floppySlider.visible = canHaveBreasts && currentTab == 2;
        wobbleIntensitySlider.visible = canHaveBreasts && currentTab == 2;
        wobbleSpeedSlider.visible = canHaveBreasts && currentTab == 2;
        btnDualPhysics.active = getPlayer().hasBreastPhysics();
        btnWobble.active = getPlayer().hasBreastPhysics();
        bounceSlider.active = getPlayer().hasBreastPhysics();
        floppySlider.active = getPlayer().hasBreastPhysics();
        wobbleIntensitySlider.active = getPlayer().hasBreastPhysics() && getPlayer().hasWobble();
        wobbleSpeedSlider.active = getPlayer().hasBreastPhysics() && getPlayer().hasWobble();

        PRESET_LIST.visible = currentTab == 3;
    }

    @Override
    public void renderBackground(DrawContext ctx) {
        super.renderBackground(ctx);
        int x = this.width / 2;
        int y = this.height / 2;
        ctx.fill(x + 28, y - 64 - 21, x + 190, y + 68, 0x55000000);
        ctx.fill(x + 29, y - 63 - 21, x + 189, y - 60, 0x55000000);
        ctx.drawText(textRenderer, getTitle(), x + 32, y - 60 - 21, 0xFFFFFF, false);
        if(currentTab == 3) {
            ctx.fill(PRESET_LIST.getLeft(), PRESET_LIST.getTop(), PRESET_LIST.getRight(), PRESET_LIST.getBottom(), 0x55000000);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if(client == null || client.player == null || client.world == null) return;

        updatePresetTab();
        super.render(ctx, mouseX, mouseY, delta);
        RenderSystem.setShaderColor(1f, 1.0F, 1.0F, 1.0F);

        int xP = this.width / 2 - 102;
        int yP = this.height / 2 + 275;
        PlayerEntity ent = client.world.getPlayerByUuid(this.playerUUID);
        if(ent != null) WardrobeBrowserScreen.drawEntityOnScreen(xP, yP, 200, -20, -20, ent);

        int x = this.width / 2;
        int y = this.height / 2;
        if(currentTab == 3) {
            PRESET_LIST.render(ctx, mouseX, mouseY, delta);
            if(PRESET_LIST.getPresetList().length == 0) {
                ctx.drawText(textRenderer, "No Presets Found", x + ((190 + 28) / 2) - textRenderer.getWidth("No Presets Found") / 2, y - 4, 0xFFFFFF, false);
            }
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int state) {
        //Ensure all sliders are saved
        breastSlider.save();
        xOffsetBoobSlider.save();
        yOffsetBoobSlider.save();
        zOffsetBoobSlider.save();
        cleavageSlider.save();
        widthSlider.save();
        heightSlider.save();
        projectionSlider.save();
        balanceSlider.save();
        nippleSizeSlider.save();
        bounceSlider.save();
        floppySlider.save();
        wobbleIntensitySlider.save();
        wobbleSpeedSlider.save();
        return super.mouseReleased(mouseX, mouseY, state);
    }
}
