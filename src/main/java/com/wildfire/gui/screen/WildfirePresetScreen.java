/*
 * Wildfire's Female Gender Mod is a female gender mod created for Minecraft.
 * Copyright (C) 2023-present WildfireRomeo
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 */

package com.wildfire.gui.screen;

import com.wildfire.gui.GuiUtils;
import com.wildfire.main.WildfireGender;
import com.wildfire.main.config.AppearancePreset;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class WildfirePresetScreen extends BaseWildfireScreen {
    private List<AppearancePreset.Info> presets = List.of();
    private int selectedIndex;
    private String editName = "";
    private Component status = Component.empty();
    private boolean deleteArmed;

    public WildfirePresetScreen(Screen parent, UUID uuid) {
        super(Component.translatable("wildfire_gender.presets.title"), parent, uuid);
    }

    @Override
    public void init() {
        refreshPresets();
        final int x = width / 2;
        final int y = height / 2;
        final var selected = selected();

        EditBox name = new EditBox(font, x - 36, y - 51, 166, 20,
                Component.translatable("wildfire_gender.presets.name"));
        name.setMaxLength(64);
        name.setHint(Component.translatable("wildfire_gender.presets.name"));
        if(editName.isBlank() && selected != null) {
            editName = selected.name();
        }
        name.setValue(editName);
        name.setResponder(value -> editName = value);
        addRenderableWidget(name);

        addButton(builder -> builder
                .message(() -> Component.translatable("wildfire_gender.presets.save"))
                .position(x - 36, y - 27)
                .size(81, 20)
                .onPress(ignored -> savePreset()));

        addButton(builder -> builder
                .message(() -> Component.translatable("wildfire_gender.presets.apply"))
                .position(x + 49, y - 27)
                .size(81, 20)
                .onPress(ignored -> applyPreset())
                .active(selected != null && selected.compatibility() != AppearancePreset.Compatibility.INVALID
                        && selected.compatibility() != AppearancePreset.Compatibility.INCOMPATIBLE_SCHEMA)
                .tooltip(selected == null ? null : Tooltip.create(versionDescription(selected))));

        addButton(builder -> builder
                .message(() -> Component.literal("◀"))
                .position(x - 36, y - 3)
                .size(28, 20)
                .onPress(ignored -> selectRelative(-1))
                .active(presets.size() > 1));

        addButton(builder -> builder
                .message(() -> selected == null
                        ? Component.translatable("wildfire_gender.presets.none")
                        : Component.literal(selected.name()))
                .position(x - 4, y - 3)
                .size(102, 20)
                .onPress(ignored -> {
                })
                .active(false)
                .tooltip(selected == null ? null : Tooltip.create(versionDescription(selected))));

        addButton(builder -> builder
                .message(() -> Component.literal("▶"))
                .position(x + 102, y - 3)
                .size(28, 20)
                .onPress(ignored -> selectRelative(1))
                .active(presets.size() > 1));

        addButton(builder -> builder
                .message(() -> Component.translatable(deleteArmed
                        ? "wildfire_gender.presets.delete_confirm"
                        : "wildfire_gender.breast_customization.presets.delete"))
                .position(x - 36, y + 42)
                .size(81, 20)
                .onPress(button -> {
                    if(!deleteArmed) {
                        deleteArmed = true;
                        status = Component.translatable("wildfire_gender.presets.delete_warning")
                                .withStyle(ChatFormatting.YELLOW);
                        button.updateMessage();
                    } else {
                        deletePreset();
                    }
                })
                .active(selected != null));

        addButton(builder -> builder
                .message(() -> Component.translatable("gui.done"))
                .position(x + 49, y + 42)
                .size(81, 20)
                .onPress(ignored -> onClose()));
    }

    private void savePreset() {
        var player = Objects.requireNonNull(getPlayer(), "getPlayer()");
        try {
            var saved = AppearancePreset.save(editName, player);
            status = Component.translatable("wildfire_gender.presets.saved", saved.name())
                    .withStyle(ChatFormatting.GREEN);
            refreshPresets();
            selectedIndex = Math.max(0, indexOf(saved));
            editName = saved.name();
            deleteArmed = false;
            rebuildWidgets();
        } catch(IllegalArgumentException e) {
            status = Component.translatable("wildfire_gender.presets.name_required")
                    .withStyle(ChatFormatting.RED);
        } catch(IOException e) {
            WildfireGender.LOGGER.error("Failed to save appearance preset", e);
            status = Component.translatable("wildfire_gender.presets.save_failed")
                    .withStyle(ChatFormatting.RED);
        }
    }

    private void applyPreset() {
        var selected = selected();
        if(selected == null) return;
        try {
            var compatibility = AppearancePreset.apply(selected, Objects.requireNonNull(getPlayer(), "getPlayer()"));
            String statusKey = switch(compatibility) {
                case EXACT -> "wildfire_gender.presets.applied";
                case MIGRATABLE -> "wildfire_gender.presets.applied_migrated";
                default -> "wildfire_gender.presets.applied_version_warning";
            };
            status = Component.translatable(statusKey, selected.name())
                    .withStyle(compatibility == AppearancePreset.Compatibility.EXACT
                            ? ChatFormatting.GREEN : ChatFormatting.YELLOW);
        } catch(AppearancePreset.IncompatiblePresetException e) {
            status = Component.translatable("wildfire_gender.presets.incompatible")
                    .withStyle(ChatFormatting.RED);
        } catch(Exception e) {
            WildfireGender.LOGGER.error("Failed to apply appearance preset {}", selected.path(), e);
            status = Component.translatable("wildfire_gender.presets.load_failed")
                    .withStyle(ChatFormatting.RED);
        }
    }

    private void deletePreset() {
        var selected = selected();
        if(selected == null) return;
        try {
            AppearancePreset.delete(selected);
            status = Component.translatable("wildfire_gender.presets.deleted", selected.name())
                    .withStyle(ChatFormatting.GREEN);
            editName = "";
            deleteArmed = false;
            refreshPresets();
            rebuildWidgets();
        } catch(IOException e) {
            WildfireGender.LOGGER.error("Failed to delete appearance preset {}", selected.path(), e);
            status = Component.translatable("wildfire_gender.presets.delete_failed")
                    .withStyle(ChatFormatting.RED);
        }
    }

    private void selectRelative(int delta) {
        if(presets.isEmpty()) return;
        selectedIndex = Math.floorMod(selectedIndex + delta, presets.size());
        editName = presets.get(selectedIndex).name();
        deleteArmed = false;
        status = Component.empty();
        rebuildWidgets();
    }

    private void refreshPresets() {
        presets = AppearancePreset.list();
        if(presets.isEmpty()) {
            selectedIndex = 0;
        } else {
            selectedIndex = Math.floorMod(selectedIndex, presets.size());
        }
    }

    private AppearancePreset.Info selected() {
        return presets.isEmpty() ? null : presets.get(selectedIndex);
    }

    private int indexOf(AppearancePreset.Info target) {
        for(int i = 0; i < presets.size(); i++) {
            if(presets.get(i).path().equals(target.path())) {
                return i;
            }
        }
        return -1;
    }

    private static Component versionDescription(AppearancePreset.Info info) {
        return Component.translatable("wildfire_gender.presets.version_details",
                info.schemaVersion(), info.modVersion(), info.minecraftVersion());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        extractTransparentBackground(graphics);
        int x = width / 2;
        int y = height / 2;
        graphics.fill(x - 44, y - 72, x + 138, y + 69, 0xCC111111);
        graphics.fill(x - 43, y - 71, x + 137, y + 68, 0xCC242424);
        GuiUtils.drawCenteredText(graphics, font, getTitle(), x + 47, y - 66, 0xFFFFFF);
        GuiUtils.drawCenteredText(graphics, font, status, x + 47, y + 23, 0xFFFFFF);
        renderPlayerInFrame(graphics, x - 90, y + 42, mouseX, mouseY);
    }
}
