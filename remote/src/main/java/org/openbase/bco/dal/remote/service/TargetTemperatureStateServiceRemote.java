package org.openbase.bco.dal.remote.service;

/*
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import java.util.Collection;
import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.service.collection.TargetTemperatureStateOperationServiceCollection;
import org.openbase.bco.dal.lib.layer.service.operation.TargetTemperatureStateOperationService;
import org.openbase.bco.dal.remote.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.TemperatureStateType.TemperatureState;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.timing.TimestampType.Timestamp;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class TargetTemperatureStateServiceRemote extends AbstractServiceRemote<TargetTemperatureStateOperationService, TemperatureState> implements TargetTemperatureStateOperationServiceCollection {

    public TargetTemperatureStateServiceRemote() {
        super(ServiceType.TARGET_TEMPERATURE_STATE_SERVICE);
    }

    public Collection<TargetTemperatureStateOperationService> getTargetTemperatureStateOperationServices() {
        return getServices();
    }

    @Override
    public Future<Void> setTargetTemperatureState(final TemperatureState temperatureState) throws CouldNotPerformException {
        return GlobalCachedExecutorService.allOf((TargetTemperatureStateOperationService input) -> input.setTargetTemperatureState(temperatureState), getServices());
    }

    @Override
    public Future<Void> setTargetTemperatureState(final TemperatureState temperatureState, final UnitType unitType) throws CouldNotPerformException {
        return GlobalCachedExecutorService.allOf((TargetTemperatureStateOperationService input) -> input.setTargetTemperatureState(temperatureState), getServices(unitType));
    }

    /**
     * {@inheritDoc}
     * Computes the average temperature value.
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected TemperatureState computeServiceState() throws CouldNotPerformException {
        return getTargetTemperatureState(UnitType.UNKNOWN);
    }

    @Override
    public TemperatureState getTargetTemperatureState() throws NotAvailableException {
        return getServiceState();
    }

    @Override
    public TemperatureState getTargetTemperatureState(final UnitType unitType) throws NotAvailableException {
        Double average = 0d;
        Collection<TargetTemperatureStateOperationService> targetTemperatureStateOperationServices = getServices(unitType);
        int amount = targetTemperatureStateOperationServices.size();
        for (TargetTemperatureStateOperationService service : targetTemperatureStateOperationServices) {
            if (!((UnitRemote) service).isDataAvailable()) {
                amount--;
                continue;
            }

            average += service.getTargetTemperatureState().getTemperature();
        }
        average /= amount;

        return TemperatureState.newBuilder().setTemperature(average).setTimestamp(Timestamp.newBuilder().setTime(System.currentTimeMillis())).build();
    }
}
