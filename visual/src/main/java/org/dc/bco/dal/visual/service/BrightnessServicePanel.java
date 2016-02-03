/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.visual.service;

/*
 * #%L
 * DAL Visualisation
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
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

import org.dc.bco.dal.lib.layer.service.BrightnessService;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import java.util.concurrent.Callable;

/**
 *
 * @author mpohling
 */
public class BrightnessServicePanel extends AbstractServicePanel<BrightnessService> {

    /**
     * Creates new form BrightnessService
     */
    public BrightnessServicePanel() throws org.dc.jul.exception.InstantiationException {
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

        brightnessSlider = new javax.swing.JSlider();
        brightnessBar = new javax.swing.JProgressBar();

        brightnessSlider.setPaintLabels(true);
        brightnessSlider.setPaintTicks(true);
        brightnessSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                brightnessSliderStateChanged(evt);
            }
        });
        brightnessSlider.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                brightnessSliderPropertyChange(evt);
            }
        });

        brightnessBar.setStringPainted(true);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(brightnessSlider, javax.swing.GroupLayout.PREFERRED_SIZE, 242, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(brightnessBar, javax.swing.GroupLayout.PREFERRED_SIZE, 242, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(brightnessBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(brightnessSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void brightnessSliderPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_brightnessSliderPropertyChange
        System.out.println("brightnessSliderPropertyChange");
    }//GEN-LAST:event_brightnessSliderPropertyChange

    private void brightnessSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_brightnessSliderStateChanged
        execute(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                try {
                    getService().setBrightness((double) brightnessSlider.getValue());
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(new CouldNotPerformException("Could not set brightness value!", ex), logger);
                }
                return null;
            }
        });
    }//GEN-LAST:event_brightnessSliderStateChanged


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JProgressBar brightnessBar;
    private javax.swing.JSlider brightnessSlider;
    // End of variables declaration//GEN-END:variables

    @Override
    protected void updateDynamicComponents() {
        try {
            brightnessBar.setValue(getService().getBrightness().intValue());
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(ex, logger);
        }
    }
}
