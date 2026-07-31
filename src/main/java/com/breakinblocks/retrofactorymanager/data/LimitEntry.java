package com.breakinblocks.retrofactorymanager.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;

/// One `resourceLimit` clause: an optional quantity, an optional retention, and optional resource ids.
public record LimitEntry(
        int quantity,
        int retain,
        List<String> resources,
        TagMode tagMode,
        List<String> tags,
        boolean tagsOr
) {
    public static final LimitEntry EMPTY = new LimitEntry(-1, -1, List.of(), TagMode.NONE, List.of(), false);

    public static final Codec<LimitEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("quantity", -1).forGetter(LimitEntry::quantity),
            Codec.INT.optionalFieldOf("retain", -1).forGetter(LimitEntry::retain),
            Codec.STRING.listOf().optionalFieldOf("resources", List.of()).forGetter(LimitEntry::resources),
            TagMode.CODEC.optionalFieldOf("tag_mode", TagMode.NONE).forGetter(LimitEntry::tagMode),
            Codec.STRING.listOf().optionalFieldOf("tags", List.of()).forGetter(LimitEntry::tags),
            Codec.BOOL.optionalFieldOf("tags_or", false).forGetter(LimitEntry::tagsOr)
    ).apply(instance, LimitEntry::new));

    public LimitEntry(int quantity, int retain, List<String> resources) {
        this(quantity, retain, resources, TagMode.NONE, List.of(), false);
    }

    public boolean isEmpty() {
        return quantity < 0 && retain < 0 && resources.isEmpty() && !hasTagFilter();
    }

    public boolean hasTagFilter() {
        return tagMode != TagMode.NONE && !tags.isEmpty();
    }

    public LimitEntry withQuantity(int quantity) {
        return new LimitEntry(quantity, retain, resources, tagMode, tags, tagsOr);
    }

    public LimitEntry withRetain(int retain) {
        return new LimitEntry(quantity, retain, resources, tagMode, tags, tagsOr);
    }

    public LimitEntry withResources(List<String> resources) {
        return new LimitEntry(quantity, retain, resources, tagMode, tags, tagsOr);
    }

    public LimitEntry withTagMode(TagMode tagMode) {
        return new LimitEntry(quantity, retain, resources, tagMode, tags, tagsOr);
    }

    public LimitEntry withTags(List<String> tags) {
        return new LimitEntry(quantity, retain, resources, tagMode, tags, tagsOr);
    }

    public LimitEntry withTagsOr(boolean tagsOr) {
        return new LimitEntry(quantity, retain, resources, tagMode, tags, tagsOr);
    }

    public LimitEntry withTagAdded(String tag) {
        if (tags.contains(tag)) {
            return this;
        }
        List<String> updated = new ArrayList<>(tags);
        updated.add(tag);
        return withTags(updated).withTagMode(tagMode == TagMode.NONE ? TagMode.WITH : tagMode);
    }

    public LimitEntry withTagRemoved(int index) {
        if (index < 0 || index >= tags.size()) {
            return this;
        }
        List<String> updated = new ArrayList<>(tags);
        updated.remove(index);
        return withTags(updated);
    }

    public LimitEntry withResourceAdded(String resource) {
        if (resources.contains(resource)) {
            return this;
        }
        List<String> updated = new ArrayList<>(resources);
        updated.add(resource);
        return withResources(updated);
    }

    public String tagsSfml() {
        if (!hasTagFilter()) {
            return "";
        }
        String joined = String.join(tagsOr ? " OR " : " AND ", tags.stream().map(tag -> "#" + tag).toList());
        return tagMode.name() + " (" + joined + ")";
    }

    public LimitEntry withResourceRemoved(int index) {
        if (index < 0 || index >= resources.size()) {
            return this;
        }
        List<String> updated = new ArrayList<>(resources);
        updated.remove(index);
        return withResources(updated);
    }

    /// Renders this clause as SFML, or an empty string when nothing is configured.
    public String toSfml() {
        StringBuilder sb = new StringBuilder();
        if (quantity >= 0) {
            sb.append(quantity);
        }
        if (retain >= 0) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append("RETAIN ").append(retain);
        }
        if (!resources.isEmpty()) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append(String.join(" OR ", resources));
        }
        String tagClause = tagsSfml();
        if (!tagClause.isEmpty()) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append(tagClause);
        }
        return sb.toString();
    }

    public String summary() {
        String sfml = toSfml();
        return sfml.isEmpty() ? "" : sfml;
    }
}
