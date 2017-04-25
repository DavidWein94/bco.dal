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
import java8.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.openbase.bco.dal.lib.layer.service.collection.TamperStateProviderServiceCollection;
import org.openbase.bco.dal.lib.layer.service.provider.TamperStateProviderService;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.Remote;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.TamperStateType.TamperState;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.timing.TimestampType.Timestamp;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class TamperStateServiceRemote extends AbstractServiceRemote<TamperStateProviderService, TamperState> implements TamperStateProviderServiceCollection {

    public TamperStateServiceRemote() {
        super(ServiceType.TAMPER_STATE_SERVICE, TamperState.class);
    }

    public Collection<TamperStateProviderService> getTamperStateProviderServices() {
        return getServices();
    }

    /**
     * {@inheritDoc}
     * Computes the tamper state as tamper if at least one underlying service detects tamper and else no tamper.
     * Additionally the last detection timestamp as set as the latest of the underlying services.
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected TamperState computeServiceState() throws CouldNotPerformException {
        return getTamperState(UnitType.UNKNOWN);
    }

    @Override
    public TamperState getTamperState() throws NotAvailableException {
        return getServiceState();
    }

    @Override
    public TamperState getTamperState(final UnitType unitType) throws NotAvailableException {
        TamperState.State tamperValue = TamperState.State.NO_TAMPER;
        long lastDetection = 0;
        long timestamp = 0;
        for (TamperStateProviderService service : getServices(unitType)) {
            if (!((UnitRemote) service).isDataAvailable()) {
                continue;
            }

            TamperState tamperState = service.getTamperState();
            if (tamperState.getValue() == TamperState.State.TAMPER) {
                tamperValue = TamperState.State.TAMPER;
            }

            if (tamperState.getLastDetection().getTime() > lastDetection) {
                lastDetection = tamperState.getLastDetection().getTime();
            }

            timestamp = Math.max(timestamp, tamperState.getTimestamp().getTime());
        }

        return TimestampProcessor.updateTimestamp(timestamp, TamperState.newBuilder().setValue(tamperValue).setLastDetection(Timestamp.newBuilder().setTime(lastDetection)), TimeUnit.MICROSECONDS, logger).build();
    }

    /////////////
    // START DEFAULT INTERFACE METHODS
    /////////////
    public void activate(boolean waitForData) throws CouldNotPerformException, InterruptedException {
        activate();
        waitForData();
    }

    public CompletableFuture<TamperState> requestData() throws CouldNotPerformException {
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
