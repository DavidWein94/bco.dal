/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.visual.service;

import de.citec.dal.hal.provider.ReedSwitchProvider;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InvalidStateException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.exception.printer.LogLevel;
import de.citec.jul.processing.StringProcessor;
import java.awt.Color;

/**
 *
 * @author kengelma
 */
public class ReedSwitchProviderPanel extends AbstractServicePanel<ReedSwitchProvider> {

    /**
     * Creates new form ReedSwitchProviderPanel
     * @throws de.citec.jul.exception.InstantiationException can't instantiate
     */
    public ReedSwitchProviderPanel() throws de.citec.jul.exception.InstantiationException  {
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        reedSwitchStatePanel = new javax.swing.JPanel();
        reedSwitchStateLabel = new javax.swing.JLabel();

        reedSwitchStatePanel.setBackground(new java.awt.Color(204, 204, 204));
        reedSwitchStatePanel.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 5, true));
        reedSwitchStatePanel.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N

        reedSwitchStateLabel.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        reedSwitchStateLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        reedSwitchStateLabel.setText("ReedSwitchState");
        reedSwitchStateLabel.setFocusable(false);
        reedSwitchStateLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        javax.swing.GroupLayout reedSwitchStatePanelLayout = new javax.swing.GroupLayout(reedSwitchStatePanel);
        reedSwitchStatePanel.setLayout(reedSwitchStatePanelLayout);
        reedSwitchStatePanelLayout.setHorizontalGroup(
            reedSwitchStatePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(reedSwitchStateLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
        );
        reedSwitchStatePanelLayout.setVerticalGroup(
            reedSwitchStatePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(reedSwitchStateLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        reedSwitchStateLabel.getAccessibleContext().setAccessibleName("ReedSwitchState");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(reedSwitchStatePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(reedSwitchStatePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel reedSwitchStateLabel;
    private javax.swing.JPanel reedSwitchStatePanel;
    // End of variables declaration//GEN-END:variables

    @Override
    protected void updateDynamicComponents() {        
        try {
            switch (getService().getReedSwitch().getValue()) {
                case UNKNOWN:
                    reedSwitchStateLabel.setForeground(Color.DARK_GRAY);
                    reedSwitchStatePanel.setBackground(Color.ORANGE.darker());
                    break;
                case CLOSED:
                    reedSwitchStateLabel.setForeground(Color.WHITE);
                    reedSwitchStatePanel.setBackground(Color.BLUE);
                    break;
                case OPEN:
                    reedSwitchStateLabel.setForeground(Color.WHITE);
                    reedSwitchStatePanel.setBackground(Color.GREEN.darker());
                    break;
                default:
                    throw new InvalidStateException("State[" + getService().getReedSwitch().getValue() + "] is unknown.");
            }
            reedSwitchStateLabel.setText("Current ReedState = " + StringProcessor.transformUpperCaseToCamelCase(getService().getReedSwitch().getValue().name()));
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(ex, logger, LogLevel.ERROR);
        }
    }
}
