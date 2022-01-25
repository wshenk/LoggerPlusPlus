package com.nccgroup.loggerplusplus.exports;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.coreyd97.BurpExtenderUtilities.Preferences;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nccgroup.loggerplusplus.LoggerPlusPlus;
import com.nccgroup.loggerplusplus.filter.logfilter.LogFilter;
import com.nccgroup.loggerplusplus.filter.parser.ParseException;
import com.nccgroup.loggerplusplus.logentry.LogEntry;
import com.nccgroup.loggerplusplus.logentry.LogEntryField;
import com.nccgroup.loggerplusplus.logentry.Status;
import com.nccgroup.loggerplusplus.util.Globals;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.amazonaws.auth.BasicAWSCredentials;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;

import static com.nccgroup.loggerplusplus.util.Globals.*;

public class S3Exporter extends AutomaticLogExporter implements ExportPanelProvider, ContextMenuExportProvider{

    private final S3ExporterControlPanel controlPanel;
    private Logger logger = LogManager.getLogger(this);
    LogFilter logFilter;
    ArrayList<LogEntry> pendingEntries;
    private final ScheduledExecutorService executorService;
    private ScheduledFuture indexTask;

    private List<LogEntryField> fields;

    private int connectFailedCounter;

    private BasicAWSCredentials awsCredentials;
    private AmazonS3 s3Client;

    protected S3Exporter(ExportController exportController, Preferences preferences) {
        super(exportController, preferences);
        executorService = Executors.newScheduledThreadPool(1);
        this.fields = new ArrayList<>(preferences.getSetting(Globals.PREF_PREVIOUS_S3_FIELDS));

        controlPanel = new S3ExporterControlPanel(this);
    }

    @Override
    void setup() throws Exception {
        awsCredentials = new BasicAWSCredentials(preferences.getSetting(PREF_S3_AWS_ACCESS_KEY_ID), preferences.getSetting(PREF_S3_AWS_ACCESS_KEY_SECRET));
        s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .withRegion((String) preferences.getSetting(PREF_S3_REGION))
                .build();
        String testkey = preferences.getSetting(PREF_S3_PREFIX) + "testburpkey.txt";
        s3Client.putObject(preferences.getSetting(PREF_S3_BUCKET_NAME), testkey, "testcontent");

        String filterString = preferences.getSetting(Globals.PREF_S3_FILTER);
        String projectPreviousFilterString = preferences.getSetting(Globals.PREF_S3_FILTER_PROJECT_PREVIOUS);

        if (!Objects.equals(projectPreviousFilterString, filterString)) {
            //The current filter isn't what we used to export last time.
            int res = JOptionPane.showConfirmDialog(LoggerPlusPlus.instance.getLoggerFrame(),
                    "Heads up! Looks like the filter being used to select which logs to export to " +
                            "S3 has changed since you last ran the exporter for this project.\n" +
                            "Do you want to continue?", "S3 Export Log Filter", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (res == JOptionPane.NO_OPTION) {
                throw new Exception("Export cancelled.");
            }
        }

        if (!StringUtils.isBlank(filterString)) {
            try {
                logFilter = new LogFilter(exportController.getLoggerPlusPlus().getLibraryController(), filterString);
            } catch (ParseException ex) {
                logger.error("The log filter configured for the S3 exporter is invalid!", ex);
            }
        }

        pendingEntries = new ArrayList<>();
        connectFailedCounter = 0;
        int delay = preferences.getSetting(Globals.PREF_S3_DELAY);
        indexTask = executorService.scheduleAtFixedRate(this::indexPendingEntries, delay, delay, TimeUnit.SECONDS);
    }

    @Override
    void exportNewEntry(LogEntry logEntry) {
        if(logEntry.getStatus() == Status.PROCESSED) {
            if (logFilter != null && !logFilter.matches(logEntry)) return;
            pendingEntries.add(logEntry);
        }
    }

    @Override
    void exportUpdatedEntry(LogEntry updatedEntry) {
        if(updatedEntry.getStatus() == Status.PROCESSED) {
            if (logFilter != null && !logFilter.matches(updatedEntry)) return;
            pendingEntries.add(updatedEntry);
        }
    }

    @Override
    void shutdown() throws Exception {
        if(this.indexTask != null){
            indexTask.cancel(true);
        }
        this.pendingEntries = null;
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

    public List<LogEntryField> getFields() {
        return fields;
    }

    public void setFields(List<LogEntryField> fields) {
        preferences.setSetting(Globals.PREF_PREVIOUS_S3_FIELDS, fields);
        this.fields = fields;
    }

    private Object formatValue(Object value){
        if (value instanceof java.net.URL) return String.valueOf((java.net.URL) value);
        else return value;
    }


    private void indexPendingEntries(){
        try {
            if (this.pendingEntries.size() == 0) return;

            ArrayList<LogEntry> entriesInBulk;
            synchronized (pendingEntries) {
                entriesInBulk = new ArrayList<>(pendingEntries);
                pendingEntries.clear();
            }
            try {
                Gson gson = exportController.getLoggerPlusPlus().getGsonProvider().getGson();
                //gson.toJson(entries, fileWriter);
                //JsonElement pendingEntriesElement = gson.toJsonTree(entriesInBulk);
                JsonArray jsonArray = new JsonArray();
                JsonObject completeJsonObject = new JsonObject();
                JsonObject partialJsonObject = new JsonObject();
                for (LogEntry logEntry : entriesInBulk) {
                    for (LogEntryField field : this.fields) {
                        Object value = formatValue(logEntry.getValueByKey(field));
                        String firstHalfOfLabel = field.getFullLabel().split("\\.")[0];
                        String secondHalfOfLabel = field.getFullLabel().split("\\.")[1];
                        if (!completeJsonObject.has(firstHalfOfLabel)){
                            completeJsonObject.add(firstHalfOfLabel, new JsonObject());
                        }
                        partialJsonObject = completeJsonObject.get(firstHalfOfLabel).getAsJsonObject();
                        partialJsonObject.addProperty(secondHalfOfLabel, value.toString());
                        completeJsonObject.add(firstHalfOfLabel, partialJsonObject);
                    }
                    jsonArray.add(completeJsonObject);
                }
                String jsonResp = jsonArray.toString();
                s3Client.putObject(preferences.getSetting(PREF_S3_BUCKET_NAME), preferences.getSetting(PREF_S3_PREFIX) + String.valueOf(System.currentTimeMillis()) + ".json", jsonResp);
                connectFailedCounter = 0;
            } catch (Exception e) {
                LoggerPlusPlus.callbacks.printError("Could not upload data to S3: " + e.toString() + " "+ e.getMessage());
                connectFailedCounter++;
                if(connectFailedCounter > 5) {
                    JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(LoggerPlusPlus.instance.getLoggerMenu()),
                            "S3 exporter could not connect after 5 attempts. S3 exporter shutting down..." + e.getMessage(),
                            "S3 Exporter - Upload Failed", JOptionPane.ERROR_MESSAGE);
                    shutdown();
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
