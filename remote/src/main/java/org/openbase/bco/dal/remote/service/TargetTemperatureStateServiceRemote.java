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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.service.collection.TargetTemperatureStateOperationServiceCollection;
import org.openbase.bco.dal.lib.layer.service.operation.TargetTemperatureStateOperationService;
import org.openbase.bco.dal.remote.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.Remote;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.TemperatureStateType.TemperatureState;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class TargetTemperatureStateServiceRemote extends AbstractServiceRemote<TargetTemperatureStateOperationService, TemperatureState> implements TargetTemperatureStateOperationServiceCollection {

    public TargetTemperatureStateServiceRemote() {
        super(ServiceType.TARGET_TEMPERATURE_STATE_SERVICE, TemperatureState.class);
    }

    public Collection<TargetTemperatureStateOperationService> getTargetTemperatureStateOperationServices() {
        return getServices();
    }

    @Override
    public Future<Void> setTargetTemperatureState(final TemperatureState temperatureState) throws CouldNotPerformException {
        return GlobalCachedExecutorService.allOf(getServices(), (TargetTemperatureStateOperationService input) -> input.setTargetTemperatureState(temperatureState));
    }

    @Override
    public Future<Void> setTargetTemperatureState(final TemperatureState temperatureState, final UnitType unitType) throws CouldNotPerformException {
        return GlobalCachedExecutorService.allOf(getServices(unitType), (TargetTemperatureStateOperationService input) -> input.setTargetTemperatureState(temperatureState));
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
        long timestamp = 0;
        for (TargetTemperatureStateOperationService service : targetTemperatureStateOperationServices) {
            if (!((UnitRemote) service).isDataAvailable()) {
                amount--;
                continue;
            }

            average += service.getTargetTemperatureState().getTemperature();
            timestamp = Math.max(timestamp, service.getTargetTemperatureState().getTimestamp().getTime());
        }
        average /= amount;

        return TimestampProcessor.updateTimestamp(timestamp, TemperatureState.newBuilder().setTemperature(average), logger).build();
    }

    /////////////
    // START DEFAULT INTERFACE METHODS
    /////////////
    public void activate(boolean waitForData) throws CouldNotPerformException, InterruptedException {
        activate();
        waitForData();
    }

    public CompletableFuture<TemperatureState> requestData() throws CouldNotPerformException {
        return requestData(true);
    }

    public void addConnectionStateObserver(Observer<ConnectionState> observer) {
        for (Remote remote : getInternalUnits()) {
            remote.addConnectionStateObserver(observer);
        }
    }

    public ConnectionState getConnectionState() {
        boolean disconnectedRemoteDetected = false;
        boolean connectedRemoteDetected = false;

        for (final Remote remote : getInternalUnits()) {
            switch (remote.getConnectionState()) {
                case CONNECTED:
                    connectedRemoteDetected = true;
                    break;
                case CONNECTING:
                case DISCONNECTED:
                    disconnectedRemoteDetected = true;
                    break;
                default:
                    //ignore unknown connection state";
            }
        }

        if (disconnectedRemoteDetected && connectedRemoteDetected) {
            return ConnectionState.CONNECTING;
        } else if (disconnectedRemoteDetected) {
            return ConnectionState.DISCONNECTED;
        } else if (connectedRemoteDetected) {
            return ConnectionState.CONNECTED;
        } else {
            return ConnectionState.UNKNOWN;
        }
    }

    public void removeConnectionStateObserver(Observer<ConnectionState> observer) {
        for (final Remote remote : getInternalUnits()) {
            remote.removeConnectionStateObserver(observer);
        }
    }
    /////////////
    // END DEFAULT INTERFACE METHODS
    /////////////
}
