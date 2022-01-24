package com.nccgroup.loggerplusplus.exports;

import com.coreyd97.BurpExtenderUtilities.Preferences;
import com.nccgroup.loggerplusplus.logentry.LogEntry;

import javax.swing.*;
import java.util.List;

public class S3Exporter extends AutomaticLogExporter implements ExportPanelProvider, ContextMenuExportProvider{

    private final S3ExporterControlPanel controlPanel;

    protected S3Exporter(ExportController exportController, Preferences preferences) {
        super(exportController, preferences);

        controlPanel = new S3ExporterControlPanel(this);
    }

    @Override
    void setup() throws Exception {

    }

    @Override
    void exportNewEntry(LogEntry logEntry) {

    }

    @Override
    void exportUpdatedEntry(LogEntry logEntry) {

    }

    @Override
    void shutdown() throws Exception {

    }

    @Override
    public JMenuItem getExportEntriesMenuItem(List<LogEntry> entries) {
        return null;
    }

    @Override
    public JComponent getExportPanel() {
        return controlPanel;
    }

    public ExportController getExportController() {
        return this.exportController;
    }
}
