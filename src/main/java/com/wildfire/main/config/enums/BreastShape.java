/*
 * Wildfire's Female Gender Mod is a female gender mod created for Minecraft.
 * Copyright (C) 2023-present WildfireRomeo
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 */

package com.wildfire.main.config.enums;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;

import java.util.Optional;
import java.util.function.IntFunction;

/**
 * Stable identifiers for the built-in breast geometry profiles.
 *
 * <p>Do not reorder existing entries: textual IDs are persisted on disk, while packet ordinals are append-only.</p>
 */
public enum BreastShape {
    CLASSIC("classic"),
    ROUND("round"),
    NATURAL("natural"),
    TEARDROP("teardrop"),
    BELL("bell"),
    ANIME("anime");

    public static final IntFunction<BreastShape> BY_ID =
            ByIdMap.continuous(BreastShape::ordinal, values(), ByIdMap.OutOfBoundsStrategy.CLAMP);
    public static final StreamCodec<ByteBuf, BreastShape> STREAM_CODEC =
            ByteBufCodecs.idMapper(BY_ID, BreastShape::ordinal);
    public static final Codec<BreastShape> CODEC = Codec.STRING.xmap(BreastShape::byName, BreastShape::id);

    private final String id;
    private final String translationKey;

    BreastShape(String id) {
        this.id = id;
        this.translationKey = "wildfire_gender.shape." + id;
    }

    public String id() {
        return id;
    }

    public static BreastShape byName(String id) {
        return fromName(id).orElse(CLASSIC);
    }

    /**
     * Resolves a persisted textual identifier without applying a compatibility fallback.
     *
     * <p>{@link #byName(String)} intentionally remains forgiving for ordinary configuration and
     * network compatibility. Importers that must distinguish corrupt input from {@code classic}
     * can use this method instead.</p>
     */
    public static Optional<BreastShape> fromName(String id) {
        if(id == null) {
            return Optional.empty();
        }
        for(BreastShape shape : values()) {
            if(shape.id.equalsIgnoreCase(id)) {
                return Optional.of(shape);
            }
        }
        return Optional.empty();
    }

    public Component displayName() {
        return Component.translatable(translationKey);
    }

    public BreastShape next() {
        return switch(this) {
            case CLASSIC -> ANIME;
            case ANIME -> ROUND;
            case ROUND -> NATURAL;
            case NATURAL -> TEARDROP;
            case TEARDROP -> BELL;
            case BELL -> CLASSIC;
        };
    }
}
