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


import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.service.operation.TargetTemperatureStateOperationService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.TemperatureStateType.TemperatureState;
import rst.homeautomation.unit.TemperatureControllerDataType.TemperatureControllerData;
import rst.homeautomation.unit.UnitConfigType;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
public class TemperatureControllerController extends AbstractUnitController<TemperatureControllerData, TemperatureControllerData.Builder> implements TemperatureController {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TemperatureControllerData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TemperatureState.getDefaultInstance()));
    }

    private TargetTemperatureStateOperationService targetTemperatureStateService;

    public TemperatureControllerController(final UnitHost unitHost, final TemperatureControllerData.Builder builder) throws InstantiationException, CouldNotPerformException {
        super(TemperatureControllerController.class, unitHost, builder);
    }

    @Override
    public void init(UnitConfigType.UnitConfig config) throws InitializationException, InterruptedException {
        super.init(config);
        try {
            this.targetTemperatureStateService = getServiceFactory().newTargetTemperatureService(this);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public Future<Void> setTargetTemperatureState(final TemperatureState value) throws CouldNotPerformException {
        logger.debug("Set " + getType().name() + "[" + getLabel() + "] to targetTemperatureState [" + value + "]");
        return targetTemperatureStateService.setTargetTemperatureState(value);
    }

    @Override
    public TemperatureState getTargetTemperatureState() throws NotAvailableException {
        try {
            return getData().getTargetTemperatureState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("targetTemperatureState", ex);
        }
    }

    public void updateTargetTemperatureStateProvider(final TemperatureState temperatureState) throws CouldNotPerformException {
        logger.info("Apply targetTemperatureState Update[" + temperatureState + "] for " + this + ".");

        try (ClosableDataBuilder<TemperatureControllerData.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setTargetTemperatureState(temperatureState);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply target temperature Update[" + temperatureState + "] for " + this + "!", ex);
        }
        
        logger.info("Target temperature update applied");
    }

    public void updateTemperatureStateProvider(final TemperatureState temperatureState) throws CouldNotPerformException {
        logger.debug("Apply actual temperatureState Update[" + temperatureState + "] for " + this + ".");

        try (ClosableDataBuilder<TemperatureControllerData.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setActualTemperatureState(temperatureState);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply actual temperatureStatee Update[" + temperatureState + "] for " + this + "!", ex);
        }
    }

    @Override
    public TemperatureState getTemperatureState() throws NotAvailableException {
        try {
            return getData().getActualTemperatureState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("actual temperatureState", ex);
        }
    }
}
