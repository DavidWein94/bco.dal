package org.openbase.bco.dal.lib.layer.unit;

/*
 * #%L
 * DAL Library
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.BatteryStateType.BatteryState;
import rst.homeautomation.unit.BatteryType.Battery;

/**
 *
 * @author thuxohl
 */
public class BatteryController extends AbstractUnitController<Battery, Battery.Builder> implements BatteryInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(Battery.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(BatteryState.getDefaultInstance()));
    }

    public BatteryController(final UnitHost unitHost, Battery.Builder builder) throws InstantiationException, CouldNotPerformException {
        super(BatteryController.class, unitHost, builder);
    }

    public void updateBatteryProvider(final BatteryState value) throws CouldNotPerformException {
        logger.debug("Apply battery Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<Battery.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setBatteryState(value);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply battery Update[" + value + "] for " + this + "!", ex);
        }
    }

    @Override
    public BatteryState getBattery() throws NotAvailableException {
        try {
            return getData().getBatteryState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("battery", ex);
        }
    }
}