package com.breakinblocks.retrofactorymanager.client.gui;

import com.breakinblocks.retrofactorymanager.data.BlueprintNode;
import com.breakinblocks.retrofactorymanager.data.PinRole;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public record NodeLayout(
        double width,
        double height,
        double headerHeight,
        List<Component> summaryLines,
        List<PinSpot> pins
) {
    public static final double HEADER_HEIGHT = 14;
    public static final double LINE_HEIGHT = 11;
    public static final double PIN_ROW_HEIGHT = 13;
    public static final double MIN_WIDTH = 90;
    public static final double MAX_WIDTH = 240;

    public record PinSpot(PinRole role, double dx, double dy) {
    }

    public static NodeLayout of(BlueprintNode node, Font font) {
        List<Component> summary = NodeSummaries.lines(node.settings());
        double width = font.width(Component.translatable(node.type().translationKey())) + 16;
        for (Component line : summary) {
            width = Math.max(width, font.width(line) + 20);
        }
        List<PinRole> outputs = node.type().outputPins();
        for (PinRole output : outputs) {
            if (output.hasLabel()) {
                width = Math.max(width, font.width(Component.translatable(output.labelKey())) + 70);
            }
        }
        width = Math.clamp(width, MIN_WIDTH, MAX_WIDTH);

        int pinRows = Math.max(outputs.size(), node.type().hasPin(PinRole.EXEC_IN) ? 1 : 0);
        double bodyHeight = Math.max(summary.size() * LINE_HEIGHT, pinRows * PIN_ROW_HEIGHT) + 9;
        double height = HEADER_HEIGHT + bodyHeight;

        List<PinSpot> pins = new ArrayList<>();
        if (node.type().hasPin(PinRole.EXEC_IN)) {
            pins.add(new PinSpot(PinRole.EXEC_IN, 0, HEADER_HEIGHT + 10));
        }
        for (int i = 0; i < outputs.size(); i++) {
            pins.add(new PinSpot(outputs.get(i), width, HEADER_HEIGHT + 10 + i * PIN_ROW_HEIGHT));
        }
        return new NodeLayout(width, height, HEADER_HEIGHT, summary, pins);
    }

    public boolean contains(BlueprintNode node, double canvasX, double canvasY) {
        return canvasX >= node.x() && canvasX <= node.x() + width
                && canvasY >= node.y() && canvasY <= node.y() + height;
    }
}
