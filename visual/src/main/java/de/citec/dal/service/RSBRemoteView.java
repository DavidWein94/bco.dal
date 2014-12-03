/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.service;

import com.google.protobuf.GeneratedMessage;
import de.citec.dal.util.NotAvailableException;
import de.citec.dal.util.Observable;
import de.citec.dal.util.Observer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mpohling
 * @param <M>
 * @param <R>
 */
public abstract class RSBRemoteView<M extends GeneratedMessage, R extends RSBRemoteService<M>> extends javax.swing.JPanel implements Observer<M> {

    protected final Logger logger;
    
    private R remoteService;
    /**
     * Creates new form RSBViewService
     */
    public RSBRemoteView() {
        this.logger = LoggerFactory.getLogger(getClass());
        this.initComponents();
    }
    
    public synchronized void setRemoteService(final R remoteService) {
        
        if(this.remoteService != null) {
            this.remoteService.shutdown();
        }
        
        this.remoteService = remoteService;
        remoteService.addObserver(this);
    }
    
    @Override
    public void update(Observable<M> source, M data) {
        updateDynamicComponents(data);
    }

    public R getRemoteService() throws NotAvailableException {
        if(remoteService == null) {
            throw new NotAvailableException("remoteService");
        }
        return remoteService;
    }
    
    public M getData() throws NotAvailableException {
        return getRemoteService().getData();
    }
    
    protected abstract void updateDynamicComponents(M data);

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
