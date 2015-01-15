/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.dal.service.rsb.RSBRemoteService;
import de.citec.dal.util.DALException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.RollershutterType;
import rst.homeautomation.states.ShutterType;

/**
 *
 * @author thuxohl
 */
public class RollershutterRemote extends RSBRemoteService<RollershutterType.Rollershutter> {

	static {
		DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(RollershutterType.Rollershutter.getDefaultInstance()));
		DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ShutterType.Shutter.getDefaultInstance()));
	}

	public RollershutterRemote() {
	}

	public void setShutterState(final ShutterType.Shutter.ShutterState state) throws DALException {
		callMethodAsync("setShutterState", state);
	}

	public void setPosition(final float position) throws DALException {
		callMethodAsync("setPosition", position);
	}

	@Override
	public void notifyUpdated(RollershutterType.Rollershutter data) {
	}

}