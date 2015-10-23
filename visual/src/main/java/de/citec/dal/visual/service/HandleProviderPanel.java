/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.visual.service;

import de.citec.dal.hal.provider.HandleProvider;
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
public class HandleProviderPanel extends AbstractServicePanel<HandleProvider> {

    /**
     * Creates new form ReedSwitchProviderPanel
     *
     * @throws de.citec.jul.exception.InstantiationException can't instantiate
     */
    public HandleProviderPanel() throws de.citec.jul.exception.InstantiationException {
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

        handleStatePanel = new javax.swing.JPanel();
        handleStateLabel = new javax.swing.JLabel();

        handleStatePanel.setBackground(new java.awt.Color(204, 204, 204));
        handleStatePanel.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 5, true));
        handleStatePanel.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N

        handleStateLabel.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        handleStateLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        handleStateLabel.setText("HandleState");
        handleStateLabel.setFocusable(false);
        handleStateLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        javax.swing.GroupLayout handleStatePanelLayout = new javax.swing.GroupLayout(handleStatePanel);
        handleStatePanel.setLayout(handleStatePanelLayout);
        handleStatePanelLayout.setHorizontalGroup(
            handleStatePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(handleStateLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
        );
        handleStatePanelLayout.setVerticalGroup(
            handleStatePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(handleStateLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        handleStateLabel.getAccessibleContext().setAccessibleName("ReedSwitchState");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(handleStatePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(handleStatePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel handleStateLabel;
    private javax.swing.JPanel handleStatePanel;
    // End of variables declaration//GEN-END:variables

    @Override
    protected void updateDynamicComponents() {
        try {
            switch (getService().getHandle().getValue()) {
                case UNKNOWN:
                    handleStateLabel.setForeground(Color.DARK_GRAY);
                    handleStatePanel.setBackground(Color.ORANGE);
                    break;
                case CLOSED:
                    handleStateLabel.setForeground(Color.WHITE);
                    handleStatePanel.setBackground(Color.BLUE);
                    break;
                case TILTED:
                    handleStateLabel.setForeground(Color.BLACK);
                    handleStatePanel.setBackground(Color.CYAN);
                    break;
                case OPEN:
                    handleStateLabel.setForeground(Color.WHITE);
                    handleStatePanel.setBackground(Color.GREEN);
                    break;
                default:
                    throw new InvalidStateException("State[" + getService().getHandle().getValue() + "] is unknown.");
            }
            handleStateLabel.setText("Current HandleState = " + StringProcessor.transformUpperCaseToCamelCase(getService().getHandle().getValue().name()));
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(ex, logger, LogLevel.ERROR);
        }
    }
}