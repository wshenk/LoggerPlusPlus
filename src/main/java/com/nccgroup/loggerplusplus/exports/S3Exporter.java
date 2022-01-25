package com.nccgroup.loggerplusplus.exports;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.coreyd97.BurpExtenderUtilities.Preferences;
import com.nccgroup.loggerplusplus.logentry.LogEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.amazonaws.auth.BasicAWSCredentials;

import static com.nccgroup.loggerplusplus.util.Globals.*;

public class S3Exporter extends AutomaticLogExporter implements ExportPanelProvider, ContextMenuExportProvider{

    private final S3ExporterControlPanel controlPanel;
    private Logger logger = LogManager.getLogger(this);

    private final ScheduledExecutorService executorService;

    private BasicAWSCredentials awsCredentials;
    private AmazonS3 s3Client;

    protected S3Exporter(ExportController exportController, Preferences preferences) {
        super(exportController, preferences);
        executorService = Executors.newScheduledThreadPool(1);

        controlPanel = new S3ExporterControlPanel(this);
    }

    @Override
    void setup() throws Exception {
        awsCredentials = new BasicAWSCredentials(preferences.getSetting(PREF_S3_AWS_ACCESS_KEY_ID), preferences.getSetting(PREF_S3_AWS_ACCESS_KEY_SECRET));
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .withRegion((String) preferences.getSetting(PREF_S3_REGION))
                .build();
        s3Client.putObject(preferences.getSetting(PREF_S3_BUCKET_NAME), "testkey.txt", "testcontent");
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
