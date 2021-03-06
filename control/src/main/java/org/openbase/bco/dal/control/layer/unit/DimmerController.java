package org.openbase.bco.dal.control.layer.unit;

import org.openbase.bco.dal.lib.layer.unit.Dimmer;
import org.openbase.bco.dal.lib.layer.unit.HostUnitController;
import org.openbase.jul.exception.InstantiationException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.BrightnessStateType.BrightnessState;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.unit.dal.DimmerDataType.DimmerData;

/*
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class DimmerController extends AbstractDALUnitController<DimmerData, DimmerData.Builder> implements Dimmer {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DimmerData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(BrightnessState.getDefaultInstance()));
    }

    public DimmerController(final HostUnitController hostUnitController, DimmerData.Builder builder) throws InstantiationException {
        super(hostUnitController, builder);
    }

    @Override
    protected void applyCustomDataUpdate(DimmerData.Builder internalBuilder, ServiceType serviceType) {
        switch (serviceType) {
            case BRIGHTNESS_STATE_SERVICE:
                if (internalBuilder.getBrightnessState().getBrightness() == 0) {
                    internalBuilder.getPowerStateBuilder().setValue(PowerState.State.OFF);
                } else {
                    internalBuilder.getPowerStateBuilder().setValue(PowerState.State.ON);
                }
                break;
        }
    }
}
