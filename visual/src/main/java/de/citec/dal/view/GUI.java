/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.view;

import de.citec.dal.Controller;
import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import rsb.Scope;
import rst.homeautomation.states.PowerType;
import rst.vision.HSVColorType.HSVColor;

/**
 *
 * @author nuc
 */
public class GUI extends javax.swing.JFrame implements PropertyChangeListener {

    /**
     * Creates new form GUI
     */
    private final Controller controller;

    public GUI(Controller controller) {
        initComponents();

        if (controller != null) {
            this.controller = controller;
        } else {
            this.controller = new Controller();
        }

        this.setLocationRelativeTo(null);

        colorChooser.getSelectionModel().addChangeListener(
                new ChangeListener() {
                    public void stateChanged(ChangeEvent e) {
                        setColor();
                    }
                }
        );
        controller.activateListener(controller.createStatusScope(scopeTextField2.getText()), controller.getPowerPlugListener());
        controller.activateListener(controller.createStatusScope(scopeTextField3.getText()), controller.getButtonListener());
        ScopeActionListener scopeActionListener = new ScopeActionListener(controller, scopeTextField2, scopeTextField3);
        scopeTextField2.addActionListener(scopeActionListener);
        scopeTextField3.addActionListener(scopeActionListener);
    }

    public void setColor() {
        float[] hsb = new float[3];
        Color.RGBtoHSB(colorChooser.getColor().getRed(), colorChooser.getColor().getGreen(), colorChooser.getColor().getBlue(), hsb);
        controller.callMethod("setColor", HSVColor.newBuilder().setHue(hsb[0] * 360).setSaturation(hsb[1] * 100).setValue(hsb[2] * 100).build(), controller.createCTRLScope(scopeTextField.getText()), true);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        ambientLightPanel = new javax.swing.JPanel();
        scopeLabel = new javax.swing.JLabel();
        scopeTextField = new javax.swing.JTextField();
        colorChooser = new javax.swing.JColorChooser();
        jLabel1 = new javax.swing.JLabel();
        powerSwitchPanel = new javax.swing.JPanel();
        scopeLabel2 = new javax.swing.JLabel();
        scopeTextField2 = new javax.swing.JTextField();
        powerState = new javax.swing.JLabel();
        powerSwitch = new javax.swing.JButton();
        buttonPanel = new javax.swing.JPanel();
        buttonPressedLabel = new javax.swing.JLabel();
        buttonPressedPanel = new javax.swing.JPanel();
        buttonRevieverLabel = new javax.swing.JLabel();
        scopeLabel3 = new javax.swing.JLabel();
        scopeTextField3 = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        ambientLightPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Ambient Light"));

        scopeLabel.setText("Scope:");

        scopeTextField.setText("/home/kitchen/ambientlight/000");
        scopeTextField.setToolTipText("");

        javax.swing.GroupLayout ambientLightPanelLayout = new javax.swing.GroupLayout(ambientLightPanel);
        ambientLightPanel.setLayout(ambientLightPanelLayout);
        ambientLightPanelLayout.setHorizontalGroup(
            ambientLightPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ambientLightPanelLayout.createSequentialGroup()
                .addComponent(scopeLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scopeTextField))
            .addComponent(colorChooser, javax.swing.GroupLayout.DEFAULT_SIZE, 664, Short.MAX_VALUE)
            .addGroup(ambientLightPanelLayout.createSequentialGroup()
                .addGap(112, 112, 112)
                .addComponent(jLabel1)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        ambientLightPanelLayout.setVerticalGroup(
            ambientLightPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ambientLightPanelLayout.createSequentialGroup()
                .addGroup(ambientLightPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(scopeTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(scopeLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(colorChooser, javax.swing.GroupLayout.PREFERRED_SIZE, 285, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addGap(91, 91, 91))
        );

        powerSwitchPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("PowerSwitch"));

        scopeLabel2.setText("Scope:");

        scopeTextField2.setText("/home/controlroom/powerplug000");

        powerState.setText("PowerState:");

        powerSwitch.setBackground(new java.awt.Color(255, 204, 0));
        powerSwitch.setText("Unknown");
        powerSwitch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                powerSwitchActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout powerSwitchPanelLayout = new javax.swing.GroupLayout(powerSwitchPanel);
        powerSwitchPanel.setLayout(powerSwitchPanelLayout);
        powerSwitchPanelLayout.setHorizontalGroup(
            powerSwitchPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(powerSwitchPanelLayout.createSequentialGroup()
                .addComponent(scopeLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scopeTextField2))
            .addGroup(powerSwitchPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(powerState)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(powerSwitch, javax.swing.GroupLayout.PREFERRED_SIZE, 175, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        powerSwitchPanelLayout.setVerticalGroup(
            powerSwitchPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(powerSwitchPanelLayout.createSequentialGroup()
                .addGroup(powerSwitchPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(scopeLabel2)
                    .addComponent(scopeTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(powerSwitchPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(powerState)
                    .addComponent(powerSwitch))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        buttonPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("ButtonPressed"));

        buttonPressedLabel.setText("Button:");

        buttonPressedPanel.setBackground(new java.awt.Color(255, 204, 0));
        buttonPressedPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        buttonRevieverLabel.setText("Unknown");

        javax.swing.GroupLayout buttonPressedPanelLayout = new javax.swing.GroupLayout(buttonPressedPanel);
        buttonPressedPanel.setLayout(buttonPressedPanelLayout);
        buttonPressedPanelLayout.setHorizontalGroup(
            buttonPressedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(buttonPressedPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(buttonRevieverLabel)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        buttonPressedPanelLayout.setVerticalGroup(
            buttonPressedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(buttonRevieverLabel, javax.swing.GroupLayout.Alignment.TRAILING)
        );

        scopeLabel3.setText("Scope:");

        scopeTextField3.setText("/home/sportsroom/button000");

        javax.swing.GroupLayout buttonPanelLayout = new javax.swing.GroupLayout(buttonPanel);
        buttonPanel.setLayout(buttonPanelLayout);
        buttonPanelLayout.setHorizontalGroup(
            buttonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(buttonPanelLayout.createSequentialGroup()
                .addGap(2, 2, 2)
                .addComponent(scopeLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scopeTextField3))
            .addGroup(buttonPanelLayout.createSequentialGroup()
                .addComponent(buttonPressedLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonPressedPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        buttonPanelLayout.setVerticalGroup(
            buttonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(buttonPanelLayout.createSequentialGroup()
                .addGroup(buttonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(scopeLabel3)
                    .addComponent(scopeTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(buttonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(buttonPressedLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(buttonPressedPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(24, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(powerSwitchPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(ambientLightPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(buttonPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(ambientLightPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 335, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(powerSwitchPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void powerSwitchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_powerSwitchActionPerformed
        Scope controlScope = controller.createCTRLScope(scopeTextField2.getText());
        switch (controller.getLastPowerState()) {
            case ON:
                controller.callMethod("setPowerState", PowerType.Power.newBuilder().setState(PowerType.Power.PowerState.OFF).build(), controlScope, true);
                powerSwitch.setBackground(Color.blue.darker().darker());
                break;
            case OFF:
                controller.callMethod("setPowerState", PowerType.Power.newBuilder().setState(PowerType.Power.PowerState.ON).build(), controlScope, true);
                powerSwitch.setBackground(Color.green.darker().darker());
                break;
            default:
                System.out.println("Power State unknown! Calling Power State ON");
                controller.callMethod("setPowerState", PowerType.Power.newBuilder().setState(PowerType.Power.PowerState.ON).build(), controlScope, true);
                powerSwitch.setBackground(Color.green.darker().darker());
                break;
        }
    }//GEN-LAST:event_powerSwitchActionPerformed


    public static void initGui(Controller controller) {
        new GUI(controller).setVisible(true);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel ambientLightPanel;
    private javax.swing.JPanel buttonPanel;
    private javax.swing.JLabel buttonPressedLabel;
    private javax.swing.JPanel buttonPressedPanel;
    private javax.swing.JLabel buttonRevieverLabel;
    private javax.swing.JColorChooser colorChooser;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel powerState;
    private javax.swing.JButton powerSwitch;
    private javax.swing.JPanel powerSwitchPanel;
    private javax.swing.JLabel scopeLabel;
    private javax.swing.JLabel scopeLabel2;
    private javax.swing.JLabel scopeLabel3;
    private javax.swing.JTextField scopeTextField;
    private javax.swing.JTextField scopeTextField2;
    private javax.swing.JTextField scopeTextField3;
    // End of variables declaration//GEN-END:variables

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        switch (evt.getPropertyName()) {
            case "ON":
                powerSwitch.setText("On");
                powerSwitch.setBackground(Color.green);
                break;
            case "OFF":
                powerSwitch.setText("Off");
                powerSwitch.setBackground(Color.blue);
                break;
            case "PP_UNKNOWN":
                powerSwitch.setText("Unknown");
                powerSwitch.setBackground(Color.orange);
                break;
            case "CLICK":
                buttonRevieverLabel.setText("Clicked");
                buttonPressedPanel.setBackground(Color.green);
                break;
            case "DCLICK":
                buttonRevieverLabel.setText("Double Clicked");
                buttonPressedPanel.setBackground(Color.green.darker().darker().darker());
                break;
            case "RELEASE":
                buttonRevieverLabel.setText("Released");
                buttonPressedPanel.setBackground(Color.blue);
                break;
            case "B_UNKNOWN":
                buttonRevieverLabel.setText("Unkown");
                buttonPressedPanel.setBackground(Color.orange);
                break;
            default:
                System.out.println("Revieced unknown property event ["+evt.getPropertyName()+"]");
                break;
        }
    }
}