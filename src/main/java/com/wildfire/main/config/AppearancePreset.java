/*
 * Wildfire's Female Gender Mod is a female gender mod created for Minecraft.
 * Copyright (C) 2023-present WildfireRomeo
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 */

package com.wildfire.main.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.wildfire.main.WildfireGender;
import com.wildfire.main.config.types.ConfigKey;
import com.wildfire.main.entitydata.PlayerConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Portable, versioned appearance presets stored in {@code config/FemaleGenderMod/presets}.
 */
public final class AppearancePreset {
    public static final int SCHEMA_VERSION = 1;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<ConfigKey<?>> APPEARANCE_KEYS = List.of(
            Configuration.BUST_SIZE,
            Configuration.BREASTS_OFFSET_X,
            Configuration.BREASTS_OFFSET_Y,
            Configuration.BREASTS_OFFSET_Z,
            Configuration.BREASTS_UNIBOOB,
            Configuration.BREASTS_CLEAVAGE,
            Configuration.BREASTS_WIDTH,
            Configuration.BREASTS_HEIGHT,
            Configuration.BREASTS_PROJECTION,
            Configuration.BREASTS_BALANCE,
            Configuration.LEFT_BREAST_UV_LAYOUT,
            Configuration.RIGHT_BREAST_UV_LAYOUT,
            Configuration.LEFT_BREAST_OVERLAY_UV_LAYOUT,
            Configuration.RIGHT_BREAST_OVERLAY_UV_LAYOUT
    );

    private AppearancePreset() {
    }

    public static Info save(String requestedName, PlayerConfig player) throws IOException {
        String name = requestedName.trim();
        if(name.isEmpty()) {
            throw new IllegalArgumentException("Preset name cannot be empty");
        }

        Path directory = directory();
        Files.createDirectories(directory);
        Path target = directory.resolve(fileName(name));
        Path temporary = directory.resolve(target.getFileName() + ".tmp");

        player.writeToConfig();
        JsonObject appearance = new JsonObject();
        for(ConfigKey<?> key : APPEARANCE_KEYS) {
            saveKey(appearance, key, player.getConfig());
        }

        JsonObject root = new JsonObject();
        root.addProperty("schema_version", SCHEMA_VERSION);
        root.addProperty("source_mod_version", versionOf(WildfireGender.MODID));
        root.addProperty("source_minecraft_version", versionOf("minecraft"));
        root.addProperty("name", name);
        root.addProperty("created_at", Instant.now().toString());
        root.add("appearance", appearance);

        try(Writer writer = Files.newBufferedWriter(temporary)) {
            GSON.toJson(root, writer);
        }
        try {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch(AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return readInfo(target);
    }

    public static Compatibility apply(Info info, PlayerConfig player) throws IOException {
        JsonObject root = readRoot(info.path);
        int schema = requiredInt(root, "schema_version");
        if(schema != SCHEMA_VERSION) {
            throw new IncompatiblePresetException(schema);
        }

        JsonObject appearance = root.getAsJsonObject("appearance");
        if(appearance == null) {
            throw new JsonParseException("Missing appearance object");
        }

        Configuration config = player.getConfig();
        for(ConfigKey<?> key : APPEARANCE_KEYS) {
            if(appearance.has(key.getKey())) {
                loadKey(appearance, key, config);
            }
        }
        player.loadFromConfig(false);
        player.save();
        return compatibility(root);
    }

    public static void delete(Info info) throws IOException {
        Files.deleteIfExists(info.path);
    }

    public static List<Info> list() {
        Path directory = directory();
        if(!Files.isDirectory(directory)) {
            return List.of();
        }
        try(var paths = Files.list(directory)) {
            return paths
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .map(AppearancePreset::readInfoQuietly)
                    .sorted(Comparator.comparing(Info::name, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        } catch(IOException e) {
            WildfireGender.LOGGER.error("Failed to list appearance presets", e);
            return List.of();
        }
    }

    public static Path directory() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve(Configuration.CONFIG_DIR)
                .resolve("presets");
    }

    private static Info readInfoQuietly(Path path) {
        try {
            return readInfo(path);
        } catch(Exception e) {
            WildfireGender.LOGGER.warn("Unable to read appearance preset {}", path, e);
            String fallback = path.getFileName().toString().replaceFirst("[.]json$", "");
            return new Info(path, fallback, "unknown", "unknown", -1, Compatibility.INVALID);
        }
    }

    private static Info readInfo(Path path) throws IOException {
        JsonObject root = readRoot(path);
        int schema = requiredInt(root, "schema_version");
        String name = requiredString(root, "name");
        String modVersion = optionalString(root, "source_mod_version", "unknown");
        String minecraftVersion = optionalString(root, "source_minecraft_version", "unknown");
        return new Info(path, name, modVersion, minecraftVersion, schema, compatibility(root));
    }

    private static JsonObject readRoot(Path path) throws IOException {
        try(Reader reader = Files.newBufferedReader(path)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if(root == null) {
                throw new JsonParseException("Preset is empty");
            }
            return root;
        }
    }

    private static Compatibility compatibility(JsonObject root) {
        int schema;
        try {
            schema = requiredInt(root, "schema_version");
        } catch(RuntimeException e) {
            return Compatibility.INVALID;
        }
        if(schema != SCHEMA_VERSION) {
            return Compatibility.INCOMPATIBLE_SCHEMA;
        }

        String sourceMod = optionalString(root, "source_mod_version", "unknown");
        String sourceMinecraft = optionalString(root, "source_minecraft_version", "unknown");
        if(sourceMod.equals(versionOf(WildfireGender.MODID)) && sourceMinecraft.equals(versionOf("minecraft"))) {
            return Compatibility.EXACT;
        }
        return Compatibility.COMPATIBLE_DIFFERENT_VERSION;
    }

    private static String fileName(String name) {
        String slug = name.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}._-]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if(slug.isBlank()) {
            slug = "preset";
        }
        if(slug.length() > 64) {
            slug = slug.substring(0, 64);
        }
        return slug + ".json";
    }

    private static String versionOf(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    private static int requiredInt(JsonObject root, String key) {
        if(!root.has(key) || !root.get(key).isJsonPrimitive()) {
            throw new JsonParseException("Missing " + key);
        }
        return root.get(key).getAsInt();
    }

    private static String requiredString(JsonObject root, String key) {
        if(!root.has(key) || !root.get(key).isJsonPrimitive()) {
            throw new JsonParseException("Missing " + key);
        }
        return root.get(key).getAsString();
    }

    private static String optionalString(JsonObject root, String key, String fallback) {
        return root.has(key) && root.get(key).isJsonPrimitive() ? root.get(key).getAsString() : fallback;
    }

    private static <T> void saveKey(JsonObject output, ConfigKey<T> key, Configuration config) {
        key.save(output, config.get(key));
    }

    private static <T> void loadKey(JsonObject input, ConfigKey<T> key, Configuration config) {
        config.set(key, key.read(input));
    }

    public enum Compatibility {
        EXACT,
        COMPATIBLE_DIFFERENT_VERSION,
        INCOMPATIBLE_SCHEMA,
        INVALID
    }

    public record Info(Path path, String name, String modVersion, String minecraftVersion, int schemaVersion,
                       Compatibility compatibility) {
    }

    public static final class IncompatiblePresetException extends IOException {
        public IncompatiblePresetException(int schema) {
            super("Unsupported preset schema " + schema + " (expected " + SCHEMA_VERSION + ")");
        }
    }
}
