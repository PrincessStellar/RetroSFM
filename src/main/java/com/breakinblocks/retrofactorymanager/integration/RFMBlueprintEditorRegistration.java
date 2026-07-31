package com.breakinblocks.retrofactorymanager.integration;

import ca.teamdman.sfm.client.screen.text_editor.ISFMTextEditScreen;
import ca.teamdman.sfm.client.text_editor.ISFMTextEditScreenOpenContext;
import ca.teamdman.sfm.client.text_editor.ISFMTextEditorRegistration;
import com.breakinblocks.retrofactorymanager.client.gui.RFMBlueprintEditorScreen;

public class RFMBlueprintEditorRegistration implements ISFMTextEditorRegistration {
    @Override
    public ISFMTextEditScreen createScreen(ISFMTextEditScreenOpenContext context) {
        return new RFMBlueprintEditorScreen(context);
    }
}
