/*
 * Wildfire's Female Gender Mod is a female gender mod created for Minecraft.
 * Copyright (C) 2023-present WildfireRomeo
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 */

package com.wildfire.main.config.types;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.function.Function;

/**
 * Persists enums by a stable textual identifier rather than an order-dependent ordinal.
 */
public final class NamedEnumConfigKey<TYPE extends Enum<TYPE>> extends ConfigKey<TYPE> {
    private final Function<String, TYPE> reader;
    private final Function<TYPE, String> writer;

    public NamedEnumConfigKey(String key, TYPE defaultValue, Function<String, TYPE> reader,
                              Function<TYPE, String> writer) {
        super(key, defaultValue);
        this.reader = reader;
        this.writer = writer;
    }

    @Override
    protected TYPE read(JsonElement element) {
        if(element instanceof JsonPrimitive primitive && primitive.isString()) {
            return reader.apply(primitive.getAsString());
        }
        return defaultValue;
    }

    @Override
    public void save(JsonObject object, TYPE value) {
        object.addProperty(key, writer.apply(value));
    }
}
