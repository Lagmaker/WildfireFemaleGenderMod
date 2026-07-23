package com.wildfire.main.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public final class BreastShapeConfigKey extends ConfigKey<BreastShape> {
    public BreastShapeConfigKey(String key, BreastShape defaultValue) {
        super(key, defaultValue);
    }

    @Override
    protected BreastShape read(JsonElement element) {
        if(element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if(primitive.isString()) return BreastShape.byName(primitive.getAsString());
        }
        return defaultValue;
    }

    @Override
    public void save(JsonObject object, BreastShape value) {
        object.addProperty(key, value.id());
    }
}
