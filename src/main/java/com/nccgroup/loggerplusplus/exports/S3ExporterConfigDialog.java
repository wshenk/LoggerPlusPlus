package com.nccgroup.loggerplusplus.exports;

import com.coreyd97.BurpExtenderUtilities.Alignment;
import com.coreyd97.BurpExtenderUtilities.ComponentGroup;
import com.coreyd97.BurpExtenderUtilities.PanelBuilder;
import com.coreyd97.BurpExtenderUtilities.Preferences;
import com.nccgroup.loggerplusplus.LoggerPlusPlus;
import com.nccgroup.loggerplusplus.filter.logfilter.LogFilter;
import com.nccgroup.loggerplusplus.filter.parser.ParseException;
import com.nccgroup.loggerplusplus.filterlibrary.FilterLibraryController;
import com.nccgroup.loggerplusplus.logentry.LogEntryField;
import com.nccgroup.loggerplusplus.util.Globals;
import com.nccgroup.loggerplusplus.util.MoreHelp;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Objects;

import static com.nccgroup.loggerplusplus.util.Globals.*;


public class S3ExporterConfigDialog extends JDialog {

    S3ExporterConfigDialog(Frame owner, S3Exporter s3Exporter) {
        super(owner, "S3 Exporter Configuration", true);

        this.setLayout(new BorderLayout());
        Preferences preferences = s3Exporter.getPreferences();


        JTextField s3AccessKeyIdField = PanelBuilder.createPreferenceTextField(preferences, PREF_S3_AWS_ACCESS_KEY_ID);
        JTextField s3AccessKeySecretField = PanelBuilder.createPreferenceTextField(preferences, PREF_S3_AWS_ACCESS_KEY_SECRET);

        JTextField s3RegionField = PanelBuilder.createPreferenceTextField(preferences, PREF_S3_REGION);
        JTextField s3BucketNameField = PanelBuilder.createPreferenceTextField(preferences, PREF_S3_BUCKET_NAME);


        ComponentGroup credentialGroup = new ComponentGroup(ComponentGroup.Orientation.VERTICAL, "AWS Credentials");
        credentialGroup.addComponentWithLabel("AWS ACCESS KEY ID: ", s3AccessKeyIdField);
        credentialGroup.addComponentWithLabel("AWS ACCESS KEY SECRET: ", s3AccessKeySecretField);

        ComponentGroup s3InformationGroup = new ComponentGroup(ComponentGroup.Orientation.VERTICAL, "S3 Information");
        s3InformationGroup.addComponentWithLabel("S3 Bucket Region: ", s3RegionField);
        s3InformationGroup.addComponentWithLabel("S3 Bucket Name: ", s3BucketNameField);



        PanelBuilder panelBuilder = new PanelBuilder();
        panelBuilder.setComponentGrid(new JComponent[][]{
                new JComponent[]{credentialGroup},
                new JComponent[]{s3InformationGroup}
        });
        int[][] weights = new int[][]{
                new int[]{1},
                new int[]{1},
                new int[]{1},
        };
        panelBuilder.setGridWeightsY(weights)
                .setGridWeightsX(weights)
                .setAlignment(Alignment.CENTER)
                .setInsetsX(5)
                .setInsetsY(5);

        this.add(panelBuilder.build(), BorderLayout.CENTER);

        //setAuthFields.run();

        this.setMinimumSize(new Dimension(600, 200));

        this.pack();
        this.setResizable(true);
        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                S3ExporterConfigDialog.this.dispose();
                super.windowClosing(e);
            }
        });


    }
}
