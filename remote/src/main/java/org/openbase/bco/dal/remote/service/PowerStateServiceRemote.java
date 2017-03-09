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
import org.openbase.bco.dal.lib.layer.service.collection.PowerStateOperationServiceCollection;
import org.openbase.bco.dal.lib.layer.service.operation.PowerStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class PowerStateServiceRemote extends AbstractServiceRemote<PowerStateOperationService, PowerState> implements PowerStateOperationServiceCollection {

    public PowerStateServiceRemote() {
        super(ServiceType.POWER_STATE_SERVICE, PowerState.class);
    }

    public Collection<PowerStateOperationService> getPowerStateOperationServices() {
        return getServices();
    }

    @Override
    public Future<Void> setPowerState(PowerState powerState) throws CouldNotPerformException {
        return GlobalCachedExecutorService.allOf((PowerStateOperationService input) -> input.setPowerState(powerState), super.getServices());
    }

    @Override
    public Future<Void> setPowerState(final PowerState powerState, final UnitType unitType) throws CouldNotPerformException {
        return GlobalCachedExecutorService.allOf((PowerStateOperationService input) -> input.setPowerState(powerState), super.getServices(unitType));
    }

    /**
     * {@inheritDoc}
     * Computes the power state as on if at least one underlying service is on and else off.
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected PowerState computeServiceState() throws CouldNotPerformException {
        return getPowerState(UnitType.UNKNOWN);
    }

    @Override
    public PowerState getPowerState() throws NotAvailableException {
        return getServiceState();
    }

    @Override
    public PowerState getPowerState(final UnitType unitType) throws NotAvailableException {
        PowerState.State powerStateValue = PowerState.State.OFF;
        long timestamp = 0;
        for (PowerStateOperationService service : getServices(unitType)) {
            if (!((UnitRemote) service).isDataAvailable()) {
                continue;
            }

            if (service.getPowerState().getValue() == PowerState.State.ON) {
                powerStateValue = PowerState.State.ON;
            }

            timestamp = Math.max(timestamp, service.getPowerState().getTimestamp().getTime());
        }

        return TimestampProcessor.updateTimestamp(timestamp, PowerState.newBuilder().setValue(powerStateValue), logger).build();
    }
}
