package com.nccgroup.loggerplusplus.exports;

import com.coreyd97.BurpExtenderUtilities.Alignment;
import com.coreyd97.BurpExtenderUtilities.PanelBuilder;
import com.nccgroup.loggerplusplus.LoggerPlusPlus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.util.concurrent.ExecutionException;

public class S3ExporterControlPanel extends JPanel {

    private final S3Exporter s3Exporter;

    private static final String STARTING_TEXT = "Starting S3 Exporter...";
    private static final String STOPPING_TEXT = "Stopping S3 Exporter...";
    private static final String START_TEXT = "Start S3 Exporter";
    private static final String STOP_TEXT = "Stop S3 Exporter";

    Logger logger = LogManager.getLogger(this);

    public S3ExporterControlPanel(S3Exporter s3Exporter) {
        this.s3Exporter = s3Exporter;

        this.setLayout(new BorderLayout());

        JButton showConfigDialogButton = new JButton(new AbstractAction("Configure S3 Exporter") {
            @Override
            public void actionPerformed(ActionEvent e) {
                new S3ExporterConfigDialog(LoggerPlusPlus.instance.getLoggerFrame(), s3Exporter).setVisible(true);
            }
        });

        JToggleButton exportButton = new JToggleButton(START_TEXT);
        exportButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean buttonNowActive = exportButton.isSelected();
                exportButton.setEnabled(false);
                exportButton.setText(buttonNowActive ? STARTING_TEXT : STOPPING_TEXT);
                new SwingWorker<Boolean, Void>(){
                    Exception exception;

                    @Override
                    protected Boolean doInBackground() throws Exception {
                        boolean success = false;
                        try {
                            if (exportButton.isSelected()) {
                                enableExporter();
                            } else {
                                disableExporter();
                            }
                            success = true;
                        }catch (Exception e){
                            this.exception = e;
                        }
                        return success;
                    }

                    @Override
                    protected void done() {
                        try {
                            if(exception != null) {
                                JOptionPane.showMessageDialog(exportButton, "Could not start S3 exporter: " +
                                        exception.getMessage() + "\nSee the logs for more information.", "S3 Exporter", JOptionPane.ERROR_MESSAGE);
                                logger.error("Could not start S3 exporter.", exception);
                            }
                            Boolean success = get();
                            boolean isRunning = buttonNowActive ^ !success;
                            exportButton.setSelected(isRunning);
                            showConfigDialogButton.setEnabled(!isRunning);

                            exportButton.setText(isRunning ? STOP_TEXT : START_TEXT);

                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                        exportButton.setEnabled(true);
                    }
                }.execute();
            }

        });

        if (isExporterEnabled()){
            exportButton.setSelected(true);
            exportButton.setText(STOP_TEXT);
            showConfigDialogButton.setEnabled(false);
        }

        this.add(PanelBuilder.build(new JComponent[][]{
                new JComponent[]{showConfigDialogButton},
                new JComponent[]{exportButton}
        }, new int[][]{
                new int[]{1},
                new int[]{1}
        }, Alignment.FILL, 1.0, 1.0), BorderLayout.CENTER);

        this.setBorder(BorderFactory.createTitledBorder("S3 Exporter"));
    }

    private void enableExporter() throws Exception {
        this.s3Exporter.getExportController().enableExporter(this.s3Exporter);
    }

    private void disableExporter() throws Exception {
        this.s3Exporter.getExportController().disableExporter(this.s3Exporter);
    }

    private boolean isExporterEnabled() {
        return this.s3Exporter.getExportController().getEnabledExporters().contains(this.s3Exporter);
    }

}
