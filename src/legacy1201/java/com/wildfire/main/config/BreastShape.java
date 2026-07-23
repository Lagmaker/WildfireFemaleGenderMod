package com.wildfire.main.config;

import net.minecraft.text.Text;

/** Stable textual identifiers used by configs, presets, and multiplayer sync. */
public enum BreastShape {
    CLASSIC("classic"),
    ANIME("anime"),
    ROUND("round"),
    NATURAL("natural"),
    TEARDROP("teardrop"),
    BELL("bell");

    private final String id;

    BreastShape(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public Text displayName() {
        return Text.translatable("wildfire_gender.shape." + id);
    }

    public BreastShape next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static BreastShape byName(String id) {
        if(id != null) {
            for(BreastShape shape : values()) {
                if(shape.id.equalsIgnoreCase(id)) return shape;
            }
        }
        return CLASSIC;
    }
}
