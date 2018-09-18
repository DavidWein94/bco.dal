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

import org.openbase.bco.dal.lib.layer.unit.PowerSwitch;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.unit.dal.PowerSwitchDataType.PowerSwitchData;

import java.util.concurrent.Future;

/**
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class PowerSwitchRemote extends AbstractUnitRemote<PowerSwitchData> implements PowerSwitch {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerSwitchData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerState.getDefaultInstance()));
    }

    public PowerSwitchRemote() {
        super(PowerSwitchData.class);
    }

    @Override
    public Future<ActionFuture> setPowerState(final PowerState powerState) throws CouldNotPerformException {
        return applyAction(ActionDescriptionProcessor.generateDefaultActionParameter(powerState, ServiceType.POWER_STATE_SERVICE, this));
    }
}
