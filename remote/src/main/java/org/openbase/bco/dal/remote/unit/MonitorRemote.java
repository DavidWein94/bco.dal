package org.openbase.bco.dal.remote.unit;

/*
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
import java.util.concurrent.Future;
import org.openbase.jul.exception.CouldNotPerformException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.state.StandbyStateType.StandbyState;
import rst.domotic.unit.dal.MonitorDataType.MonitorData;
import org.openbase.bco.dal.lib.layer.unit.Monitor;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import rst.domotic.action.ActionFutureType.ActionFuture;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class MonitorRemote extends AbstractUnitRemote<MonitorData> implements Monitor {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(MonitorData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(StandbyState.getDefaultInstance()));
    }

    public MonitorRemote() {
        super(MonitorData.class);
    }

    @Override
    public Future<ActionFuture> setPowerState(final PowerState powerState) throws CouldNotPerformException {
        return applyAction(ActionDescriptionProcessor.generateDefaultActionParameter(powerState, ServiceType.POWER_STATE_SERVICE, this));
    }

    @Override
    public Future<ActionFuture> setStandbyState(StandbyState standbyState) throws CouldNotPerformException {
        return applyAction(ActionDescriptionProcessor.generateDefaultActionParameter(standbyState, ServiceType.STANDBY_STATE_SERVICE, this));
    }
}
