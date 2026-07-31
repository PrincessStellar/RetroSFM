package com.breakinblocks.retrofactorymanager.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class BlueprintGraph {
    public static final Codec<BlueprintGraph> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("name", "").forGetter(BlueprintGraph::name),
            BlueprintNode.CODEC.listOf().optionalFieldOf("nodes", List.of()).forGetter(g -> List.copyOf(g.nodes.values())),
            BlueprintEdge.CODEC.listOf().optionalFieldOf("edges", List.of()).forGetter(g -> List.copyOf(g.edges))
    ).apply(instance, BlueprintGraph::new));

    private final Map<UUID, BlueprintNode> nodes = new LinkedHashMap<>();
    private final List<BlueprintEdge> edges = new ArrayList<>();
    private String name = "";

    public BlueprintGraph() {
    }

    public BlueprintGraph(String name, List<BlueprintNode> nodes, List<BlueprintEdge> edges) {
        this.name = name;
        for (BlueprintNode node : nodes) {
            this.nodes.put(node.id(), node);
        }
        for (BlueprintEdge edge : edges) {
            BlueprintNode from = this.nodes.get(edge.fromNode());
            BlueprintNode to = this.nodes.get(edge.toNode());
            if (from != null && to != null && from.type().hasPin(edge.fromPin()) && to.type().hasPin(edge.toPin())) {
                this.edges.add(edge);
            }
        }
    }

    public BlueprintGraph copy() {
        List<BlueprintNode> copiedNodes = new ArrayList<>();
        for (BlueprintNode node : nodes.values()) {
            copiedNodes.add(new BlueprintNode(node.id(), node.x(), node.y(), node.settings()));
        }
        return new BlueprintGraph(name, copiedNodes, List.copyOf(edges));
    }

    public void restoreFrom(BlueprintGraph other) {
        name = other.name;
        nodes.clear();
        for (BlueprintNode node : other.nodes.values()) {
            nodes.put(node.id(), new BlueprintNode(node.id(), node.x(), node.y(), node.settings()));
        }
        edges.clear();
        edges.addAll(other.edges);
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Collection<BlueprintNode> nodes() {
        return nodes.values();
    }

    public List<BlueprintEdge> edges() {
        return edges;
    }

    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    @Nullable
    public BlueprintNode node(UUID id) {
        return nodes.get(id);
    }

    public void addNode(BlueprintNode node) {
        nodes.put(node.id(), node);
    }

    public void removeNodes(Collection<UUID> ids) {
        for (UUID id : ids) {
            nodes.remove(id);
        }
        edges.removeIf(edge -> ids.contains(edge.fromNode()) || ids.contains(edge.toNode()));
    }

    public Optional<BlueprintEdge> edgeFrom(UUID nodeId, PinRole pin) {
        return edges.stream().filter(e -> e.fromNode().equals(nodeId) && e.fromPin() == pin).findFirst();
    }

    public Optional<BlueprintEdge> edgeInto(UUID nodeId) {
        return edges.stream().filter(e -> e.toNode().equals(nodeId)).findFirst();
    }

    public boolean disconnect(UUID nodeId, PinRole pin) {
        return edges.removeIf(edge -> edge.touchesPin(nodeId, pin));
    }

    public boolean removeEdge(BlueprintEdge edge) {
        return edges.remove(edge);
    }

    @Nullable
    public String connect(UUID fromNodeId, PinRole fromPin, UUID toNodeId) {
        BlueprintNode from = nodes.get(fromNodeId);
        BlueprintNode to = nodes.get(toNodeId);
        if (from == null || to == null) {
            return "error.retrofactorymanager.node_missing";
        }
        if (fromNodeId.equals(toNodeId)) {
            return "error.retrofactorymanager.self_connect";
        }
        if (!fromPin.isOutput() || !from.type().hasPin(fromPin)) {
            return "error.retrofactorymanager.invalid_source";
        }
        if (!to.type().hasPin(PinRole.EXEC_IN)) {
            return "error.retrofactorymanager.no_input_pin";
        }
        if (reaches(toNodeId, fromNodeId)) {
            return "error.retrofactorymanager.cycle";
        }
        edges.removeIf(edge -> (edge.fromNode().equals(fromNodeId) && edge.fromPin() == fromPin)
                || edge.toNode().equals(toNodeId));
        edges.add(new BlueprintEdge(fromNodeId, fromPin, toNodeId, PinRole.EXEC_IN));
        return null;
    }

    private boolean reaches(UUID start, UUID target) {
        Set<UUID> visited = new HashSet<>();
        Deque<UUID> pending = new ArrayDeque<>();
        pending.push(start);
        while (!pending.isEmpty()) {
            UUID current = pending.pop();
            if (current.equals(target)) {
                return true;
            }
            if (!visited.add(current)) {
                continue;
            }
            for (BlueprintEdge edge : edges) {
                if (edge.fromNode().equals(current)) {
                    pending.push(edge.toNode());
                }
            }
        }
        return false;
    }

    public List<BlueprintNode> triggers() {
        return nodes.values().stream().filter(node -> node.type().isTrigger()).toList();
    }
}
