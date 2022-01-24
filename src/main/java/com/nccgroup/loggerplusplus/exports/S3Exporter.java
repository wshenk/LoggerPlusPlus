package com.nccgroup.loggerplusplus.exports;

import com.coreyd97.BurpExtenderUtilities.Preferences;
import com.nccgroup.loggerplusplus.logentry.LogEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class S3Exporter extends AutomaticLogExporter implements ExportPanelProvider, ContextMenuExportProvider{

    private final S3ExporterControlPanel controlPanel;
    private Logger logger = LogManager.getLogger(this);

    private final ScheduledExecutorService executorService;

    protected S3Exporter(ExportController exportController, Preferences preferences) {
        super(exportController, preferences);
        executorService = Executors.newScheduledThreadPool(1);


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
