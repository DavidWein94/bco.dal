package org.openbase.bco.dal.visual.service;

/*
 * #%L
 * DAL Visualisation
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.processing.StringProcessor;
import java.awt.Color;
import org.openbase.bco.dal.lib.layer.service.consumer.ConsumerService;
import org.openbase.bco.dal.lib.layer.service.operation.ActivationStateOperationService;
import org.openbase.bco.dal.lib.layer.service.provider.ActivationStateProviderService;
import rst.domotic.state.ActivationStateType.ActivationState;
import static rst.domotic.state.ActivationStateType.ActivationState.State.UNKNOWN;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class ActivationStateServicePanel extends AbstractServicePanel<ActivationStateProviderService, ConsumerService, ActivationStateOperationService> {

    private static final ActivationState ACTIVE = ActivationState.newBuilder().setValue(ActivationState.State.ACTIVE).build();
    private static final ActivationState DEACTIVE = ActivationState.newBuilder().setValue(ActivationState.State.DEACTIVE).build();

    /**
     * Creates new form BrightnessService
     * @throws org.openbase.jul.exception.InstantiationException
     */
    public ActivationStateServicePanel() throws org.openbase.jul.exception.InstantiationException {
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

        powerButton = new javax.swing.JButton();
        powerStatePanel = new javax.swing.JPanel();
        powerStatusLabel = new javax.swing.JLabel();

        powerButton.setText("PowerButton");
        powerButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                powerButtonActionPerformed(evt);
            }
        });

        powerStatePanel.setBackground(new java.awt.Color(204, 204, 204));
        powerStatePanel.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 5, true));
        powerStatePanel.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N

        powerStatusLabel.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        powerStatusLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        powerStatusLabel.setText("PowerState");
        powerStatusLabel.setFocusable(false);
        powerStatusLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        javax.swing.GroupLayout powerStatePanelLayout = new javax.swing.GroupLayout(powerStatePanel);
        powerStatePanel.setLayout(powerStatePanelLayout);
        powerStatePanelLayout.setHorizontalGroup(
            powerStatePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(powerStatusLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 177, Short.MAX_VALUE)
        );
        powerStatePanelLayout.setVerticalGroup(
            powerStatePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(powerStatusLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 23, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(powerStatePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(powerButton, javax.swing.GroupLayout.DEFAULT_SIZE, 187, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(powerStatePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(powerButton))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void powerButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_powerButtonActionPerformed
        try {
            switch (getOperationService().getActivationState().getValue()) {
            case ACTIVE:
                notifyActionProcessing(getOperationService().setActivationState(ACTIVE));
                break;
            case DEACTIVE:
            case UNKNOWN:
                notifyActionProcessing(getOperationService().setActivationState(DEACTIVE));
                break;
            default:
                throw new InvalidStateException("State[" + getProviderService().getActivationState().getValue() + "] is unknown.");
            }
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not set activation state!", ex), logger);
        }
    }//GEN-LAST:event_powerButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton powerButton;
    private javax.swing.JPanel powerStatePanel;
    private javax.swing.JLabel powerStatusLabel;
    // End of variables declaration//GEN-END:variables

    @Override
    protected void updateDynamicComponents() {
        try {
            logger.info("state: " + getProviderService().getActivationState().getValue().name());
            switch (getProviderService().getActivationState().getValue()) {
            case ACTIVE:
                powerStatusLabel.setForeground(Color.BLACK);
                powerStatePanel.setBackground(Color.GREEN.darker());
                powerButton.setText("Deactivate");
                break;
            case DEACTIVE:
                powerStatusLabel.setForeground(Color.LIGHT_GRAY);
                powerStatePanel.setBackground(Color.GRAY.darker());
                powerButton.setText("Activate");
                break;
            case UNKNOWN:
                powerStatusLabel.setForeground(Color.BLACK);
                powerStatePanel.setBackground(Color.ORANGE.darker());
                powerButton.setText("Deactivate");
                break;
            default:
                throw new InvalidStateException("State[" + getProviderService().getActivationState().getValue() + "] is unknown.");
            }
            powerStatusLabel.setText("Current ActivationState = " + StringProcessor.transformUpperCaseToCamelCase(getProviderService().getActivationState().getValue().name()));
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(ex, logger);
        }
    }
}