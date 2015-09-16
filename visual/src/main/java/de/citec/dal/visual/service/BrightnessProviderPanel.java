/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.visual.service;

import de.citec.dal.hal.provider.BrightnessProvider;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.exception.printer.LogLevel;
import java.awt.Color;
import java.text.DecimalFormat;

/**
 *
 * @author mpohling
 */
public class BrightnessProviderPanel extends AbstractServicePanel<BrightnessProvider> {

    private final DecimalFormat numberFormat = new DecimalFormat("#.##");

    private final static float MAX_BRIGHTNESS = 32000f;
    private final static float MIN_BRIGHTNESS = 0f;

    /**
     * Creates new form BrightnessService
     *
     * @throws de.citec.jul.exception.InstantiationException
     */
    public BrightnessProviderPanel() throws de.citec.jul.exception.InstantiationException {
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        brightnessLevelLabelPanel = new javax.swing.JPanel();
        brightnessLevelLabel = new javax.swing.JLabel();

        brightnessLevelLabelPanel.setBackground(new java.awt.Color(204, 204, 204));
        brightnessLevelLabelPanel.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 5, true));
        brightnessLevelLabelPanel.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N

        brightnessLevelLabel.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        brightnessLevelLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        brightnessLevelLabel.setText("PowerState");
        brightnessLevelLabel.setFocusable(false);
        brightnessLevelLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        javax.swing.GroupLayout brightnessLevelLabelPanelLayout = new javax.swing.GroupLayout(brightnessLevelLabelPanel);
        brightnessLevelLabelPanel.setLayout(brightnessLevelLabelPanelLayout);
        brightnessLevelLabelPanelLayout.setHorizontalGroup(
            brightnessLevelLabelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(brightnessLevelLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 277, Short.MAX_VALUE)
        );
        brightnessLevelLabelPanelLayout.setVerticalGroup(
            brightnessLevelLabelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(brightnessLevelLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 23, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(brightnessLevelLabelPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(brightnessLevelLabelPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel brightnessLevelLabel;
    private javax.swing.JPanel brightnessLevelLabelPanel;
    // End of variables declaration//GEN-END:variables

    @Override
    protected void updateDynamicComponents() {
        try {
            Double brightness = getService().getBrightness();

            if (brightness < 0) {
                brightnessLevelLabel.setForeground(Color.WHITE);
                brightnessLevelLabelPanel.setBackground(Color.RED.darker());
                brightnessLevelLabel.setText("UNKOWN");
                return;
            }

            brightnessLevelLabel.setForeground(Color.BLACK);
            float colorValue = ((float) Math.max(Math.min(MAX_BRIGHTNESS, brightness), MIN_BRIGHTNESS)) / MAX_BRIGHTNESS;
            brightnessLevelLabelPanel.setBackground(Color.getHSBColor((float) 40 / 360, 1f - colorValue, colorValue));
            brightnessLevelLabel.setText(numberFormat.format(getService().getBrightness()) + " LUX");
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(ex, logger, LogLevel.ERROR);
        }
    }
}
