/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.remote.unit;

import org.dc.bco.dal.lib.layer.unit.TamperSwitchInterface;
import org.dc.jul.exception.CouldNotPerformException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.TamperStateType.TamperState;
import rst.homeautomation.unit.TamperSwitchType.TamperSwitch;

/**
 *
 * @author thuxohl
 */
public class TamperSwitchRemote extends DALRemoteService<TamperSwitch> implements TamperSwitchInterface {
    
    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TamperSwitch.getDefaultInstance()));
    }

    public TamperSwitchRemote() {
    }

    @Override
    public void notifyUpdated(TamperSwitch data) {
    }

    @Override
    public TamperState getTamper() throws CouldNotPerformException {
        return getData().getTamperState();
    }
    
}