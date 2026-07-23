package com.wildfire.main.config;

import com.google.gson.JsonPrimitive;

public final class IntegerConfigKey extends NumberConfigKey<Integer> {
    public IntegerConfigKey(String key, int defaultValue, int minInclusive, int maxInclusive) {
        super(key, defaultValue, minInclusive, maxInclusive);
    }

    @Override
    protected Integer fromPrimitive(JsonPrimitive primitive) {
        return primitive.getAsInt();
    }
}
