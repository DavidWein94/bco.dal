/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.hal.AbstractHALController;
import rsb.Event;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rsb.patterns.EventCallback;
import rst.homeautomation.ReedSwitchType;
import rst.homeautomation.ReedSwitchType.ReedSwitch;
import rst.homeautomation.states.OpenClosedType;
import rst.homeautomation.states.OpenClosedType.OpenClosed.OpenClosedState;

/**
 *
 * @author thuxohl
 */
public class ReedSwitchController extends AbstractHALController<ReedSwitch, ReedSwitch.Builder> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(ReedSwitchType.ReedSwitch.getDefaultInstance()));
    }

    public ReedSwitchController(String id, HardwareUnit hardwareUnit, ReedSwitch.Builder builder) throws RSBBindingException {
        super(id, hardwareUnit, builder);
    }

    public void updateOpenClosedState(final OpenClosedType.OpenClosed.OpenClosedState state) {
        builder.getStateBuilder().setState(state);
        notifyChange();
    }

    public OpenClosedState getReedSwitchState() {
        logger.debug("Getting [" + id + "] State: [" + builder.getState() + "]");
        return builder.getState().getState();
    }

    public class GetReedSwitchState extends EventCallback {

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                return new Event(OpenClosedState.class, ReedSwitchController.this.getReedSwitchState());
            } catch (Exception ex) {
                logger.warn("Could not invoke method for [" + ReedSwitchController.this.getId() + "}", ex);
                return new Event(String.class, "Failed");
            }
        }
    }

}