package com.breakinblocks.retrofactorymanager.data;

import com.breakinblocks.retrofactorymanager.RetroFactoryManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class GraphSerde {
    public static final String MARKER = "-- rfm:graph:v1 ";

    private GraphSerde() {
    }

    public static String embed(BlueprintGraph graph) {
        JsonElement json = BlueprintGraph.CODEC.encodeStart(JsonOps.INSTANCE, graph)
                .result()
                .orElseThrow(() -> new IllegalStateException("Failed to encode blueprint graph"));
        byte[] compressed = gzip(json.toString().getBytes(StandardCharsets.UTF_8));
        return MARKER + Base64.getEncoder().encodeToString(compressed);
    }

    public static Optional<BlueprintGraph> extract(String program) {
        for (String line : program.split("\n", -1)) {
            String trimmed = line.trim();
            if (!trimmed.startsWith(MARKER.trim()) || trimmed.length() <= MARKER.length()) {
                continue;
            }
            String payload = trimmed.substring(MARKER.length()).trim();
            try {
                byte[] jsonBytes = gunzip(Base64.getDecoder().decode(payload));
                JsonElement json = JsonParser.parseString(new String(jsonBytes, StandardCharsets.UTF_8));
                Optional<BlueprintGraph> graph = BlueprintGraph.CODEC.parse(JsonOps.INSTANCE, json).result();
                if (graph.isPresent()) {
                    return graph;
                }
            } catch (Exception e) {
                RetroFactoryManager.LOGGER.warn("Failed to decode embedded blueprint graph", e);
            }
        }
        return Optional.empty();
    }

    public static String strip(String program) {
        return program.lines()
                .filter(line -> !line.trim().startsWith(MARKER.trim()))
                .reduce((a, b) -> a + "\n" + b)
                .orElse("")
                .strip();
    }

    private static byte[] gzip(byte[] data) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (GZIPOutputStream gz = new GZIPOutputStream(out)) {
                gz.write(data);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to compress blueprint graph", e);
        }
    }

    private static byte[] gunzip(byte[] data) throws IOException {
        try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(data))) {
            return gz.readAllBytes();
        }
    }
}
