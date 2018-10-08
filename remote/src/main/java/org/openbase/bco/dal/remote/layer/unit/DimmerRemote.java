package org.openbase.bco.dal.remote.layer.unit;

import org.openbase.bco.dal.lib.layer.unit.Dimmer;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.BrightnessStateType.BrightnessState;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.unit.dal.DimmerDataType.DimmerData;

import java.util.concurrent.Future;

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

/**
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class DimmerRemote extends AbstractUnitRemote<DimmerData> implements Dimmer {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DimmerData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(BrightnessState.getDefaultInstance()));
    }

    public DimmerRemote() {
        super(DimmerData.class);
    }

    @Override
    public Future<ActionDescription> setPowerState(PowerState powerState) throws CouldNotPerformException {
        return applyAction(ActionDescriptionProcessor.generateDefaultActionParameter(powerState, ServiceType.POWER_STATE_SERVICE, this));
    }

    @Override
    public Future<ActionDescription> setBrightnessState(BrightnessState brightnessState) throws CouldNotPerformException {
        return applyAction(ActionDescriptionProcessor.generateDefaultActionParameter(brightnessState, ServiceType.BRIGHTNESS_STATE_SERVICE, this));
    }
}