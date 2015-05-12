/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.device;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.NotAvailableException;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;

/**
 *
 * @author mpohling
 */
public class DeviceFactory implements DeviceFactoryInterface {

    public DeviceFactory() {
    }

    @Override
    public Device newDevice(final DeviceConfig deviceConfig) throws CouldNotPerformException {
        try {
            if (deviceConfig == null) {
                throw new NotAvailableException("deviceClass");
            }
            if (!deviceConfig.hasDeviceClass()) {
                throw new NotAvailableException("deviceClass");
            }
            if (!deviceConfig.getDeviceClass().hasCompany()) {
                throw new NotAvailableException("deviceClass.company");
            }
            if (!deviceConfig.hasId()) {
                throw new NotAvailableException("deviceConfig.id");
            }
            if (!deviceConfig.hasLabel()) {
                throw new NotAvailableException("deviceConfig.label");
            }
            if (!deviceConfig.hasPlacementConfig()) {
                throw new NotAvailableException("deviceConfig.placement");
            }
            if (!deviceConfig.getPlacementConfig().hasLocationConfig()) {
                throw new NotAvailableException("deviceConfig.placement.location");
            }
            Class deviceClass = getClass().getClassLoader().loadClass(AbstractDeviceController.class.getPackage().getName() + "." + deviceConfig.getDeviceClass().getCompany() + "." + deviceConfig.getDeviceClass().getLabel() + "Controller");
            return (Device) deviceClass.getConstructor(DeviceConfig.class).newInstance(deviceConfig);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not instantiate Device[" + deviceConfig + "]!", ex);
        }
    }
}