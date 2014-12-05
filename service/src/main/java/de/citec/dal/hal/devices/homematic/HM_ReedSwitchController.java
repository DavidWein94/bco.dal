/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.devices.homematic;

import de.citec.dal.data.Location;
import de.citec.dal.data.transform.OpenClosedStateTransformer;
import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.hal.AbstractDeviceController;
import de.citec.dal.hal.al.BatteryController;
import de.citec.dal.hal.al.ReedSwitchController;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.devices.homematic.HM_ReedSwitchType;
import rst.devices.homematic.HM_ReedSwitchType.HM_ReedSwitch;
import rst.homeautomation.openhab.OpenClosedHolderType.OpenClosedHolder.OpenClosed;

/**
 *
 * @author mpohling
 */
public class HM_ReedSwitchController extends AbstractDeviceController<HM_ReedSwitch, HM_ReedSwitch.Builder> {

    private final static String COMPONENT_REED_SWITCH = "ReedSwitch";
    private final static String COMPONENT_BATTERY = "Battery";

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(HM_ReedSwitchType.HM_ReedSwitch.getDefaultInstance()));
    }

    private final ReedSwitchController reedSwitch;
    private final BatteryController battery;

    public HM_ReedSwitchController(final String id, final String label, final Location location) throws RSBBindingException {
        super(id, label, location, HM_ReedSwitch.newBuilder());

        builder.setId(id);
        this.reedSwitch = new ReedSwitchController(COMPONENT_REED_SWITCH, label, this, builder.getReedSwitchBuilder());
        this.battery = new BatteryController(COMPONENT_BATTERY, label, this, builder.getBatteryBuilder());
        this.register(reedSwitch);
        this.register(battery);
    }

    @Override
    protected void initHardwareMapping() throws NoSuchMethodException, SecurityException {
        halFunctionMapping.put(COMPONENT_REED_SWITCH, getClass().getMethod("updateReedSwitch", OpenClosed.class));
        halFunctionMapping.put(COMPONENT_BATTERY, getClass().getMethod("updateBatteryLevel", double.class));
    }

    public void updateReedSwitch(OpenClosed type) {
        try {
            reedSwitch.updateOpenClosedState(OpenClosedStateTransformer.transform(type));
        } catch (RSBBindingException ex) {
            logger.error("Not able to transform from OpenClosedType(openhab) to OpenClosedState!", ex);
        }
    }

    public void updateBatteryLevel(double value) {
        battery.updateBatteryLevel((float) value);
    }
}
