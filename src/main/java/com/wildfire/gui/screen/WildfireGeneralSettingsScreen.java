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

import com.wildfire.events.EntityHurtSoundEvent;
import com.wildfire.main.WildfireGender;
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
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Objects;
import java.util.UUID;

/** Character-wide presentation controls that do not belong in the appearance editor. */
@Environment(EnvType.CLIENT)
public class WildfireGeneralSettingsScreen extends BaseWildfireScreen {
    private static final Identifier BACKGROUND_MALE = Identifier.fromNamespaceAndPath(
            WildfireGender.MODID, "textures/gui/wardrobe_bg_male.png");
    private static final Identifier BACKGROUND_FEMALE = Identifier.fromNamespaceAndPath(
            WildfireGender.MODID, "textures/gui/wardrobe_bg_female.png");
    private static final Identifier BACKGROUND_OTHER = Identifier.fromNamespaceAndPath(
            WildfireGender.MODID, "textures/gui/wardrobe_bg_other.png");

    private static final Component ENABLED = Component.translatable("wildfire_gender.label.enabled")
            .withStyle(ChatFormatting.GREEN);
    private static final Component DISABLED = Component.translatable("wildfire_gender.label.disabled")
            .withStyle(ChatFormatting.RED);

    private static final int CONTROL_WIDTH = 157;

    public WildfireGeneralSettingsScreen(Screen parent, UUID uuid) {
        super(Component.translatable("wildfire_gender.general_settings.title"), parent, uuid);
    }

    @Override
    public void init() {
        final var client = Objects.requireNonNull(this.minecraft, "client");
        final var player = Objects.requireNonNull(getPlayer(), "getPlayer()");
        final int x = this.width / 2 - 36;
        final int y = this.height / 2;
        final var ref = new Object() {
            @UnknownNullability
            AbstractWidget pitchSlider;
        };

        addButton(builder -> builder
                .message(() -> Component.translatable("wildfire_gender.char_settings.hurt_sounds",
                        player.hasHurtSounds() ? ENABLED : DISABLED))
                .position(x, y - 49)
                .size(CONTROL_WIDTH, 20)
                .onPress(button -> {
                    player.updateHurtSounds(!player.hasHurtSounds());
                    player.save();
                    ref.pitchSlider.active = player.hasHurtSounds();
                    button.updateMessage();
                })
                .tooltip(Tooltip.create(Component.translatable("wildfire_gender.tooltip.hurt_sounds"))));

        ref.pitchSlider = addSlider(builder -> builder
                .message(value -> Component.translatable("wildfire_gender.slider.voice_pitch", Math.round(value * 100)))
                .position(x, y - 26)
                .size(CONTROL_WIDTH, 20)
                .range(Configuration.VOICE_PITCH)
                .current(player.getVoicePitch())
                .update(player::updateVoicePitch)
                .save(_ -> {
                    player.save();
                    var clientPlayer = client.player;
                    if(clientPlayer != null && player.hasHurtSounds()) {
                        EntityHurtSoundEvent.EVENT.invoker().onHurt(
                                clientPlayer, clientPlayer.damageSources().generic());
                    }
                })
                .step(0.01)
                .mouseStep(0.005)
                .active(player.hasHurtSounds())
                .tooltip(Tooltip.create(Component.translatable("wildfire_gender.tooltip.voice_pitch"))));

        addButton(builder -> builder
                .message(() -> Component.translatable("wildfire_gender.misc.holiday_themes",
                        player.hasHolidayThemes() ? ENABLED : DISABLED))
                .position(x, y + 13)
                .size(CONTROL_WIDTH, 20)
                .onPress(button -> {
                    player.updateHolidayThemes(!player.hasHolidayThemes());
                    player.save();
                    button.updateMessage();
                })
                .tooltip(Tooltip.create(Component.translatable("wildfire_gender.tooltip.holiday_themes.line1"))));

        addButton(builder -> builder
                .message(() -> Component.translatable("wildfire_gender.general_settings.reset"))
                .position(x, y + 36)
                .size(CONTROL_WIDTH, 14)
                .onPress(_ -> {
                    player.updateHurtSounds(Configuration.HURT_SOUNDS.getDefault());
                    player.updateVoicePitch(Configuration.VOICE_PITCH.getDefault());
                    player.updateHolidayThemes(Configuration.HOLIDAY_THEMES.getDefault());
                    player.save();
                    rebuildWidgets();
                })
                .tooltip(Tooltip.create(Component.translatable("wildfire_gender.general_settings.reset.tooltip"))));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        extractTransparentBackground(graphics);

        PlayerConfig player = getPlayer();
        if(player == null) return;
        Identifier backgroundTexture = switch(player.getGender()) {
            case Gender.MALE -> BACKGROUND_MALE;
            case Gender.FEMALE -> BACKGROUND_FEMALE;
            case Gender.OTHER -> BACKGROUND_OTHER;
        };

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, backgroundTexture,
                centerX - 136, centerY - 69, 0, 0, 268, 124, 512, 512);

        // A soft inset keeps the two setting groups readable over every gender-specific background.
        graphics.fill(centerX - 41, centerY - 63, centerX + 126, centerY + 53, 0x66070B16);
        graphics.fill(centerX - 41, centerY - 63, centerX - 39, centerY + 53, 0xCC8D5FC7);
        renderPlayerInFrame(graphics, centerX - 90, centerY + 18, mouseX, mouseY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        int x = this.width / 2;
        int y = this.height / 2;

        graphics.text(font, getTitle(), x - font.width(getTitle()) / 2, y - 82, 0xFFFFFF, false);
        graphics.text(font, Component.translatable("wildfire_gender.general_settings.voice_section"),
                x - 32, y - 60, 0xFFCEB7E8, false);
        graphics.text(font, Component.translatable("wildfire_gender.general_settings.seasonal_section"),
                x - 32, y + 2, 0xFFCEB7E8, false);
    }
}
